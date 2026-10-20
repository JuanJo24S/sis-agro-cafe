import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// El front no habla directo con los microservicios (no exponen CORS): Vite hace de
// proxy en desarrollo y nginx en Docker, con las mismas rutas (/graphql y /ms/<servicio>).
// Los puertos salen del .env de la raiz del repositorio, igual que docker-compose.
export default defineConfig(({ mode }) => {
  const env = { ...loadEnv(mode, '..', ''), ...process.env }
  const host = env.AGROCAFE_HOST ?? 'localhost'
  const puerto = (nombre: string, porDefecto: number) => Number(env[nombre] ?? porDefecto)
  const puertos = {
    gateway: puerto('PUERTO_GATEWAY', 18080),
    auth: puerto('PUERTO_AUTH', 18081),
    agronomico: puerto('PUERTO_AGRONOMICO', 18082),
    cosecha: puerto('PUERTO_COSECHA', 18083),
    financiero: puerto('PUERTO_FINANCIERO', 18084),
  }
  const puertosHerramientas = {
    gateway: puerto('PUERTO_GATEWAY', 18080),
    auth: puerto('PUERTO_AUTH', 18081),
    agronomico: puerto('PUERTO_AGRONOMICO', 18082),
    cosecha: puerto('PUERTO_COSECHA', 18083),
    financiero: puerto('PUERTO_FINANCIERO', 18084),
    grafana: puerto('PUERTO_GRAFANA', 13000),
    jaeger: puerto('PUERTO_JAEGER', 18686),
    prometheus: puerto('PUERTO_PROMETHEUS', 19090),
    kafkaUi: puerto('PUERTO_KAFKA_UI', 18085),
  }
  const servicio = (nombre: keyof typeof puertos) => ({
    target: `http://${host}:${puertos[nombre]}`,
    changeOrigin: true,
    rewrite: (ruta: string) => ruta.replace(new RegExp(`^/ms/${nombre}`), ''),
  })

  return {
    plugins: [react()],
    define: {
      __PUERTOS_CONFIG__: JSON.stringify(puertosHerramientas),
    },
    server: {
      port: Number(env.PUERTO_FRONTEND ?? 15173),
      proxy: {
        '/graphql': { target: `http://${host}:${puertos.gateway}`, changeOrigin: true },
        '/ms/gateway': servicio('gateway'),
        '/ms/auth': servicio('auth'),
        '/ms/agronomico': servicio('agronomico'),
        '/ms/cosecha': servicio('cosecha'),
        '/ms/financiero': servicio('financiero'),
      },
    },
  }
})
