package no.nav.sokos.spk.mottak.domain

enum class FileState {
    AVV, // Avvist
    GOD, //Godkjent
    INN, // Innlest
    OPR, //Opprettet
    RET, //Returnert
    KTR // Knyttet til retur
}