// 파일 확장자 → Monaco Editor 언어 ID 매핑
const languageMap = {
  // JavaScript/TypeScript
  js: "javascript",
  jsx: "javascript",
  ts: "typescript",
  tsx: "typescript",
  mjs: "javascript",
  cjs: "javascript",

  // Web
  html: "html",
  htm: "html",
  css: "css",
  scss: "scss",
  sass: "scss",
  less: "less",

  // Data
  json: "json",
  yaml: "yaml",
  yml: "yaml",
  xml: "xml",
  toml: "ini",
  csv: "plaintext",

  // Markdown
  md: "markdown",
  markdown: "markdown",

  // Programming Languages
  py: "python",
  java: "java",
  c: "c",
  cpp: "cpp",
  h: "c",
  hpp: "cpp",
  cs: "csharp",
  go: "go",
  rs: "rust",
  php: "php",
  rb: "ruby",
  swift: "swift",
  kt: "kotlin",
  scala: "scala",

  // Shell
  sh: "shell",
  bash: "shell",
  zsh: "shell",

  // SQL
  sql: "sql",

  // Config
  ini: "ini",
  conf: "ini",
  env: "plaintext",
  properties: "properties",

  // Svelte/Vue
  svelte: "html",
  vue: "html",

  // Other
  txt: "plaintext",
  log: "plaintext",
  gitignore: "plaintext",
};

export function getMonacoLanguage(filename) {
  const ext = filename.split(".").pop()?.toLowerCase();
  return languageMap[ext] || "plaintext";
}
