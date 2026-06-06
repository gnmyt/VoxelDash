import path from "path";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
import { VitePWA } from "vite-plugin-pwa";

import pkg from "./package.json";

const ICON_BASE = "/assets/images";

export default defineConfig({
    define: {
        __APP_VERSION__: JSON.stringify(pkg.version),
    },
    plugins: [
        react(),
        VitePWA({
            registerType: "prompt",
            injectRegister: null,
            includeAssets: [
                "assets/images/favicon.ico",
                "assets/images/favicon.png",
                "assets/images/apple-touch-icon.png",
            ],
            manifest: {
                id: "/",
                name: "VoxelDash",
                short_name: "VoxelDash",
                description: "The Web Interface for Minecraft servers",
                start_url: "/",
                scope: "/",
                display: "standalone",
                display_override: ["standalone", "minimal-ui"],
                orientation: "any",
                background_color: "#0d0b0f",
                theme_color: "#0d0b0f",
                categories: ["utilities", "productivity"],
                lang: "en",
                icons: [
                    {
                        src: `${ICON_BASE}/pwa-192x192.png`,
                        sizes: "192x192",
                        type: "image/png",
                        purpose: "any",
                    },
                    {
                        src: `${ICON_BASE}/pwa-512x512.png`,
                        sizes: "512x512",
                        type: "image/png",
                        purpose: "any",
                    },
                    {
                        src: `${ICON_BASE}/pwa-maskable-512x512.png`,
                        sizes: "512x512",
                        type: "image/png",
                        purpose: "maskable",
                    },
                ],
            },
            workbox: {
                globPatterns: ["**/*.{js,css,html,woff2,png,webp,svg,ico,json}"],
                maximumFileSizeToCacheInBytes: 6 * 1024 * 1024,
                cleanupOutdatedCaches: true,
                navigateFallback: "/index.html",
                navigateFallbackDenylist: [/^\/api/, /^\/master/],
                runtimeCaching: [
                    {
                        urlPattern: ({ url }) => url.pathname.startsWith("/assets/locales/"),
                        handler: "StaleWhileRevalidate",
                        options: {
                            cacheName: "voxeldash-locales",
                            expiration: { maxEntries: 40, maxAgeSeconds: 60 * 60 * 24 * 30 },
                        },
                    },
                    {
                        urlPattern: ({ request }) => request.destination === "image",
                        handler: "CacheFirst",
                        options: {
                            cacheName: "voxeldash-images",
                            expiration: { maxEntries: 80, maxAgeSeconds: 60 * 60 * 24 * 30 },
                        },
                    },
                ],
            },
            devOptions: {
                enabled: false,
            },
        }),
    ],
    server: {
        proxy: {
            "/api": {
                target: "http://localhost:7867",
                changeOrigin: true,
                ws: true,
            },
            "/master": {
                target: "http://localhost:7867",
                changeOrigin: true,
            },
        },
    },
    resolve: {
        alias: {
            "@": path.resolve(__dirname, "./src"),
        },
    },
});
