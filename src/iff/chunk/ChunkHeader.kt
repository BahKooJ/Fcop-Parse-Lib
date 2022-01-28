package iff.chunk

/**
 * Contains all known chunk ascii headers, with a [ByteArray] as a raw value([fourCC]) for comparing.
 * Note that the ByteArray Stores the values backwards because that's how they appear in the file.
 */
enum class ChunkHeader(val fourCC: ByteArray) {

    ALL(byteArrayOf(0)),

    //primary headers
    CTRL(byteArrayOf(76,82,84,67)),
    SHOC(byteArrayOf(67,79,72,83)),
    FILL(byteArrayOf(76,76,73,70)),
    MSIC(byteArrayOf(67,73,83,77)),
    SWVR(byteArrayOf(82,86,87,83)),

    //secondary headers
    SDAT(byteArrayOf(84,65,68,83)),
    SHDR(byteArrayOf(82,68,72,83)),
    FILE(byteArrayOf(69,76,73,70)),

    //content headers
    NULL(byteArrayOf(76,76,85,78)),
    RPNS(byteArrayOf(83,78,80,82)),
    Cwav(byteArrayOf(118,97,119,67)),
    Cobj(byteArrayOf(106,98,111,67)),
    snds(byteArrayOf(115,100,110,115)),
    Cctr(byteArrayOf(114,116,99,67)),
    Cact(byteArrayOf(116,99,97,67)),
    Csac(byteArrayOf(99,97,115,67)),
    Cbmp(byteArrayOf(112,109,98,67)),
    Cnet(byteArrayOf(116,101,110,67)),
    Cfun(byteArrayOf(110,117,102,67)),
    Ctil(byteArrayOf(108,105,116,67)),
    Cptc(byteArrayOf(99,116,112,67)),
    Ctos(byteArrayOf(115,111,116,67)),
    Cshd(byteArrayOf(100,104,115,67)),
    Cfnt(byteArrayOf(116,110,102,67)),
    Cdcs(byteArrayOf(115,99,100,67)),
    canm(byteArrayOf(109,110,97,99)),


    //Nested chunks
    PTNF(byteArrayOf(70,78,84,80)),
    RIFF(byteArrayOf(82,73,70,70)),

    //Cobj
    fourDGI(byteArrayOf(73,71,68,52)),
    threeDTL(byteArrayOf(76,84,68,51)),
    threeDQL(byteArrayOf(76,81,68,51)),
    threeDRF(byteArrayOf(70,82,68,51)),
    fourDVL(byteArrayOf(76,86,68,52)),
    fourDNL(byteArrayOf(76,78,68,52)),
    threeDRL(byteArrayOf(76,82,68,51)),
    threeDBB(byteArrayOf(66,66,68,51)),
    threeDHY(byteArrayOf(89,72,68,51)),
    threeDMI(byteArrayOf(73,77,68,51)),
    threeDHS(byteArrayOf(83,72,68,51)),
    AnmD(byteArrayOf(68,109,110,65)),

    //Cfun
    tFUN(byteArrayOf(78,85,70,116)),
    tEXT(byteArrayOf(84,88,69,116)),

    //Cbmp
    LCCB(byteArrayOf(66,67,67,76)),
    LkUp(byteArrayOf(112,85,107,76)),
    PX16(byteArrayOf(54,49,88,80)),
    PLUT(byteArrayOf(84,85,76,80)),

    //Csac
    tACT(byteArrayOf(84,67,65,116)),
    aRSL(byteArrayOf(76,83,82,97)),
    tSAC(byteArrayOf(67,65,83,116))
}