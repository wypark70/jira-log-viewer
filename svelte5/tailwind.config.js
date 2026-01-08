/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./src/**/*.{html,js,svelte,ts}",
    "./node_modules/flowbite-svelte/**/*.{html,js,svelte,ts}",
  ],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        // VSCode 테마 색상
        "vscode-bg": "#1e1e1e",
        "vscode-sidebar": "#252526",
        "vscode-editor": "#1e1e1e",
        "vscode-border": "#3c3c3c",
        "vscode-text": "#cccccc",
        "vscode-text-muted": "#858585",
        "vscode-accent": "#007acc",
        "vscode-hover": "#2a2d2e",
      },
    },
  },
  plugins: [require("flowbite/plugin")],
};
