import path from "path";
import { fileURLToPath } from "url";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  build: {
    rollupOptions: {
      onwarn(warning, warn) {
        if (
          warning.code === "MODULE_LEVEL_DIRECTIVE" &&
          typeof warning.id === "string" &&
          warning.id.includes("node_modules")
        ) {
          return;
        }

        warn(warning);
      },
    },
  },
  server: {
    port: 5173,
    strictPort: true,
    host: true,
    allowedHosts: ["21f6a1498cc3.ngrok-free.app"],
    watch: {
      // Нативный запуск (npm run dev на хосте) — нативные FS-события, CPU не грузится.
      // VITE_USE_POLLING=1 включать ТОЛЬКО при запуске фронта в Docker на Windows (bind-mount требует поллинга).
      usePolling: process.env.VITE_USE_POLLING === "1",
      interval: 300,
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: "./src/app/setupTests.js",
    globals: true,
    coverage: {
      provider: "v8",
      reporter: ["text", "json", "html"],
    },
  },
});
