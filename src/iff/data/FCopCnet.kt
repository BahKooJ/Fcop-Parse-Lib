package iff.data

import iff.toBytes16bit
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN


class FCopCnet(var bytes: ByteArray) {

    companion object {
        const val numberOfNodesOffset: Int = 14
        const val startOfNodesOffset: Int = 16
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

        File("netNodeList.txt").writeText(total)

    }

    fun readNodes() {

        var total = bytes.copyOfRange(0,16)

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

    }

    private fun parseNodes(): MutableList<CnetNode> {

        val total = mutableListOf<CnetNode>()

        for ((pointIndex, index) in (16 until bytes.count() step 12).withIndex()) {

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

    private fun getIntAt(inx: Int, data: ByteArray = bytes): Int {

        val bytes = data.copyOfRange(inx,inx + 4)

        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() and 0xFF shl 8 * i)
        }
        return result
    }

    private fun getShortAt(inx: Int, data: ByteArray = bytes): Int {

        val bytes = data.copyOfRange(inx,inx + 2)

        return ByteBuffer.wrap(bytes).order(LITTLE_ENDIAN).asShortBuffer()[0].toInt()

    }

    private fun getUShortAt(inx: Int, data: ByteArray = bytes): Int {

        val bytes = data.copyOfRange(inx,inx + 2)

        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() and 0xFF shl 8 * i)
        }
        return result
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