CREATE TABLE recolecciones (
    id                  UUID          PRIMARY KEY,
    lote_id             UUID          NOT NULL,
    finca_id            UUID          NOT NULL,
    usuario_id          UUID          NOT NULL,
    fecha               DATE          NOT NULL,
    peso_cascara_kg     NUMERIC(12,2) NOT NULL,
    valor_por_kg_arroba NUMERIC(14,2) NOT NULL,
    recolector          VARCHAR(120),
    estado_grano        VARCHAR(20)   NOT NULL DEFAULT 'CEREZA',
    costo_mano_obra     NUMERIC(14,2) NOT NULL DEFAULT 0,
    fecha_registro      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    anulada             BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_recolecciones_estado CHECK (
        estado_grano IN ('CEREZA','MOJADO','SECO_AGUA','SECO_TRILLA','ALMACENADO'))
);
CREATE INDEX idx_recolecciones_lote_fecha ON recolecciones (lote_id, fecha);

CREATE TABLE ventas (
    id                UUID          PRIMARY KEY,
    lote_id           UUID          NOT NULL,
    finca_id          UUID          NOT NULL,
    usuario_id        UUID          NOT NULL,
    fecha             DATE          NOT NULL,
    cantidad          NUMERIC(12,2) NOT NULL,
    precio_por_arroba NUMERIC(14,2) NOT NULL,
    comprador         VARCHAR(160)  NOT NULL,
    monto_total       NUMERIC(16,2) NOT NULL DEFAULT 0,
    fecha_registro    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    anulada           BOOLEAN       NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_ventas_usuario_fecha ON ventas (usuario_id, fecha);
CREATE INDEX idx_ventas_lote ON ventas (lote_id);

CREATE TABLE claves_idempotencia (
    clave      UUID        PRIMARY KEY,
    recurso    VARCHAR(40) NOT NULL,
    recurso_id UUID        NOT NULL,
    usuario_id UUID        NOT NULL,
    creada_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);
