-- TABLE T_LOPENR
DROP TABLE IF EXISTS T_LOPENR;

CREATE TABLE T_LOPENR
(
    LOPENR_ID      INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    SISTE_LOPENR   INTEGER,
    K_FIL_T        VARCHAR(20),
    K_ANVISER      VARCHAR(20),
    DATO_OPPRETTET TIMESTAMP(6) NOT NULL,
    OPPRETTET_AV   VARCHAR(20)  NOT NULL,
    DATO_ENDRET    TIMESTAMP(6) NOT NULL,
    ENDRET_AV      VARCHAR(20)  NOT NULL,
    VERSJON        INTEGER      NOT NULL
);

INSERT INTO T_LOPENR (LOPENR_ID, SISTE_LOPENR, K_FIL_T, K_ANVISER, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON)
VALUES (1, 33, 'ANV', 'SPK', '2024-02-01 00:00:00.000000', 'Endre', '2024-04-03 11:33:33.260122', 'sokos-spk-mottak', 1);


-- TABLE T_INN_TRANSAKSJON
DROP TABLE IF EXISTS T_INN_TRANSAKSJON;

CREATE TABLE T_INN_TRANSAKSJON
(
    INN_TRANSAKSJON_ID INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    FIL_INFO_ID        INTEGER      NOT NULL,
    K_TRANSAKSJON_S    VARCHAR(20),
    FNR_FK             CHAR(11),
    BELOPSTYPE         VARCHAR(20),
    ART                VARCHAR(20),
    AVSENDER           VARCHAR(20),
    UTBETALES_TIL      CHAR(11),
    DATO_FOM_STR       VARCHAR(20),
    DATO_TOM_STR       VARCHAR(20),
    DATO_ANVISER_STR   VARCHAR(20),
    BELOP_STR          VARCHAR(20),
    REF_TRANS_ID       VARCHAR(20),
    TEKSTKODE          VARCHAR(20),
    RECTYPE            VARCHAR(20),
    TRANS_ID_FK        VARCHAR(20),
    DATO_FOM           DATE,
    DATO_TOM           DATE,
    DATO_ANVISER       DATE,
    BELOP              INTEGER,
    BEHANDLET          CHAR(1),
    DATO_OPPRETTET     TIMESTAMP(6) NOT NULL,
    OPPRETTET_AV       VARCHAR(20)  NOT NULL,
    DATO_ENDRET        TIMESTAMP(6) NOT NULL,
    ENDRET_AV          VARCHAR(20)  NOT NULL,
    VERSJON            INTEGER      NOT NULL,
    PRIORITET_STR      VARCHAR(20),
    TREKKANSVAR        VARCHAR(20),
    SALDO_STR          VARCHAR(20),
    KID                VARCHAR(26),
    PRIORITET          DATE,
    SALDO              INTEGER,
    GRAD               INTEGER,
    GRAD_STR           VARCHAR(4)
);

-- TABLE T_FIL_INFO
DROP TABLE IF EXISTS T_FIL_INFO;

CREATE TABLE T_FIL_INFO
(
    FIL_INFO_ID      INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    K_FIL_S          VARCHAR(20)  NOT NULL,
    K_ANVISER        VARCHAR(20),
    K_FIL_T          VARCHAR(20)  NOT NULL,
    K_FIL_TILSTAND_T VARCHAR(20),
    FIL_NAVN         VARCHAR(200),
    LOPENR           VARCHAR(20),
    FEILTEKST        VARCHAR(200),
    DATO_MOTTATT     DATE,
    DATO_SENDT       DATE,
    DATO_OPPRETTET   TIMESTAMP(6) NOT NULL,
    OPPRETTET_AV     VARCHAR(20)  NOT NULL,
    DATO_ENDRET      TIMESTAMP(6) NOT NULL,
    ENDRET_AV        VARCHAR(20)  NOT NULL,
    VERSJON          INTEGER      NOT NULL
);

-- TABLE T_PERSON
DROP TABLE IF EXISTS T_PERSON;

