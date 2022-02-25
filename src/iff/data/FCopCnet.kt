package iff.data

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN
import java.nio.ShortBuffer


class FCopCnet(var bytes: ByteArray) {

    companion object {
        const val numberOfNodesOffset: Int = 14
        const val startOfNodesOffset: Int = 16
    }

    var nodes = readNodes()

    fun createTextFile() {

        var total = ""

        for (node in nodes) {

            for (number in node) {
                total += number.toString()
                total += " "
            }

            total += "\n"

        }

        File("netNodeList.txt").writeText(total)

    }

    fun readNodes(): List<List<Int>> {

        val total = mutableListOf<List<Int>>()

        for (index in 16 until bytes.count() step 12) {

            val currentList = mutableListOf<Int>()

//            currentList += getUShortAt(index)
//            currentList += getUShortAt(index + 2)
//            currentList += getUShortAt(index + 4)
//            currentList += getUShortAt(index + 6)
//            currentList += getUShortAt(index + 8)
//            currentList += getUShortAt(index + 10)

            currentList += getShortAt(index)
            currentList += getShortAt(index + 2)
            currentList += getShortAt(index + 4)
            currentList += getShortAt(index + 6)
            currentList += getShortAt(index + 8)
            currentList += getShortAt(index + 10)

//            currentList += getIntAt(index)
//            currentList += getIntAt(index + 4)
//            currentList += getIntAt(index + 8)


            total += currentList

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