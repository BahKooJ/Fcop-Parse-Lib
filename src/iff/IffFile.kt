package iff

import iff.chunk.ChunkHeader
import iff.chunk.IffFormatData
import iff.chunk.IffFormatting
import iff.data.FCopMusic
import iff.data.FCopSound
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import java.lang.IndexOutOfBoundsException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.NoSuchElementException

class NoGivenDataException(message: String): Exception(message)
class DataNotLocatedException(message: String): Exception(message)
class InvalidFileSizeException(message: String): Exception(message)
class FileAlreadyExistException(message: String): Exception(message)
class InvalidFileFormatException(message: String): Exception(message)
class DirectoryNotFoundException(message: String): Exception(message)
class FILLsInsideFileException(message: String): Exception(message)

/**
 * Handles everything needed for Future Cop's mission files. Including parsing, exporting, importing and more.
 *
 * @param fileName The name of the mission file, or just any file name
 * @param binaryData The contents of a given mission file. Use [java.io.File] to read the bytes of a file
 */
class IffFile(private var fileName: String, binaryData: ByteArray) {

    companion object {
        /**
         * The amount of data needed to filled, if normal data doesn't cut it, it'll need to be filled using FILL chunks
         */
        const val iffFileSectionSize = 24576

        /**
         * Method used for searching the [data] to find any fourCCs.
         */
        fun createChunkOffsetList(header: ChunkHeader, data: ByteArray): Array<Int> {

            val index: MutableList<Int> = mutableListOf()

            for ((i, byte) in data.withIndex()){
                if (byte == header.fourCC[0]){
                    try {
                        if (data.copyOfRange(i, i + 4).contentEquals(header.fourCC)) {
                            index.add(i)
                        }
                    } catch (e: IndexOutOfBoundsException) {  }
                }

            }
            return index.toTypedArray()
        }

    }

    /**
     * The contents of the mission file. All changes done are applied to this property
     */
    var fileBytes: ByteArray = binaryData
    /**
     * All offsets for chunks, stores a array of [IffChunkHeader]. All exporting and import is done using this.
     * Make sure it is always up-to-date. Any external mutation of [fileBytes] should have [redoIndexing] called
     */
    var allPrimaryIndexes = createAllPrimaryChunkOffsetList()

    /**
     * Recreates the CTRL chunk at the start of the file. After any importing, be sure to recreate the CTRL, or the file
     * might not work properly
     */
    fun recreateCTRL(){

        val ctrlChunk: IffChunkHeader
        try {
            ctrlChunk = allPrimaryIndexes.first { it.primaryHeader == ChunkHeader.CTRL }
        } catch (e: NoSuchElementException) {
            throw DataNotLocatedException("could not find A CTRL chunk")
        }

        var gameData = 0
        var soundData = 0
        var musicData = 0

        var insideSound = false
        var insideMusic = false

        for (chunk in allPrimaryIndexes) {
            if (chunk.primaryHeader == ChunkHeader.SWVR && !insideSound){
                insideSound = true
            } else if (chunk.primaryHeader == ChunkHeader.SWVR && insideSound) {
                if (allPrimaryIndexes[chunk.chunkIndex + 1].primaryHeader == ChunkHeader.MSIC) {
                    insideSound = false
                    insideMusic = true
                }
            }
            when {
                insideSound -> {
                    soundData += chunk.chunkSize
                }
                insideMusic -> {
                    musicData += chunk.chunkSize
                }
                else -> {
                    gameData += chunk.chunkSize
                }
            }
        }
        val dataAfterCTRL = fileBytes.copyOfRange(24,fileBytes.count())
        fileBytes = ChunkHeader.CTRL.fourCC + 24.toBytes32bit() + 0.toBytes32bit() + musicData.toBytes32bit() +
                soundData.toBytes32bit() + gameData.toBytes32bit() + dataAfterCTRL

        ctrlChunk.controlMusicSize = musicData
        ctrlChunk.controlSoundSize = soundData
        ctrlChunk.controlDataSize = gameData
    }

    /**
     * Makes a deep copy off this object
     */
    fun clone(): IffFile {
        return IffFile(fileName, fileBytes)
    }

    // -IMPORTING-

    /**
     * Inserts data, for prototyping.
     */
    fun insert(index: Int, bytes: ByteArray){
        fileBytes = fileBytes.copyOfRange(0,index) + bytes + fileBytes.copyOfRange(index, fileBytes.count())
        redoIndexing()
    }

