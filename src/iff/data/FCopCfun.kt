package iff.data

import iff.chunk.ChunkHeader
import java.io.File


//FIXME OLD BAD CODE
class FCopCfun(dataFileName: String) {

    private val funFile = File(dataFileName)
    val funBytes = funFile.readBytes()
    val allIndexes = createDataList()

    fun createDataTextList(){

        val file: File = File(funFile.path + "chunklist.txt")

        var total:String = ""

        var size = 0
        val chunks = allIndexes.keys.count()

        for (i in allIndexes){

            if (i.value[1] is Int) {
                size += i.value[1] as Int
            }

            val array = i.value
            total += i.key.toString() + " "

            var newLineCount = 2
            for (item in array){
                if (item != "null") {
                    if (newLineCount == 5) {
                        total += "\n"
                        newLineCount = 0
                    }
                    total += item.toString() + " "
                    newLineCount++
                }
            }
            total += "\n"
        }


        file.writeText(chunkListHeader(size,chunks) + total)
    }

    private fun createChunkOffsetList(id: ChunkHeader, data: ByteArray = funBytes): Array<Int> {

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

    private fun createDataList(): Map<Int, Array<Any>> {

        val totalIndexes = mutableListOf<Int>()
        val finalMap: MutableMap<Int, Array<Any>> = mutableMapOf()

        totalIndexes += createChunkOffsetList(ChunkHeader.tFUN).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.tEXT).toMutableList()

        totalIndexes.sort()

        for (i in totalIndexes) {

            if (funBytes.copyOfRange(i, i + 4).contentEquals(ChunkHeader.tFUN.fourCC)){
                val size = getIntAt(i + 4)
                val content = mutableListOf<Int>()
                for (num in 1..(get32intCount(size) - 2)) {
                    content.add(getIntAt((i + 4) + (num * 4)))
                }
                finalMap[i] = (mutableListOf<Any>("tFun", size) + content).toTypedArray()
            }

        }

        return finalMap
    }

    private fun getIntAt(inx: Int, data: ByteArray = funBytes): Int {

        val bytes = data.copyOfRange(inx,inx + 4)

        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() and 0xFF shl 8 * i)
        }
        return result
    }

    private fun get32intCount(count: Int) : Int {
        if (count % 4 != 0){
            throw error("count not divisible by 4")
        }
        return count / 4
    }

    private fun chunkListHeader(size: Int, chunks: Int) = "size: $size, chunks: $chunks \n"
}