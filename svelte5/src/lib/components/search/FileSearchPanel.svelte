<script>
  import { Search, X } from "lucide-svelte";
  import { searchStore } from "$lib/stores/search.svelte.js";
  import { editorStore } from "$lib/stores/editor.svelte.js";
  import FileIcon from "$lib/components/explorer/FileIcon.svelte";

  let inputElement = $state();

  $effect(() => {
    if (searchStore.isFileSearchOpen && inputElement) {
      inputElement.focus();
    }
  });

  function handleKeyDown(e) {
    if (e.key === "Escape") {
      searchStore.closeFileSearch();
    } else if (e.key === "ArrowDown") {
      e.preventDefault();
      searchStore.selectNext();
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      searchStore.selectPrevious();
    } else if (e.key === "Enter") {
      e.preventDefault();
      const selected = searchStore.getSelectedResult();
      if (selected) {
        editorStore.openFile(selected.item);
        searchStore.closeFileSearch();
      }
    }
  }

  function handleInput(e) {
    searchStore.search(e.target.value);
  }

  function selectResult(result) {
    editorStore.openFile(result.item);
    searchStore.closeFileSearch();
  }
</script>

{#if searchStore.isFileSearchOpen}
  <!-- 배경 오버레이 -->
  <button
    class="fixed inset-0 bg-black bg-opacity-50 z-40 border-none cursor-default w-full h-full block"
    onclick={() => searchStore.closeFileSearch()}
    aria-label="Close search"
  ></button>

  <!-- 검색 패널 -->
  <div
    class="fixed top-20 left-1/2 transform -translate-x-1/2 w-full max-w-2xl z-50"
  >
    <div
      class="bg-vscode-sidebar border border-vscode-border rounded-lg shadow-2xl overflow-hidden"
    >
      <!-- 검색 입력 -->
      <div
        class="flex items-center gap-2 px-4 py-3 border-b border-vscode-border"
      >
        <Search size={18} class="text-vscode-text-muted" />
        <input
          bind:this={inputElement}
          type="text"
          placeholder="파일 이름으로 검색... (Ctrl+P)"
          class="flex-1 bg-transparent text-vscode-text outline-none"
          value={searchStore.searchQuery}
          oninput={handleInput}
          onkeydown={handleKeyDown}
        />
        <button
          onclick={() => searchStore.closeFileSearch()}
          class="p-1 hover:bg-vscode-hover rounded transition-colors"
        >
          <X size={18} />
        </button>
      </div>

      <!-- 검색 결과 -->
      <div class="max-h-96 overflow-y-auto">
        {#if searchStore.searchResults.length > 0}
          {#each searchStore.searchResults as result, index}
            <button
              class="w-full flex items-center gap-3 px-4 py-2 text-left hover:bg-vscode-hover transition-colors"
              class:bg-vscode-accent={index === searchStore.selectedIndex}
              class:text-white={index === searchStore.selectedIndex}
              onclick={() => selectResult(result)}
            >
              <FileIcon item={{ name: result.name, type: "file" }} />
              <div class="flex-1 min-w-0">
                <div class="font-medium truncate">{result.name}</div>
                <div class="text-xs text-vscode-text-muted truncate">
                  {result.path}
                </div>
              </div>
            </button>
          {/each}
        {:else if searchStore.searchQuery}
          <div class="px-4 py-8 text-center text-vscode-text-muted">
            검색 결과가 없습니다
          </div>
        {:else}
          <div class="px-4 py-8 text-center text-vscode-text-muted">
            파일 이름을 입력하세요
          </div>
        {/if}
      </div>
    </div>
  </div>
{/if}