CREATE TABLE T_PERSON
(
    PERSON_ID      INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    FNR_FK         CHAR(11)     not null,
    DATO_OPPRETTET TIMESTAMP(6) not null,
    OPPRETTET_AV   VARCHAR(20)  not null,
    DATO_ENDRET    TIMESTAMP(6) not null,
    ENDRET_AV      VARCHAR(20)  not null,
    VERSJON        INTEGER      not null
);

-- TABLE T_TRANS_TILSTAND

DROP TABLE IF EXISTS T_TRANS_TILSTAND;

CREATE TABLE T_TRANS_TILSTAND
(
    TRANS_TILSTAND_ID INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    TRANSAKSJON_ID    INTEGER,
    K_TRANS_TILST_T   VARCHAR(20),
    FEILKODE          VARCHAR(20),
    FEILKODEMELDING   VARCHAR(200),
    DATO_OPPRETTET    TIMESTAMP(6) not null,
    OPPRETTET_AV      VARCHAR(20)  not null,
    DATO_ENDRET       TIMESTAMP(6) not null,
    ENDRET_AV         VARCHAR(20)  not null,
    VERSJON           INTEGER      not null
);

-- TABLE T_INN_TRANSAKSJON

DROP TABLE IF EXISTS T_INN_TRANSAKSJON;

create table T_INN_TRANSAKSJON
(
    INN_TRANSAKSJON_ID INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    FIL_INFO_ID        INTEGER      not null,
    K_TRANSAKSJON_S    VARCHAR(20),
    FNR_FK             CHAR(11),
    BELOPSTYPE         VARCHAR(20),
    ART                VARCHAR(20),
    AVSENDER           VARCHAR(20),
    UTBETALES_TIL      CHAR(11),
    DATO_FOM_STR       VARCHAR(20),
    DATO_TOM_STR       VARCHAR(20),
    DATO_ANVISER_STR   VARCHAR(20),
    BELOP_STR          VARCHAR(20),
    REF_TRANS_ID       VARCHAR(20),
    TEKSTKODE          VARCHAR(20),
    RECTYPE            VARCHAR(20),
    TRANS_ID_FK        VARCHAR(20),
    DATO_FOM           DATE,
    DATO_TOM           DATE,
    DATO_ANVISER       DATE,
    BELOP              INTEGER,
    BEHANDLET          CHAR(1),
    DATO_OPPRETTET     TIMESTAMP(6) not null,
    OPPRETTET_AV       VARCHAR(20)  not null,
    DATO_ENDRET        TIMESTAMP(6) not null,
    ENDRET_AV          VARCHAR(20)  not null,
    VERSJON            INTEGER      not null,
    PRIORITET_STR      VARCHAR(20),
    TREKKANSVAR        VARCHAR(20),
    SALDO_STR          VARCHAR(20),
    KID                VARCHAR(26),
    PRIORITET          DATE,
    SALDO              INTEGER,
    GRAD               INTEGER,
    GRAD_STR           VARCHAR(4)
);

-- TABLE T_TRANSAKSJON

DROP TABLE IF EXISTS T_TRANSAKSJON;

create table T_TRANSAKSJON
(
    TRANSAKSJON_ID    INTEGER      not null,
    TRANS_TILSTAND_ID INTEGER,
    FIL_INFO_ID       INTEGER      not null,
    K_TRANSAKSJON_S   VARCHAR(20)  not null,
    PERSON_ID         INTEGER      not null,
    K_BELOP_T         VARCHAR(20)  not null,
    K_ART             VARCHAR(20)  not null,
    K_ANVISER         VARCHAR(20)  not null,
    FNR_FK            CHAR(11)     not null,
    UTBETALES_TIL     CHAR(11),
    OS_ID_FK          VARCHAR(20),
    OS_LINJE_ID_FK    VARCHAR(20),
    DATO_FOM          DATE         not null,
    DATO_TOM          DATE,
    DATO_ANVISER      DATE,
    DATO_PERSON_FOM   DATE         not null,
    DATO_REAK_FOM     DATE,
    BELOP             INTEGER,
    REF_TRANS_ID      VARCHAR(20),
    TEKSTKODE         VARCHAR(20),
    RECTYPE           VARCHAR(20),
    TRANS_EKS_ID_FK   VARCHAR(20),
    K_TRANS_TOLKNING  VARCHAR(20),
    SENDT_TIL_OPPDRAG CHAR(1)      not null,
    TREKKVEDTAK_ID_FK VARCHAR(20),
    FNR_ENDRET        CHAR(1)      not null,
    MOT_ID            VARCHAR(20),
    OS_STATUS         VARCHAR(20),
    DATO_OPPRETTET    TIMESTAMP(6) not null,
    OPPRETTET_AV      VARCHAR(20)  not null,
    DATO_ENDRET       TIMESTAMP(6) not null,
    ENDRET_AV         VARCHAR(20)  not null,
    VERSJON           INTEGER      not null,
    SALDO             INTEGER,
    KID               VARCHAR(26),
    PRIORITET         DATE,
    K_TREKKANSVAR     VARCHAR(4),
    K_TRANS_TILST_T   VARCHAR(20),
    GRAD              INTEGER
);

