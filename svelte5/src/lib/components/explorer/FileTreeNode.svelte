<script>
  import { ChevronRight, ChevronDown } from "lucide-svelte";
  import FileIcon from "./FileIcon.svelte";
  import FileTree from "./FileTree.svelte";
  import { fileSystemStore } from "$lib/stores/fileSystem.svelte.js";
  import { editorStore } from "$lib/stores/editor.svelte.js";

  let { item, parentPath, level = 0 } = $props();

  const fullPath = $derived(
    `${parentPath}${item.name}${item.type === "folder" ? "/" : ""}`
  );
  const isFolder = $derived(item.type === "folder");
  const isExpanded = $derived(fileSystemStore.isExpanded(fullPath));

  function handleClick() {
    if (isFolder) {
      fileSystemStore.toggleFolder(fullPath);
    } else {
      editorStore.openFile({
        path: fullPath,
        name: item.name,
        content: item.content,
        size: item.size,
      });
    }
  }
</script>

<div>
  <!-- 노드 -->
  <button
    class="w-full flex items-center gap-1 px-2 py-1 text-sm text-vscode-text hover:bg-vscode-hover rounded transition-colors text-left"
    style="padding-left: {level * 12 + 8}px"
    onclick={handleClick}
  >
    <!-- 확장 아이콘 -->
    {#if isFolder}
      <span class="flex-shrink-0">
        {#if isExpanded}
          <ChevronDown size={16} />
        {:else}
          <ChevronRight size={16} />
        {/if}
      </span>
    {:else}
      <span class="w-4"></span>
    {/if}

    <!-- 파일/폴더 아이콘 -->
    <FileIcon {item} />

    <!-- 이름 -->
    <span class="flex-1 truncate">{item.name}</span>
  </button>

  <!-- 자식 노드 (재귀) -->
  {#if isFolder && isExpanded && item.children}
    <FileTree items={item.children} parentPath={fullPath} level={level + 1} />
  {/if}
</div>
