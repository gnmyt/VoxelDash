import path from "path";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

import pkg from "./package.json";

export default defineConfig({
    define: {
        __APP_VERSION__: JSON.stringify(pkg.version),
    },
    plugins: [react()],
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