-- TABLE T_AVV_TRANSAKSJON

DROP TABLE IF EXISTS T_AVV_TRANSAKSJON;

create table T_AVV_TRANSAKSJON
(
    AVV_TRANSAKSJON_ID INTEGER      not null,
    FIL_INFO_ID        INTEGER      not null,
    K_TRANSAKSJON_S    VARCHAR(20)  not null,
    FNR_FK             CHAR(11),
    BELOPSTYPE         VARCHAR(20),
    ART                VARCHAR(20),
    AVSENDER           VARCHAR(20),
    UTBETALES_TIL      CHAR(11),
    DATO_FOM           VARCHAR(20),
    DATO_TOM           VARCHAR(20),
    DATO_ANVISER       VARCHAR(20),
    BELOP              VARCHAR(20),
    REF_TRANS_ID       VARCHAR(20),
    TEKSTKODE          VARCHAR(20),
    RECTYPE            VARCHAR(20),
    TRANS_EKS_ID_FK    VARCHAR(20),
    DATO_OPPRETTET     TIMESTAMP(6) not null,
    OPPRETTET_AV       VARCHAR(20)  not null,
    DATO_ENDRET        TIMESTAMP(6) not null,
    ENDRET_AV          VARCHAR(20)  not null,
    VERSJON            INTEGER      not null,
    PRIORITET          VARCHAR(20),
    SALDO              VARCHAR(20),
    TREKKANSVAR        VARCHAR(20),
    KID                VARCHAR(26),
    GRAD               INTEGER
);

-- TABLE T_K_BELOP_T

DROP TABLE IF EXISTS T_K_BELOP_T;

create table T_K_BELOP_T
(
    K_BELOP_T      VARCHAR(20)  not null,
    DEKODE         VARCHAR(200) not null,
    DATO_FOM       DATE         not null,
    DATO_TOM       DATE,
    ER_GYLDIG      CHAR(1)      not null,
    DATO_OPPRETTET TIMESTAMP(6) not null,
    OPPRETTET_AV   VARCHAR(20)  not null,
    DATO_ENDRET    TIMESTAMP(6) not null,
    ENDRET_AV      VARCHAR(20)  not null
);

