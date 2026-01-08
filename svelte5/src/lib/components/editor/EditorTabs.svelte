<script>
  import EditorTab from "./EditorTab.svelte";
  import { editorStore } from "$lib/stores/editor.svelte.js";
</script>

<div
  class="flex items-center bg-vscode-sidebar border-b border-vscode-border overflow-x-auto"
>
  {#each editorStore.openTabs as tab, index}
    <EditorTab
      {tab}
      {index}
      isActive={index === editorStore.activeTabIndex}
      onClose={() => editorStore.closeTab(index)}
      onClick={() => editorStore.setActiveTab(index)}
    />
  {/each}
</div>
