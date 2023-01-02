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
        This determines the frequency of the queue loop
         */
        val SEND_QUEUE_WAIT: Long = 100     // milliseconds between putting packets on wire


        /*
            If we haven't been nudged to send in this many ms, go ahead and release another packet
         */
        val QUEUE_TIMEOUT_THRESHOLD: Long = 3500

        /* how many messages should we look at per sync cycle to find missing fragments for?
            fewer can cause repeats, too many can cause many messages only partly loaded
         */
        val FRAG_SYNC_SHADOWS_TO_ANALYZE: Int = 5

        /*
            the outer "for loop" runs FRAG_SYNC_SHADOWS_TO_ANALYZE times, and the inner, up to MAX_FRAGS_AT_ONCE times
            making this higher than one should cut down on duplicates
         */
        val MAX_FRAGS_AT_ONCE: Int = 3


        /*
            If the send queue drops below this size, welcome to add more
         */
        val MIN_DESIRED_QUEUE_SIZE: Int = 3

    }
}