package iff

import iff.chunk.ChunkHeader
import iff.chunk.IffFormatting
import iff.data.*
import java.io.File

class FCopMissionFile(private var path: String) {

    val iffFile: IffFile

    lateinit var RPNS: FCopData
    lateinit var Cshd: FCopData
    var soundEffects: MutableMap<Int, FCopData> = mutableMapOf()
    lateinit var Ctos: FCopData
    lateinit var mapLayout: FCopCptc
    var tiles: MutableMap<Int, FCopCtil> = mutableMapOf()
    lateinit var Cfun: FCopCfun
    var paths: MutableMap<Int, FCopCnet> = mutableMapOf()
    var bitmaps: MutableMap<Int, FCopBitmap> = mutableMapOf()
    var Cdcs: MutableMap<Int, FCopData> = mutableMapOf()
    var objects: MutableMap<Int, FCopData> = mutableMapOf()
    var actions: MutableMap<Int, FCopCact> = mutableMapOf()
    lateinit var Cctr: FCopData
    var sounds: MutableMap<String, FCopSound> = mutableMapOf()
    lateinit var music: FCopMusic

    init {

        val file = File(path)
        iffFile = IffFile(file.name,file.readBytes())

        for (chunk in iffFile.allPrimaryIndexes) {

            if (chunk.primaryHeader == ChunkHeader.SWVR) {

                // Because music and snds use the same SWVR chunk, it checks the next chunks header to see if it's music or not
                val possibleHeader = iffFile.allPrimaryIndexes[chunk.chunkIndex + 1]
                if (possibleHeader.secondaryHeader == ChunkHeader.MSIC) {
                    music = FCopMusic(iffFile.exportMusicAsBytes(), chunk)
                    continue
                }

                sounds += Pair(chunk.fileName,FCopSound(iffFile.exportSoundAsBytes(chunk.fileName),chunk))

            }

            if (chunk.dataDeclaration == null) {
                continue
            }

            when(chunk.dataDeclaration) {

                ChunkHeader.RPNS -> RPNS = FCopData(iffFile.exportDataAsBytes(chunk.dataDeclaration,chunk.dataID),chunk.dataID,chunk)
                ChunkHeader.Cshd -> Cshd = FCopData(iffFile.exportDataAsBytes(chunk.dataDeclaration,chunk.dataID),chunk.dataID,chunk)
                ChunkHeader.Cwav -> soundEffects += Pair(chunk.dataID,FCopData(iffFile.exportDataAsBytes(chunk.dataDeclaration,chunk.dataID),chunk.dataID,chunk))
                ChunkHeader.Ctos -> Ctos = FCopData(iffFile.exportDataAsBytes(chunk.dataDeclaration,chunk.dataID),chunk.dataID,chunk)
                ChunkHeader.Cptc -> mapLayout = FCopCptc(iffFile.exportDataAsBytes(chunk.dataDeclaration,chunk.dataID),chunk.dataID,chunk)
                ChunkHeader.Ctil -> tiles += Pair(chunk.dataID,FCopCtil(iffFile.exportDataAsBytes(chunk.dataDeclaration,chunk.dataID),chunk.dataID,chunk))
                ChunkHeader.Cfun -> Cfun = FCopCfun(iffFile.exportDataAsBytes(chunk.dataDeclaration,chunk.dataID),chunk.dataID,chunk)
                ChunkHeader.Cnet -> paths += Pair(chunk.dataID,FCopCnet(iffFile.exportDataAsBytes(chunk.dataDeclaration,chunk.dataID),chunk.dataID,chunk))
                ChunkHeader.Cbmp -> bitmaps += Pair(chunk.dataID,FCopBitmap(iffFile.exportDataAsBytes(chunk.dataDeclaration,chunk.dataID),chunk.dataID,chunk))
                ChunkHeader.Cdcs -> Cdcs += Pair(chunk.dataID,FCopData(iffFile.exportDataAsBytes(chunk.dataDeclaration,chunk.dataID),chunk.dataID,chunk))
                ChunkHeader.Cobj -> objects += Pair(chunk.dataID,FCopData(iffFile.exportDataAsBytes(chunk.dataDeclaration,chunk.dataID),chunk.dataID,chunk))
                ChunkHeader.Cact -> actions += Pair(chunk.dataID,FCopCact(iffFile.exportDataAsBytes(chunk.dataDeclaration,chunk.dataID),chunk))
                ChunkHeader.Csac -> actions += Pair(chunk.dataID,FCopCact(iffFile.exportDataAsBytes(chunk.dataDeclaration,chunk.dataID),chunk))
                ChunkHeader.Cctr -> Cctr = FCopData(iffFile.exportDataAsBytes(chunk.dataDeclaration,chunk.dataID),chunk.dataID,chunk)
                ChunkHeader.snds -> continue
                else -> error(chunk)
            }

        }

    }

    fun compile() {

        var dataData = byteArrayOf()
        var dataSound = byteArrayOf()
        var dataMusic = byteArrayOf()

        val allData = mutableListOf<FCopData>()

        allData += RPNS
        allData += Cshd

        for (sound in soundEffects) {
            allData += sound.value
        }

        allData += Ctos
        allData += mapLayout

        for (tile in tiles) {
            allData += tile.value
        }

        allData += Cfun

        for (path in paths) {
            allData += path.value
        }

        for (bitmap in bitmaps) {
            allData += bitmap.value
        }

        for (item in Cdcs) {
            allData += item.value
        }

        for (obj in objects) {
            allData += obj.value
        }

        for (action in actions) {
            allData += action.value
        }

        allData += Cctr

        for (data in allData) {

            val chunkedData = IffFormatting.addChunksToBytes(data.bytes)
            dataData += data.dataHeader.compile(newDataSize = data.bytes.count())
            dataData += chunkedData

        }

        for (sound in sounds) {

            dataSound += sound.value.dataHeader.compile()
            dataSound += IffFormatting.createSoundHeader(sound.value.bytes.count())
            dataSound += IffFormatting.addChunksToBytes(sound.value.bytes)

        }

        dataMusic += music.dataHeader.compile()
        dataMusic += IffFormatting.addMusicChunksToBytes(music.bytes)

        // Since there are no fills, we can't accurately create a CTRL, but we still need to fill up the data so the IffFile can
        val CTRL: ByteArray = ChunkHeader.CTRL.fourCC + 24.toBytes32bit() + 0.toBytes32bit() + 0.toBytes32bit() + 0.toBytes32bit() + 0.toBytes32bit()

        iffFile.fileBytes = CTRL + dataData + dataSound + dataMusic
        iffFile.redoIndexing()
        iffFile.addFills()
        iffFile.recreateCTRL()

    }


}