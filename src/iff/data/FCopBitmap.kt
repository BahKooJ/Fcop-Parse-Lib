package iff.data

import iff.*
import iff.chunk.ChunkHeader
import java.io.File
import java.io.FileNotFoundException


/**
 * Object for working with Future Cop's Cbmp files
 */
class FCopBitmap(bytes: ByteArray, id: Int): FCopData(bytes, id) {

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
     * All sub chunk offsets
     */
    val allIndexes: Array<IffChunkHeader>

    /**
     * The raw image data, without the bitmap header, this is very useful for grabbing texture offsets
     */
    var unformattedImage: ByteArray

    /**
     * The image data formatted, in which the OS and programs can read it
     */
    var formattedImage: ByteArray

    init {

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
                contents = bytes.copyOfRange(chunk.index + 8, chunk.indexAfterSize)
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
                content += bytes.copyOfRange(0, chunk.index).toMutableList()
                content += bytes.copyOfRange(chunk.index, chunk.index + 8).toMutableList()
                content += data.toMutableList()
            }
            if (chunk.primaryHeader == ChunkHeader.PLUT) {
                content += bytes.copyOfRange(chunk.index, chunk.indexAfterSize).toMutableList()
            }
        }

        if (bytes.count() != content.count()){
            throw InvalidFileSizeException("size does not match original")
        }

        return content.toByteArray()
    }


    private fun createAllChunkOffsetList(): Array<IffChunkHeader> {

        val totalIndexes = mutableListOf<Int>()
        val total = mutableListOf<IffChunkHeader>()

        totalIndexes += IffFile.createChunkOffsetList(ChunkHeader.LCCB, bytes).toMutableList()
        totalIndexes += IffFile.createChunkOffsetList(ChunkHeader.LkUp, bytes).toMutableList()
        totalIndexes += IffFile.createChunkOffsetList(ChunkHeader.PX16, bytes).toMutableList()
        totalIndexes += IffFile.createChunkOffsetList(ChunkHeader.PLUT, bytes).toMutableList()

        if (totalIndexes.isEmpty()){
            throw error("This file is not a Cbmp")
        }

        totalIndexes.sort()

        for ((index, i) in totalIndexes.withIndex()) {

            total.add(
                IffChunkHeader(
                    chunkIndex = index,
                    index = i,
                    primaryHeader = ChunkHeader.valueOf(bytes.copyOfRange(i,i + 4).decodeToString().reversed()),
                    chunkSize = getIntAt(i + 4)
                )
            )

        }

        return total.toTypedArray()
    }

}