<script>
  import { X } from "lucide-svelte";
  import FileIcon from "$lib/components/explorer/FileIcon.svelte";

  let { tab, index, isActive, onClose, onClick } = $props();
</script>

<div
  class="flex items-center border-r border-vscode-border hover:bg-vscode-hover transition-colors group"
  class:bg-vscode-editor={isActive}
>
  <button
    type="button"
    class="flex items-center gap-2 px-3 py-2 text-sm transition-colors outline-none"
    class:text-vscode-text={isActive}
    class:text-vscode-text-muted={!isActive}
    onclick={onClick}
  >
    <FileIcon item={{ name: tab.name, type: "file" }} />
    <span class="truncate max-w-[150px]">
      {tab.name}
      {#if tab.isDirty}
        <span class="text-vscode-accent">●</span>
      {/if}
    </span>
  </button>
  <button
    type="button"
    class="mr-2 hover:bg-vscode-border rounded p-0.5 transition-colors opacity-0 group-hover:opacity-100 focus:opacity-100"
    class:opacity-100={isActive}
    onclick={(e) => {
      e.stopPropagation();
      onClose();
    }}
    aria-label="Close tab"
  >
    <X size={14} />
  </button>
</div>
