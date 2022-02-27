package iff.data

import iff.InvalidFileSizeException
import iff.chunk.ChunkHeader
import iff.toBytes16bit
import iff.toBytes32bit
import java.io.File

/**
 * Very early state, do not use unless you know what you're doing
 */
class FCopCtil(tileBinaryData: ByteArray, val id: Int) {

    companion object {

        const val ctilSizeCords = 131072.0

        const val textureCordAmountOffset: Int = 10
        const val heightMapOffset: Int = 12
        const val heightMapEndingOffset: Int = 879
        const val renderDistanceOffset: Int = 880
        const val renderDistanceEndingOffset: Int = 970

        const val thirdSectionOffset: Int = 970
        const val thirdSectionEndingOffset: Int = 1488

        const val tileDeclarationOffset: Int = 1488 //todo unguaranteed offset
        const val textureOffsetsOffset: Int = 2512  //todo offset is NOT constant, for prototyping!
    }

    var tileBytes: ByteArray = tileBinaryData

    var unknownNumber: Short = getShortAt(8).toShort()
    var textureCordAmount = getShortAt(textureCordAmountOffset)
    var heightMapPoints = getHeightPoints()
    var textureCoordinates = getTextureCord()
    var declaredTiles = getTileDeclaration()

    var renderDistanceData: ByteArray = getRenderDistance()
    var thirdSectionData: ByteArray = getThirdSection()

    var sortedDeclaredTiles = getSortedTiles()

    fun addTexture(texture: TextureCoordinate) {
        textureCoordinates.add(texture)
        textureCordAmount += 4
    }

    private fun getSortedTiles(): MutableList<CtilTile> {

        val total = mutableListOf<CtilTile>()

        var x = 0
        var y = 0

        for (i in 0 until 16) {

            total += declaredTiles[(y * 16) + x]
            total += declaredTiles[(y * 16) + x + 1]
            total += declaredTiles[(y * 16) + x + 2]
            total += declaredTiles[(y * 16) + x + 3]

            total += declaredTiles[((y + 1) * 16) + x]
            total += declaredTiles[((y + 1) * 16) + x + 1]
            total += declaredTiles[((y + 1) * 16) + x + 2]
            total += declaredTiles[((y + 1) * 16) + x + 3]

            total += declaredTiles[((y + 2) * 16) + x]
            total += declaredTiles[((y + 2) * 16) + x + 1]
            total += declaredTiles[((y + 2) * 16) + x + 2]
            total += declaredTiles[((y + 2) * 16) + x + 3]

            total += declaredTiles[((y + 3) * 16) + x]
            total += declaredTiles[((y + 3) * 16) + x + 1]
            total += declaredTiles[((y + 3) * 16) + x + 2]
            total += declaredTiles[((y + 3) * 16) + x + 3]

            x += 4

            if (x == 16) {
                x = 0
                y += 4
            }

        }

        return total

    }

    // -Reading data-
    private fun getHeightPoints(): Array<CtilPoint> {

        val content = mutableListOf<CtilPoint>()

        val map = tileBytes.copyOfRange(heightMapOffset,heightMapEndingOffset)

        var index = 0
        var x = 0
        var y = 0
        var heightMap1: Byte = 0
        var heightMap2: Byte = 0
        var heightMap3: Byte = 0

        var iterator = 0
        for (byte in map) {

            if (iterator == 3) {

                content.add(CtilPoint(index, x, y, heightMap1, heightMap2, heightMap3))

                x++

                if (x == 17) {
                    x = 0
                    y++
                }

                index++
                iterator = 0
            }

            when(iterator) {
                0 -> heightMap1 = byte
                1 -> heightMap2 = byte
                2 -> heightMap3 = byte
            }
            iterator++

        }

        content.add(CtilPoint(index, x, y, heightMap1, heightMap2, heightMap3))

        return content.toTypedArray()
    }

    private fun getRenderDistance(): ByteArray {
        return tileBytes.copyOfRange(renderDistanceOffset, renderDistanceEndingOffset)
    }