    /**
     * Writes the contents of [fileBytes] to a File.
     *
     * @param directory The directory on where to file should be written. INCLUDE A "/" AT THE END OF THE DIRECTORY. Default is "output/"
     */
    fun writeToFile(directory: String = "output/") {

        if (directory == "output/"){
            try {
                val file = File("output/$fileName MODIFIED")
                file.writeBytes(fileBytes)
            } catch (e: FileNotFoundException) {
                val file = File("$fileName MODIFIED")
                file.writeBytes(fileBytes)
            }
        } else {
            try {
                val file = File("$directory$fileName")
                file.writeBytes(fileBytes)
            } catch (e: FileNotFoundException) {
                throw DirectoryNotFoundException("Could not find directory : $directory")
            }
        }

    }

    /**
     * Removes a file/data and replaces it with a new file/data. FILLs must be removed before using this method
     *
     * @param chunkHeader What kind of data it is
     * @param dataID The specific ID
     * @param newData The file/data to replace
     *
     * @throws [FILLsInsideFileException] If fills are present
     */
    fun removeGivenDataAndImport(chunkHeader: ChunkHeader, dataID: Int, newData: ByteArray) {

        checkForFills()

        val contents = mutableListOf<Byte>()

        var ignoreChunks = 0
        for (chunk in allPrimaryIndexes) {

            if (chunk.dataDeclaration == chunkHeader && chunk.dataID == dataID) {

                ignoreChunks = getChunkAmount(chunk.dataSize)

                //TODO magic number (what the hell is 32?)
                contents += fileBytes.copyOfRange(chunk.index, chunk.index + 32).toMutableList()
                contents += newData.count().toBytes32bit().toMutableList()
                //TODO magic number (what the hell is 36?)
                contents += fileBytes.copyOfRange(chunk.index + 36, chunk.index + chunk.chunkSize).toMutableList()

                contents += IffFormatting.addChunksToBytes(newData).toMutableList()

            } else if (ignoreChunks == 0){
                contents += fileBytes.copyOfRange(chunk.index, chunk.indexAfterSize).toMutableList()
            } else {
                ignoreChunks--
            }

        }

        fileBytes = contents.toByteArray()
        redoIndexing()

    }

    /**
     * Removes a sound file and replaces it with a new file. FILLs must be removed before using this method.
     *
     * @param soundFileName The name of the sound file (you can find this by looking in FCEditor or a chunk list)
     * @param newFile The new sound file. (wav file, 8-bits, 22050 sample rate)
     *
     * @throws [FILLsInsideFileException] If fills are present
     */
    fun removeGivenSoundAndImport(soundFileName: String, newFile: ByteArray) {

        checkForFills()

        val contents = mutableListOf<Byte>()

        val startingIndex: Int

        try {
            val potentialIndex = allPrimaryIndexes.first { it.fileName == soundFileName }
            startingIndex = potentialIndex.index
        } catch (e: NoSuchElementException) {
            throw DataNotLocatedException("could not find sound with the given sound name: $soundFileName")
        }


        contents += fileBytes.copyOfRange(0, startingIndex).toMutableList()

        val primaryChunksAtFile = allPrimaryIndexes.filter { it.index >= startingIndex }

        var ignoreChunks = 0
        var insideFile = true
        for (chunk in primaryChunksAtFile){

            if (chunk.secondaryHeader == ChunkHeader.SHDR && insideFile) {

                ignoreChunks = getChunkAmount(chunk.dataSize)

                // TODO this creates the header, later when an actual header builder is made this will get replaced, but until then this is fine
                contents += (ChunkHeader.SHOC.fourCC + IffFormatData.SHOCHeaderSoundData.contents + ChunkHeader.SHDR.fourCC +
                        IffFormatData.DataAfterSHDRSound.contents + ChunkHeader.snds.fourCC + IffFormatData.SoundIDData.contents +
                        newFile.count().toBytes32bit() + IffFormatData.HeaderDataAfterSizeSound.contents).toMutableList()

                contents += IffFormatting.addChunksToBytes(newFile).toMutableList()

                insideFile = false

            } else if (ignoreChunks == 0){
                contents += fileBytes.copyOfRange(chunk.index, chunk.indexAfterSize).toMutableList()
            } else {
                ignoreChunks--
            }

        }

        fileBytes = contents.toByteArray()
        redoIndexing()
    }

