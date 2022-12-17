package app.meshmail.android

class Parameters {
    companion object {
        val MESHMAIL_PORT: Int = 309
        val MAX_MESSAGE_FRAGMENT_SIZE = 160     // max bytes that can go into a message fragment payload

        val FRAGMENT_SYNC_PERIOD: Long = 5 // seconds between fragment sync checks
        val MAIL_SYNC_PERIOD: Long = 60     // seconds between polling imap server

        val SEND_QUEUE_WAIT: Long = 1000     // milliseconds between putting packets on wire
    }
}