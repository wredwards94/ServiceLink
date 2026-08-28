# syntax=docker/dockerfile:1

# --- build -------------------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /build

# npm ci needs both files and is cached separately from the source.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# The production configuration swaps in environment.prod.ts, whose apiUrl is
# empty — the bundle calls /api on its own origin, so this image carries no
# environment-specific hostname and runs anywhere.
RUN npm run build

# --- serve -------------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
# Angular 17+ emits to dist/<project>/browser, not dist/<project>.
COPY --from=build /build/dist/servicelink/browser /usr/share/nginx/html

# nginx:alpine ships an `nginx` user; port 8080 (not 80) so it needs no
# privileged bind and the container can run non-root.
RUN touch /var/run/nginx.pid \
 && chown -R nginx:nginx /var/run/nginx.pid /var/cache/nginx /usr/share/nginx/html
USER nginx

EXPOSE 8080
