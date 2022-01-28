package iff.chunk

import iff.toBytes32bit
import java.lang.Exception

class ChunkAlreadyInsideException(message: String): Exception(message)

/**
 * Used for formatting data to conform with Future Cop's IFF format
 */
class IffFormatting {

    companion object{

        const val fourKB = 4096
        const val musicChunkMax = 24540
        const val shocHeaderSize = 20
        const val msicHeaderSize = 28
        const val msicLoopNumberIncrease = 65536

        fun createBasicHeader(chunkID: ChunkHeader, id: Int, type: Int, size: Int, ref1: Int, ref2: Int, ref3: Int): ByteArray {

            return ChunkHeader.SHOC.fourCC + 60.toBytes32bit() + IffFormatData.SHOCDataAfterSize.contents + ChunkHeader.SHDR.fourCC +
                    type.toBytes32bit() + chunkID.fourCC + id.toBytes32bit() + size.toBytes32bit() +
                    ref1.toBytes32bit() + ref2.toBytes32bit() + ref3.toBytes32bit() + 1.toBytes32bit() +
                    1.toBytes32bit() + byteArrayOf(0x00, 0x00, 0x43, 0x4F)


        }

        /**
         * Adds chunks to [data] and returns the result
         *
         * @param[data] The data to be given chunks
         * @throws[ChunkAlreadyInsideException] If chunks are already present in the data
         */
        fun addChunksToBytes(data: ByteArray): ByteArray {
            val contents = mutableListOf<Byte>()

            if (data.copyOfRange(0,4).contentEquals(ChunkHeader.SHOC.fourCC)) {
                throw ChunkAlreadyInsideException("data already has chunks inside")
            }

            var size = 0
            var chunkData = mutableListOf<Byte>()
            for (byte in data){
                if (size == 4076) {
                    val header = ChunkHeader.SHOC.fourCC + fourKB.toBytes32bit() +
                            IffFormatData.SHOCDataAfterSize.contents + ChunkHeader.SDAT.fourCC
                    contents += header.toMutableList() + chunkData
                    size = 0
                    chunkData = mutableListOf()
                }
                chunkData += byte
                size++
            }

            if (chunkData.isNotEmpty()) {
                val header = ChunkHeader.SHOC.fourCC + (chunkData.count() + shocHeaderSize).toBytes32bit() +
                        IffFormatData.SHOCDataAfterSize.contents + ChunkHeader.SDAT.fourCC
                contents += header.toMutableList() + chunkData
            }
            return contents.toByteArray()
        }

        /**
         * Adds music chunks(MSIC) to [data] and returns the result
         *
         * @param[data] The data to be given music chunks
         * @throws[ChunkAlreadyInsideException] If chunks are already present in the data
         */
        fun addMusicChunksToBytes(data: ByteArray): ByteArray {
            val contents = mutableListOf<Byte>()

            if (data.copyOfRange(0,4).contentEquals(ChunkHeader.MSIC.fourCC)) {
                throw ChunkAlreadyInsideException("data already has chunks inside")
            }

            val chunkAmount = getChunkAmount(data.count(), musicChunkMax)

            var size = 0
            var chunkData = mutableListOf<Byte>()
            var loopNumber = chunkAmount
            for (byte in data){
                if (size == musicChunkMax - msicHeaderSize) {
                    val header = ChunkHeader.MSIC.fourCC + musicChunkMax.toBytes32bit() +
                            IffFormatData.SHOCDataAfterSize.contents + ChunkHeader.MSIC.fourCC + loopNumber.toBytes32bit() +
                            12256.toBytes32bit()
                    contents += header.toMutableList() + chunkData
                    size = 0
                    chunkData = mutableListOf()
                    loopNumber += msicLoopNumberIncrease
                }
                chunkData += byte
                size++
            }

            if (chunkData.isNotEmpty()) {
                val header = ChunkHeader.MSIC.fourCC + (chunkData.count() + msicHeaderSize).toBytes32bit() +
                        IffFormatData.SHOCDataAfterSize.contents + ChunkHeader.MSIC.fourCC + loopNumber.toBytes32bit() +
                        12256.toBytes32bit()
                contents += header.toMutableList() + chunkData
            }
            return contents.toByteArray()
        }

        private fun getChunkAmount(size: Int, chunkSize: Int = fourKB): Int{

            var total = (size / (chunkSize - 20))
            if (size % (chunkSize - 20) != 0 ){ total++ }
            return total
        }
    }
}