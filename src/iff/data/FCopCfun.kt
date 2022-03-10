package iff.data

import iff.IffChunkHeader
import iff.IffFile
import iff.chunk.ChunkHeader
import java.io.File


class FCopCfun(bytes: ByteArray, id: Int): FCopData(bytes, id) {

    val allIndexes = createDataList()

    val funContents = parseFunContents()

    fun createDataTextList(){

        val file = File("output/CfunChunklist.txt")

        var total = ""

        var lineLength = 0

        for (num in funContents) {

            total += "$num "
            lineLength += 1

            if (lineLength == 5) {
                lineLength = 0
                total += "\n"
            }

        }

        file.writeText(total)

    }

    private fun parseFunContents() : List<Int> {

        val total = mutableListOf<Int>()

        val funHeader = allIndexes.first { it.primaryHeader == ChunkHeader.tFUN }

        val data = bytes.copyOfRange(funHeader.index + 12, funHeader.indexAfterSize)

        for (i in data.indices step 4) {

            total += getIntAt(i, data)

        }

        return total

    }

    private fun createDataList(): List<IffChunkHeader> {

        val totalIndexes = mutableListOf<Int>()
        val total = mutableListOf<IffChunkHeader>()

        totalIndexes += IffFile.createChunkOffsetList(ChunkHeader.tFUN, bytes).toMutableList()
        totalIndexes += IffFile.createChunkOffsetList(ChunkHeader.tEXT, bytes).toMutableList()

        totalIndexes.sort()

        for (i in totalIndexes) {

            when {

                bytes.copyOfRange(i, i + 4).contentEquals(ChunkHeader.tFUN.fourCC) -> {

                    total += IffChunkHeader(
                        chunkIndex = 0,
                        index = i,
                        primaryHeader = ChunkHeader.tFUN,
                        chunkSize = getIntAt(i + 4)
                    )

                }

                bytes.copyOfRange(i, i + 4).contentEquals(ChunkHeader.tEXT.fourCC) -> {

                    total += IffChunkHeader(
                        chunkIndex = 0,
                        index = i,
                        primaryHeader = ChunkHeader.tEXT,
                        chunkSize = getIntAt(i + 4)
                    )

                }

            }

        }

        return total
    }


}