    private fun getThirdSection(): ByteArray {
        return tileBytes.copyOfRange(thirdSectionOffset, thirdSectionEndingOffset)
    }

    private fun getTextureCord(): MutableList<TextureCoordinate> {

        val total = mutableListOf<TextureCoordinate>()

        var textureAmount = textureCordAmount / 4

        val textureOffsets = mutableListOf<Int>()

        for (offset in textureOffsetsOffset..tileBytes.count() step 2) {
            textureOffsets.add(getShortAt(offset))

            if (textureOffsets.count() == 4) {
                textureAmount--
                total.add(TextureCoordinate(textureOffsets[0],textureOffsets[1],textureOffsets[2],textureOffsets[3]))
                textureOffsets.clear()
            }
            if (textureAmount == 0) {
                return total
            }

        }

        error("should never be getting to this point")

    }

    private fun getTileDeclaration(): MutableList<CtilTile> {

        val total = mutableListOf<CtilTile>()

        val numberOfTiles = 256

        // Takes the number of tiles and multiplies it by 4 for both the declaration and texture
        for ((index, offset) in (tileDeclarationOffset until tileDeclarationOffset + (numberOfTiles * 4) step 4).withIndex()) {

            total.add(
                CtilTile(
                    index = index,
                    textureIndex = getShortAt(offset),
                    type = getShortAt(offset + 2)
                )
            )

        }

        return total

    }

    fun reInit() {

        unknownNumber = getShortAt(8).toShort()
        textureCordAmount = getShortAt(textureCordAmountOffset)
        heightMapPoints = getHeightPoints()
        textureCoordinates = getTextureCord()
        declaredTiles = getTileDeclaration()

        renderDistanceData = getRenderDistance()
        thirdSectionData = getThirdSection()

        sortedDeclaredTiles = getSortedTiles()

    }

    // -Modifying data-

    fun writeClassToBytes() {

        val total = mutableListOf<Byte>()

        // The headers fourCC
        total += listOf(0x74, 0x63, 0x65, 0x53)

        // The files size is unknown right now, so it fills the space to be changed later
        total += listOf(0x00,0x00,0x00,0x00)
        total += unknownNumber.toBytes16bit().toList()
        total += textureCordAmount.toShort().toBytes16bit().toList()

        // Gets the height map points
        for (point in heightMapPoints) {
            total += listOf(point.heightMap1, point.heightMap2, point.heightMap3)
        }

        // Spacer in the file
        total.add(0x00)

        total += renderDistanceData.toList()
        total += thirdSectionData.toList()

        for (tile in declaredTiles) {
            total += tile.textureIndex.toShort().toBytes16bit().toList()
            total += tile.type.toShort().toBytes16bit().toList()
        }

        for (texture in textureCoordinates) {
            total += (
                    texture.topLeftIndex.toShort().toBytes16bit()
                            + texture.topRightIndex.toShort().toBytes16bit()
                            + texture.bottomRightIndex.toShort().toBytes16bit()
                            + texture.bottomLeftIndex.toShort().toBytes16bit()
                    ).toList()
        }

        //TODO This is data for where textures are, literal for prototyping!
        total += listOf(0x74, 0x26, 0x00, 0x00)

        tileBytes = total.toByteArray()

//        if (tileBytes.count() != 2524) {
//            error("size is wrong")
//        }

        changeSize()

        File("output/testtile").writeBytes(tileBytes)

        reInit()

    }

    fun readHeightPoints() {

        val total = mutableListOf<Byte>()

        for (point in heightMapPoints) {
            total += listOf(point.heightMap1, point.heightMap2, point.heightMap3)
        }


        tileBytes = tileBytes.copyOfRange(0,heightMapOffset) + total + tileBytes.copyOfRange(heightMapEndingOffset,tileBytes.count())

    }

    fun changeRenderDistance(data: ByteArray) {

        if (data.count() != 90) {
            throw InvalidFileSizeException("Must be 90 bytes long")
        }

        tileBytes = tileBytes.copyOfRange(0,renderDistanceOffset) + data + tileBytes.copyOfRange(renderDistanceEndingOffset, tileBytes.count())

    }

