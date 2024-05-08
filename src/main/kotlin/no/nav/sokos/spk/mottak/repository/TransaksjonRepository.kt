package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.util.Util.asMap
import no.nav.sokos.spk.mottak.util.Util.toChar

class TransaksjonRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.dataSource()
) {
    fun insertBatch(transaksjonList: List<Transaksjon>, session: Session): List<Int> {
        return session.batchPreparedNamedStatement(
            """
                INSERT INTO T_TRANSAKSJON  (
                    TRANSAKSJON_ID,
                    FIL_INFO_ID, 
                    K_TRANSAKSJON_S, 
                    PERSON_ID, 
                    K_BELOP_T, 
                    K_ART, 
                    K_ANVISER, 
                    FNR_FK, 
                    UTBETALES_TIL, 
                    DATO_FOM, 
                    DATO_TOM, 
                    DATO_ANVISER, 
                    DATO_PERSON_FOM, 
                    DATO_REAK_FOM, 
                    BELOP, 
                    REF_TRANS_ID, 
                    TEKSTKODE, 
                    RECTYPE, 
                    TRANS_EKS_ID_FK, 
                    K_TRANS_TOLKNING, 
                    SENDT_TIL_OPPDRAG,
                    FNR_ENDRET, 
                    MOT_ID, 
                    DATO_OPPRETTET, 
                    OPPRETTET_AV, 
                    DATO_ENDRET, 
                    ENDRET_AV, 
                    VERSJON, 
                    K_TRANS_TILST_T,
                    GRAD
                ) VALUES (:transaksjonId, :filInfoId, :transaksjonStatus, :personId, :belopstype, :art, :anviser, :fnr, :utbetalesTil, :datoFom, :datoTom, :datoAnviser, :datoPersonFom, :datoReakFom, :belop, :refTransId, :tekstkode, :rectype, :transEksId, :transTolkning, :sendtTilOppdrag, :fnrEndret, :motId, :datoOpprettet, :opprettetAv, :datoEndret, :endretAv, :versjon, :transTilstandType, :grad)
            """.trimIndent(),
            transaksjonList.map { it.asMap() }
        )
    }

    fun getLastTransaksjonByPersonId(personIdListe: List<Int>): List<Transaksjon> {
        return using(sessionOf(dataSource)) { session ->
            session.list(
                queryOf(
                    """
                        SELECT t.*
                        FROM T_INN_TRANSAKSJON inn 
                        INNER JOIN T_PERSON p ON inn.FNR_FK = p.FNR_FK 
                        INNER JOIN T_TRANSAKSJON t ON t.PERSON_ID = p.PERSON_ID
                        WHERE p.PERSON_ID IN (${personIdListe.joinToString()}) 
                        AND ((inn.BELOPSTYPE = '01' OR inn.BELOPSTYPE = '02') AND (t.K_BELOP_T = '01' OR t.K_BELOP_T = '02'))
                        AND t.K_ANVISER = 'SPK' 
                        AND t.DATO_TOM IN 
                            (SELECT MAX(t2.DATO_TOM) FROM T_TRANSAKSJON t2 
                            WHERE t2.PERSON_ID = p.PERSON_ID 
                            AND t2.K_ANVISER = 'SPK');
                    """.trimIndent()
                ), toTransaksjon
            )
        }
    }

    fun getTransaksjonerForNyArtForPerson(innTransaksjon: InnTransaksjon): Int? {
        return using(sessionOf(dataSource)) { session ->
            session.single(
                queryOf(
                    """ 
                    SELECT COUNT(*) FROM T_TRANSAKSJON t1 
                    WHERE t1.person_id = ${innTransaksjon.personId}
                    AND t1.k_anviser = 'SPK' 
                    AND t1.k_art != '${innTransaksjon.art}' 
                    AND (t1.dato_tom >= '${innTransaksjon.datoTom}'
                        OR t1.dato_tom IN 
                            (SELECT max(t2.dato_tom) FROM T_TRANSAKSJON t2 
                            WHERE t2.person_id = ${innTransaksjon.personId}
                            AND t2.k_anviser = 'SPK'
                            AND t2.dato_tom < '${innTransaksjon.datoTom}'))
                    """.trimIndent()
                ), { it.int(1) }
            )
        }
    }

    fun getTransaksjonerForNyArtINyttFagomraadeForPerson(innTransaksjon: InnTransaksjon): Int? {
        return using(sessionOf(dataSource)) { session ->
            session.single(
                queryOf(
                    """ 
                        SELECT count(*)
                        FROM T_TRANSAKSJON t, T_K_GYLDIG_KOMBIN g
	                    WHERE t.person_Id = ${innTransaksjon.personId}
	                    AND g.k_art = t.k_art
	                    AND g.k_anviser = 'SPK'
	                    AND g.k_belop_t = t.k_belop_t
	                    AND g.k_fagomrade IN (
                            SELECT g2.k_fagomrade 
                            FROM T_K_GYLDIG_KOMBIN g2 
                            WHERE g2.k_art = '${innTransaksjon.art}'
                            AND g2.k_anviser = 'SPK'
                            AND g2.k_belop_t = '${innTransaksjon.belopstype}')
                    """.trimIndent()
                ), { it.int(1) }
            )
        }
    }

    fun getByTransaksjonId(transaksjonId: Int): Transaksjon? {
        return using(sessionOf(dataSource)) { session ->
            session.single(
                queryOf(
                    """
                        SELECT * FROM T_TRANSAKSJON WHERE TRANSAKSJON_ID = $transaksjonId
                    """.trimIndent()
                ), toTransaksjon
            )
        }
    }

    private val toTransaksjon: (Row) -> Transaksjon = { row ->
        Transaksjon(
            transaksjonId = row.int("TRANSAKSJON_ID"),
            filInfoId = row.int("FIL_INFO_ID"),
            transaksjonStatus = row.string("K_TRANSAKSJON_S"),
            personId = row.int("PERSON_ID"),
            belopstype = row.string("K_BELOP_T"),
            art = row.string("K_ART"),
            anviser = row.string("K_ANVISER"),
            fnr = row.string("FNR_FK"),
            utbetalesTil = row.stringOrNull("UTBETALES_TIL"),
            osId = row.stringOrNull("OS_ID_FK"),
            osLinjeId = row.stringOrNull("OS_LINJE_ID_FK"),
            datoFom = row.localDate("DATO_FOM"),
            datoTom = row.localDate("DATO_TOM"),
            datoAnviser = row.localDate("DATO_ANVISER"),
            datoPersonFom = row.localDate("DATO_PERSON_FOM"),
            datoReakFom = row.localDate("DATO_REAK_FOM"),
            belop = row.int("BELOP"),
            refTransId = row.stringOrNull("REF_TRANS_ID"),
            tekstkode = row.stringOrNull("TEKSTKODE"),
            rectype = row.string("RECTYPE"),
            transEksId = row.string("TRANS_EKS_ID_FK"),
            transTolkning = row.string("K_TRANS_TOLKNING"),
            sendtTilOppdrag = row.string("SENDT_TIL_OPPDRAG"),
            trekkvedtakId = row.stringOrNull("TREKKVEDTAK_ID_FK"),
            fnrEndret = row.boolean("FNR_ENDRET").toChar(),
            motId = row.string("MOT_ID"),
            osStatus = row.stringOrNull("OS_STATUS"),
            datoOpprettet = row.localDateTime("DATO_OPPRETTET"),
            opprettetAv = row.string("OPPRETTET_AV"),
            datoEndret = row.localDateTime("DATO_ENDRET"),
            endretAv = row.string("ENDRET_AV"),
            versjon = row.int("VERSJON"),
            transTilstandType = row.stringOrNull("K_TRANS_TILST_T"),
            grad = row.intOrNull("GRAD")
        )
    }
}
