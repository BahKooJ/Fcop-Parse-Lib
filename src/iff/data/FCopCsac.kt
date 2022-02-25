package iff.data

import iff.IffChunkHeader
import iff.NoGivenDataException
import iff.chunk.ChunkHeader
import iff.chunk.IffFormatData
import iff.chunk.IffFormatting
import iff.toBytes16bit
import iff.toBytes32bit
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FCopCsac(bytes: ByteArray) {

    companion object {

        const val idOffset: Int = 8
        const val typeOffset: Int = 12
        const val cordYOffset: Int = 16
        const val cordXOffset: Int = 24
        const val unknownDataOffset: Int = 28

        fun createHeavyWeaponResupplyWithHeader(id: Int, positionX: Int, positionY: Int): ByteArray {

            val header = IffFormatting.createBasicHeader(ChunkHeader.Csac,id, 2,144,1807,1775,107)

            val shocChunk = ChunkHeader.SHOC.fourCC + 164.toBytes32bit() + IffFormatData.SHOCDataAfterSize.contents +
                    ChunkHeader.SDAT.fourCC

            val contents = ChunkHeader.tACT.fourCC + 68.toBytes32bit() + id.toBytes32bit() + 16.toBytes32bit() +
                    positionY.toBytes32bit() + byteArrayOf(0x00,0x00,0x00,0x00) + positionX.toBytes32bit() +
                    byteArrayOf(0x81.toByte(), 0x00,0x08,0x00
                        ,0x00,0x00,0x00,0x00,0x00,0x00,0x05,0x00,0x00,0x00,0x00,0x00,0x00,0x01,0x01,0x00,
                        0x51,0x00,0x33,0x03,0x33,0x03,0x00,0x08,0x00,0x00,0x63,0x00,0x20,0x00,0x00,0x03,
                        0xFF.toByte(),0xFF.toByte(),0xCC.toByte(),0xEC.toByte()
                    ) + ChunkHeader.aRSL.fourCC + 28.toBytes32bit() + id.toBytes32bit() + ChunkHeader.Cobj.fourCC + 24.toBytes32bit() +
                    ChunkHeader.NULL.fourCC + 0.toBytes32bit() + ChunkHeader.tSAC.fourCC + 48.toBytes32bit() + id.toBytes32bit() +
                    byteArrayOf(0x90.toByte(),0xE8.toByte(),0x01,0x00,0x00,0x00,0xFF.toByte(),0xFF.toByte(),0x00,0x00,
                        0x00,0x00,0x00,0x00,0x00,0x00,0x00, 0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
                        0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00)

            return header + shocChunk + contents
        }

    }


    var sacBytes: ByteArray = bytes
    val allIndexes = createChunkOffsetList()

    var id = getIntAt(idOffset)

    var type = getIntAt(typeOffset)

    var cordY = getIntAt(cordYOffset)

    var cordX = getIntAt(cordXOffset)

    var rotation: Int? = getRotationIfPossible()

    var objectReferences = discoverObjectReferences()

    var unknownData: List<Int> = getUnknownDataInACT()

    var unknownDataInSac: List<Int> = getUnknownDataInCSac()

    private fun getUnknownDataInACT(): List<Int> {

        val total = mutableListOf<Int>()

        val data = sacBytes.copyOfRange(28,allIndexes[1].index)

        for (index in 0 until data.count() step 2) {

            total += getShortAt(index, data)

        }

        return total

    }

    private fun getUnknownDataInCSac(): List<Int> {

        val total = mutableListOf<Int>()

        val data = sacBytes.copyOfRange(allIndexes[2].index, sacBytes.count())

        for (index in 0 until data.count() step 2) {

            total += getShortAt(index, data)

        }

        return total

    }

    private fun getRotationIfPossible(): Int? {

        return when(type) {
            8 -> getShortAt(64)
            36 -> getShortAt(64)
            else -> null
        }

    }

    fun createTextFile(path: String) {

        var total = ""

        for (chunk in allIndexes) {

            when(chunk.primaryHeader) {

                ChunkHeader.tACT -> {
                    total += "tACT "
                    total += "id: $id "
                    total += "type: $type "
                    total += "y: $cordY "
                    total += "x: $cordX "

                    total += "\n"

                    for (short in unknownData) {
                        total += "$short "
                    }

                    total += "\n \n"

                }
                ChunkHeader.aRSL -> {
                    total += "aRSL \n"

                    for (ref in objectReferences) {

                        total += ref.fourCC + " " + ref.id.toString()
                        total += "\n"

                    }

                    total += "\n"

                }
                ChunkHeader.tSAC -> {

                    total += "tSAC \n"

                    for (short in unknownDataInSac) {
                        total += "$short "
                    }

                }

            }

        }

        File(path + "sac$id chunklist.txt").writeText(total)

    }

    fun readCords() {

        sacBytes = sacBytes.copyOfRange(0,cordYOffset) + cordY.toBytes32bit() + sacBytes.copyOfRange(cordYOffset + 4,sacBytes.count())
        sacBytes = sacBytes.copyOfRange(0,cordXOffset) + cordX.toBytes32bit() + sacBytes.copyOfRange(cordXOffset + 4,sacBytes.count())

        if (rotation != null) {

            sacBytes = sacBytes.copyOfRange(0,64) + rotation!!.toShort().toBytes16bit() + sacBytes.copyOfRange(66,sacBytes.count())
            sacBytes = sacBytes.copyOfRange(0,78) + rotation!!.toShort().toBytes16bit() + sacBytes.copyOfRange(80,sacBytes.count())

        }

    }

    private fun discoverObjectReferences(): List<ActReference> {

        val total = mutableListOf<ActReference>()

        var objectAmount: Int
        for (chunk in allIndexes){

            if (chunk.primaryHeader == ChunkHeader.aRSL) {

                // Each object reference is 8 bytes long (fourCC and then a number). It subs the header size which is 12
                objectAmount = (chunk.chunkSize - 12) / 8

                for (i in 0 until objectAmount) {

                    val index = chunk.index + (12 + (i * 8))

                    total.add(
                        ActReference(sacBytes.copyOfRange(index, index + 4).decodeToString().reversed(),getIntAt(index + 4))
                    )

                }

            }

        }

        return total

    }

    private fun createChunkOffsetList(): List<IffChunkHeader> {

        val total = mutableListOf<IffChunkHeader>()

        val totalIndexes = mutableListOf<Int>()

        totalIndexes += createChunkOffsetList(ChunkHeader.tACT).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.aRSL).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.tSAC).toMutableList()

        totalIndexes.sort()

        for ((chunkIndex, i) in totalIndexes.withIndex()) {

            total += IffChunkHeader(
                chunkIndex = chunkIndex,
                index = i,
                primaryHeader = ChunkHeader.valueOf(sacBytes.copyOfRange(i, i + 4).decodeToString().reversed()),
                chunkSize = getIntAt(i + 4)
            )

        }

        return total
    }

    private fun createChunkOffsetList(id: ChunkHeader, data: ByteArray = sacBytes): Array<Int> {

        val index: MutableList<Int> = mutableListOf()

        for ((i, byte) in data.withIndex()){
            if (byte == id.fourCC[0]){
                try {
                    if (data.copyOfRange(i, i + 4).contentEquals(id.fourCC)) {
                        index.add(i)
                    }
                } catch (e: IndexOutOfBoundsException) {  }
            }

        }
        return index.toTypedArray()
    }

    private fun getIntAt(inx: Int, data: ByteArray = sacBytes): Int {

        val bytes = data.copyOfRange(inx,inx + 4)

        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() and 0xFF shl 8 * i)
        }
        return result
    }

    private fun getUShortAt(inx: Int, data: ByteArray = sacBytes): Int {

        val bytes = data.copyOfRange(inx,inx + 2)

        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() and 0xFF shl 8 * i)
        }
        return result
    }

    private fun getShortAt(inx: Int, data: ByteArray = sacBytes): Int {

        val bytes = data.copyOfRange(inx,inx + 2)

        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()[0].toInt()

    }

}

data class ActReference(
    val fourCC: String,
    val id: Int
)