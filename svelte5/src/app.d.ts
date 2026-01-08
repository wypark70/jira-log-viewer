/// <reference types="svelte" />
/// <reference types="vite/client" />
/// <reference types="svelte/types/runtime/ambient" />

// See https://kit.svelte.dev/docs/types#app
// for information about these interfaces
declare global {
  namespace App {
    // interface Error {}
    // interface Locals {}
    // interface PageData {}
    // interface PageState {}
    // interface Platform {}
  }

  interface Window {
    MonacoEnvironment?: any;
    __monacoEditorFocused?: boolean;
  }
}

// Enable importing .svelte.js files (Svelte 5 runes mode)
declare module "*.svelte.js" {
  const content: any;
  export default content;
  export * from "*.svelte.js";
}

export {};
