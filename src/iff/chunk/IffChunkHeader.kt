package iff.chunk

import iff.toBytes32bit

/**
 * Object for storing information about Future Cop's IFF chunks
 */
data class IffChunkHeader(

    /**
     * Chunk index is the index of item in an array, UNRELATED TO THE FILE
     */
    val chunkIndex: Int,
    /**
     * Index or offset of the chunk
     */
    val index: Int,
    /**
     * The fourCC the chunk starts with
     */
    val primaryHeader: ChunkHeader,
    /**
     * The size of the chunk
     */
    val chunkSize: Int,
    /**
     * The fourCC describing what the chunk is
     */
    val secondaryHeader: ChunkHeader? = null,
    /**
     * If the chunk is a header, right after the secondary fourCC, there's some sort of number. This is that number.
     */
    val maybeHeaderDataType: Int = 0,
    /**
     * If the [secondaryHeader] is [ChunkHeader.SHDR], then this is the fourCC for what data/file is stored
     */
    val dataDeclaration: ChunkHeader? = null,
    /**
     * The ID of the [dataDeclaration]
     */
    val dataID: Int = 0,
    /**
     * The size of [dataDeclaration]
     */
    val dataSize: Int = 0,
    /**
     * Refereces to Cacts in the header (unused until more is learned about Cacts)
     */
    val actReferences: Array<Int>? = null,
    /**
     * File name, if it has one
     */
    val fileName: String = "",

    /**
     * Number relating to music files
     */
    val musicLoopNumber: Int = 0,
    val unknownMusicNumber: Int = 0,

    // Used only by CTRL chunks
    var controlMusicSize: Int = 0,
    var controlDataSize: Int = 0,
    var controlSoundSize: Int = 0,

    // since not all data is parsable in the headers, it has to keep some data to fill in
    val fullData: ByteArray? = null
) {
    val indexAfterSize: Int
        get() = index + chunkSize

    fun compile(newChunkSize: Int = chunkSize, newDataID: Int = dataID, newDataSize: Int = dataSize): ByteArray {

        var total = byteArrayOf()

        total += primaryHeader.fourCC
        total += newChunkSize.toBytes32bit()

        if (secondaryHeader == null) { return total }

        total += IffFormatData.SHOCDataAfterSize.contents
        total += secondaryHeader.fourCC

        if (secondaryHeader == ChunkHeader.FILE) {
            total += fullData!!.copyOfRange(20,fullData.count())
            return total
        }

        if (dataDeclaration == null) { return total }

        total += maybeHeaderDataType.toBytes32bit()

        total += dataDeclaration.fourCC
        total += newDataID.toBytes32bit()
        total += newDataSize.toBytes32bit()

        // Here's the stuff I don't know what does
        total += fullData!!.copyOfRange(36,fullData.count())

        return total
    }

}