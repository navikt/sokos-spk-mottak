CREATE TABLE t_k_anviser (
    k_anviser varchar(20) NOT NULL,
    tss_ekstern_id_fk varchar(20),
    dekode varchar(200) NOT NULL,
    dato_fom date NOT NULL,
    dato_tom date,
    er_gyldig char(1) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(20) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(20) NOT NULL,
    nytt_filformat char(1),
    CONSTRAINT k_anviser PRIMARY KEY (k_anviser)
);

CREATE TABLE t_k_art (
    k_art varchar(20) NOT NULL,
    dekode varchar(200) NOT NULL,
    dato_fom date NOT NULL,
    dato_tom date,
    er_gyldig char(1) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(20) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(20) NOT NULL,
    CONSTRAINT k_art PRIMARY KEY (k_art)
);

CREATE TABLE t_k_belop_t (
    k_belop_t varchar(20) NOT NULL,
    dekode varchar(200) NOT NULL,
    dato_fom date NOT NULL,
    dato_tom date,
    er_gyldig char(1) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(20) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(20) NOT NULL,
    CONSTRAINT k_belop_t PRIMARY KEY (k_belop_t)
);

CREATE TABLE t_k_fagomrade (
    k_fagomrade varchar(20) NOT NULL,
    dekode varchar(200) NOT NULL,
    dato_fom date NOT NULL,
    dato_tom date,
    er_gyldig char(1) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(20) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(20) NOT NULL,
    CONSTRAINT k_fagomrade PRIMARY KEY (k_fagomrade)
);

CREATE TABLE t_k_fil_s (
    k_fil_s varchar(20) NOT NULL,
    dekode varchar(200) NOT NULL,
    dato_fom date NOT NULL,
    dato_tom date,
    er_gyldig char(1) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(20) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(20) NOT NULL,
    CONSTRAINT k_fil_s PRIMARY KEY (k_fil_s)
);

CREATE TABLE t_k_fil_t (
    k_fil_t varchar(20) NOT NULL,
    dekode varchar(200) NOT NULL,
    dato_fom date NOT NULL,
    dato_tom date,
    er_gyldig char(1) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(20) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(20) NOT NULL,
    CONSTRAINT k_fil_t PRIMARY KEY (k_fil_t)
);

CREATE TABLE t_k_fil_tilstand_t (
    k_fil_tilstand_t varchar(20) NOT NULL,
    dekode varchar(200) NOT NULL,
    dato_fom date NOT NULL,
    dato_tom date,
    er_gyldig char(1) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(20) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(20) NOT NULL,
    CONSTRAINT k_fil_tilstand_t PRIMARY KEY (k_fil_tilstand_t)
);

CREATE TABLE t_k_retur_t (
    k_retur_t varchar(20) NOT NULL,
    dekode varchar(200) NOT NULL,
    dato_fom date NOT NULL,
    dato_tom date,
    er_gyldig char(1) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(20) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(20) NOT NULL,
    CONSTRAINT k_retur_t PRIMARY KEY (k_retur_t)
);

CREATE TABLE t_k_trekk_t (
    k_trekk_t varchar(20) NOT NULL,
    dekode varchar(200) NOT NULL,
    dato_fom date NOT NULL,
    dato_tom date,
    er_gyldig char(1) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(20) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(20) NOT NULL,
    CONSTRAINT k_trekk_t PRIMARY KEY (k_trekk_t)
);

CREATE TABLE t_k_trekkalt_t (
    k_trekkalt_t varchar(20) NOT NULL,
    dekode varchar(200) NOT NULL,
    dato_fom date NOT NULL,
    dato_tom date,
    er_gyldig char(1) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(20) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(20) NOT NULL,
    CONSTRAINT k_trekkalt_t PRIMARY KEY (k_trekkalt_t)
);

