package app.meshmail.ui

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.meshmail.R
import app.meshmail.data.MessageAdapter
import app.meshmail.data.MessageEntity
import com.google.android.material.tabs.TabLayout

class ClientMessageListFragment : Fragment() {
    private lateinit var app: Application
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messagesLayoutManager: RecyclerView.LayoutManager
    private lateinit var tabLayout: TabLayout
    private val clientMessagesListViewModel by viewModels<ClientMessagesListViewModel> {
        ClientMessagesListViewModelFactory(app)
    }
    private val folders: ArrayList<String> = arrayListOf("ARCHIVE","INBOX","TRASH")
    private lateinit var trashMenuItem: MenuItem

    /*
        Listener to handle tabs; letting viewModel respond to tab changes
     */
    private val tabSelectedListener = object: TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            if(tab != null) {
                clientMessagesListViewModel.setCurrentFolder(folders[tab.position])
            }
            // now reconfigure the swipe left/right actions based on current tab
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

            val swipeFlags = when(tabLayout.selectedTabPosition) {
                0 -> ItemTouchHelper.RIGHT                              // inbox
                1 -> ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT      // archive
                2 -> ItemTouchHelper.LEFT                               // trash
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
            val position = viewHolder.absoluteAdapterPosition
            val message: MessageEntity = messageAtPosition(position)
            messageAdapter.notifyItemRemoved(position)
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
            message?.folder = newFolder
            clientMessagesListViewModel.database.messageDao().update(message)
        }
    }

    public interface FragmentRequestListener {
        fun loadMessageFragment(message: MessageEntity)
    }
    var requestListener: FragmentRequestListener? = null

    fun emptyTrash() {
        Toast.makeText(app, "emptying trash", Toast.LENGTH_SHORT).show()
    }

    fun messageAtPosition(position: Int): MessageEntity {
        val list: List<MessageEntity> = clientMessagesListViewModel.messagesList.value!!
        return list[position]
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        app = requireActivity().application
        if(context is FragmentRequestListener) {
            requestListener = context
        } else {
            throw RuntimeException("$context must implement FragmentRequestListener")
        }
    }

    fun loadMessage(message: MessageEntity) {
        requestListener?.loadMessageFragment(message)
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
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.messages_fragment, container, false)
        tabLayout = view.findViewById(R.id.folders_tab_layout)

        tabLayout.addOnTabSelectedListener(tabSelectedListener)

        // set the tab names according to our folders defined above
        for(i in 0 until tabLayout.tabCount) {
            tabLayout.getTabAt(i)?.text = folders[i]
        }

        val startingFolder = 1  // todo: get this from saved fragment state
        // select starting tab
        tabLayout.selectTab(tabLayout.getTabAt(startingFolder))
        // set current folder to correspond
        clientMessagesListViewModel.setCurrentFolder(folders[startingFolder])

        // Initialize the RecyclerView and adapter
        messagesRecyclerView = view.findViewById(R.id.messages_recycler_view)

        // set the handler for a message being tapped (open new fragment)
        messageAdapter = MessageAdapter(MessageAdapter.OnClickListener { message ->
            val position = clientMessagesListViewModel.getPosition(message)
            if(position >= 0) {
                val viewHolder =
                    messagesRecyclerView.findViewHolderForAdapterPosition(position) as MessageAdapter.ViewHolder
                viewHolder.setTypeface(bold = false)
            }
            clientMessagesListViewModel.markMessageRead(message)
            loadMessage(message)
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

        return view
    }


}
