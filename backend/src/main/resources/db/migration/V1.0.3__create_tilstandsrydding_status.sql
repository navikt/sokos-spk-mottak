CREATE TABLE tilstandsrydding_status (
    id smallint PRIMARY KEY CHECK (id = 1),
    regelversjon varchar(100) NOT NULL,
    oppbevaring_maaneder integer NOT NULL CHECK (oppbevaring_maaneder >= 6),
    grensetidspunkt timestamp(6) without time zone NOT NULL,
    forste_id bigint,
    ovre_id bigint,
    siste_ferdige_id bigint,
    fullfort boolean NOT NULL,
    sist_oppdatert timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    siste_fullfort_tidspunkt timestamp with time zone,
    CHECK (
        (forste_id IS NULL AND ovre_id IS NULL AND siste_ferdige_id IS NULL AND fullfort)
        OR (forste_id IS NOT NULL AND ovre_id IS NOT NULL AND forste_id <= ovre_id
            AND (siste_ferdige_id IS NULL OR siste_ferdige_id BETWEEN forste_id AND ovre_id))
    )
);

DO $$
BEGIN
    EXECUTE format(
        'GRANT SELECT, INSERT, UPDATE ON TABLE tilstandsrydding_status TO %I',
        current_database() || '-user'
    );
END
$$;