    /**
     * Removes the music of a mission file, and replaces it with a new file. FILLs must be removed before using this method
     *
     * @param newFile the new music file. (wav files, 8-bites, 14212 sample rate)
     *
     * @throws [FILLsInsideFileException] If fills are present
     */
    fun removeMusicAndImport(newFile: ByteArray) {

        checkForFills()

        val contents = mutableListOf<Byte>()

        val startingIndex: Int

        try {
            val potentialIndex = allPrimaryIndexes.first { it.primaryHeader == ChunkHeader.MSIC }
            startingIndex = potentialIndex.index
        } catch (e: NoSuchElementException) {
            throw DataNotLocatedException("Could not find music inside file")
        }

        contents += fileBytes.copyOfRange(0, startingIndex).toMutableList()
        contents += IffFormatting.addMusicChunksToBytes(newFile).toMutableList()

        fileBytes = contents.toByteArray()
        redoIndexing()

    }

    /**
     * Replaces specific data, this is very small use case, but it can be helpful. The new data must be exactly the same
     * size as the original
     *
     * @param chunkHeader what kind of data it is
     * @param fileID The specific ID
     * @param replaceData The new data
     */
    fun replaceGivenDataByData(chunkHeader: ChunkHeader, fileID: Int, replaceData: ByteArray){
        fileBytes = replaceGivenDataByDataUtil(chunkHeader, fileID, replaceData)
        redoIndexing()
    }

    /**
     * Removes data. FILLs must be removed before using this method
     *
     * @param chunkHeader What kind of data it is
     * @param dataID The specific ID, giving null will remove all [chunkHeader] data
     *
     * @throws [FILLsInsideFileException] If fills are present
     */
    fun removeGivenData(chunkHeader: ChunkHeader, dataID: Int?){

        checkForFills()

        val contents = mutableListOf<Byte>()

        var ignoreChunks = 0
        for (chunk in allPrimaryIndexes) {

            if (chunk.dataDeclaration == chunkHeader && if (dataID != null) { chunk.dataID == dataID } else { true }) {
                ignoreChunks = getChunkAmount(chunk.dataSize)

            } else if (ignoreChunks == 0){
                contents += fileBytes.copyOfRange(chunk.index, chunk.indexAfterSize).toMutableList()
            } else {
                ignoreChunks--
            }
        }

        fileBytes = contents.toByteArray()
        redoIndexing()
    }

    /**
     * Removes all FILL chunks, vital for importing.
     */
    fun removeAllFills(){
        val contents = mutableListOf<Byte>()

        for (chunk in allPrimaryIndexes) {

            if (chunk.primaryHeader != ChunkHeader.FILL) {
                contents += fileBytes.copyOfRange(chunk.index, chunk.indexAfterSize).toMutableList()
            }

        }

        fileBytes = contents.toByteArray()
        redoIndexing()
    }

    /**
     * Adds FILL chunks back into the file, making it readable to Future Cop
     *
     * @throws [FILLsInsideFileException] If fills are present
     */
    fun addFills(){

        checkForFills()

        val contents = mutableListOf<Byte>()

        var section = 0

        for (chunk in allPrimaryIndexes){

            when {
                chunk.chunkSize + section == iffFileSectionSize -> {
                    section = 0
                    contents += fileBytes.copyOfRange(chunk.index, chunk.indexAfterSize).toMutableList()
                }
                chunk.chunkSize + section > iffFileSectionSize -> {
                    val offsetSpace = iffFileSectionSize - section

                    // if the space needed to fill is only 4 bytes, it just uses the header (which is 4 bytes long)
                    if (offsetSpace != 4) {
                        contents += ChunkHeader.FILL.fourCC.toMutableList()
                        contents += offsetSpace.toBytes32bit().toMutableList()
                        // subtracting 8 because the header, and size are both 4 bytes
                        for (i in 1..(offsetSpace) - 8) {
                            contents.add(0x00)
                        }
                    } else {
                        contents += ChunkHeader.FILL.fourCC.toMutableList()
                    }

                    contents += fileBytes.copyOfRange(chunk.index, chunk.indexAfterSize).toMutableList()
                    section = (chunk.chunkSize)
                }
                chunk.primaryHeader == ChunkHeader.SWVR -> {
                    val offsetSpace = iffFileSectionSize - section

                    // if the space needed to fill is only 4 bytes, it just uses the header (which is 4 bytes long)
                    if (offsetSpace != 4) {
                        contents += ChunkHeader.FILL.fourCC.toMutableList()
                        contents += offsetSpace.toBytes32bit().toMutableList()
                        // subtracting 8 because the header, and size are both 4 bytes
                        for (i in 1..(offsetSpace) - 8) {
                            contents.add(0x00)
                        }
                    } else {
                        contents += ChunkHeader.FILL.fourCC.toMutableList()
                    }

                    contents += fileBytes.copyOfRange(chunk.index, chunk.indexAfterSize).toMutableList()
                    section = (chunk.chunkSize)
                }
                else -> {
                    section += (chunk.chunkSize)
                    contents += fileBytes.copyOfRange(chunk.index, chunk.indexAfterSize).toMutableList()
                }
            }
        }

        contents += ChunkHeader.FILL.fourCC.toMutableList()
        val emptyData = iffFileSectionSize - section
        contents += emptyData.toBytes32bit().toMutableList()
        for (i in 1..(emptyData) - 8) {
            contents.add(0)
        }

        fileBytes = contents.toByteArray()
        redoIndexing()
    }

