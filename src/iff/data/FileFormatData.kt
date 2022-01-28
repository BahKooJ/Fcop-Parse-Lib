package iff.data

enum class FileFormatData(val contents: ByteArray){
    WaveFormatHeader(byteArrayOf(87,65,86,69,102,109,116,32)),
    RIFF(byteArrayOf(82,73,70,70)),
    Wavedata(byteArrayOf(100,97,116,97)),
    BitmapHeader(byteArrayOf(66,77))
}