export const FileType = {
  TEXT: "text",
  IMAGE: "image",
  BINARY: "binary",
};

const textExtensions = new Set([
  // 코드
  "js",
  "ts",
  "jsx",
  "tsx",
  "svelte",
  "vue",
  "py",
  "java",
  "go",
  "rust",
  "c",
  "cpp",
  "h",
  "hpp",
  "cs",
  "php",
  "rb",
  "swift",
  "kt",
  "scala",
  // 마크업
  "html",
  "xml",
  "svg",
  "md",
  "markdown",
  "rst",
  // 데이터
  "json",
  "yaml",
  "yml",
  "toml",
  "csv",
  "tsv",
  // 설정
  "ini",
  "conf",
  "env",
  "properties",
  "gitignore",
  "editorconfig",
  // 스타일
  "css",
  "scss",
  "sass",
  "less",
  "styl",
  // 기타
  "txt",
  "log",
  "sh",
  "bash",
  "zsh",
  "fish",
  "sql",
  "graphql",
  "proto",
]);

const imageExtensions = new Set([
  "png",
  "jpg",
  "jpeg",
  "gif",
  "svg",
  "webp",
  "ico",
  "bmp",
  "tiff",
  "tif",
]);

export function detectFileType(filename) {
  const ext = filename.split(".").pop()?.toLowerCase();

  if (textExtensions.has(ext)) {
    return FileType.TEXT;
  }

  if (imageExtensions.has(ext)) {
    return FileType.IMAGE;
  }

  return FileType.BINARY;
}

export function isTextFile(filename) {
  return detectFileType(filename) === FileType.TEXT;
}

export function isImageFile(filename) {
  return detectFileType(filename) === FileType.IMAGE;
}

export function isBinaryFile(filename) {
  return detectFileType(filename) === FileType.BINARY;
}
