<script>
  import { X, Settings as SettingsIcon } from "lucide-svelte";
  import { settingsStore } from "$lib/stores/settings.svelte.js";

  const fontFamilies = [
    "Consolas",
    "Monaco",
    "Courier New",
    "Menlo",
    "Source Code Pro",
  ];

  const wordWrapOptions = [
    { value: "off", label: "줄바꿈 안함" },
    { value: "on", label: "줄바꿈" },
    { value: "wordWrapColumn", label: "열 기준" },
  ];

  const themes = [
    { value: "vs-dark", label: "Dark" },
    { value: "vs", label: "Light" },
    { value: "hc-black", label: "High Contrast" },
  ];
</script>

{#if settingsStore.isPanelOpen}
  <!-- 배경 오버레이 -->
  <button
    class="fixed inset-0 bg-black bg-opacity-50 z-40 border-none cursor-default w-full h-full block"
    onclick={() => settingsStore.closePanel()}
    aria-label="Close settings"
  ></button>

  <!-- 설정 패널 -->
  <div
    class="fixed top-0 right-0 h-full w-full max-w-md bg-vscode-sidebar border-l border-vscode-border z-50 overflow-y-auto"
  >
    <!-- 헤더 -->
    <div
      class="sticky top-0 bg-vscode-sidebar border-b border-vscode-border px-6 py-4 flex items-center justify-between"
    >
      <div class="flex items-center gap-2">
        <SettingsIcon size={20} />
        <h2 class="text-lg font-semibold">설정</h2>
      </div>
      <button
        onclick={() => settingsStore.closePanel()}
        class="p-2 hover:bg-vscode-hover rounded transition-colors"
      >
        <X size={20} />
      </button>
    </div>

    <!-- 설정 내용 -->
    <div class="p-6 space-y-6">
      <!-- 에디터 설정 -->
      <section>
        <h3 class="text-sm font-semibold text-vscode-text-muted mb-4">
          에디터
        </h3>

        <!-- 폰트 크기 -->
        <div class="mb-4">
          <label class="block text-sm mb-2" for="font-size">
            폰트 크기: {settingsStore.fontSize}px
          </label>
          <input
            id="font-size"
            type="range"
            min="10"
            max="24"
            value={settingsStore.fontSize}
            oninput={(e) =>
              settingsStore.setFontSize(Number(e.currentTarget.value))}
            class="w-full"
          />
          <div class="flex justify-between text-xs text-vscode-text-muted mt-1">
            <span>10px</span>
            <span>24px</span>
          </div>
        </div>

        <!-- 폰트 패밀리 -->
        <div class="mb-4">
          <label class="block text-sm mb-2" for="font-family">폰트 패밀리</label
          >
          <select
            id="font-family"
            value={settingsStore.fontFamily}
            onchange={(e) => settingsStore.setFontFamily(e.currentTarget.value)}
            class="w-full bg-vscode-bg border border-vscode-border rounded px-3 py-2 text-vscode-text"
          >
            {#each fontFamilies as font}
              <option value={font}>{font}</option>
            {/each}
          </select>
        </div>

        <!-- 줄바꿈 -->
        <div class="mb-4">
          <label class="block text-sm mb-2" for="word-wrap">줄바꿈</label>
          <select
            id="word-wrap"
            value={settingsStore.wordWrap}
            onchange={(e) => settingsStore.setWordWrap(e.currentTarget.value)}
            class="w-full bg-vscode-bg border border-vscode-border rounded px-3 py-2 text-vscode-text"
          >
            {#each wordWrapOptions as option}
              <option value={option.value}>{option.label}</option>
            {/each}
          </select>
        </div>

        <!-- 미니맵 -->
        <div class="mb-4">
          <label class="flex items-center justify-between cursor-pointer">
            <span class="text-sm">미니맵 표시</span>
            <input
              type="checkbox"
              checked={settingsStore.showMinimap}
              onchange={() => settingsStore.toggleMinimap()}
              class="w-4 h-4"
            />
          </label>
        </div>

        <!-- 줄 번호 -->
        <div class="mb-4">
          <label class="flex items-center justify-between cursor-pointer">
            <span class="text-sm">줄 번호 표시</span>
            <input
              type="checkbox"
              checked={settingsStore.showLineNumbers}
              onchange={() => settingsStore.toggleLineNumbers()}
              class="w-4 h-4"
            />
          </label>
        </div>
      </section>

      <!-- 테마 설정 -->
      <section>
        <h3 class="text-sm font-semibold text-vscode-text-muted mb-4">테마</h3>

        <div class="mb-4">
          <label class="block text-sm mb-2" for="editor-theme"
            >에디터 테마</label
          >
          <select
            id="editor-theme"
            value={settingsStore.editorTheme}
            onchange={(e) =>
              settingsStore.setEditorTheme(e.currentTarget.value)}
            class="w-full bg-vscode-bg border border-vscode-border rounded px-3 py-2 text-vscode-text"
          >
            {#each themes as theme}
              <option value={theme.value}>{theme.label}</option>
            {/each}
          </select>
        </div>
      </section>

      <!-- 초기화 버튼 -->
      <section>
        <button
          onclick={() => settingsStore.resetSettings()}
          class="w-full px-4 py-2 bg-vscode-border hover:bg-vscode-hover rounded transition-colors"
        >
          설정 초기화
        </button>
      </section>
    </div>
  </div>
{/if}
