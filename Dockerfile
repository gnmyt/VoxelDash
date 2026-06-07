FROM node:20-slim AS ui
RUN corepack enable && corepack prepare pnpm@9 --activate
WORKDIR /app/ui

COPY ui/package.json ui/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY ui/ ./
RUN pnpm run build


FROM oven/bun:1.3.13-debian AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates tar \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app/one

COPY one/package.json one/bun.lock ./
RUN bun install --frozen-lockfile

COPY one/ ./
COPY --from=ui /app/ui/dist /app/ui/dist

ENV VOXELDASH_HOME=/data \
    VOXELDASH_UI=/app/ui/dist \
    PORT=7867 \
    NODE_ENV=production

EXPOSE 7867
VOLUME ["/data"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD bun -e "fetch('http://127.0.0.1:'+(process.env.PORT||7867)+'/').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"

CMD ["bun", "run", "src/index.js"]
