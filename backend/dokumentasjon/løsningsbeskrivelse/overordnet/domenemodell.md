````mermaid
erDiagram
    T_FIL_INFO ||--|{ T_INN_TRANSAKSJON: ""
    T_FIL_INFO {
        int fil_info_id
        string fil_navn
        string k_fil_s
        string k_fil_t
        string k_fil_tilstand_t
        string k_anviser
        string lopenr
        LocalDate dato_mottatt
        LocalDate dato_sendt
        LocalDateTime dato_opprettet
        string opprettet_av
        LocalDateTime dato_endret
        string endret_av
        int versjon
        string k_avstemming_s
        string feiltekst
    }
    T_INN_TRANSAKSJON {
        int inn_transaksjon_id
        int fil_info_id
        string k_transaksjon_s
        string fnr_fk
        string belopstype
        string art
        string avsender
        string utbetales_til
        string dato_fom_str
        string dato_tom_str
        string dato_anviser_str
        string belop_str
        string ref_trans_id
        string tekstkode
        string rectype
        string trans_id_fk
        LocalDate dato_fom
        LocalDate dato_tom
        LocalDate dato_anviser
        int belop
        string behandlet
        LocalDateTime dato_opprettet
        string opprettet_av
        LocalDateTime dato_endret
        string endret_av
        int versjon
        string grad
        int grad_str
    }
    T_FIL_INFO ||--|{ T_AVV_TRANSAKSJON: ""
    T_AVV_TRANSAKSJON {
        int avv_transaksjon_id
        int fil_info_id
        string k_transaksjon_s
        string fnr_fk
        string belopstype
        string art
        string avsender
        string utbetales_til
        string ref_trans_id
        string tekstkode
        string rectype
        string trans_eks_id_fk
        string dato_fom
        string dato_tom
        string dato_anviser
        string belop
        LocalDateTime dato_opprettet
        string opprettet_av
        LocalDateTime dato_endret
        string endret_av
        int versjon
        int grad
    }
    T_FIL_INFO ||--|{ T_TRANSAKSJON: ""
    T_TRANSAKSJON {
        int transaksjon_id
        int trans_tilstand_id
        int fil_info_id
        string k_transaksjon_s
        int person_id
        string k_belop_t
        string k_art
        string k_anviser
        string fnr_fk
        string utbetales_til
        string os_id_fk
        string os_linje_id_fk
        LocalDate dato_fom
        LocalDate dato_tom
        LocalDate dato_anviser
        LocalDate dato_person_fom
        LocalDate dato_reak_fom
        int belop
        string ref_trans_id
        string tekstkode
        string rectype
        string trans_eks_id_fk
        string k_trans_tolkning
        string sendt_til_oppdrag
        string belopstype
        string art
        string avsender
        string dato_fom_str
        string dato_tom_str
        string dato_anviser_str
        string belop_str
        string trekkvedtak_id_fk
        string fnr_endret
        string mot_id
        string os_status
        LocalDateTime dato_opprettet
        string opprettet_av
        LocalDateTime dato_endret
        string endret_av
        int versjon
        string saldo
        string prioritet
        string kid
        string k_trekkansvar
        string k_trans_tilst_t
        string grad
    }
    T_FIL_INFO ||--|{ T_RETUR_TIL_ANV: ""
    T_TRANSAKSJON ||--|{ T_RETUR_TIL_ANV: ""
    T_RETUR_TIL_ANV {
        int retur_til_anv_id
        string rectype
        string k_retur_t
        string k_anviser
        string os_id_fk
        string os_linje_id_fk
        string trekkvedtak_id_fk
        string gjelder_id
        string fnr_fk
        LocalDate dato_status
        string status
        string bilagsnr_serie
        string bilagsnr
        LocalDate dato_fom
        LocalDate dato_tom
        string belop
        string debet_kredit
        string utbetaling_type
        string trans_tekst
        string trans_eks_id_fk
        LocalDate dato_avsender
        string utbetales_til
        string returtype_kode
        string status_tekst
        string duplikat
        int transaksjon_id
        int fil_info_inn_id
        int fil_info_ut_id
        string dato_valutering
        string konto
        string mot_id
        int person_id
        string kreditor_ref
        LocalDateTime dato_opprettet
        string opprettet_av
        LocalDateTime dato_endret
        string endret_av
        int versjon
    }
    T_TRANSAKSJON ||--|{ T_TRANS_TILSTAND: ""
    T_TRANS_TILSTAND ||--|| T_TRANSAKSJON: ""
    T_TRANS_TILSTAND {
        int trans_tilstand_id
        int transaksjon_id
        string k_trans_tilst_t
        LocalDateTime dato_opprettet
        string opprettet_av
        LocalDateTime dato_endret
        string endret_av
        int versjon
        string feilkode
        string feilkodemelding
    }
    T_PERSON ||--|{ T_TRANSAKSJON: ""
    T_PERSON {
        int person_id
        string fnr_fk
        LocalDateTime dato_opprettet
        string opprettet_av
        LocalDateTime dato_endret
        string endret_av
        int versjon
    }
    T_K_ART ||--|{ T_TRANSAKSJON: ""
    T_K_ART {
        string k_art
        string dekode
        LocalDate dato_fom
        LocalDate dato_tom
        string er_gyldig
        LocalDateTime dato_opprettet
        string opprettet_av
        LocalDateTime dato_endret
        string endret_av
    }
    T_K_ART ||--|{ T_K_GYLDIG_KOMBIN: ""
    T_K_GYLDIG_KOMBIN {
        int k_gyldig_kombin_id
        string k_art
        string k_belop_t
        string k_trekkgruppe
        string k_trekk_t
        string k_trekkalt_t
        string k_anviser
        string k_fagomrade
        string os_klassifiksajon
        LocalDate dato_fom
        LocalDate dato_tom
        string er_gyldig
        LocalDateTime dato_opprettet
        string opprettet_av
        LocalDateTime dato_endret
        string endret_av
    }
````