insert into T_K_BELOP_T (K_BELOP_T, DEKODE, DATO_FOM, DATO_TOM, ER_GYLDIG, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV)
values ('01', 'Skattepliktig utbetaling', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2007-09-11 13:13:00.000000', 'F.Andersen'),
       ('02', 'Ikke skattepliktig utbetaling', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2007-09-11 13:13:00.000000', 'F.Andersen'),
       ('03', 'Trekk', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2007-09-11 13:13:00.000000', 'F.Andersen'),
       ('04', 'Annulering utbetaling', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2007-09-11 13:13:00.000000', 'F.Andersen'),
       ('05', 'Annulering trekk', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2007-09-11 13:13:00.000000', 'F.Andersen');

-- TABLE T_K_ART

DROP TABLE IF EXISTS T_K_ART;

create table T_K_ART
(
    K_ART          VARCHAR(20)  not null,
    DEKODE         VARCHAR(200) not null,
    DATO_FOM       DATE         not null,
    DATO_TOM       DATE,
    ER_GYLDIG      CHAR(1)      not null,
    DATO_OPPRETTET TIMESTAMP(6) not null,
    OPPRETTET_AV   VARCHAR(20)  not null,
    DATO_ENDRET    TIMESTAMP(6) not null,
    ENDRET_AV      VARCHAR(20)  not null
);

insert into T_K_ART (K_ART, DEKODE, DATO_FOM, DATO_TOM, ER_GYLDIG, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV)
values ('AFP', 'AFP', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2007-09-11 13:13:00.000000', 'F.Andersen'),
       ('ALD', 'Alder', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2007-09-11 13:13:00.000000', 'F.Andersen'),
       ('BPE', 'Barnepensjon', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2007-09-11 13:13:00.000000', 'F.Andersen'),
       ('ETT', 'Gjenlevendepensjon', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2007-09-11 13:13:00.000000', 'F.Andersen'),
       ('KBT', 'Kommunalt boligtilskudd', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2007-09-11 13:13:00.000000', 'F.Andersen'),
       ('KBY', 'Bykassetillegg', '1899-12-31', null, '1', '2008-07-04 13:13:00.000000', 'PSB2812', '2008-07-04 13:13:00.000000', 'PSB2812'),
       ('KTB', 'Kommunalt barnetillegg', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2008-07-04 13:13:00.000000', 'PSB2812'),
       ('KTP', 'Kommunal tilleggspensjon', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2007-09-11 13:13:00.000000', 'F.Andersen'),
       ('MVA', 'SKD merverdiavgift', '1899-12-31', null, '1', '2009-08-20 13:13:00.000000', 'Mugunthan D.', '2009-08-20 13:13:00.000000', 'Mugunthan D.'),
       ('RNT', 'Renter', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2007-09-11 13:13:00.000000', 'F.Andersen'),
       ('TFT', 'Trekk folketrygd ved tilbakekreving', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2007-09-11 13:13:00.000000', 'F.Andersen'),
       ('TLA', 'Lånetrekk', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2008-07-04 13:13:00.000000', 'PSB2812'),
       ('TMS', 'Trekk i SPK ytelse ved tilbakekreving', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2007-09-11 13:13:00.000000', 'F.Andersen'),
       ('TNS', 'NSB trekk', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2007-09-11 13:13:00.000000', 'F.Andersen'),
       ('UFO', 'Ufør', '1899-12-31', null, '1', '2007-09-11 13:13:00.000000', 'F.Andersen', '2007-09-11 13:13:00.000000', 'F.Andersen'),
       ('UFT', 'Uførepensjon 1.1.2015', '1899-12-31', null, '1', '2014-12-01 17:12:07.783445', 'PATCH_PK-17312', '2014-12-01 17:12:07.783445', 'PATCH_PK-17312'),
       ('U67', 'Uførepensjon over 67 år', '1899-12-31', null, '1', '2012-12-07 22:08:09.795414', 'M.Khan', '2012-12-07 22:08:09.795414', 'M.Khan'),
       ('UFE', 'Uføre etteroppgjør', '1899-12-31', null, '1', '2016-09-24 00:54:09.896870', 'PK-30848', '2016-09-24 00:54:09.896870', 'PK-30848'),
       ('AFPNY', 'AFP Ny', '2024-02-01', null, '1', '2024-03-11 17:51:25.557579', 'TOB-3425', '2024-03-11 17:51:25.557579', 'TOB-3425'),
       ('APBR', 'Alder, oppsatt brutto', '2024-02-01', null, '1', '2024-03-11 17:51:25.588372', 'TOB-3425', '2024-03-11 17:51:25.588372', 'TOB-3425'),
       ('APBTP', 'Alder, betinget tjenestepensjon', '2024-02-01', null, '1', '2024-03-11 17:51:25.617782', 'TOB-3425', '2024-03-11 17:51:25.617782', 'TOB-3425'),
       ('APOT', 'Alder, overgangstillegg', '2024-02-01', null, '1', '2024-03-11 17:51:25.647735', 'TOB-3425', '2024-03-11 17:51:25.647735', 'TOB-3425'),
       ('APPP', 'Alder, påslagspensjon', '2024-02-01', null, '1', '2024-03-11 17:51:25.678671', 'TOB-3425', '2024-03-11 17:51:25.678671', 'TOB-3425');

-- TABLE T_K_GYLDIG_KOMBIN

DROP TABLE IF EXISTS T_K_GYLDIG_KOMBIN;

create table T_K_GYLDIG_KOMBIN
(
    K_GYLDIG_KOMBIN_ID INTEGER      not null,
    K_ART              VARCHAR(20)  not null,
    K_BELOP_T          VARCHAR(20)  not null,
    K_TREKKGRUPPE      VARCHAR(20),
    K_TREKK_T          VARCHAR(20),
    K_TREKKALT_T       VARCHAR(20),
    K_ANVISER          VARCHAR(20)  not null,
    K_FAGOMRADE        VARCHAR(20),
    OS_KLASSIFIKASJON  VARCHAR(40),
    DATO_FOM           DATE         not null,
    DATO_TOM           DATE,
    ER_GYLDIG          CHAR(1)      not null,
    DATO_OPPRETTET     TIMESTAMP(6) not null,
    OPPRETTET_AV       VARCHAR(20)  not null,
    DATO_ENDRET        TIMESTAMP(6) not null,
    ENDRET_AV          VARCHAR(20)  not null
);

insert into T_K_GYLDIG_KOMBIN (K_GYLDIG_KOMBIN_ID, K_ART, K_BELOP_T, K_TREKKGRUPPE, K_TREKK_T, K_TREKKALT_T, K_ANVISER, K_FAGOMRADE, OS_KLASSIFIKASJON, DATO_FOM, DATO_TOM, ER_GYLDIG, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV)
values (1, 'ALD', '01', null, null, null, 'SPK', 'PENSPK', 'PENSPKALD01', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (2, 'ALD', '02', null, null, null, 'SPK', 'PENSPK', 'PENSPKALD-OP', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (3, 'ALD', '04', null, null, null, 'SPK', 'PENSPK', null, '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-09-19 13:13:00.000000', 'Ø. Wiborg'),
       (4, 'AFP', '01', null, null, null, 'SPK', 'PENSPK', 'PENSPKAFP01', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (5, 'AFP', '02', null, null, null, 'SPK', 'PENSPK', 'PENSPKAFP-OP', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (6, 'AFP', '04', null, null, null, 'SPK', 'PENSPK', null, '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-09-19 13:13:00.000000', 'Ø. Wiborg'),
       (7, 'UFO', '01', null, null, null, 'SPK', 'PENSPK', 'PENSPKUFO01', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'MEHTAB KHAN'),
       (8, 'UFO', '02', null, null, null, 'SPK', 'PENSPK', 'PENSPKUFO-OP', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (9, 'UFO', '04', null, null, null, 'SPK', 'PENSPK', null, '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-09-19 13:13:00.000000', 'Ø. Wiborg'),
       (10, 'ETT', '01', null, null, null, 'SPK', 'PENSPK', 'PENSPKETT01', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (11, 'ETT', '02', null, null, null, 'SPK', 'PENSPK', 'PENSPKETT-OP', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (12, 'ETT', '04', null, null, null, 'SPK', 'PENSPK', null, '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (13, 'BPE', '01', null, null, null, 'SPK', 'PENSPK', 'PENSPKBPE01', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (14, 'BPE', '02', null, null, null, 'SPK', 'PENSPK', 'PENSPKBPE02', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (15, 'BPE', '04', null, null, null, 'SPK', 'PENSPK', null, '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (16, 'RNT', '01', null, null, null, 'SPK', 'PENSPK', 'PENSPKRNT01', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (17, 'RNT', '02', null, null, null, 'SPK', 'PENSPK', 'PENSPKRNT-OP', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (18, 'RNT', '04', null, null, null, 'SPK', 'PENSPK', null, '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (19, 'TLA', '03', 'SPK', 'SPK1', 'LOPM', 'SPK', null, null, '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (20, 'TLA', '05', 'SPK', 'SPK1', 'LOPM', 'SPK', null, null, '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (21, 'TNS', '03', 'SPK', 'SPK1', 'LOPM', 'SPK', null, null, '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (22, 'TNS', '05', 'SPK', 'SPK1', 'LOPM', 'SPK', null, null, '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (23, 'TMS', '03', 'SPK', 'SPK2', 'LOPM', 'SPK', null, null, '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (24, 'TMS', '05', 'SPK', 'SPK2', 'LOPM', 'SPK', null, null, '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (25, 'TFT', '03', 'LIV', 'SPK2', 'LOPM', 'SPK', null, null, '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-11-10 16:00:00.000000', 'Johan Dahl'),
       (26, 'TFT', '05', 'LIV', 'SPK2', 'LOPM', 'SPK', null, null, '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (27, 'ALD', '01', null, null, null, 'PTS', 'PENPTS', 'PENPTSALD01', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (28, 'ALD', '02', null, null, null, 'PTS', 'PENPTS', 'PENPTSALD02', '1899-12-31', null, '1', '2008-09-19 13:13:00.000000', 'Ø. Wiborg', '2008-09-19 13:13:00.000000', 'Ø. Wiborg'),
       (29, 'UFO', '01', null, null, null, 'PTS', 'PENPTS', 'PENPTSUFO01', '1899-12-31', null, '0', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (30, 'UFO', '02', null, null, null, 'PTS', 'PENPTS', 'PENPTSUFO02', '1899-12-31', null, '0', '2008-09-19 13:13:00.000000', 'Ø. Wiborg', '2008-09-19 13:13:00.000000', 'Ø. Wiborg'),
       (31, 'ETT', '01', null, null, null, 'PTS', 'PENPTS', 'PENPTSETT01', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (32, 'ETT', '02', null, null, null, 'PTS', 'PENPTS', 'PENPTSETT02', '1899-12-31', null, '1', '2008-09-19 13:13:00.000000', 'Ø. Wiborg', '2008-09-19 13:13:00.000000', 'Ø. Wiborg'),
       (33, 'BPE', '01', null, null, null, 'PTS', 'PENPTS', 'PENPTSBPE01', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-02-04 16:00:00.000000', 'A.Braathen'),
       (34, 'BPE', '02', null, null, null, 'PTS', 'PENPTS', 'PENPTSBPE02', '1899-12-31', null, '1', '2008-09-19 13:13:00.000000', 'Ø. Wiborg', '2008-09-19 13:13:00.000000', 'Ø. Wiborg'),
       (35, 'KTP', '01', null, null, null, 'OK', 'PENOSLO', 'PENOSLOKTP01', '1899-12-31', null, '1', '2008-09-19 13:13:00.000000', 'Ø. Wiborg', '2008-09-19 13:13:00.000000', 'Ø. Wiborg'),
       (36, 'KTP', '02', null, null, null, 'OK', 'PENOSLO', 'PENOSLOKTP02', '1899-12-31', null, '1', '2008-09-19 13:13:00.000000', 'Ø. Wiborg', '2008-09-19 13:13:00.000000', 'Ø. Wiborg'),
       (37, 'KTB', '01', null, null, null, 'OK', 'PENOSLO', 'PENOSLOKTB01', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-07-04 16:00:00.000000', 'PSB2812'),
       (38, 'KTB', '02', null, null, null, 'OK', 'PENOSLO', 'PENOSLOKTB02', '1899-12-31', null, '1', '2008-09-19 13:13:00.000000', 'Ø. Wiborg', '2008-09-19 13:13:00.000000', 'Ø. Wiborg'),
       (39, 'KBT', '02', null, null, null, 'OK', 'PENOSLO', 'PENOSLOKBT02', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-07-04 16:00:00.000000', 'PSB2812'),
       (40, 'KBY', '01', null, null, null, 'OK', 'PENOSLO', 'PENOSLOKBY01', '1899-12-31', null, '1', '2007-11-26 13:13:00.000000', 'J.Stillingen', '2008-07-04 16:00:00.000000', 'PSB2812'),
       (41, 'KBY', '02', null, null, null, 'OK', 'PENOSLO', 'PENOSLOKBY02', '1899-12-31', null, '1', '2008-09-19 13:13:00.000000', 'Ø. Wiborg', '2008-09-19 13:13:00.000000', 'Ø. Wiborg'),
       (42, 'MVA', '03', 'LIV', 'KRED', 'LOPM', 'SKDMVA', null, null, '1899-12-31', null, '1', '2009-08-20 13:13:00.000000', 'Mugunthan D.', '2009-08-20 13:13:00.000000', 'Mugunthan D.'),
       (43, 'MVA', '05', 'LIV', 'KRED', 'LOPM', 'SKDMVA', null, null, '1899-12-31', null, '1', '2009-08-20 13:13:00.000000', 'Mugunthan D.', '2009-08-20 13:13:00.000000', 'Mugunthan D.'),
       (46, 'U67', '01', null, null, null, 'SPK', 'PENSPK', 'PENSPKU67', '1899-12-31', null, '1', '2012-12-07 22:08:09.980936', 'M.Khan', '2012-12-07 22:08:09.980936', 'M.Khan'),
       (47, 'U67', '02', null, null, null, 'SPK', 'PENSPK', 'PENSPKU67-OP', '1899-12-31', null, '1', '2012-12-07 22:08:10.469008', 'M.Khan', '2012-12-07 22:08:10.469008', 'M.Khan'),
       (48, 'U67', '04', null, null, null, 'SPK', 'PENSPK', null, '1899-12-31', null, '1', '2012-12-07 22:08:10.470395', 'M.Khan', '2012-12-07 22:08:10.470395', 'M.Khan'),
       (81, 'UFT', '02', null, null, null, 'SPK', 'UFORESPK', 'UFORESPKUT-IOP', '1899-12-31', null, '1', '2014-12-01 17:12:07.973581', 'PATCH_PK-17312', '2014-12-01 17:12:07.973581', 'PATCH_PK-17312'),
       (82, 'UFT', '04', null, null, null, 'SPK', 'UFORESPK', 'UFORESPKUT-OP', '1899-12-31', null, '1', '2014-12-01 17:12:08.306580', 'PATCH_PK-17312', '2014-12-01 17:12:08.306580', 'PATCH_PK-17312'),
       (83, 'UFT', '01', null, null, null, 'SPK', 'UFORESPK', 'UFORESPKUT', '1899-12-31', null, '1', '2014-12-01 17:12:08.307946', 'PATCH_PK-17312', '2014-12-01 17:12:08.307946', 'PATCH_PK-17312'),
       (84, 'UFE', '01', null, null, null, 'SPK', 'UFORESPK', 'UFORESPKUT', '1899-12-31', null, '1', '2016-09-24 00:54:10.005838', 'PK-30848', '2016-09-24 00:54:10.005838', 'PK-30848'),
       (51, 'AFPNY', '01', null, null, null, 'SPK', 'PENSPK', 'PENAFPSTGP', '2024-02-01', null, '1', '2024-03-11 17:58:03.483555', 'TOB-3425', '2024-03-11 17:58:03.483555', 'TOB-3425'),
       (52, 'APBR', '01', null, null, null, 'SPK', 'PENSPK', 'PENAPGP', '2024-02-01', null, '1', '2024-03-11 17:58:03.518545', 'TOB-3425', '2024-03-11 17:58:03.518545', 'TOB-3425'),
       (53, 'APBTP', '01', null, null, null, 'SPK', 'PENSPK', 'PENAPTP', '2024-02-01', null, '1', '2024-03-11 17:58:03.551320', 'TOB-3425', '2024-03-11 17:58:03.551320', 'TOB-3425'),
       (54, 'APOT', '01', null, null, null, 'SPK', 'PENSPK', 'PENAPPT', '2024-02-01', null, '1', '2024-03-11 17:58:03.610397', 'TOB-3425', '2024-03-11 17:58:03.610397', 'TOB-3425'),
       (55, 'APPP', '01', null, null, null, 'SPK', 'PENSPK', 'PENAPMTI', '2024-02-01', null, '1', '2024-03-11 17:58:03.690298', 'TOB-3425', '2024-03-11 17:58:03.690298', 'TOB-3425');
