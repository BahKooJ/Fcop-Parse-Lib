package iff.data

import iff.NoGivenDataException
import iff.chunk.ChunkHeader
import iff.chunk.IffFormatData
import iff.chunk.IffFormatting
import iff.toBytes32bit
import java.io.File
import java.lang.IndexOutOfBoundsException

//FIXME OLD BAD CODE
class FCopCsac(sacFileName: String? = null, sacBinaryData: ByteArray? = null) {

    init {
        if (sacBinaryData == null && sacFileName == null){
            throw NoGivenDataException("Both constructors are null and have no data to read. Please give either a file name, or a byte array")
        }
    }

    private val sacFile: File? = if (sacFileName != null) { File(sacFileName) } else { null }
    val sacBytes: ByteArray = sacBinaryData ?: sacFile!!.readBytes()
    val allIndexes = createChunkOffsetList()

    var id = getIntAt(8)

    var type = getIntAt(12)

    var cordY = getIntAt(16)

    var cordX = getIntAt(24)

    var objectReferences = discoverObjectReferences()

    private fun discoverObjectReferences(): List<Map<Int, Pair<String, Int>>> {

        val total = mutableListOf<Map<Int, Pair<String, Int>>>()

        var objectAmount: Int
        for (chunk in allIndexes){

            if (chunk.value[0] == "aRSL") {
                objectAmount = ((chunk.value[1] as Int) - 12) / 8

                for (i in 0 until objectAmount) {
                    val index = chunk.key + (12 + (i * 8))
                    total.add(mapOf(Pair(
                        index,
                        Pair(sacBytes.copyOfRange(index, index + 4).decodeToString().reversed(),
                            getIntAt(index + 4)
                        )
                    )))
                }

            }

        }
        return total
    }

    private fun createChunkOffsetList(): Map<Int, Array<Any>> {

        val totalIndexes = mutableListOf<Int>()
        val finalMap: MutableMap<Int, Array<Any>> = mutableMapOf()

        totalIndexes += createChunkOffsetList(ChunkHeader.tACT).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.aRSL).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.tSAC).toMutableList()

        totalIndexes.sort()

        for (i in totalIndexes) {

            finalMap[i] = arrayOf(
                sacBytes.copyOfRange(i, i + 4).decodeToString().reversed(),
                getIntAt(i + 4)
            )

        }

        return finalMap
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

    fun getObject(): String{

        return when(type){
//            8 -> "Base Turret"
//            9 -> "Flying Fortress"
//            36 -> "Turret"
            else -> type.toString()
        }
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