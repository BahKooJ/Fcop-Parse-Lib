package iff.data

import iff.chunk.IffChunkHeader
import iff.toBytes16bit
import iff.toBytes32bit
import java.io.File


class FCopCnet(bytes: ByteArray, id: Int, dataHeader: IffChunkHeader): FCopData(bytes, id, dataHeader) {

    companion object {
        const val numberOfNodesOffset: Int = 14
        const val startOfNodesOffset: Int = 16
        const val nodeSize: Int = 12
    }

    var nodes = parseNodes()

    fun createTextFile() {

        var total = ""

        for (node in nodes) {

            total += node.index.toString() + " : "

            total += node.nextPointIndex.toString() + " "
            total += node.x.toString() + " "
            total += node.y.toString() + " "
            total += node.isStartingPoint.toString() + " "

            total += ": "

            total += node.fullData[0]
            total += " "
            total += node.fullData[1]
            total += " "
            total += node.fullData[2]
            total += " "
            total += node.fullData[3]
            total += " "

            total += "\n"

        }

        File("output/netNodeList$id.txt").writeText(total)

    }

    fun readNodes() {

        var total = bytes.copyOfRange(0,numberOfNodesOffset)

        total += nodes.count().toShort().toBytes16bit()

        for (node in nodes) {

            if (node.nextPointIndex != null) {

                total += node.fullData.copyOfRange(0, 2)
                total += ((node.nextPointIndex!! * 64) + 63).toShort().toBytes16bit()

            } else {

                total += node.fullData.copyOfRange(0, 4)

            }


            total += byteArrayOf(0xC0.toByte(), 0xFF.toByte())
            total += node.x.toShort().toBytes16bit()
            total += node.y.toShort().toBytes16bit()
            total += if (node.isStartingPoint) { byteArrayOf(1,0) } else { byteArrayOf(0,0) }

        }

        bytes = total

        changeSize()

    }

    private fun changeSize() {
        bytes = bytes.copyOfRange(0,4) + bytes.count().toBytes32bit() + bytes.copyOfRange(8,bytes.count())
    }

    private fun parseNodes(): MutableList<CnetNode> {

        val total = mutableListOf<CnetNode>()

        for ((pointIndex, index) in (startOfNodesOffset until bytes.count() step nodeSize).withIndex()) {

            val possibleNextPoint: Double = (getShortAt(index + 2) - 63.0) / 64.0
            var point: Int? = null

            if (possibleNextPoint % 1.0 == 0.0) {
                point = possibleNextPoint.toInt()
            }

            total += CnetNode(
                index = pointIndex,
                nextPointIndex = point,
                x = getShortAt(index + 6),
                y = getShortAt(index + 8),
                isStartingPoint = getShortAt(index + 10) == 1,
                fullData = bytes.copyOfRange(index, index + 12)

            )

        }

        return total

    }

}

data class CnetNode(
    var index: Int,
    var nextPointIndex: Int?,
    var x: Int,
    var y: Int,
    var isStartingPoint: Boolean,
    var fullData: ByteArray
)