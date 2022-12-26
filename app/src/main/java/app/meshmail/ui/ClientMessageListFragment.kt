package app.meshmail.ui

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.meshmail.R
import app.meshmail.data.MessageAdapter
import app.meshmail.data.MessageEntity

class ClientMessageListFragment : Fragment() {
    private lateinit var app: Application
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messagesLayoutManager: RecyclerView.LayoutManager


    val ithCallback = object: ItemTouchHelper.Callback() {
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

            // val item = items[position]
            // items.removeAt(position)
            messageAdapter.notifyItemRemoved(position)
            // Add the item to the archive or perform other actions as needed

            // need to check for nulls here
            val list: List<MessageEntity> = clientMessagesListViewModel.messagesList.value!!
            val message: MessageEntity = list.get(position)!!
            message?.folder = "ARCHIVE"
            clientMessagesListViewModel.database.messageDao().update(message)
        }

    }

    private val clientMessagesListViewModel by viewModels<ClientMessagesListViewModel> {
        ClientMessagesListViewModelFactory(app)
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

        // Initialize the RecyclerView and adapter
        messagesRecyclerView = view.findViewById(R.id.messages_recycler_view)
        messageAdapter = MessageAdapter()
        messagesLayoutManager = LinearLayoutManager(context)

        // Set the adapter and layout manager for the RecyclerView
        messagesRecyclerView.adapter = messageAdapter
        messagesRecyclerView.layoutManager = messagesLayoutManager

        clientMessagesListViewModel.messagesList.observe(viewLifecycleOwner, Observer {
            messages -> messageAdapter.submitList(messages)
        })

        val itemTouchHelper = ItemTouchHelper(ithCallback)
        itemTouchHelper.attachToRecyclerView(messagesRecyclerView)

        return view
    }


}
