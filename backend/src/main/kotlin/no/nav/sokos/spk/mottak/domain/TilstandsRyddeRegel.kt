package no.nav.sokos.spk.mottak.domain

import java.time.LocalDateTime

interface TilstandsRyddeRegel {
    val versjon: String
    val oppbevaringMaaneder: Int
    val alderspredikat: String

    fun grensetidspunkt(db2Tidspunkt: LocalDateTime): LocalDateTime

    fun omfatter(datoEndret: LocalDateTime, grensetidspunkt: LocalDateTime): Boolean =
        datoEndret.isBefore(grensetidspunkt)
}

class SeksMaanedersRegel(
    override val oppbevaringMaaneder: Int = 6,
) : TilstandsRyddeRegel {
    init {
        require(oppbevaringMaaneder >= 6) { "Oppbevaring må være minst seks kalendermåneder" }
    }

    override val versjon = "seks-maaneder-v1"
    override val alderspredikat = "DATO_ENDRET < ?"

    override fun grensetidspunkt(db2Tidspunkt: LocalDateTime): LocalDateTime {
        require(db2Tidspunkt.nano % 1000 == 0) { "Db2-tidspunkt må ha mikrosekundpresisjon" }
        return db2Tidspunkt.minusMonths(oppbevaringMaaneder.toLong())
    }
}
