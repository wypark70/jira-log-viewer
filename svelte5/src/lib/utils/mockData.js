export const mockFileSystem = {
  name: "root",
  type: "folder",
  children: [
    {
      name: "src",
      type: "folder",
      children: [
        {
          name: "lib",
          type: "folder",
          children: [
            {
              name: "Button.svelte",
              type: "file",
              content:
                '<script>\n  export let label = "Click me";\n</script>\n\n<button class="px-4 py-2 bg-blue-500 text-white rounded">\n  {label}\n</button>',
              size: 125,
            },
            {
              name: "utils.js",
              type: "file",
              content:
                "export function formatDate(date) {\n  return new Date(date).toLocaleDateString();\n}\n\nexport function capitalize(str) {\n  return str.charAt(0).toUpperCase() + str.slice(1);\n}",
              size: 156,
            },
            {
              name: "api.ts",
              type: "file",
              content:
                'interface User {\n  id: number;\n  name: string;\n  email: string;\n}\n\nexport async function fetchUsers(): Promise<User[]> {\n  const response = await fetch("/api/users");\n  return response.json();\n}',
              size: 198,
            },
          ],
        },
        {
          name: "routes",
          type: "folder",
          children: [
            {
              name: "+page.svelte",
              type: "file",
              content:
                '<script>\n  import Button from "$lib/Button.svelte";\n</script>\n\n<h1 class="text-3xl font-bold">Welcome to SvelteKit</h1>\n<Button label="Get Started" />',
              size: 145,
            },
          ],
        },
        {
          name: "app.css",
          type: "file",
          content:
            "@tailwind base;\n@tailwind components;\n@tailwind utilities;\n\nbody {\n  font-family: system-ui, sans-serif;\n}",
          size: 108,
        },
      ],
    },
    {
      name: "assets",
      type: "folder",
      children: [
        {
          name: "logo.png",
          type: "file",
          content:
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
          size: 1024,
        },
        {
          name: "screenshot.jpg",
          type: "file",
          content:
            "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAIBAQIBAQICAgICAgICAwUDAwMDAwYEBAMFBwYHBwcGBwcICQsJCAgKCAcHCg0KCgsMDAwMBwkODw0MDgsMDAz/2wBDAQICAgMDAwYDAwYMCAcIDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAz/wAARCAABAAEDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlbaWmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD9/KKKKAP/2Q==",
          size: 2048,
        },
        {
          name: "video.mp4",
          type: "file",
          size: 5242880, // 5MB
        },
        {
          name: "document.pdf",
          type: "file",
          size: 1048576, // 1MB
        },
      ],
    },
    {
      name: "config",
      type: "folder",
      children: [
        {
          name: "database.json",
          type: "file",
          content:
            '{\n  "host": "localhost",\n  "port": 5432,\n  "database": "myapp",\n  "user": "admin"\n}',
          size: 95,
        },
        {
          name: "settings.yaml",
          type: "file",
          content:
            "app:\n  name: My Application\n  version: 1.0.0\n  debug: true\n\nserver:\n  port: 3000\n  host: 0.0.0.0",
          size: 98,
        },
      ],
    },
    {
      name: "package.json",
      type: "file",
      content:
        '{\n  "name": "my-app",\n  "version": "0.0.1",\n  "type": "module",\n  "scripts": {\n    "dev": "vite dev",\n    "build": "vite build"\n  }\n}',
      size: 135,
    },
    {
      name: "README.md",
      type: "file",
      content:
        "# My Project\n\nThis is a sample project built with **SvelteKit** and **Monaco Editor**.\n\n## Features\n\n- 📁 File Explorer\n- 💻 Monaco Editor\n- 🖼️ Image Viewer\n- 📦 Binary File Support\n\n## Getting Started\n\n```bash\nnpm install\nnpm run dev\n```",
      size: 245,
    },
  ],
};
