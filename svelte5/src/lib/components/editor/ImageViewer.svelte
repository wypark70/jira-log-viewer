<script>
  import { ZoomIn, ZoomOut, Maximize2, Download } from "lucide-svelte";
  import { formatFileSize } from "$lib/utils/formatUtils";

  let { tab } = $props();

  let zoom = $state(100);
  let imageInfo = $state(null);

  function handleImageLoad(e) {
    const img = e.target;
    imageInfo = {
      width: img.naturalWidth,
      height: img.naturalHeight,
    };
  }

  function zoomIn() {
    zoom = Math.min(zoom + 25, 400);
  }

  function zoomOut() {
    zoom = Math.max(zoom - 25, 25);
  }

  function resetZoom() {
    zoom = 100;
  }

  function downloadImage() {
    const link = document.createElement("a");
    link.href = tab.content;
    link.download = tab.name;
    link.click();
  }
</script>

<div class="flex flex-col h-full bg-vscode-editor">
  <!-- 툴바 -->
  <div
    class="flex items-center justify-between px-4 py-2 bg-vscode-sidebar border-b border-vscode-border"
  >
    <div class="flex items-center gap-4 text-sm text-vscode-text-muted">
      <span>{tab.name}</span>
      {#if imageInfo}
        <span>{imageInfo.width} × {imageInfo.height}</span>
      {/if}
      {#if tab.size}
        <span>{formatFileSize(tab.size)}</span>
      {/if}
    </div>

    <div class="flex items-center gap-2">
      <button
        onclick={zoomOut}
        class="p-2 hover:bg-vscode-hover rounded transition-colors"
        title="축소"
      >
        <ZoomOut size={16} />
      </button>

      <span class="text-sm text-vscode-text-muted min-w-[60px] text-center">
        {zoom}%
      </span>

      <button
        onclick={zoomIn}
        class="p-2 hover:bg-vscode-hover rounded transition-colors"
        title="확대"
      >
        <ZoomIn size={16} />
      </button>

      <button
        onclick={resetZoom}
        class="p-2 hover:bg-vscode-hover rounded transition-colors"
        title="실제 크기"
      >
        <Maximize2 size={16} />
      </button>

      <button
        onclick={downloadImage}
        class="p-2 hover:bg-vscode-hover rounded transition-colors"
        title="다운로드"
      >
        <Download size={16} />
      </button>
    </div>
  </div>

  <!-- 이미지 -->
  <div class="flex-1 overflow-auto flex items-center justify-center p-8">
    <img
      src={tab.content}
      alt={tab.name}
      onload={handleImageLoad}
      style="transform: scale({zoom / 100}); transition: transform 0.2s;"
      class="max-w-none"
    />
  </div>
</div>
