<script>
  import { Search, Settings } from "lucide-svelte";
  import { searchStore } from "$lib/stores/search.svelte.js";
  import { settingsStore } from "$lib/stores/settings.svelte.js";
</script>

<div
  class="flex items-center gap-2 px-4 py-2 bg-vscode-sidebar border-b border-vscode-border"
>
  <div class="flex items-center gap-2">
    <button
      onclick={() => searchStore.openFileSearch()}
      class="flex items-center gap-2 px-3 py-1.5 hover:bg-vscode-hover rounded transition-colors text-sm"
      title="파일 검색 (Ctrl+P)"
    >
      <Search size={16} />
      <span class="hidden sm:inline">검색</span>
    </button>

    <button
      onclick={() => settingsStore.togglePanel()}
      class="flex items-center gap-2 px-3 py-1.5 hover:bg-vscode-hover rounded transition-colors text-sm"
      title="설정 (Ctrl+,)"
    >
      <Settings size={16} />
      <span class="hidden sm:inline">설정</span>
    </button>
  </div>
</div>
