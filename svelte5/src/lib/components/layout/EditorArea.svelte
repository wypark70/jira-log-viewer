<script>
  import EditorTabs from "$lib/components/editor/EditorTabs.svelte";
  import MonacoEditor from "$lib/components/editor/MonacoEditor.svelte";
  import ImageViewer from "$lib/components/editor/ImageViewer.svelte";
  import BinaryFileView from "$lib/components/editor/BinaryFileView.svelte";
  import EmptyEditor from "$lib/components/editor/EmptyEditor.svelte";
  import { editorStore } from "$lib/stores/editor.svelte.js";
  import { FileType } from "$lib/utils/fileTypeDetector";
</script>

<div class="flex flex-col h-full bg-vscode-editor">
  {#if editorStore.openTabs.length > 0}
    <!-- 탭 바 -->
    <EditorTabs />

    <!-- 에디터 -->
    <div class="flex-1 overflow-hidden">
      {#if editorStore.activeTab}
        {#key editorStore.activeTab.path}
          {#if editorStore.activeTab.fileType === FileType.TEXT}
            <MonacoEditor tab={editorStore.activeTab} />
          {:else if editorStore.activeTab.fileType === FileType.IMAGE}
            <ImageViewer tab={editorStore.activeTab} />
          {:else}
            <BinaryFileView tab={editorStore.activeTab} />
          {/if}
        {/key}
      {/if}
    </div>
  {:else}
    <EmptyEditor />
  {/if}
</div>