CREATE TABLE t_k_trekkansvar (
    k_trekkansvar varchar(4) NOT NULL,
    dekode varchar(200) NOT NULL,
    dato_fom date NOT NULL,
    dato_tom date,
    er_gyldig char(1) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(20) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(20) NOT NULL,
    CONSTRAINT k_trekkansvar PRIMARY KEY (k_trekkansvar)
);

CREATE TABLE t_k_trekkgruppe (
    k_trekkgruppe varchar(20) NOT NULL,
    dekode varchar(200) NOT NULL,
    dato_fom date NOT NULL,
    dato_tom date,
    er_gyldig char(1) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(20) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(20) NOT NULL,
    CONSTRAINT k_trekkgruppe PRIMARY KEY (k_trekkgruppe)
);

CREATE TABLE t_k_transaksjon_s (
    k_transaksjon_s varchar(20) NOT NULL,
    dekode varchar(200) NOT NULL,
    dato_fom date NOT NULL,
    dato_tom date,
    er_gyldig char(1) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(20) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(20) NOT NULL,
    CONSTRAINT k_transaksjon_s PRIMARY KEY (k_transaksjon_s)
);

CREATE TABLE t_k_trans_tolkning (
    k_trans_tolkning varchar(20) NOT NULL,
    dekode varchar(200) NOT NULL,
    dato_fom date NOT NULL,
    dato_tom date,
    er_gyldig char(1) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(20) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(20) NOT NULL,
    CONSTRAINT k_trans_tolkning PRIMARY KEY (k_trans_tolkning)
);

CREATE TABLE t_k_trans_tilst_t (
    k_trans_tilst_t varchar(20) NOT NULL,
    dekode varchar(200) NOT NULL,
    dato_fom date NOT NULL,
    dato_tom date,
    er_gyldig char(1) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(20) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(20) NOT NULL,
    CONSTRAINT k_trans_tilst_t PRIMARY KEY (k_trans_tilst_t)
);

CREATE TABLE t_person (
    person_id integer NOT NULL GENERATED BY DEFAULT AS IDENTITY
        (START WITH 20000000 INCREMENT BY 1 MINVALUE 20000000
         MAXVALUE 2147483647 CACHE 200 NO CYCLE),
    fnr_fk char(11) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(50) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(50) NOT NULL,
    versjon integer NOT NULL,
    CONSTRAINT fnr_fk UNIQUE (fnr_fk),
    CONSTRAINT person_id PRIMARY KEY (person_id)
);

CREATE TABLE t_retur_til_anv (
    retur_til_anv_id integer NOT NULL GENERATED BY DEFAULT AS IDENTITY
        (START WITH 20000000 INCREMENT BY 1 MINVALUE 20000000
         MAXVALUE 2147483647 CACHE 200 NO CYCLE),
    rectype varchar(20),
    k_retur_t varchar(20),
    k_anviser varchar(20) NOT NULL,
    os_id_fk varchar(20),
    os_linje_id_fk varchar(20),
    trekkvedtak_id_fk varchar(20),
    gjelder_id varchar(20),
    fnr_fk char(11),
    dato_status date,
    status varchar(20),
    bilagsnr_serie varchar(20),
    bilagsnr varchar(20),
    dato_fom date,
    dato_tom date,
    belop varchar(20),
    debet_kredit varchar(20),
    utbetaling_type varchar(20),
    trans_tekst varchar(200),
    trans_eks_id_fk varchar(20),
    dato_avsender date,
    utbetales_til char(11),
    status_tekst varchar(200),
    returtype_kode varchar(20),
    duplikat char(1) NOT NULL,
    transaksjon_id integer,
    fil_info_inn_id integer NOT NULL,
    fil_info_ut_id integer,
    dato_valutering varchar(20),
    konto varchar(20),
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(50) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(50) NOT NULL,
    versjon integer NOT NULL,
    mot_id varchar(10),
    person_id varchar(30),
    kreditor_ref varchar(30),
    CONSTRAINT retur_til_anv_id PRIMARY KEY (retur_til_anv_id)
);

