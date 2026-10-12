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
                db.sql("ALTER TABLE T_TRANSAKSJON ADD FIL_INFO_ID INT DEFAULT 10")
                db.sql("ALTER TABLE T_TRANSAKSJON ADD K_ART VARCHAR(20) DEFAULT '01'")
                db.sql("ALTER TABLE T_TRANSAKSJON ADD K_BELOP_T VARCHAR(20) DEFAULT '01'")
                db.sql("ALTER TABLE T_TRANSAKSJON ADD FNR_FK VARCHAR(11) DEFAULT '00000000000'")
                db.sql("ALTER TABLE T_TRANSAKSJON ADD OS_STATUS VARCHAR(20) DEFAULT '08'")
                db.sql("ALTER TABLE T_TRANSAKSJON ADD K_TRANS_TILST_T VARCHAR(20) DEFAULT 'FEI'")
                db.sql("ALTER TABLE T_TRANSAKSJON ADD DATO_OPPRETTET TIMESTAMP(6) DEFAULT TIMESTAMP '2026-01-01 00:00:00'")
                db.sql("ALTER TABLE T_TRANS_TILSTAND ADD FEILKODE VARCHAR(20) DEFAULT '08'")
                db.sql("ALTER TABLE T_TRANS_TILSTAND ADD FEILKODEMELDING VARCHAR(100) DEFAULT 'Syntetisk feil'")
                db.sql("CREATE TABLE T_K_GYLDIG_KOMBIN (K_ART VARCHAR(20), K_BELOP_T VARCHAR(20), K_ANVISER VARCHAR(20), K_FAGOMRADE VARCHAR(20), ER_GYLDIG CHAR(1))")
                db.sql("INSERT INTO T_K_GYLDIG_KOMBIN VALUES ('01','01','SPK','PENSJON','1')")
                db.tilstand(1, start.minusMonths(7))
                db.tilstand(2, start.minusMonths(2))
                db.parent(10, 1)
                db.parent(11, 2)
                db.sql("UPDATE T_TRANS_TILSTAND SET TRANSAKSJON_ID = 10 WHERE TRANS_TILSTAND_ID = 1")
                db.sql("UPDATE T_TRANS_TILSTAND SET TRANSAKSJON_ID = 11 WHERE TRANS_TILSTAND_ID = 2")
                val repository = TransaksjonRepository(db.dataSource)
                repository.findTransaksjonDetaljerByFilInfoId(listOf(10)).size shouldBe 2
                val nyereDetaljer = repository.findTransaksjonDetaljerByFilInfoId(listOf(10)).last()
                db.repository.nullstillOgSlett(listOf(1, 2), regel, regel.grensetidspunkt(start), 200, db.kontroll) shouldBe
                    RyddeBatchresultat(1, 1)
                repository.findTransaksjonDetaljerByFilInfoId(listOf(10)) shouldBe listOf(nyereDetaljer)
                db.antall("T_TRANSAKSJON") shouldBe 2
                db.antall("T_RETUR_TIL_ANV") shouldBe 2
            }
        }
    })
