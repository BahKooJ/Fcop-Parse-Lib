package iff.data

import iff.*
import iff.chunk.ChunkHeader
import java.io.File
import java.io.FileNotFoundException


/**
 * Object for working with Future Cop's Cbmp files
 */
class FCopBitmap {

    companion object {

        const val imageSize = 131072
        const val imageDimensions = 256

        /**
         * Formats a bitmap file in the way Future Cop formats them
         */
        fun formatBitmap(bitmap: ByteArray, width: Int = imageDimensions, height: Int = imageDimensions): ByteArray{

            val headerSize = 54
            val infoSize = 40

            val header = FileFormatData.BitmapHeader.contents
            val fileSize = (bitmap.count() + headerSize).toBytes32bit()
            val reserved = 0.toBytes32bit()
            val dataOffset = headerSize.toBytes32bit()
            //info
            val size = infoSize.toBytes32bit()
            val width = width.toBytes32bit()
            val height = height.toBytes32bit()
            val planes = (1.toShort()).toBytes16bit()
            val bitsPerPixel = (16.toShort()).toBytes16bit()
            val compression = 0.toBytes32bit()
            val imageSize = bitmap.count().toBytes32bit()
            val xPxPerMeter = 0.toBytes32bit()
            val yPxPerMeter = 0.toBytes32bit()
            val colorUsed = 0.toBytes32bit()
            val importantColor = 0.toBytes32bit()

            val bitmapHeader = header + fileSize + reserved + dataOffset + size + width + height + planes +bitsPerPixel + compression +
                    imageSize + xPxPerMeter + yPxPerMeter + colorUsed + importantColor

            return bitmapHeader + bitmap
        }

    }

    /**
     * ID of the Cbmp
     */
    val id: Int

    /**
     * All sub chunk offsets
     */
    val allIndexes: Array<IffChunkHeader>

    /**
     * The contents of a Cbmp
     */
    var bitmapBytes: ByteArray

    /**
     * The raw image data, without the bitmap header, this is very useful for grabbing texture offsets
     */
    var unformattedImage: ByteArray

    /**
     * The image data formatted, in which the OS and programs can read it
     */
    var formattedImage: ByteArray

    constructor(data: ByteArray, id: Int) {

        bitmapBytes = data

        this.id = id

        allIndexes = createAllChunkOffsetList()

        unformattedImage = extractUnformattedBitmaps()
        formattedImage = formatImage()

    }

    constructor(file: File, id: Int) {

        bitmapBytes = file.readBytes()

        this.id = id

        allIndexes = createAllChunkOffsetList()

        unformattedImage = extractUnformattedBitmaps()
        formattedImage = formatImage()

    }

    fun writeToFile(path: String) {

        File(path + "bitmap${id}.bmp").writeBytes(formattedImage)

    }

    private fun extractUnformattedBitmaps(): ByteArray {

        var contents: ByteArray = byteArrayOf()

        for (chunk in allIndexes){
            if (chunk.primaryHeader == ChunkHeader.PX16){
                contents = bitmapBytes.copyOfRange(chunk.index + 8, chunk.indexAfterSize)
                break
            }
        }

        return contents
    }

    private fun formatImage(): ByteArray = formatBitmap(unformattedImage)

    /**
     * replaces the current bitmap, with a new bitmap and returns the result
     */
    fun replacedBitmapDataCompressed(bitmap: ByteArray): ByteArray{

        val content = mutableListOf<Byte>()

        val offset = getIntAt(10, bitmap)

        val data = bitmap.copyOfRange(offset,offset + imageSize)

        //todo magic numbers (34 and 10)
        if (getIntAt(34, bitmap) != imageSize){
            if (getIntAt(34, bitmap) != imageSize + 2) {
                throw InvalidFileSizeException("image size is bigger or smaller than the original data. image size must be 131072")
            }
        }


        for (chunk in allIndexes){
            if (chunk.primaryHeader == ChunkHeader.PX16) {
                content += bitmapBytes.copyOfRange(0, chunk.index).toMutableList()
                content += bitmapBytes.copyOfRange(chunk.index, chunk.index + 8).toMutableList()
                content += data.toMutableList()
            }
            if (chunk.primaryHeader == ChunkHeader.PLUT) {
                content += bitmapBytes.copyOfRange(chunk.index, chunk.indexAfterSize).toMutableList()
            }
        }

        if (bitmapBytes.count() != content.count()){
            throw InvalidFileSizeException("size does not match original")
        }

        return content.toByteArray()
    }

    private fun createChunkOffsetList(id: ChunkHeader, data: ByteArray = bitmapBytes): Array<Int> {

        val index: MutableList<Int> = mutableListOf()

        for ((i, byte) in data.withIndex()){
            if (byte == id.fourCC[0]){
                if (data.copyOfRange(i,i+4).contentEquals(id.fourCC)){
                    index.add(i)
                }
            }

        }
        return index.toTypedArray()
    }

    private fun createAllChunkOffsetList(): Array<IffChunkHeader> {

        val totalIndexes = mutableListOf<Int>()
        val total = mutableListOf<IffChunkHeader>()

        totalIndexes += createChunkOffsetList(ChunkHeader.LCCB).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.LkUp).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.PX16).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.PLUT).toMutableList()

        if (totalIndexes.isEmpty()){
            throw error("This file is not a Cbmp")
        }

        totalIndexes.sort()

        for ((index, i) in totalIndexes.withIndex()) {

            total.add(
                IffChunkHeader(
                    chunkIndex = index,
                    index = i,
                    primaryHeader = ChunkHeader.valueOf(bitmapBytes.copyOfRange(i,i + 4).decodeToString().reversed()),
                    chunkSize = getIntAt(i + 4)
                )
            )

        }

        return total.toTypedArray()
    }

    private fun getIntAt(inx: Int, data: ByteArray = bitmapBytes): Int {

        val bytes = data.copyOfRange(inx,inx + 4)

        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() and 0xFF shl 8 * i)
        }
        return result
    }

}