package app.meshmail.data

/*

Will contain num fragments, and an array of ByteArrays containing tranches of of a protobuf

 */
class FragmentedMessage(val numFragments: Int, val subjectPreview: String, val messageID: Int) {
    private val fragments = ArrayList<ByteArray>(numFragments)

    fun addFragment(position: Int, bytes: ByteArray) {
        fragments[position] = bytes
    }

    /*
    returns whether or not all the pieces are in
     */
    fun isComplete(): Boolean = fragments.count() == numFragments

    /*
    assembles all the byte arrays into one
     */
    fun getAssembledByteArray(): ByteArray = fragments.reduce { buffer, bytes -> buffer + bytes }

    /*
        This should provide the protobuf class for email.
     */
    fun deserializeMessage(bytes: ByteArray): Unit {
        return Unit
    }

}