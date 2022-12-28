package app.meshmail.ui

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
    private val folders: ArrayList<String> = arrayListOf("INBOX","ARCHIVE","TRASH")

    private val tabSelectedListener = object: TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            if(tab != null) {
                clientMessagesListViewModel.setCurrentFolder(folders[tab.position])
            }
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {
            return Unit
        }

        override fun onTabReselected(tab: TabLayout.Tab?) {
            return Unit
        }
    }

    private val itemTouchHelperCallback = object: ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            // these need to change based on the tab being viewed--if inbox then RIGHT if Archive then R and L, if trash, then L only
            val swipeFlags = ItemTouchHelper.RIGHT // or ItemTouchHelper.LEFT
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
            message?.folder = "ARCHIVE"
            clientMessagesListViewModel.database.messageDao().update(message)
        }

    }


    fun messageAtPosition(position: Int): MessageEntity {
        val list: List<MessageEntity> = clientMessagesListViewModel.messagesList.value!!
        return list[position]
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        app = requireActivity().application
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.messages_fragment, container, false)
        tabLayout = view.findViewById(R.id.folders_tab_layout)

        tabLayout.addOnTabSelectedListener(tabSelectedListener)

        val inboxTab = tabLayout.getTabAt(0)
        tabLayout.selectTab(inboxTab)

        // Initialize the RecyclerView and adapter
        messagesRecyclerView = view.findViewById(R.id.messages_recycler_view)
        messageAdapter = MessageAdapter(MessageAdapter.OnClickListener { message ->
            Toast.makeText(app, message.subject, Toast.LENGTH_SHORT).show()
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
