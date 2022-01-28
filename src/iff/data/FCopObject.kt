package iff.data

import iff.chunk.ChunkHeader
import java.io.File

/**
 * In development, do not use yet
 */
class FCopObject(dataFileName: String) {

    private val objFile = File(dataFileName)
    val objBytes = objFile.readBytes()
    val allIndexes = createAllChunkOffsetList()

    fun createChunkTextList(){

        val file: File = File(objFile.path + "chunklist.txt")

        var total:String = ""

        var size = 0
        val chunks = allIndexes.keys.count()

        for (i in allIndexes){

            if (i.value[1] is Int) {
                size += i.value[1] as Int
            }


            val array = i.value
            total += i.key.toString() + " "

            for (item in array){
                if (item != "null") {
                    total += item.toString() + " "
                }
            }
            total += "\n"
        }


        file.writeText(chunkListHeader(size,chunks) + total)
    }

    private fun createChunkOffsetList(id: ChunkHeader, data: ByteArray = objBytes): Array<Int> {

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

    private fun createAllChunkOffsetList(): Map<Int, Array<Any>> {

        val totalIndexes = mutableListOf<Int>()
        val finalMap: MutableMap<Int, Array<Any>> = mutableMapOf()

        totalIndexes += createChunkOffsetList(ChunkHeader.threeDTL).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.threeDQL).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.threeDRF).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.fourDVL).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.fourDNL).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.threeDRL).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.threeDBB).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.threeDHY).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.threeDMI).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.threeDHS).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.AnmD).toMutableList()

        totalIndexes.sort()

        finalMap[0] = arrayOf(
            "4DGI",
            getIntAt(4),
            getIntAt(8),
            getIntAt(12),
            getIntAt(16),
            getIntAt(20),
            getIntAt(24),
            getIntAt(28),
            getIntAt(32),
            getIntAt(36),
            getIntAt(40),
            getIntAt(44),
            getIntAt(48),
            getIntAt(52),
            getIntAt(56),
        )

        var previousIndex = 0
        var previousSize = 60

        for (i in totalIndexes) {

            if (previousIndex + previousSize != i){
                finalMap[i] = arrayOf("NESTED", objBytes.copyOfRange(i, i + 4).decodeToString().reversed(), getIntAt(i + 4), getIntAt(i + 8))
                continue
            }

            finalMap[i] = arrayOf(objBytes.copyOfRange(i, i + 4).decodeToString().reversed(), getIntAt(i + 4))


            previousIndex = i
            previousSize = finalMap[previousIndex]!![1] as Int

        }

        return finalMap
    }

    private fun getIntAt(inx: Int, data: ByteArray = objBytes): Int {

        val bytes = data.copyOfRange(inx,inx + 4)

        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() and 0xFF shl 8 * i)
        }
        return result
    }

    private fun chunkListHeader(size: Int, chunks: Int) = "size: $size, chunks: $chunks \n"

}