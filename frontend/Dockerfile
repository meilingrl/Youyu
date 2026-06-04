FROM node:22-alpine AS builder

WORKDIR /app

ARG VITE_AMAP_JS_KEY=
ARG VITE_AMAP_SECURITY_CODE=
ENV VITE_AMAP_JS_KEY=${VITE_AMAP_JS_KEY}
ENV VITE_AMAP_SECURITY_CODE=${VITE_AMAP_SECURITY_CODE}

COPY package.json package-lock.json ./
RUN npm ci
RUN npm install --no-save @rollup/rollup-linux-x64-musl@4.60.3

COPY . .
RUN npm run build

FROM nginx:1.27-alpine

COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
