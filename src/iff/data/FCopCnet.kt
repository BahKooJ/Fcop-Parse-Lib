package iff.data

import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN
import java.nio.ShortBuffer


class FCopCnet(var bytes: ByteArray) {

    companion object {
        const val numberOfNodesOffset: Int = 14
        const val startOfNodesOffset: Int = 16
    }

    var nodes = readNodes()

    fun readNodes(): List<List<Int>> {

        val total = mutableListOf<List<Int>>()

        for (index in 16 until bytes.count() step 12) {

            val currentList = mutableListOf<Int>()

            currentList += getShortAt(index)
            currentList += getShortAt(index + 2)
            currentList += getShortAt(index + 4)
            currentList += getShortAt(index + 6)
            currentList += getShortAt(index + 8)
            currentList += getShortAt(index + 10)

            total += currentList

        }

        return total

    }

    private fun getShortAt(inx: Int, data: ByteArray = bytes): Int {

        val bytes = data.copyOfRange(inx,inx + 2)

        return ByteBuffer.wrap(bytes).order(LITTLE_ENDIAN).asShortBuffer()[0].toInt()

    }

}