CREATE TABLE t_lopenr (
    lopenr_id integer NOT NULL GENERATED BY DEFAULT AS IDENTITY
        (START WITH 20000000 INCREMENT BY 1 MINVALUE 20000000
         MAXVALUE 2147483647 CACHE 200 NO CYCLE),
    siste_lopenr integer,
    k_fil_t varchar(20),
    k_anviser varchar(20),
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(50) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(50) NOT NULL,
    versjon integer NOT NULL,
    CONSTRAINT lopenr_id PRIMARY KEY (lopenr_id)
);

CREATE TABLE t_avregning_avvik (
    avregning_avvik_id integer NOT NULL GENERATED BY DEFAULT AS IDENTITY
        (START WITH 1 INCREMENT BY 1 MINVALUE 1
         MAXVALUE 2147483647 CACHE 200 NO CYCLE),
    oppdrags_id integer,
    linje_id integer,
    trekkvedtak_id integer,
    gjelder_id varchar(11),
    utbetales_til varchar(11),
    dato_status_satt varchar(8),
    status varchar(4),
    bilagsnr_serie varchar(4),
    bilagsnr varchar(10),
    konto varchar(9),
    fom_dato varchar(8),
    tom_dato varchar(8),
    belop integer,
    debet_kredit varchar(1),
    utbetaling_type varchar(3),
    trans_tekst varchar(35),
    dato_valutert varchar(8),
    delytelse_id varchar(10),
    fagsystem_id varchar(30),
    kreditor_ref varchar(30),
    feilmelding varchar(100),
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(50) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(50) NOT NULL,
    CONSTRAINT avregning_avvik_id PRIMARY KEY (avregning_avvik_id)
);

CREATE TABLE t_avv_transaksjon (
    avv_transaksjon_id integer NOT NULL,
    fil_info_id integer NOT NULL,
    k_transaksjon_s varchar(20) NOT NULL,
    fnr_fk char(11),
    belopstype varchar(20),
    art varchar(20),
    avsender varchar(20),
    utbetales_til char(11),
    dato_fom varchar(20),
    dato_tom varchar(20),
    dato_anviser varchar(20),
    belop varchar(20),
    ref_trans_id varchar(20),
    tekstkode varchar(20),
    rectype varchar(20),
    trans_eks_id_fk varchar(20),
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(50) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(50) NOT NULL,
    versjon integer NOT NULL,
    prioritet varchar(20),
    saldo varchar(20),
    trekkansvar varchar(20),
    kid varchar(26),
    grad integer,
    CONSTRAINT avv_transaksjon_id PRIMARY KEY (avv_transaksjon_id)
);

CREATE TABLE t_fil_info (
    fil_info_id integer NOT NULL GENERATED BY DEFAULT AS IDENTITY
        (START WITH 20000000 INCREMENT BY 1 MINVALUE 20000000
         MAXVALUE 2147483647 CACHE 200 NO CYCLE),
    k_fil_s varchar(20) NOT NULL,
    k_anviser varchar(20),
    k_fil_t varchar(20) NOT NULL,
    k_fil_tilstand_t varchar(20),
    fil_navn varchar(200),
    lopenr varchar(20),
    feiltekst varchar(200),
    dato_mottatt date,
    dato_sendt date,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(50) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(50) NOT NULL,
    versjon integer NOT NULL,
    k_avstemming_s varchar(20),
    dato_transaksjon_sendt date,
    CONSTRAINT fil_info_id PRIMARY KEY (fil_info_id)
);

