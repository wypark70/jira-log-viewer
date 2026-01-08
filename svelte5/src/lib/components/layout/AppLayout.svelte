<script>
  import { onMount } from "svelte";
  import { ChevronRight } from "lucide-svelte";
  import Toolbar from "$lib/components/toolbar/Toolbar.svelte";
  import Sidebar from "./Sidebar.svelte";
  import EditorArea from "./EditorArea.svelte";
  import FileSearchPanel from "$lib/components/search/FileSearchPanel.svelte";
  import SettingsPanel from "$lib/components/settings/SettingsPanel.svelte";
  import { uiStore } from "$lib/stores/ui.svelte.js";
  import { searchStore } from "$lib/stores/search.svelte.js";
  import { settingsStore } from "$lib/stores/settings.svelte.js";

  let isResizing = $state(false);

  function startResize() {
    isResizing = true;
  }

  function handleMouseMove(e) {
    if (isResizing) {
      uiStore.setSidebarWidth(e.clientX);
    }
  }

  function stopResize() {
    isResizing = false;
  }

  // 키보드 단축키
  onMount(() => {
    function handleKeyDown(e) {
      // Ctrl+F: Monaco Editor에 포커스가 있으면 에디터 검색, 아니면 무시
      if (e.ctrlKey && e.key === "f") {
        if (window.__monacoEditorFocused) {
          // Monaco Editor가 포커스되어 있으면 기본 동작 허용 (에디터 자체 Find)
          return;
        }
        // Monaco에 포커스가 없으면 기본 동작 방지 (브라우저 검색 방지)
        e.preventDefault();
      }
      // Ctrl+P: 파일 검색
      if (e.ctrlKey && e.key === "p") {
        e.preventDefault();
        searchStore.openFileSearch();
      }
      // Ctrl+,: 설정
      if (e.ctrlKey && e.key === ",") {
        e.preventDefault();
        settingsStore.togglePanel();
      }
      // Ctrl+B: 사이드바 토글
      if (e.ctrlKey && e.key === "b") {
        e.preventDefault();
        uiStore.toggleSidebar();
      }
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  });
</script>

<svelte:window onmousemove={handleMouseMove} onmouseup={stopResize} />

<div class="flex flex-col h-screen overflow-hidden bg-vscode-bg">
  <!-- 툴바 -->
  <Toolbar />

  <!-- 메인 영역 -->
  <div class="flex flex-1 overflow-hidden">
    {#if uiStore.isSidebarVisible}
      <!-- 왼쪽 사이드바 -->
      <div
        class="flex-shrink-0 bg-vscode-sidebar border-r border-vscode-border"
        style="width: {uiStore.sidebarWidth}px"
      >
        <Sidebar />
      </div>

      <!-- 크기 조절 핸들 -->
      <button
        class="w-1 cursor-col-resize bg-vscode-border hover:bg-vscode-accent transition-colors border-none outline-none p-0"
        onmousedown={startResize}
        aria-label="사이드바 크기 조절"
      ></button>
    {:else}
      <!-- 사이드바 숨김 시 토글 버튼 -->
      <div
        class="flex-shrink-0 bg-vscode-sidebar border-r border-vscode-border"
      >
        <button
          onclick={() => uiStore.toggleSidebar()}
          class="p-2 hover:bg-vscode-hover transition-colors"
          title="사이드바 표시 (Ctrl+B)"
        >
          <ChevronRight size={20} />
        </button>
      </div>
    {/if}

    <!-- 오른쪽 에디터 영역 -->
    <div class="flex-1 overflow-hidden">
      <EditorArea />
    </div>
  </div>
</div>

<!-- 검색 패널 -->
<FileSearchPanel />

<!-- 설정 패널 -->
<SettingsPanel />
