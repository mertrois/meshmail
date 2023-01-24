package app.meshmail.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.meshmail.MainActivity
import app.meshmail.MeshmailApplication
import app.meshmail.R
import app.meshmail.android.PrefsManager
import app.meshmail.data.MessageAdapter
import app.meshmail.data.MessageEntity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ClientMessageListFragment : Fragment() {
    private lateinit var app: MeshmailApplication
    private lateinit var prefs: PrefsManager
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messagesLayoutManager: RecyclerView.LayoutManager
    private lateinit var tabLayout: TabLayout
    private val clientMessagesListViewModel by viewModels<ClientMessagesListViewModel> {
        ClientMessagesListViewModelFactory(app)
    }
    private lateinit var composeFAB: FloatingActionButton

    companion object {
        val folders: ArrayList<String> = arrayListOf("ARCHIVE", "INBOX", "TRASH")
        val FOLDER_ARCHIVE = 0
        val FOLDER_INBOX = 1
        val FOLDER_TRASH = 2
    }

    private lateinit var trashMenuItem: MenuItem

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.empty_trash -> {
                emptyTrash()
                return true
            }
        }
        return false
    }

    /*
        Listener to handle tabs; letting viewModel respond to tab changes
     */
    private val tabSelectedListener = object: TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            if(tab != null) {
                clientMessagesListViewModel.setCurrentFolder(folders[tab.position])
                // save to prefs because savedinstacestate doesn't work with stack
                prefs.putInt("selectedTabPosition", tab.position)
                Log.d("ClientMessageListFragment","tab position is now ${tab.position}")
            }
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {
            return Unit
        }

        override fun onTabReselected(tab: TabLayout.Tab?) {
            // tap the tab to scroll to the top
            messagesRecyclerView.scrollToPosition(0)
        }
    }

    /*
    Handles all swipe functionality for moving messages from inbox to archive or trash
     */
    private val itemTouchHelperCallback = object: ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {

            // allowed flags depending on which folder is open
            val swipeFlags = when(tabLayout.selectedTabPosition) {
                0 -> ItemTouchHelper.RIGHT
                1 -> ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                2 -> ItemTouchHelper.LEFT
                else -> 0
            }
            return makeMovementFlags(0, swipeFlags)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val newFolder: String = when(tabLayout.selectedTabPosition) {
                0 -> {
                    folders[1]
                } 1 -> {
                    if(direction == ItemTouchHelper.LEFT) folders[0]
                    else folders[2]
                } 2 -> {
                    folders[1]
                } else -> {
                    throw Exception("Tab does not exist")
                }
            }
            val position = viewHolder.absoluteAdapterPosition
            val message: MessageEntity? = clientMessagesListViewModel.messageAtPosition(position)
            message?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    it.folder = newFolder
                    clientMessagesListViewModel.database.messageDao().update(it)
                }
                //messageAdapter.notifyItemRemoved(position)
            }
        }
    }

    public interface FragmentRequestListener {
        companion object {
            val MODE_VIEW: Int = 0
            val MODE_EDIT: Int = 1
        }
        fun loadMessageFragment(message: MessageEntity, mode: Int)
    }

    var requestListener: FragmentRequestListener? = null

    private fun emptyTrash() {
        CoroutineScope(Dispatchers.IO).launch {
            for(m in app.database.messageDao().getMessagesByFolder("TRASH")) {
                app.database.messageFragmentDao().deleteByFingerprint(m.fingerprint)
                app.database.messageDao().delete(m)
            }
        }
    }

//    fun messageAtPosition(position: Int): MessageEntity {
//        val list: List<MessageEntity> = clientMessagesListViewModel.messagesList.value!!
//        return list[position]
//    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        app = requireActivity().application as MeshmailApplication
        prefs = app.prefs
        if(context is FragmentRequestListener) {
            requestListener = context
        } else {
            throw RuntimeException("$context must implement FragmentRequestListener")
        }
    }

    private fun loadMessage(message: MessageEntity, mode: Int) {
        requestListener?.loadMessageFragment(message, mode)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_messages, menu)
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_messages, container, false)
        tabLayout = view.findViewById(R.id.folders_tab_layout)
        tabLayout.addOnTabSelectedListener(tabSelectedListener)

        // Initialize the RecyclerView and adapter
        messagesRecyclerView = view.findViewById(R.id.messages_recycler_view)

        // set the tab names according to our folders defined above
        for(i in 0 until tabLayout.tabCount) {
            tabLayout.getTabAt(i)?.text = folders[i]
        }


        val startingFolder = prefs.getInt("selectedTabPosition", 1) ?: 1
        // select starting tab
        tabLayout.selectTab(tabLayout.getTabAt(startingFolder))
        // set current folder to correspond
        clientMessagesListViewModel.setCurrentFolder(folders[startingFolder])

        // set the handler for a message being tapped (open new fragment)
        messageAdapter = MessageAdapter(MessageAdapter.OnClickListener { message ->
            val position = clientMessagesListViewModel.getPosition(message)
            if(position >= 0) {
                val viewHolder =
                    messagesRecyclerView.findViewHolderForAdapterPosition(position) as MessageAdapter.ViewHolder
                viewHolder.setTypeface(bold = false)
            }
            CoroutineScope(Dispatchers.IO).launch {
                clientMessagesListViewModel.markMessageRead(message)
            }
            loadMessage(message, FragmentRequestListener.MODE_VIEW)
        })
        messagesLayoutManager = LinearLayoutManager(context)

        // Set the adapter and layout manager for the RecyclerView
        messagesRecyclerView.adapter = messageAdapter
        messagesRecyclerView.layoutManager = messagesLayoutManager

        // push updates in the DB to the adaptor
        clientMessagesListViewModel.messagesList.observe(viewLifecycleOwner, Observer {
            messages -> messageAdapter.submitList(messages)
        })

        // setup the listener for swipe events
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(messagesRecyclerView)

        // setup the compose fab
        composeFAB = view.findViewById(R.id.fab_compose)
        composeFAB.setOnClickListener {
            val message: MessageEntity = MessageEntity(folder="DRAFTS")
            loadMessage(message, FragmentRequestListener.MODE_EDIT)
        }

        return view
    }



}
