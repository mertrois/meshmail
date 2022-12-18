package app.meshmail.android

class Parameters {
    companion object {
        val MESHMAIL_PORT: Int = 309
        val MAX_MESSAGE_FRAGMENT_SIZE = 160     // max bytes that can go into a message fragment payload

        /*
        the seconds between scanning for missing fragments and making requests to the relay to have them
        sent over. A received fragment can short circuit this. This is mainly in case there is a prolonged
        outage, to restart the request process.  Can by Nudged by the receipt of a shadow or receipt of a fragment
         */
        val FRAGMENT_SYNC_PERIOD: Long = 60

        /*
        Seconds between polling the imap server for new mail. This may become irrelevent if push mail
        is implemented.
         */
        val MAIL_SYNC_PERIOD: Long = 60

        /*
        The delay between dequeueing waiting outgoing packets. Need to dig into the firmware and meshtastic software
        to see if they are doing any flow control, e.g. can we just dump them all to to the next layer and let
        more advanced control take care of timing?
         */
        val SEND_QUEUE_WAIT: Long = 1000     // milliseconds between putting packets on wire
    }
}