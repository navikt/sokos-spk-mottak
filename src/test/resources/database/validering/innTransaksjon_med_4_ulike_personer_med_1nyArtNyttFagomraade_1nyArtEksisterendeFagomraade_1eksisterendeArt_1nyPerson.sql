insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, REF_TRANS_ID, TEKSTKODE,
                               RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, GRAD, GRAD_STR)
values (20000816, null, '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517600', '2023-06-01', '2023-06-30', '2023-05-25', 1151600,
        'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null),
       (20000816, null, '66043800215', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517601', '2023-06-01', '2023-06-30', '2023-05-25', 1151600,
        'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null),
       (20000816, null, '66043800216', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517602', '2023-06-01', '2023-06-30', '2023-05-25', 1151600,
        'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null),
       (20000816, null, '66043800217', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517603', '2023-06-01', '2023-06-30', '2023-05-25', 1151600,
        'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null);

insert into T_PERSON (PERSON_ID, FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON)
values  (1, '66043800214', '2008-12-31 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1),
        (2, '66043800215', '2008-12-31 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1),
        (3, '66043800216', '2008-12-31 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1),
        (4, '66043800217', '2008-12-31 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1);

insert into T_TRANSAKSJON (TRANSAKSJON_ID, TRANS_TILSTAND_ID, FIL_INFO_ID, K_TRANSAKSJON_S, PERSON_ID, K_BELOP_T, K_ART, K_ANVISER, FNR_FK, UTBETALES_TIL, OS_ID_FK, OS_LINJE_ID_FK, DATO_FOM, DATO_TOM,
                           DATO_ANVISER, DATO_PERSON_FOM, DATO_REAK_FOM, BELOP, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_EKS_ID_FK, K_TRANS_TOLKNING, SENDT_TIL_OPPDRAG, TREKKVEDTAK_ID_FK, FNR_ENDRET,
                           MOT_ID, OS_STATUS, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, K_TREKKANSVAR, K_TRANS_TILST_T, GRAD)
values (98, null, 20000816, '00', 1, '02', 'UFT', 'SPK', '66043800214', null, null, null, '2023-04-01', '2023-04-30', '2023-04-25', '1900-01-01', '1900-01-01', 51700, null, null, '02',
        '11', 'NY', '0', null, '0', '98', null, '2024-04-24 08:45:08.998930', 'sokos-spk-mottak', '2024-04-24 08:45:08.999190', 'sokos-spk-mottak', 1, '4819', 'ORO', null),
       (99, null, 20000816, '00', 1, '01', 'UFE', 'SPK', '66043800214', null, null, null, '2023-05-01', '2023-05-31', '2023-04-25', '1900-01-01', '1900-01-01', 51700, null, null, '02',
        '22', 'NY_EKSIST', '0', null, '0', '99', null, '2024-04-24 08:45:08.998930', 'sokos-spk-mottak', '2024-04-24 08:45:08.999190', 'sokos-spk-mottak', 1, '4819', 'ORO', null),
       (100, null, 20000816, '00', 2, '01', 'RNT', 'SPK', '66043800215', null, null, null, '2023-04-01', '2023-04-30', '2023-04-25', '1900-01-01', '1900-01-01', 51700, null, null, '02',
        '33', 'NY', '0', null, '0', '100', null, '2024-04-24 08:45:08.998930', 'sokos-spk-mottak', '2024-04-24 08:45:08.999190', 'sokos-spk-mottak', 1, '4819', 'ORO', null),
       (101, null, 20000816, '00', 2, '02', 'ALD', 'SPK', '66043800215', null, null, null, '2023-05-01', '2023-05-31', '2023-04-25', '1900-01-01', '1900-01-01', 51700, null, null, '02',
        '44', 'NY_EKSIST', '0', null, '0', '101', null, '2024-04-24 08:45:08.998930', 'sokos-spk-mottak', '2024-04-24 08:45:08.999190', 'sokos-spk-mottak', 1, '4819', 'ORO', null),
       (102, null, 20000816, '00', 3, '02', 'ETT', 'SPK', '66043800216', null, null, null, '2023-05-01', '2023-05-31', '2023-04-25', '1900-01-01', '1900-01-01', 51700, null, null, '02',
        '55', 'NY', '0', null, '0', '102', null, '2024-04-24 08:45:08.998930', 'sokos-spk-mottak', '2024-04-24 08:45:08.999190', 'sokos-spk-mottak', 1, '4819', 'ORO', null);