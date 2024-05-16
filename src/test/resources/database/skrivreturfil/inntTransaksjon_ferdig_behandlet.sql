insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, REF_TRANS_ID, TEKSTKODE,
                               RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, GRAD, GRAD_STR)
values (20000816, '00', '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 1151600,
        'J', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null),
       (20000816, '01', '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 1151600,
        'J', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null),
       (20000816, '02', '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 1151600,
        'J', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null),
       (20000816, '03', '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 1151600,
        'J', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null),
       (20000816, '04', '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 1151600,
        'J', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null),
       (20000816, '05', '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 1151600,
        'J', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null),
       (20000816, '09', '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 1151600,
        'J', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null),
       (20000816, '10', '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 1151600,
        'J', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null),
       (20000816, '11', '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 1151600,
        'J', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null);

insert into T_PERSON (FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON)
values ('66043800214', '2008-12-01 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1);


insert into T_FIL_INFO (FIL_INFO_ID, K_FIL_S, K_ANVISER, K_FIL_T, K_FIL_TILSTAND_T, FIL_NAVN, LOPENR, FEILTEKST, DATO_MOTTATT, DATO_SENDT, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV,
                        VERSJON)
values (20000816, '00', 'SPK', 'ANV', 'GOD', 'SPK_NAV_20242503_070026814_INL.txt', '34', null, '2024-01-31', null, '2024-04-10 09:28:49.749039', 'sokos-spk-mottak', '2024-04-10 09:28:49.749137',
        'sokos-spk-mottak', 1);