package iff.data

import iff.InvalidFileFormatException
import iff.NoGivenDataException
import iff.chunk.ChunkHeader
import iff.toBytes16bit
import iff.toBytes32bit
import java.io.File

/**
 * Object for working with Future Cop's sound files
 */
class FCopSound(bytes: ByteArray, val soundFileName: String): FCopData(bytes, -1) {

    companion object {
        const val sndsSampleRate = 22050
    }

    /**
     * Formats the raw sound data to a wav file
     *
     * @throws[InvalidFileFormatException] if the file is already formatted
     */
    fun formatFileAsBytes(): ByteArray{

        if (bytes.copyOfRange(0,4).contentEquals(ChunkHeader.RIFF.fourCC)){
            throw InvalidFileFormatException("this file is already formatted")
        }

        val sampleRate: Int = sndsSampleRate

        val bitsPerSample: Short = 8
        val totalSize: Int = bytes.count() + 40
        val subChunkSize: Int = 16
        val audioFormat: Short = 1
        val channels: Short = 1

        val byteRate: Int = sampleRate * 1 * 8 / 8
        val blockAlign: Short = 1 * 8 / 8

        val contents = mutableListOf<Byte>()

        contents += FileFormatData.RIFF.contents.toMutableList()
        contents += (totalSize).toBytes32bit().toMutableList()
        contents += FileFormatData.WaveFormatHeader.contents.toMutableList()
        contents += (subChunkSize).toBytes32bit().toMutableList()
        contents += (audioFormat).toBytes16bit().toMutableList()
        contents += (channels).toBytes16bit().toMutableList()
        contents += (sampleRate).toBytes32bit().toMutableList()
        contents += (byteRate).toBytes32bit().toMutableList() // ByteRate
        contents += (blockAlign).toBytes16bit().toMutableList() // BlockAlign
        contents += (bitsPerSample).toBytes16bit().toMutableList()
        contents += FileFormatData.Wavedata.contents.toMutableList()

        return contents.toByteArray() + bytes
    }

    /**
     * Removes the wav formatting of a sound file.
     *
     * @throws[InvalidFileFormatException] If the file is not formatted or is not a wav file
     */
    fun removeFormattingAsBytes(): ByteArray{
        if (!bytes.copyOfRange(0,4).contentEquals(ChunkHeader.RIFF.fourCC)){
            throw InvalidFileFormatException("this file is not formatted")
        }

        return bytes.copyOfRange(40,bytes.count())
    }

}