package iff.data

import iff.chunk.IffChunkHeader
import iff.chunk.ChunkHeader
import iff.toBytes16bit
import iff.toBytes32bit
import java.io.File
import java.lang.IndexOutOfBoundsException

class FCopCact(bytes: ByteArray, dataHeader: IffChunkHeader): FCopData(bytes, -1, dataHeader) {

    companion object {

        const val idOffset: Int = 8
        const val typeOffset: Int = 12
        const val cordYOffset: Int = 16
        const val cordXOffset: Int = 24
        const val unknownDataOffset: Int = 28

    }


    val allIndexes = createChunkOffsetList()

    var isCSac = findIfCSac()

    init {
        id = getIntAt(idOffset)
    }

    var type = getIntAt(typeOffset)

    var cordY = getIntAt(cordYOffset)

    var cordX = getIntAt(cordXOffset)

    var rotation: Int? = getRotationIfPossible()

    var objectReferences = discoverObjectReferences()

    var unknownData: List<Int> = getUnknownDataInACT()

    var unknownDataInSac: List<Int> = getUnknownDataInCSac()

    private fun findIfCSac(): Boolean {

        return allIndexes.find { it.primaryHeader == ChunkHeader.tSAC } != null

    }

    private fun getUnknownDataInACT(): List<Int> {

        val total = mutableListOf<Int>()

        val data = bytes.copyOfRange(unknownDataOffset,allIndexes[1].index)

        for (index in 0 until data.count() step 2) {

            total += getShortAt(index, data)

        }

        return total

    }

    private fun getUnknownDataInCSac(): List<Int> {

        if (allIndexes.count() < 3) {
            return listOf()
        }

        val total = mutableListOf<Int>()

        val data = bytes.copyOfRange(allIndexes[2].index, bytes.count())

        for (index in 0 until data.count() step 2) {

            total += getShortAt(index, data)

        }

        return total

    }

    private fun getRotationIfPossible(): Int? {

        return when(type) {
            8 -> getShortAt(64)
            11 -> getShortAt(46)
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

        bytes = bytes.copyOfRange(0,cordYOffset) + cordY.toBytes32bit() + bytes.copyOfRange(cordYOffset + 4,bytes.count())
        bytes = bytes.copyOfRange(0,cordXOffset) + cordX.toBytes32bit() + bytes.copyOfRange(cordXOffset + 4,bytes.count())

        if (rotation != null) {

            if (type == 11) {
                bytes = bytes.copyOfRange(0,46) + rotation!!.toShort().toBytes16bit() + bytes.copyOfRange(48,bytes.count())
                return
            }

            bytes = bytes.copyOfRange(0,64) + rotation!!.toShort().toBytes16bit() + bytes.copyOfRange(66,bytes.count())
            bytes = bytes.copyOfRange(0,78) + rotation!!.toShort().toBytes16bit() + bytes.copyOfRange(80,bytes.count())

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
                        ActReference(bytes.copyOfRange(index, index + 4).decodeToString().reversed(),getIntAt(index + 4))
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
                primaryHeader = ChunkHeader.valueOf(bytes.copyOfRange(i, i + 4).decodeToString().reversed()),
                chunkSize = getIntAt(i + 4)
            )

        }

        return total
    }

    private fun createChunkOffsetList(id: ChunkHeader, data: ByteArray = bytes): Array<Int> {

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


}

data class ActReference(
    val fourCC: String,
    val id: Int
)