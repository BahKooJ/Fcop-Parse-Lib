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

class FCopCsac(bytes: ByteArray) {

    var sacBytes: ByteArray = bytes
    val allIndexes = createChunkOffsetList()

    var id = getIntAt(8)

    var type = getIntAt(12)

    var cordY = getIntAt(16)

    var cordX = getIntAt(24)

    var rotation: Int? = getRotationIfPossible()

    var objectReferences = discoverObjectReferences()

    private fun getRotationIfPossible(): Int? {

        return when(type) {
            8 -> getShortAt(64)
            36 -> getShortAt(64)
            else -> null
        }

    }

    fun readCords() {

        sacBytes = sacBytes.copyOfRange(0,16) + cordY.toBytes32bit() + sacBytes.copyOfRange(20,sacBytes.count())
        sacBytes = sacBytes.copyOfRange(0,24) + cordX.toBytes32bit() + sacBytes.copyOfRange(28,sacBytes.count())

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

    private fun getShortAt(inx: Int, data: ByteArray = sacBytes): Int {

        val bytes = data.copyOfRange(inx,inx + 2)

        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() and 0xFF shl 8 * i)
        }
        return result
    }

    companion object {

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

}

data class ActReference(
    val fourCC: String,
    val id: Int
)