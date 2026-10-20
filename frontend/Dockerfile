# Etapa 1: compila la SPA (React + Vite).
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# Etapa 2: nginx sirve los estaticos y hace de proxy hacia el gateway y los microservicios,
# con las mismas rutas que usa Vite en desarrollo (/graphql y /ms/<servicio>).
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