CREATE TABLE t_inn_transaksjon (
    inn_transaksjon_id integer NOT NULL GENERATED BY DEFAULT AS IDENTITY
        (START WITH 20000000 INCREMENT BY 1 MINVALUE 20000000
         MAXVALUE 2147483647 CACHE 200 NO CYCLE),
    fil_info_id integer NOT NULL,
    k_transaksjon_s varchar(20),
    fnr_fk char(11),
    belopstype varchar(20),
    art varchar(20),
    avsender varchar(20),
    utbetales_til char(11),
    dato_fom_str varchar(20),
    dato_tom_str varchar(20),
    dato_anviser_str varchar(20),
    belop_str varchar(20),
    ref_trans_id varchar(20),
    tekstkode varchar(20),
    rectype varchar(20),
    trans_id_fk varchar(20),
    dato_fom date,
    dato_tom date,
    dato_anviser date,
    belop integer,
    behandlet char(1),
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(50) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(50) NOT NULL,
    versjon integer NOT NULL,
    prioritet_str varchar(20),
    trekkansvar varchar(20),
    saldo_str varchar(20),
    kid varchar(26),
    prioritet date,
    saldo integer,
    grad integer,
    grad_str varchar(4),
    CONSTRAINT inn_transaksjon_id PRIMARY KEY (inn_transaksjon_id)
);

CREATE TABLE t_k_gyldig_kombin (
    k_gyldig_kombin_id integer NOT NULL,
    k_art varchar(20) NOT NULL,
    k_belop_t varchar(20) NOT NULL,
    k_trekkgruppe varchar(20),
    k_trekk_t varchar(20),
    k_trekkalt_t varchar(20),
    k_anviser varchar(20) NOT NULL,
    k_fagomrade varchar(20),
    os_klassifikasjon varchar(40),
    dato_fom date NOT NULL,
    dato_tom date,
    er_gyldig char(1) NOT NULL,
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(20) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(20) NOT NULL,
    CONSTRAINT k_gyldig_kombin_id PRIMARY KEY (k_gyldig_kombin_id)
);

CREATE TABLE t_trans_tilstand (
    trans_tilstand_id integer NOT NULL GENERATED BY DEFAULT AS IDENTITY
        (START WITH 20000000 INCREMENT BY 1 MINVALUE 20000000
         MAXVALUE 2147483647 CACHE 200 NO CYCLE),
    transaksjon_id integer,
    k_trans_tilst_t varchar(20),
    feilkode varchar(20),
    feilkodemelding varchar(200),
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(50) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(50) NOT NULL,
    versjon integer NOT NULL,
    CONSTRAINT trans_tilstand_id PRIMARY KEY (trans_tilstand_id)
);

CREATE TABLE t_transaksjon (
    transaksjon_id integer NOT NULL,
    trans_tilstand_id integer,
    fil_info_id integer NOT NULL,
    k_transaksjon_s varchar(20) NOT NULL,
    person_id integer NOT NULL,
    k_belop_t varchar(20) NOT NULL,
    k_art varchar(20) NOT NULL,
    k_anviser varchar(20) NOT NULL,
    fnr_fk char(11) NOT NULL,
    utbetales_til char(11),
    os_id_fk varchar(20),
    os_linje_id_fk varchar(20),
    dato_fom date NOT NULL,
    dato_tom date,
    dato_anviser date,
    dato_person_fom date NOT NULL,
    dato_reak_fom date,
    belop integer,
    ref_trans_id varchar(20),
    tekstkode varchar(20),
    rectype varchar(20),
    trans_eks_id_fk varchar(20),
    k_trans_tolkning varchar(20),
    sendt_til_oppdrag char(1) NOT NULL,
    trekkvedtak_id_fk varchar(20),
    fnr_endret char(1) NOT NULL,
    mot_id varchar(20),
    os_status varchar(20),
    dato_opprettet timestamp(6) without time zone NOT NULL,
    opprettet_av varchar(50) NOT NULL,
    dato_endret timestamp(6) without time zone NOT NULL,
    endret_av varchar(50) NOT NULL,
    versjon integer NOT NULL,
    saldo integer,
    kid varchar(26),
    prioritet date,
    k_trekkansvar varchar(4),
    k_trans_tilst_t varchar(20),
    grad integer,
    CONSTRAINT transaksjon_id PRIMARY KEY (transaksjon_id)
);

