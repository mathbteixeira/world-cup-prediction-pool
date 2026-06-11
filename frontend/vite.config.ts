import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  preview: {
    allowedHosts: ['.up.railway.app'],
  },
  test: {
    environment: "node",
    fileParallelism: false,
    globals: true,
    pool: "forks",
    poolOptions: {
      forks: {
        singleFork: true,
      },
    },
    setupFiles: "./src/test/setup.ts",
  },
});