    /**
     * Method for checking FILLs inside a file. This is done to prevent import when FILLs are inside the file.
     * FILLs cannot be present during import because once a file/data size has changed the file will be misaligned
     *
     * @throws [FILLsInsideFileException] If fills are present
     */
    private fun checkForFills() {
        allPrimaryIndexes.forEach {
            if (it.primaryHeader == ChunkHeader.FILL){
                throw FILLsInsideFileException("FILLs must be removed before any importing")
            }
        }
    }

    /**
     * Called in [replaceGivenDataByData].
     */
    private fun replaceGivenDataByDataUtil(chunkHeader: ChunkHeader, dataID: Int, replaceData: ByteArray): ByteArray{

        val includedData = 20

        val contents = mutableListOf<Byte>()

        var index = 0
        var dataChunkAmount = 0
        var locatedFile = false

        for (chunk in allPrimaryIndexes) {

            if (chunk.primaryHeader == ChunkHeader.FILL){
                contents += fileBytes.copyOfRange(chunk.index, chunk.indexAfterSize).toMutableList()
                continue
            }

            if (chunk.dataDeclaration == chunkHeader && chunk.dataID == dataID){

                dataChunkAmount = getChunkAmount(chunk.dataSize)

                if (chunk.dataSize != replaceData.count()) {
                    throw InvalidFileSizeException("""data given to replace is bigger or smaller than the size of the actual data.
                    given data size = ${replaceData.count()} actual data size = ${chunk.dataSize}
                    """)
                }

                locatedFile = true

                contents += fileBytes.copyOfRange(chunk.index, chunk.indexAfterSize).toMutableList()

            } else if (dataChunkAmount != 0){

                contents += fileBytes.copyOfRange(chunk.index, chunk.indexAfterSize).toMutableList()

                for (i in 1..(chunk.chunkSize) - includedData){
                    contents.add(replaceData[index])
                    index++
                }
                dataChunkAmount--

            } else {
                contents += fileBytes.copyOfRange(chunk.index, chunk.indexAfterSize).toMutableList()
            }
        }

        if (!locatedFile){
            throw DataNotLocatedException("could not find the data/file, double check to make sure you have the right ChunkID, and file/data ID")
        }

        return contents.toByteArray()
    }

    // -EXPORTING-

    /**
     * Exports data
     *
     * @param item What kind of data it is, default is [ChunkHeader.ALL], which will export all data
     * @param itemID The specific ID. If set to null will export all files of [item]
     * @param path The path where the files will go. Put a "/" at the end of the path
     */
    fun exportData(item: ChunkHeader = ChunkHeader.ALL, itemID: Int?, path: String) {

        fun isSearchingForItem(header: IffChunkHeader): Boolean {
            return (item == ChunkHeader.ALL && header.secondaryHeader == ChunkHeader.SHDR) || (header.dataDeclaration == item)
        }

        var contents = mutableListOf<Byte>()

        var dataHeader: IffChunkHeader? = null
        var chunkAmount = 0
        var locatedFile = false

        fun writeToFile(){

            val fileName = dataHeader!!.dataDeclaration.toString() + dataHeader!!.dataID.toString() + getFileExtension(dataHeader!!.dataDeclaration!!.toString())

            val file = File(path + fileName)
            file.writeBytes(formatFile(dataHeader!!.dataDeclaration!!, contents.toByteArray()))

            contents = mutableListOf()

        }

        for (header in allPrimaryIndexes) {

            if (locatedFile && chunkAmount == 0){
                break
            }

            if (isSearchingForItem(header) && (itemID == null || header.dataID == itemID)) {

                if (dataHeader != null) { writeToFile() }

                if (itemID != null){ locatedFile = true }

                dataHeader = header
                chunkAmount = getChunkAmount(header.dataSize)

            } else {
                if (header.secondaryHeader == ChunkHeader.SDAT && dataHeader != null && chunkAmount != 0) {
                    //TODO magic number
                    val includedData = 20
                    contents += readBytesByOffset(header.index + includedData, header.indexAfterSize).toMutableList()
                    chunkAmount--
                }
            }

        }

        if (contents.isNotEmpty()){
            writeToFile()
        }

    }

