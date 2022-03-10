package iff.data

import java.nio.ByteBuffer
import java.nio.ByteOrder


open class FCopData(var bytes: ByteArray, id: Int) {

    var id = id
    protected set

    protected fun getIntAt(inx: Int, data: ByteArray = bytes): Int {

        val bytes = data.copyOfRange(inx,inx + 4)

        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() and 0xFF shl 8 * i)
        }
        return result
    }

    protected fun getUShortAt(inx: Int, data: ByteArray = bytes): Int {

        val bytes = data.copyOfRange(inx,inx + 2)

        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() and 0xFF shl 8 * i)
        }
        return result
    }

    protected fun getShortAt(inx: Int, data: ByteArray = bytes): Int {

        val bytes = data.copyOfRange(inx,inx + 2)

        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()[0].toInt()

    }

}