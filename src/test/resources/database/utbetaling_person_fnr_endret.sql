insert into T_TRANSAKSJON (TRANSAKSJON_ID, TRANS_TILSTAND_ID, FIL_INFO_ID, K_TRANSAKSJON_S, PERSON_ID, K_BELOP_T, K_ART, K_ANVISER, FNR_FK, UTBETALES_TIL, OS_ID_FK, OS_LINJE_ID_FK, DATO_FOM,
                           DATO_TOM, DATO_ANVISER, DATO_PERSON_FOM, DATO_REAK_FOM, BELOP, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_EKS_ID_FK, K_TRANS_TOLKNING, SENDT_TIL_OPPDRAG,
                           TREKKVEDTAK_ID_FK, FNR_ENDRET, MOT_ID, OS_STATUS, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, SALDO, KID, PRIORITET, K_TREKKANSVAR,
                           K_TRANS_TILST_T, GRAD)
values (20025925, 20569297, 20000002, '00', 2168245, '01', 'ALD', 'SPK', '07050842880', null, '2302468', '1', '2009-01-01', '2009-01-31', '2008-12-20', '2009-01-01', null, 978500, '', null, '02',
        '9805367', 'NY_EKSIST', '0', null, '1', '20025925', null, '2008-12-21 10:47:17.435000', 'BMOT001', '2011-01-12 23:33:25.862434', 'PATCH348', 3, null, null, null, null, 'OPR', 100);


insert into T_PERSON (PERSON_ID, FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON)
values (2168245, '07050842880', '2008-12-01 15:44:18.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1);