    /**
     * Exports specific data and returns the contents as a [ByteArray]
     *
     * @param item What kind of data it is, cannot be [ChunkHeader.ALL]
     * @param itemID The specific ID, cannot but null
     */
    fun exportDataAsBytes(item: ChunkHeader, itemID: Int): ByteArray {

        val contents = mutableListOf<Byte>()
        var fileId: Int = -1
        var chunkAmount = 0
        var locatedFile = false

        for (chunk in allPrimaryIndexes) {

            if (locatedFile && chunkAmount == 0){
                break
            }

            if (chunk.dataDeclaration == item && chunk.dataID == itemID) {

                locatedFile = true

                fileId = chunk.dataID
                chunkAmount = getChunkAmount(chunk.dataSize)

            } else {
                if (chunk.secondaryHeader == ChunkHeader.SDAT && fileId != -1 && chunkAmount != 0) {
                    //TODO magic number
                    val includedData = 20
                    contents.plusAssign(fileBytes.copyOfRange(chunk.index + includedData, chunk.indexAfterSize).toMutableList())
                    chunkAmount--
                }
            }
        }

        return contents.toByteArray()
    }

    /**
     * Exports a sound file
     *
     * @param givenFileName the name of the sound file. If set to null will export all sound files
     * @param path The path where the files will go. Put a "/" at the end of the path
     */
    fun exportSound(givenFileName: String?, path: String) {

        var contents = mutableListOf<Byte>()

        var fileName = ""
        var chunkAmount = 0
        var locatedFile = false

        fun writeToFile(){
            val string = fileName + getFileExtension("snds")

            val file = File(path + string)
            file.writeBytes(formatFile(ChunkHeader.snds, contents.toByteArray()))

            contents = mutableListOf()
        }

        for (chunk in allPrimaryIndexes){

            if (locatedFile && chunkAmount == 0){
                break
            }

            if (chunk.primaryHeader == ChunkHeader.SWVR && (givenFileName == null || chunk.fileName == givenFileName)) {

                // Because music and snds use the same SWVR chunk, it checks the next chunks header to see if it's music or not
                val possibleHeader = allPrimaryIndexes[chunk.chunkIndex + 1]
                if (possibleHeader.secondaryHeader == ChunkHeader.MSIC) {
                    continue
                }

                if (fileName != "") { writeToFile() }

                if (givenFileName != null){ locatedFile = true }

                fileName = chunk.fileName
                chunkAmount = getChunkAmount(chunk.dataSize)
            } else {
                if (chunk.secondaryHeader == ChunkHeader.SDAT && fileName != "" && chunkAmount != 0) {
                    //TODO magic number
                    val includedData = 20
                    contents.plusAssign(fileBytes.copyOfRange(chunk.index + includedData, chunk.indexAfterSize).toMutableList())
                    chunkAmount--
                }
            }
        }
        if (contents.isNotEmpty()){
            writeToFile()
        }

    }

    /**
     * Exports a specific sound file, and returns the contents as a [ByteArray]
     *
     * @param givenFileName The name of the sound file (you can find this by looking in FCEditor or a chunk list)
     */
    fun exportSoundAsBytes(givenFileName: String): ByteArray {

        val contents = mutableListOf<Byte>()

        var fileName = ""
        var chunkAmount = 0
        var locatedFile = false

        for (chunk in allPrimaryIndexes){

            if (locatedFile && chunkAmount == 0){
                break
            }

            if (chunk.primaryHeader == ChunkHeader.snds && chunk.fileName == givenFileName) {

                // Because music and snds use the same SWVR chunk, it checks the next chunks header to see if it's music or not
                val possibleHeader = allPrimaryIndexes[chunk.chunkIndex + 1]
                if (possibleHeader.secondaryHeader == ChunkHeader.MSIC) {
                    continue
                }

                locatedFile = true

                fileName = chunk.fileName
                chunkAmount = getChunkAmount(chunk.dataSize)

            } else {
                if (chunk.secondaryHeader == ChunkHeader.SDAT && fileName != "" && chunkAmount != 0) {
                    //TODO magic number
                    val includedData = 20
                    contents.plusAssign(fileBytes.copyOfRange(chunk.index + includedData, chunk.indexAfterSize).toMutableList())
                    chunkAmount--
                }
            }
        }
        return try { formatFile(ChunkHeader.snds, contents.toByteArray()) } catch (e: InvalidFileFormatException) {
            contents.toByteArray()
        }

    }

