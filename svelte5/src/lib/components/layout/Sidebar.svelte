<script>
  import { ChevronLeft, ChevronRight } from "lucide-svelte";
  import FileExplorer from "$lib/components/explorer/FileExplorer.svelte";
  import { uiStore } from "$lib/stores/ui.svelte.js";
</script>

<div class="flex flex-col h-full">
  <!-- 헤더 -->
  <div
    class="flex items-center justify-between px-4 py-2 border-b border-vscode-border"
  >
    <h2 class="text-xs font-semibold uppercase text-vscode-text-muted">
      탐색기
    </h2>
    <button
      onclick={() => uiStore.toggleSidebar()}
      class="p-1 hover:bg-vscode-hover rounded transition-colors"
      title="사이드바 숨기기 (Ctrl+B)"
    >
      <ChevronLeft size={16} />
    </button>
  </div>

  <!-- 파일 탐색기 -->
  <div class="flex-1 overflow-auto">
    <FileExplorer />
  </div>
</div>
