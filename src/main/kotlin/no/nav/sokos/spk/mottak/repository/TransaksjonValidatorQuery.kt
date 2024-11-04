package no.nav.sokos.spk.mottak.repository

import no.nav.sokos.spk.mottak.domain.BELOPSTYPE_IKKE_SKATTEPLIKTIG_UTBETALING
import no.nav.sokos.spk.mottak.domain.BELOPSTYPE_SKATTEPLIKTIG_UTBETALING
import no.nav.sokos.spk.mottak.domain.BELOPSTYPE_TREKK

object TransaksjonValidatorQuery {
    val VALIDATOR_01_UNIK_ID =
        """
        UPDATE T_INN_TRANSAKSJON
        SET K_TRANSAKSJON_S = '01'
        WHERE K_TRANSAKSJON_S IS NULL
          AND INN_TRANSAKSJON_ID IN (SELECT t1.INN_TRANSAKSJON_ID
                                     FROM T_INN_TRANSAKSJON t1
                                              JOIN T_TRANSAKSJON t2
                                                   ON t1.TRANS_ID_FK = t2.TRANS_EKS_ID_FK AND t1.AVSENDER = t2.K_ANVISER
                                     UNION
                                     SELECT t1.INN_TRANSAKSJON_ID
                                     FROM T_INN_TRANSAKSJON t1
                                              JOIN T_AVV_TRANSAKSJON t2
                                                   ON t1.TRANS_ID_FK = t2.TRANS_EKS_ID_FK AND t1.AVSENDER = t2.AVSENDER
                                     UNION
                                     SELECT t1.INN_TRANSAKSJON_ID
                                     FROM T_INN_TRANSAKSJON t1,
                                          (SELECT AVSENDER, TRANS_ID_FK
                                           FROM T_INN_TRANSAKSJON
                                           WHERE TRANS_ID_FK IS NOT NULL
                                           GROUP BY TRANS_ID_FK, AVSENDER
                                           HAVING COUNT(*) > 1) t2
                                     WHERE t1.AVSENDER = t2.AVSENDER
                                       AND t1.TRANS_ID_FK = t2.TRANS_ID_FK)	
        """.trimIndent()

    val VALIDATOR_03_GYLDIG_PERIODE =
        """
        UPDATE T_INN_TRANSAKSJON
        SET K_TRANSAKSJON_S = '03'
        WHERE K_TRANSAKSJON_S IS NULL
          AND INN_TRANSAKSJON_ID IN (SELECT INN_TRANSAKSJON_ID
                                     FROM T_INN_TRANSAKSJON
                                     WHERE (BELOPSTYPE IN ('01', '02') AND NOT (day(COALESCE(DATO_FOM, '1900-01-02')) = 1 AND day(COALESCE(DATO_TOM, '1900-01-01') + 1 DAY) = 1 AND month(DATO_FOM) = month(DATO_TOM)))
                                        OR (BELOPSTYPE IN ('03') AND NOT (day(COALESCE(DATO_FOM, '1900-01-02')) = 1 AND day(COALESCE(DATO_TOM, '1900-01-31') + 1 DAY) = 1)))
        """.trimIndent()

    val VALIDATOR_04_GYDLIG_BELOPSTYPE =
        """
        UPDATE T_INN_TRANSAKSJON
        SET K_TRANSAKSJON_S = '04'
        WHERE K_TRANSAKSJON_S IS NULL AND BELOPSTYPE NOT IN ('$BELOPSTYPE_SKATTEPLIKTIG_UTBETALING', '$BELOPSTYPE_IKKE_SKATTEPLIKTIG_UTBETALING', '$BELOPSTYPE_TREKK')
        """.trimIndent()

    val VALIDATOR_05_UGYLDIG_ART =
        """
        UPDATE T_INN_TRANSAKSJON
        SET K_TRANSAKSJON_S = '05'
        WHERE K_TRANSAKSJON_S IS NULL
          AND INN_TRANSAKSJON_ID in (SELECT t1.INN_TRANSAKSJON_ID
                                     FROM T_INN_TRANSAKSJON t1
                                              LEFT JOIN T_K_ART t2 ON t1.ART = t2.K_ART AND t2.ER_GYLDIG != '0'
                                     WHERE t2.K_ART IS NULL)
        """.trimIndent()

    val VALIDATOR_09_GYLDIG_ANVISER_DATO =
        """
        UPDATE T_INN_TRANSAKSJON
        SET K_TRANSAKSJON_S = '09'
        WHERE K_TRANSAKSJON_S IS NULL
          AND DATO_ANVISER IS NULL
        """.trimIndent()

    val VALIDATOR_10_GYLDIG_BELOP =
        """
        UPDATE T_INN_TRANSAKSJON
        SET K_TRANSAKSJON_S= '10'
        WHERE K_TRANSAKSJON_S IS NULL
          AND BELOP <= 0
        """.trimIndent()

    val VALIDATOR_11_GYLDIG_KOMBINASJON_ART_OG_BELOPSTYPE =
        """
        UPDATE T_INN_TRANSAKSJON
        SET K_TRANSAKSJON_S = '11'
        WHERE K_TRANSAKSJON_S IS NULL
          AND INN_TRANSAKSJON_ID IN (SELECT t1.INN_TRANSAKSJON_ID
                                     FROM T_INN_TRANSAKSJON t1
                                              LEFT JOIN T_K_GYLDIG_KOMBIN t2 ON t1.BELOPSTYPE = t2.K_BELOP_T
                                         AND t1.ART = t2.K_ART
                                         AND t1.AVSENDER = t2.K_ANVISER
                                         AND t2.ER_GYLDIG != '0'
                                     WHERE t2.K_BELOP_T is NULL)
        """.trimIndent()

    val VALIDATOR_16_GYLDIG_GRAD =
        """
        UPDATE T_INN_TRANSAKSJON
        SET K_TRANSAKSJON_S = '16'
        WHERE K_TRANSAKSJON_S IS NULL
        AND (ART IN ('UFO', 'U67', 'AFP', 'UFE', 'UFT', 'ALP', 'AFL') AND GRAD IS NULL)
        OR GRAD < 0 
        OR GRAD > 100
        """.trimIndent()
}
