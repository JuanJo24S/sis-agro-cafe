-- Una base de datos por microservicio dentro de una unica instancia PostgreSQL.
-- Mantiene el aislamiento del patron "database per service" sin multiplicar
-- contenedores (ver ROADMAP, decision D2).
CREATE DATABASE agrocafe_auth;
CREATE DATABASE agrocafe_agronomico;
CREATE DATABASE agrocafe_cosecha;
CREATE DATABASE agrocafe_financiero;
