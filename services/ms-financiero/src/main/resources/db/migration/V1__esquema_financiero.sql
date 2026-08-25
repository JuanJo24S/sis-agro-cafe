CREATE TABLE balances_lote (
    lote_id        UUID          PRIMARY KEY,
    finca_id       UUID          NOT NULL,
    usuario_id     UUID          NOT NULL,
    costo_total    NUMERIC(18,2) NOT NULL DEFAULT 0,
    ingreso_total  NUMERIC(18,2) NOT NULL DEFAULT 0,
    actualizado_en TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version        BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX idx_balances_finca ON balances_lote (finca_id, usuario_id);

CREATE TABLE eventos_procesados (
    evento_id    UUID        PRIMARY KEY,
    topico       VARCHAR(60) NOT NULL,
    procesado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
