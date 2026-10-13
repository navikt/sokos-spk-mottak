package no.nav.sokos.spk.mottak.repository

import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.spk.mottak.domain.SeksMaanedersRegel

class TilstandsRyddeAvstemmingTest :
    FunSpec({
        test("feildetaljer innen tremånedersfristen beholdes mens gamle feildetaljer fjernes tilsiktet") {
            RyddeDb().use { db ->
                val start = LocalDateTime.parse("2026-10-12T08:00:00.123456")
                val regel = SeksMaanedersRegel()
                db.sql("INSERT INTO T_K_GYLDIG_KOMBIN VALUES ('01','01','SPK','PENSJON','1')")
                db.tilstand(1, start.minusMonths(7))
                db.tilstand(2, start.minusMonths(2))
                db.parent(10, 1)
                db.parent(11, 2)
                db.sql(
                    """
                    UPDATE T_TRANSAKSJON SET FIL_INFO_ID = 10, K_ART = '01', K_BELOP_T = '01',
                        FNR_FK = '00000000000', OS_STATUS = '08', K_TRANS_TILST_T = 'FEI',
                        DATO_OPPRETTET = TIMESTAMP '2026-01-01 00:00:00'
                    WHERE TRANSAKSJON_ID IN (10, 11)
                    """.trimIndent(),
                )
                db.sql("UPDATE T_TRANS_TILSTAND SET FEILKODE = '08', FEILKODEMELDING = 'Syntetisk feil' WHERE TRANS_TILSTAND_ID IN (1, 2)")
                db.sql("UPDATE T_TRANS_TILSTAND SET TRANSAKSJON_ID = 10 WHERE TRANS_TILSTAND_ID = 1")
                db.sql("UPDATE T_TRANS_TILSTAND SET TRANSAKSJON_ID = 11 WHERE TRANS_TILSTAND_ID = 2")
                val repository = TransaksjonRepository(db.dataSource)
                repository.findTransaksjonDetaljerByFilInfoId(listOf(10)).size shouldBe 2
                val nyereDetaljer = repository.findTransaksjonDetaljerByFilInfoId(listOf(10)).last()
                db.nullstillOgSlett(listOf(1, 2), regel, regel.grensetidspunkt(start), 200) shouldBe
                    RyddeBatchresultat(1, 1)
                repository.findTransaksjonDetaljerByFilInfoId(listOf(10)) shouldBe listOf(nyereDetaljer)
                db.antall("T_TRANSAKSJON") shouldBe 2
                db.antall("T_RETUR_TIL_ANV") shouldBe 2
            }
        }
    })
