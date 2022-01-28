package iff.data

import iff.NoGivenDataException
import iff.toBytes32bit
import java.io.File
import java.io.FileNotFoundException

//FIXME OLD BAD CODE
class FCopCptc(cptcFileName: String? = null, cptcBinaryData: ByteArray? = null) {

    init {
        if (cptcBinaryData == null && cptcFileName == null){
            throw NoGivenDataException("Both constructors are null and have no data to read. Please give either a file name, or a byte array")
        }
    }

    private val cptcFile: File? = if (cptcFileName != null) { File(cptcFileName) } else { null }
    var cptcBytes: ByteArray = cptcBinaryData ?: cptcFile!!.readBytes()

    val width = getIntAt(16)
    val height = getIntAt(20)

    val boarders = getIntAt(36)

    var layout = createLayoutMap()

    fun createLayoutTextList() {

        val file: File = if (cptcFile != null){
            File(cptcFile.path + "layoutList.txt")
        } else {
            try {
                File("output/layoutList.text")
            } catch (e: FileNotFoundException) {
                File("layoutList.text")
            }
        }

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

        cptcBytes = cptcBytes.copyOfRange(0,16) +
                layout.values.first().count().toBytes32bit() +
                layout.count().toBytes32bit() + cptcBytes.copyOfRange(24,48) + total

        cptcBytes = cptcBytes.copyOfRange(0,4) + cptcBytes.count().toBytes32bit() + cptcBytes.copyOfRange(8,cptcBytes.count())

    }

    fun createLayoutMap(): MutableMap<Int, MutableList<Int>> {

        val total = mutableMapOf<Int, MutableList<Int>>()

        val offsetStart = 48

        val data = (cptcBytes.count() - offsetStart) / 4

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

    private fun getIntAt(inx: Int, data: ByteArray = cptcBytes): Int {

        val bytes = data.copyOfRange(inx,inx + 4)

        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() and 0xFF shl 8 * i)
        }
        return result
    }
}