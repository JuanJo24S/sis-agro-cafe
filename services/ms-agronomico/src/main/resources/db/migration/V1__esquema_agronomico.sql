CREATE TABLE fincas (
    id                   UUID          PRIMARY KEY,
    propietario_id       UUID          NOT NULL,
    nombre               VARCHAR(120)  NOT NULL,
    municipio            VARCHAR(120)  NOT NULL,
    departamento         VARCHAR(120),
    area_total_hectareas NUMERIC(12,4),
    fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_fincas_propietario ON fincas (propietario_id);

CREATE TABLE lotes (
    id                   UUID          PRIMARY KEY,
    finca_id             UUID          NOT NULL REFERENCES fincas (id) ON DELETE CASCADE,
    nombre               VARCHAR(120)  NOT NULL,
    variedad             VARCHAR(80)   NOT NULL,
    fecha_siembra        DATE          NOT NULL,
    area_hectareas       NUMERIC(12,4) NOT NULL,
    densidad_siembra     INTEGER,
    altura_msnm          INTEGER,
    ph_suelo             NUMERIC(4,2),
    tipo_suelo           VARCHAR(120),
    temperatura_promedio NUMERIC(5,2),
    fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_lotes_finca ON lotes (finca_id);

CREATE TABLE labores (
    id              UUID          PRIMARY KEY,
    lote_id         UUID          NOT NULL REFERENCES lotes (id) ON DELETE CASCADE,
    tipo            VARCHAR(20)   NOT NULL,
    descripcion     VARCHAR(300),
    fecha_ejecucion DATE          NOT NULL,
    jornales        NUMERIC(8,2),
    costo_mano_obra NUMERIC(14,2) NOT NULL DEFAULT 0,
    costo_insumos   NUMERIC(14,2) NOT NULL DEFAULT 0,
    costo_total     NUMERIC(14,2) NOT NULL DEFAULT 0,
    fecha_registro  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    anulada         BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_labores_tipo CHECK (tipo IN ('LIMPIA','FUMIGACION','DESHIERBE','PODA','OTRA'))
);
CREATE INDEX idx_labores_lote_fecha ON labores (lote_id, fecha_ejecucion);

CREATE TABLE fertilizaciones (
    id                 UUID          PRIMARY KEY,
    lote_id            UUID          NOT NULL REFERENCES lotes (id) ON DELETE CASCADE,
    estado             VARCHAR(20)   NOT NULL,
    fecha              DATE          NOT NULL,
    composicion        VARCHAR(120)  NOT NULL,
    dosis_kg_por_arbol NUMERIC(8,3),
    cantidad_total_kg  NUMERIC(12,2),
    costo_insumos      NUMERIC(14,2) NOT NULL DEFAULT 0,
    costo_mano_obra    NUMERIC(14,2) NOT NULL DEFAULT 0,
    costo_total        NUMERIC(14,2) NOT NULL DEFAULT 0,
    observaciones      VARCHAR(300),
    fecha_registro     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_fertilizaciones_estado CHECK (estado IN ('PROGRAMADA','EJECUTADA','CANCELADA'))
);
CREATE INDEX idx_fertilizaciones_lote_fecha ON fertilizaciones (lote_id, fecha);

CREATE TABLE claves_idempotencia (
    clave      UUID        PRIMARY KEY,
    recurso    VARCHAR(40) NOT NULL,
    recurso_id UUID        NOT NULL,
    usuario_id UUID        NOT NULL,
    creada_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);