    /**
     * Exports the games music
     *
     * @param path The path where the files will go. Put a "/" at the end of the path
     */
    fun exportMusic(path: String) {

        val contents = mutableListOf<Byte>()

        var musicName = ""

        for (chunk in allPrimaryIndexes) {

            if (chunk.primaryHeader == ChunkHeader.SWVR) {

                val possibleName = allPrimaryIndexes[chunk.chunkIndex + 1]
                if (possibleName.primaryHeader == ChunkHeader.MSIC) {
                    musicName = chunk.fileName
                }

            }

            if (chunk.secondaryHeader == ChunkHeader.MSIC) {
                //TODO magic number
                contents.plusAssign(fileBytes.copyOfRange(chunk.index + 28, chunk.indexAfterSize).toMutableList())
            }

        }

        val string = musicName + getFileExtension("Music")

        val file = File(path + string)
        file.writeBytes(formatFile(ChunkHeader.MSIC, contents.toByteArray()))
    }

    /**
     * Exports the music and returns the contents as a [ByteArray]
     */
    fun exportMusicAsBytes(): ByteArray {

        val contents = mutableListOf<Byte>()

        for (chunk in allPrimaryIndexes) {

            if (chunk.secondaryHeader == ChunkHeader.MSIC) {
                //TODO magic number
                contents.plusAssign(fileBytes.copyOfRange(chunk.index + 28, chunk.indexAfterSize).toMutableList())
            }

        }

        return formatFile(ChunkHeader.MSIC, contents.toByteArray())
    }

    /**
     * Called by various export methods to format the raw contents of a file
     * User by nearly every export method
     */
    private fun formatFile(fileType: ChunkHeader, contents: ByteArray): ByteArray{

        if (fileType == ChunkHeader.snds) {
            return FCopSound(contents, "").formatFileAsBytes()
        } else if (fileType == ChunkHeader.MSIC) {
            return FCopMusic(contents, "").formatFileAsBytes()
        }
        return contents
    }

    /**
     * Utility for writing a file name, called by any export method that writes to a path
     */
    private fun getFileExtension(type: String): String {
        if (type[0] == 'C'){
            return "." + type.drop(1)
        }
        if (type == "snds" || type == "Music"){
            return ".wav"
        }
        return ".$type"
    }

    // -INDEXING-

    /**
     * Method called to make sure [allPrimaryIndexes] is up-to-date, making it so offsets/indexes are not wrong when
     * importing files that are bigger than the original. When modifying [fileBytes] in an external matter, be sure to
     * call this method
     */
    fun redoIndexing(){
        allPrimaryIndexes = createAllPrimaryChunkOffsetList()
    }

    /**
     * For creating a chunk text list
     */
    fun createChunkTextList(){

        val file = File("chunklist.txt")

        var total = ""

        var size = 0
        val chunks = allPrimaryIndexes.count()

        for (header in allPrimaryIndexes){

            size += header.chunkSize

            total += header.chunkIndex
            total += " "
            total += header.index
            total += " "
            total += header.primaryHeader
            total += " "
            total += header.chunkSize
            total += " "
            if (header.secondaryHeader != null) {
                total += header.secondaryHeader
                total += " "
            }
            if (header.dataDeclaration != null) {
                total += header.dataDeclaration
                total += " "
            }
            if (header.dataID != 0) {
                total += header.dataID
                total += " "
            }
            if (header.dataSize != 0) {
                total += header.dataSize
                total += " "
            }
            if (header.fileName != "") {
                total += header.fileName
                total += " "
            }
            if (header.musicLoopNumber != 0) {
                total += header.musicLoopNumber
                total += " "
            }
            if (header.unknownMusicNumber != 0) {
                total += header.unknownMusicNumber
                total += " "
            }
            if (header.controlMusicSize != 0) {
                total += header.controlMusicSize
                total += " "
            }
            if (header.controlSoundSize != 0) {
                total += header.controlSoundSize
                total += " "
            }
            if (header.controlDataSize != 0){
                total += header.controlDataSize
                total += " "
            }

            total += "\n"
        }

        file.writeText("size: $size, chunks: $chunks \n $total")
    }

