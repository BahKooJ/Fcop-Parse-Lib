package iff.data

import iff.chunk.IffChunkHeader
import iff.toBytes32bit
import java.io.File

class FCopCptc(bytes: ByteArray, id: Int, dataHeader: IffChunkHeader): FCopData(bytes, id, dataHeader) {

    val width = getIntAt(16)
    val height = getIntAt(20)

    val boarders = getIntAt(36)

    var layout = createLayoutMap()

    fun createLayoutTextList() {

        val file = File("")

        var total:String = ""


        for (i in layout){

            val array = i.value

            if (i.key > 9) {
                total += i.key.toString() + ": "
            } else {
                total += i.key.toString() + ":  "
            }

            for (item in array){
                if (item > 9){
                    total += item.toString() + " "
                } else {
                    total += item.toString() + "  "
                }
            }
            total += "\n"
        }


        file.writeText(total)
    }

    fun readLayout() {

        val total = mutableListOf<Byte>()

        for (row in layout) {

            for (gridTile in row.value) {
                total += (gridTile * 4).toBytes32bit().toMutableList()
            }

        }

        bytes = bytes.copyOfRange(0,16) +
                layout.values.first().count().toBytes32bit() +
                layout.count().toBytes32bit() + bytes.copyOfRange(24,48) + total

        bytes = bytes.copyOfRange(0,4) + bytes.count().toBytes32bit() + bytes.copyOfRange(8,bytes.count())

    }

    fun createLayoutMap(): MutableMap<Int, MutableList<Int>> {

        val total = mutableMapOf<Int, MutableList<Int>>()

        val offsetStart = 48

        val data = (bytes.count() - offsetStart) / 4

        var widthTiles = mutableListOf<Int>()
        var widthPosition = 1
        var tilePosition = 1

        for (num in 0 until data){
            val numOffset = num * 4

            if (tilePosition != width){
                widthTiles.add(getIntAt(numOffset + offsetStart) / 4)
            } else {
                widthTiles.add(getIntAt(numOffset + offsetStart) / 4)
                total[widthPosition] = widthTiles

                widthTiles = mutableListOf()
                widthPosition++
                tilePosition = 0
            }

            tilePosition++

        }
        return total
    }

}