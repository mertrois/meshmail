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

//    private val clientMessagesListViewModel: ClientMessagesListViewModel by viewModels()
//    private val clientMessageListViewModel by viewModels<ClientMessagesListViewModel> {
//        ViewModelProvider.AndroidViewModelFactory(app)
//    }
//    private val clientMessagesListViewModel = ViewModelProvider(this, ClientMessagesListViewModelFactory(app))
//        .get(ClientMessagesListViewModel::class.java)

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
        return view
    }

//    private fun getEmails(): List<MessageEntity> {
//        // Return a list of emails
//        return listOf(
//            MessageEntity(subject="this is a test subject", body="yo I can't make it today"),
//            MessageEntity(subject="We have a very special offer for you, bitch", body="Your rental car upgrade coupons are expiring today")
//        )
//    }
}