    /**
     * Method used for create the contents of [allPrimaryIndexes]. Called during init, also called in [redoIndexing]
     */
    private fun createAllPrimaryChunkOffsetList(): Array<IffChunkHeader> {

        val totalIndexes = mutableListOf<Int>()
        val iffChunkHeaders = mutableListOf<IffChunkHeader>()

        totalIndexes += createChunkOffsetList(ChunkHeader.CTRL, fileBytes).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.SHOC, fileBytes).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.FILL, fileBytes).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.SWVR, fileBytes).toMutableList()
        totalIndexes += createChunkOffsetList(ChunkHeader.MSIC, fileBytes).toMutableList()

        totalIndexes.sort()


        var previousIndex = 0
        var previousSize = 0

        var chunkIndex = 0

        for (i in totalIndexes) {

            if (previousIndex + previousSize != i){
                continue
            }

            when {
                readBytesByOffset(i, i + 4).contentEquals(ChunkHeader.SHOC.fourCC) -> {
                    iffChunkHeaders.add(gettingSHOCData(chunkIndex, i))
                    chunkIndex++
                }
                readBytesByOffset(i, i + 4).contentEquals(ChunkHeader.FILL.fourCC) -> {
                    iffChunkHeaders.add(gettingFILLData(chunkIndex, i))
                    chunkIndex++
                }
                readBytesByOffset(i, i + 4).contentEquals(ChunkHeader.SWVR.fourCC) -> {
                    iffChunkHeaders.add(gettingSWVRData(chunkIndex, i))
                    chunkIndex++
                }
                readBytesByOffset(i, i + 4).contentEquals(ChunkHeader.MSIC.fourCC) -> {
                    iffChunkHeaders.add(gettingMSICData(chunkIndex, i))
                    chunkIndex++
                }
                readBytesByOffset(i, i + 4).contentEquals(ChunkHeader.CTRL.fourCC) -> {
                    iffChunkHeaders.add(gettingCTRLData(chunkIndex, i))
                    chunkIndex++
                }
                else -> {
                    iffChunkHeaders.add(IffChunkHeader(chunkIndex, i, ChunkHeader.NULL,0))
                    chunkIndex++
                }
            }

            previousIndex = i
            previousSize = iffChunkHeaders.last().chunkSize

        }

        return iffChunkHeaders.toTypedArray()
    }

    /**
     * Called by [createAllPrimaryChunkOffsetList], this method returns a [IffChunkHeader] with all the data regarding SHOC chunks
     */
    private fun gettingSHOCData(chunkIndex: Int, index : Int): IffChunkHeader {

        //todo way to many magic numbers, but until I make a object for working with headers, this will do

        if (readBytesByOffset(index + 16,index + 20).contentEquals(ChunkHeader.SDAT.fourCC)){

            return IffChunkHeader(
                chunkIndex = chunkIndex,
                index = index,
                primaryHeader = ChunkHeader.SHOC,
                chunkSize = getIntAt(index + 4),
                secondaryHeader = ChunkHeader.SDAT,
            )

        }

        if (readBytesByOffset(index + 16, index + 20).contentEquals(ChunkHeader.SHDR.fourCC)) {

            return IffChunkHeader(
                chunkIndex = chunkIndex,
                index = index,
                primaryHeader = ChunkHeader.SHOC,
                chunkSize = getIntAt(index + 4),
                secondaryHeader = ChunkHeader.SHDR,
                dataDeclaration = ChunkHeader.valueOf(readBytesByOffset(index + 24, index + 28).decodeToString().reversed()),
                dataID = getIntAt(index + 28),
                dataSize = getIntAt(index + 32),
                actReferences = null,
            )

        }

        return IffChunkHeader(
            chunkIndex = chunkIndex,
            index = index,
            primaryHeader = ChunkHeader.SHOC,
            chunkSize = getIntAt(index + 4),
        )

    }

    /**
     * Called by [createAllPrimaryChunkOffsetList], this method returns a [IffChunkHeader] with all the data regarding FILL chunks
     */
    private fun gettingFILLData(chunkIndex: Int, index: Int): IffChunkHeader {

        val size = if (readBytesByOffset(index + 4, index + 8).contentEquals(ChunkHeader.SHOC.fourCC)){
            4
        } else {
            getIntAt(index + 4)
        }

        return IffChunkHeader(
            chunkIndex = chunkIndex,
            index = index,
            primaryHeader = ChunkHeader.FILL,
            chunkSize = size,
        )

    }

    /**
     * Called by [createAllPrimaryChunkOffsetList], this method returns a [IffChunkHeader] with all the data regarding SWVR chunks
     */
    private fun gettingSWVRData(chunkIndex: Int, index: Int): IffChunkHeader {

        if (readBytesByOffset(index + 16, index + 20).contentEquals(ChunkHeader.FILE.fourCC)) {

            return IffChunkHeader(
                chunkIndex = chunkIndex,
                index = index,
                primaryHeader = ChunkHeader.SWVR,
                chunkSize = getIntAt(index + 4),
                secondaryHeader = ChunkHeader.FILE,
                fileName = getSWVRStringName(readBytesByOffset(index + 20, index + 36))
            )

        }

        return IffChunkHeader(
            chunkIndex = chunkIndex,
            index = index,
            primaryHeader = ChunkHeader.SWVR,
            chunkSize = getIntAt(index + 4)
        )

    }

    /**
     * Called by [createAllPrimaryChunkOffsetList], this method returns a [IffChunkHeader] with all the data regarding MSIC chunks
     */
    private fun gettingMSICData(chunkIndex: Int, index: Int): IffChunkHeader {
        return IffChunkHeader(
            chunkIndex = chunkIndex,
            index = index,
            primaryHeader = ChunkHeader.MSIC,
            chunkSize = getIntAt(index + 4),
            secondaryHeader = ChunkHeader.MSIC,
            musicLoopNumber = getIntAt(index + 20),
            unknownMusicNumber = getIntAt(index + 24)
        )
    }

    /**
     * Called by [createAllPrimaryChunkOffsetList], this method returns a [IffChunkHeader] with all the data regarding CTRL chunks
     */
    private fun gettingCTRLData(chunkIndex: Int, index: Int): IffChunkHeader {
        return IffChunkHeader(
            chunkIndex = chunkIndex,
            index = index,
            primaryHeader = ChunkHeader.CTRL,
            chunkSize = getIntAt(index + 4),
            controlMusicSize = getIntAt(index + 12),
            controlSoundSize = getIntAt(index + 16),
            controlDataSize = getIntAt(index + 20)
            )
    }

    /**
     * Method used to get the name of a sound or music file. Called by [gettingSWVRData]
     */
    private fun getSWVRStringName(binaryString: ByteArray): String {
        var fileName = ""

        for (byte in binaryString){
            if (byte != 0.toByte()){
                fileName += byteArrayOf(byte).decodeToString()
            } else {
                break
            }
        }

        return fileName
    }


    // -UTILS-

    /**
     * Given a size, this method finds the total amount of chunks present in a file size.
     * Called by most export and import methods
     */
    private fun getChunkAmount(size: Int, chunkSize: Int = 4096): Int{

        var total = (size / (chunkSize - 20))
        if (size % (chunkSize - 20) != 0 ){ total++ }
        return total
    }

    /**
     * Basically [copyOfRange] but under a different name
     */
    private fun readBytesByOffset(starting: Int, ending: Int = fileBytes.size): ByteArray  {
        return fileBytes.copyOfRange(starting,ending)
    }

    /**
     * Method used for getting a 32 bit [Int] from a [ByteArray], default is [fileBytes]
     */
    private fun getIntAt(inx: Int, data: ByteArray = fileBytes): Int {

        val bytes = data.copyOfRange(inx,inx + 4)

        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() and 0xFF shl 8 * i)
        }
        return result
    }

}