    private fun changeSize() {
        tileBytes = tileBytes.copyOfRange(0,4) + tileBytes.count().toBytes32bit() + tileBytes.copyOfRange(8,tileBytes.count())
    }

    // -TXT file methods-
    fun createRandomTextList(path: String) {
        val file = File(path + "randomShiz.txt")

        var total = ""

        val randomShiz = tileBytes.copyOfRange(renderDistanceOffset, tileBytes.count())

        for (i in 0 until (randomShiz.count() / 2)) {
            total += getShortAt((i * 2) + renderDistanceOffset)
            total += " "
        }

        file.writeText(total)
    }

    fun convertNumberTextFileToBinary(path: String): ByteArray {

        val file = File(path).readBytes()

        val data = mutableListOf<Byte>()

        val space: Byte = 0x20
        val enter1: Byte = 0x0D
        val enter2: Byte = 0X0A

        var tempByteArray = byteArrayOf()

        for (byte in file) {

            if (byte == space || byte == enter1 || byte == enter2) {

                if (tempByteArray.isNotEmpty()) {
                    val string = tempByteArray.decodeToString()
                    val short = string.toUShort()
                    data += short.toShort().toBytes16bit().toMutableList()
                }

                tempByteArray = byteArrayOf()

            } else {
                tempByteArray += byte
            }

        }

        changeSize()

        return tileBytes.copyOfRange(0,880) + data.toByteArray()

    }

    fun convertHeightMapListToBinary(path: String): ByteArray {

        val file = File(path).readBytes()

        val data = mutableListOf<Byte>()

        val space: Byte = 0x20
        val enter1: Byte = 0x0D
        val enter2: Byte = 0X0A
        val bar: Byte = 0x7C
        val negative: Byte = 0x2D

        var tempByteArray = byteArrayOf()

        for (byte in file) {

            if (byte == space || byte == enter1 || byte == enter2 || byte == bar) {

                if (tempByteArray.isNotEmpty()) {
                    val string = tempByteArray.decodeToString()
                    val readByte = string.toByte()
                    data += readByte
                }

                tempByteArray = byteArrayOf()

            } else {
                tempByteArray += byte
            }

        }

        return tileBytes.copyOfRange(0,12) + data.toByteArray() + tileBytes.copyOfRange(879,tileBytes.count())

    }

    // -Utils-
    private fun getShortAt(inx: Int, data: ByteArray = tileBytes): Int {

        val bytes = data.copyOfRange(inx,inx + 2)

        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() and 0xFF shl 8 * i)
        }
        return result
    }

    inner class CtilTile(
        val index: Int,
        var textureIndex: Int,
        val type: Int
    )

    inner class CtilPoint(
        val index: Int,
        val x: Int,
        val y: Int,
        var heightMap1: Byte,
        var heightMap2: Byte,
        var heightMap3: Byte
    )

}

data class TextureCoordinate(
    var topLeftIndex: Int,
    var topRightIndex: Int,
    var bottomRightIndex: Int,
    var bottomLeftIndex: Int
) {

    fun receiveTexture(bmp: FCopBitmap): ByteArray {

        val bytesPerPixel = 2

        val total = mutableListOf<Byte>()

        val width: Int = topRightIndex - topLeftIndex
        val height: Int = (topRightIndex / FCopBitmap.imageDimensions) - (bottomRightIndex / FCopBitmap.imageDimensions)

        for (row in 0 until height) {
            total += bmp.unformattedImage.copyOfRange(
                (bottomLeftIndex * bytesPerPixel) + ((row * FCopBitmap.imageDimensions) * bytesPerPixel),
                ((bottomLeftIndex * bytesPerPixel) + ((row * FCopBitmap.imageDimensions) * bytesPerPixel)) + (width * bytesPerPixel)
            ).toList()
        }

        return FCopBitmap.formatBitmap(total.toByteArray(), width, height)

    }

}









