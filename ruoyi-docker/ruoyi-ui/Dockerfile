FROM node:20-alpine AS builder
WORKDIR /app

COPY ruoyi-ui/package*.json ./
RUN npm install

COPY ruoyi-ui/ ./
ARG VITE_APP_BASE_API=/prod-api
ENV VITE_APP_BASE_API=${VITE_APP_BASE_API}
RUN npm run build:prod

FROM nginx:1.27-alpine
COPY ruoyi-docker/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