/**
 * Object for storing information about Future Cop's IFF chunks
 */
data class IffChunkHeader(

    /**
     * Chunk index is the index of item in an array, UNRELATED TO THE FILE
     */
    val chunkIndex: Int,
    /**
     * Index or offset of the chunk
     */
    val index: Int,
    /**
     * The fourCC the chunk starts with
     */
    val primaryHeader: ChunkHeader,
    /**
     * The size of the chunk
     */
    val chunkSize: Int,
    /**
     * The fourCC describing what the chunk is
     */
    val secondaryHeader: ChunkHeader? = null,
    /**
     * If the [secondaryHeader] is [ChunkHeader.SHDR], then this is the fourCC for what data/file is stored
     */
    val dataDeclaration: ChunkHeader? = null,
    /**
     * The ID of the [dataDeclaration]
     */
    val dataID: Int = 0,
    /**
     * The size of [dataDeclaration]
     */
    val dataSize: Int = 0,
    /**
     * Refereces to Cacts in the header (unused until more is learned about Cacts)
     */
    val actReferences: Array<Int>? = null,
    /**
     * File name, if it has one
     */
    val fileName: String = "",

    /**
     * Number relating to music files
     */
    val musicLoopNumber: Int = 0,
    val unknownMusicNumber: Int = 0,

    // Used only by CTRL chunks
    var controlMusicSize: Int = 0,
    var controlDataSize: Int = 0,
    var controlSoundSize: Int = 0
) {
    val indexAfterSize: Int
        get() = index + chunkSize

}

/**
 * Extension for converting a Int, into a [ByteArray]
 */
fun Int.toBytes32bit(): ByteArray{
    return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(this).array()
}

/**
 * Extension for converting a Short, into a [ByteArray]
 */
fun Short.toBytes16bit(): ByteArray{
    return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(this).array()
}

