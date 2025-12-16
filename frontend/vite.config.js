import { defineConfig } from "vite";
import { svelte } from "@sveltejs/vite-plugin-svelte";

// https://vite.dev/config/
export default defineConfig({
  plugins: [svelte()],
  build: {
    rollupOptions: {
      output: {
        entryFileNames: `assets/jira-log-viewer.js`,
        chunkFileNames: `assets/[name].js`,
        assetFileNames: `assets/jira-log-viewer.[ext]`,
      },
    },
  },
});