ALTER TABLE t_avv_transaksjon
    ADD CONSTRAINT fk_t_avv_transaksjon_fil_info_id
        FOREIGN KEY (fil_info_id) REFERENCES t_fil_info (fil_info_id)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_avv_transaksjon
    ADD CONSTRAINT fk_t_avv_transaksjon_k_transaksjon_s
        FOREIGN KEY (k_transaksjon_s) REFERENCES t_k_transaksjon_s (k_transaksjon_s)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_fil_info
    ADD CONSTRAINT fk_t_fil_info_k_anviser
        FOREIGN KEY (k_anviser) REFERENCES t_k_anviser (k_anviser)
        ON DELETE SET NULL NOT DEFERRABLE;
ALTER TABLE t_fil_info
    ADD CONSTRAINT fk_t_fil_info_k_fil_s
        FOREIGN KEY (k_fil_s) REFERENCES t_k_fil_s (k_fil_s)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_fil_info
    ADD CONSTRAINT fk_t_fil_info_k_fil_t
        FOREIGN KEY (k_fil_t) REFERENCES t_k_fil_t (k_fil_t)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_fil_info
    ADD CONSTRAINT fk_t_fil_info_k_fil_tilstand_t
        FOREIGN KEY (k_fil_tilstand_t) REFERENCES t_k_fil_tilstand_t (k_fil_tilstand_t)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_inn_transaksjon
    ADD CONSTRAINT fk_t_inn_transaksjon_fil_info_id
        FOREIGN KEY (fil_info_id) REFERENCES t_fil_info (fil_info_id)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_inn_transaksjon
    ADD CONSTRAINT fk_t_inn_transaksjon_k_transaksjon_s
        FOREIGN KEY (k_transaksjon_s) REFERENCES t_k_transaksjon_s (k_transaksjon_s)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_k_gyldig_kombin
    ADD CONSTRAINT fk_t_k_gyldig_kombin_k_anviser
        FOREIGN KEY (k_anviser) REFERENCES t_k_anviser (k_anviser)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_k_gyldig_kombin
    ADD CONSTRAINT fk_t_k_gyldig_kombin_k_art
        FOREIGN KEY (k_art) REFERENCES t_k_art (k_art)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_k_gyldig_kombin
    ADD CONSTRAINT fk_t_k_gyldig_kombin_k_belop_t
        FOREIGN KEY (k_belop_t) REFERENCES t_k_belop_t (k_belop_t)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_k_gyldig_kombin
    ADD CONSTRAINT fk_t_k_gyldig_kombin_k_fagomrade
        FOREIGN KEY (k_fagomrade) REFERENCES t_k_fagomrade (k_fagomrade)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_k_gyldig_kombin
    ADD CONSTRAINT fk_t_k_gyldig_kombin_k_trekk_t
        FOREIGN KEY (k_trekk_t) REFERENCES t_k_trekk_t (k_trekk_t)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_k_gyldig_kombin
    ADD CONSTRAINT fk_t_k_gyldig_kombin_k_trekkalt_t
        FOREIGN KEY (k_trekkalt_t) REFERENCES t_k_trekkalt_t (k_trekkalt_t)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_k_gyldig_kombin
    ADD CONSTRAINT fk_t_k_gyldig_kombin_k_trekkgruppe
        FOREIGN KEY (k_trekkgruppe) REFERENCES t_k_trekkgruppe (k_trekkgruppe)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_lopenr
    ADD CONSTRAINT fk_t_lopenr_k_anviser
        FOREIGN KEY (k_anviser) REFERENCES t_k_anviser (k_anviser)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_lopenr
    ADD CONSTRAINT fk_t_lopenr_k_fil_t
        FOREIGN KEY (k_fil_t) REFERENCES t_k_fil_t (k_fil_t)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_retur_til_anv
    ADD CONSTRAINT fk_t_retur_til_anv_fil_info_ut_id
        FOREIGN KEY (fil_info_ut_id) REFERENCES t_fil_info (fil_info_id)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_retur_til_anv
    ADD CONSTRAINT fk_t_retur_til_anv_k_anviser
        FOREIGN KEY (k_anviser) REFERENCES t_k_anviser (k_anviser)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_retur_til_anv
    ADD CONSTRAINT fk_t_retur_til_anv_k_retur_t
        FOREIGN KEY (k_retur_t) REFERENCES t_k_retur_t (k_retur_t)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_retur_til_anv
    ADD CONSTRAINT fk_t_retur_til_anv_transaksjon_id
        FOREIGN KEY (transaksjon_id) REFERENCES t_transaksjon (transaksjon_id)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_transaksjon
    ADD CONSTRAINT fk_t_transaksjon_fil_info_id
        FOREIGN KEY (fil_info_id) REFERENCES t_fil_info (fil_info_id)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_transaksjon
    ADD CONSTRAINT fk_t_transaksjon_k_anviser
        FOREIGN KEY (k_anviser) REFERENCES t_k_anviser (k_anviser)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_transaksjon
    ADD CONSTRAINT fk_t_transaksjon_k_art
        FOREIGN KEY (k_art) REFERENCES t_k_art (k_art)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_transaksjon
    ADD CONSTRAINT fk_t_transaksjon_k_belop_t
        FOREIGN KEY (k_belop_t) REFERENCES t_k_belop_t (k_belop_t)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_transaksjon
    ADD CONSTRAINT fk_t_transaksjon_k_trans_tilst_t
        FOREIGN KEY (k_trans_tilst_t) REFERENCES t_k_trans_tilst_t (k_trans_tilst_t)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_transaksjon
    ADD CONSTRAINT fk_t_transaksjon_k_trans_tolkning
        FOREIGN KEY (k_trans_tolkning) REFERENCES t_k_trans_tolkning (k_trans_tolkning)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_transaksjon
    ADD CONSTRAINT fk_t_transaksjon_k_transaksjon_s
        FOREIGN KEY (k_transaksjon_s) REFERENCES t_k_transaksjon_s (k_transaksjon_s)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_transaksjon
    ADD CONSTRAINT fk_t_transaksjon_k_trekkansvar
        FOREIGN KEY (k_trekkansvar) REFERENCES t_k_trekkansvar (k_trekkansvar)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_transaksjon
    ADD CONSTRAINT fk_t_transaksjon_person_id
        FOREIGN KEY (person_id) REFERENCES t_person (person_id)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_transaksjon
    ADD CONSTRAINT fk_t_transaksjon_trans_tilstand_id
        FOREIGN KEY (trans_tilstand_id) REFERENCES t_trans_tilstand (trans_tilstand_id)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_trans_tilstand
    ADD CONSTRAINT fk_t_trans_tilstand_k_trans_tilst_t
        FOREIGN KEY (k_trans_tilst_t) REFERENCES t_k_trans_tilst_t (k_trans_tilst_t)
        ON DELETE RESTRICT NOT DEFERRABLE;
