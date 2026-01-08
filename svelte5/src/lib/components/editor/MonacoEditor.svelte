<script>
  import { onMount, onDestroy, tick } from "svelte";
  import { browser } from "$app/environment";
  import { settingsStore } from "$lib/stores/settings.svelte.js";

  let { tab } = $props();
  let editorContainer = $state();
  let editor = $state();
  let monaco = $state();
  let loading = $state(true);
  let error = $state(null);

  onMount(async () => {
    if (!browser) return;

    try {
      // 먼저 loading을 false로 설정하여 DOM을 렌더링
      loading = false;

      // DOM이 렌더링될 때까지 대기
      await tick();

      // editorContainer가 존재하는지 확인
      if (!editorContainer) {
        throw new Error("Editor container not found in DOM");
      }

      // Monaco Editor를 동적으로 import
      const monacoModule = await import("monaco-editor");
      monaco = monacoModule;

      // Monaco Editor 언어 워커 설정
      self.MonacoEnvironment = {
        getWorker(_, label) {
          if (label === "json") {
            return new Worker(
              new URL(
                "monaco-editor/esm/vs/language/json/json.worker.js",
                import.meta.url
              ),
              { type: "module" }
            );
          }
          if (label === "css" || label === "scss" || label === "less") {
            return new Worker(
              new URL(
                "monaco-editor/esm/vs/language/css/css.worker.js",
                import.meta.url
              ),
              { type: "module" }
            );
          }
          if (label === "html" || label === "handlebars" || label === "razor") {
            return new Worker(
              new URL(
                "monaco-editor/esm/vs/language/html/html.worker.js",
                import.meta.url
              ),
              { type: "module" }
            );
          }
          if (label === "typescript" || label === "javascript") {
            return new Worker(
              new URL(
                "monaco-editor/esm/vs/language/typescript/ts.worker.js",
                import.meta.url
              ),
              { type: "module" }
            );
          }
          return new Worker(
            new URL(
              "monaco-editor/esm/vs/editor/editor.worker.js",
              import.meta.url
            ),
            { type: "module" }
          );
        },
      };

      // 언어가 없으면 plaintext로 설정
      const editorLanguage = tab.language || "plaintext";

      // Monaco Editor 초기화 (설정 적용)
      editor = monaco.editor.create(editorContainer, {
        value: tab.content,
        language: editorLanguage,
        theme: settingsStore.editorTheme,
        automaticLayout: true,
        minimap: {
          enabled: settingsStore.showMinimap,
        },
        fontSize: settingsStore.fontSize,
        fontFamily: settingsStore.fontFamily,
        lineNumbers: settingsStore.showLineNumbers ? "on" : "off",
        roundedSelection: false,
        scrollBeyondLastLine: false,
        readOnly: false,
        cursorStyle: "line",
        wordWrap: settingsStore.wordWrap,
      });

      // 내용 변경 감지
      editor.onDidChangeModelContent(() => {
        const content = editor.getValue();
        // 여기서 상태 업데이트 가능
      });

      // 에디터 포커스 추적 (Ctrl+F를 위해)
      editor.onDidFocusEditorText(() => {
        // 에디터에 포커스가 있을 때 전역 플래그 설정
        window.__monacoEditorFocused = true;
      });

      editor.onDidBlurEditorText(() => {
        // 에디터에서 포커스가 벗어났을 때 플래그 해제
        window.__monacoEditorFocused = false;
      });
    } catch (err) {
      console.error("Failed to load Monaco Editor:", err);
      error = err.message;
    }
  });

  // 설정 변경 시 에디터 업데이트
  $effect(() => {
    if (editor) {
      editor.updateOptions({
        theme: settingsStore.editorTheme,
        fontSize: settingsStore.fontSize,
        fontFamily: settingsStore.fontFamily,
        minimap: { enabled: settingsStore.showMinimap },
        lineNumbers: settingsStore.showLineNumbers ? "on" : "off",
        wordWrap: settingsStore.wordWrap,
      });
    }
  });

  // 탭 변경 시 내용 및 언어 업데이트
  $effect(() => {
    if (editor && monaco) {
      // 현재 모델 가져오기
      const model = editor.getModel();
      if (model) {
        // 내용이 다르면 업데이트
        if (tab.content !== model.getValue()) {
          model.setValue(tab.content);
        }
        // 언어가 다르면 업데이트
        const currentLanguage = model.getLanguageId();
        if (tab.language && tab.language !== currentLanguage) {
          monaco.editor.setModelLanguage(model, tab.language);
        }
      }
    }
  });

  onDestroy(() => {
    editor?.dispose();
  });
</script>

{#if error}
  <div class="flex items-center justify-center h-full text-red-400">
    <div class="text-center">
      <p class="text-lg mb-2">Failed to load editor</p>
      <p class="text-sm">{error}</p>
    </div>
  </div>
{:else}
  <div bind:this={editorContainer} class="monaco-editor-container"></div>
{/if}

<style>
  .monaco-editor-container {
    width: 100%;
    height: 100%;
  }
</style>