ALTER TABLE t_trans_tilstand
    ADD CONSTRAINT fk_t_trans_tilstand_transaksjon_id
        FOREIGN KEY (transaksjon_id) REFERENCES t_transaksjon (transaksjon_id)
        ON DELETE RESTRICT NOT DEFERRABLE;

CREATE INDEX xif1t_trans_tilsta ON t_trans_tilstand (k_trans_tilst_t);
CREATE INDEX xif2t_trans_tilsta ON t_trans_tilstand (transaksjon_id);
CREATE INDEX xif1t_transaksjon ON t_transaksjon (k_transaksjon_s);
CREATE INDEX xif11t_transaksjon ON t_transaksjon (k_trans_tolkning);
CREATE INDEX xif12t_transaksjon ON t_transaksjon (trans_tilstand_id);
CREATE INDEX xif2t_transaksjon ON t_transaksjon (k_belop_t);
CREATE INDEX xif4t_transaksjon ON t_transaksjon (person_id);
CREATE INDEX xif5t_transaksjon ON t_transaksjon (fil_info_id);
CREATE INDEX xif6t_transaksjon ON t_transaksjon (k_anviser);
CREATE INDEX xie1t_transaksjon ON t_transaksjon (mot_id);
CREATE INDEX xie2t_transaksjon ON t_transaksjon (os_id_fk, os_linje_id_fk, dato_tom);
CREATE INDEX xie3t_transaksjon ON t_transaksjon (trekkvedtak_id_fk);
CREATE INDEX xie4t_transaksjon ON t_transaksjon (trans_eks_id_fk, k_anviser);
CREATE INDEX xie5t_transaksjon ON t_transaksjon (k_anviser, k_art, k_belop_t);
CREATE INDEX xif14t_transaksjon ON t_transaksjon (k_trans_tilst_t);
CREATE INDEX itrnsa3 ON t_transaksjon (dato_fom);
CREATE INDEX xif2t_avv_transaks ON t_avv_transaksjon (k_transaksjon_s);
CREATE INDEX xie1t_avv_transaks ON t_avv_transaksjon (trans_eks_id_fk, avsender);
CREATE INDEX xif1t_fil_info ON t_fil_info (k_fil_s);
CREATE INDEX xif2t_fil_info ON t_fil_info (k_anviser);
CREATE INDEX xif3t_fil_info ON t_fil_info (k_fil_t);
CREATE INDEX xif5t_fil_info ON t_fil_info (k_fil_tilstand_t);
CREATE INDEX xif1t_k_gyldig_kom ON t_k_gyldig_kombin (k_art);
CREATE INDEX xif2t_k_gyldig_kom ON t_k_gyldig_kombin (k_belop_t);
CREATE INDEX xif5t_k_gyldig_kom ON t_k_gyldig_kombin (k_anviser);
CREATE INDEX xif6t_k_gyldig_kom ON t_k_gyldig_kombin (k_fagomrade);
CREATE INDEX xif9t_k_gyldig_kom ON t_k_gyldig_kombin (k_trekkalt_t);
CREATE INDEX xif1t_inn_transaks ON t_inn_transaksjon (fil_info_id);
CREATE INDEX xif2t_inn_transaks ON t_inn_transaksjon (k_transaksjon_s);
CREATE INDEX xie1t_inn_transaks ON t_inn_transaksjon (trans_id_fk, avsender);
CREATE INDEX iinntrn2 ON t_inn_transaksjon (fnr_fk);
CREATE INDEX xif2t_retur_til_an ON t_retur_til_anv (k_anviser);
CREATE INDEX xif5t_retur_til_an ON t_retur_til_anv (fil_info_inn_id);
CREATE INDEX xif7t_retur_til_an ON t_retur_til_anv (fil_info_ut_id);
CREATE INDEX xif9t_retur_til_an ON t_retur_til_anv (k_retur_t);
CREATE INDEX xie1t_retur_til_an ON t_retur_til_anv (os_id_fk, os_linje_id_fk, dato_tom);
CREATE INDEX xie3t_retur_til_an ON t_retur_til_anv (trans_eks_id_fk);
CREATE INDEX xie2retur_til_anv ON t_retur_til_anv (trekkvedtak_id_fk, fil_info_inn_id);
CREATE INDEX xie6retur_til_anv ON t_retur_til_anv (k_anviser, fil_info_inn_id, duplikat, fil_info_ut_id, dato_status);
CREATE INDEX iretilan ON t_retur_til_anv (mot_id);
CREATE INDEX xif1t_lopenr ON t_lopenr (k_fil_t);
CREATE INDEX xif2t_lopenr ON t_lopenr (k_anviser);
