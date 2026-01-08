<script>
  import {
    Download,
    FileArchive,
    FileVideo,
    FileAudio,
    File,
  } from "lucide-svelte";
  import { formatFileSize, getFileExtension } from "$lib/utils/formatUtils.js";

  let { tab } = $props();

  const ext = $derived(getFileExtension(tab.name));

  const iconMap = {
    // 압축
    zip: FileArchive,
    tar: FileArchive,
    gz: FileArchive,
    rar: FileArchive,
    "7z": FileArchive,
    // 비디오
    mp4: FileVideo,
    avi: FileVideo,
    mov: FileVideo,
    mkv: FileVideo,
    webm: FileVideo,
    // 오디오
    mp3: FileAudio,
    wav: FileAudio,
    ogg: FileAudio,
    flac: FileAudio,
    // 기본
    default: File,
  };

  const Icon = $derived(iconMap[ext] || iconMap.default);

  function downloadFile() {
    const link = document.createElement("a");
    link.href = tab.content || "#";
    link.download = tab.name;
    link.click();
  }
</script>

<div
  class="flex flex-col items-center justify-center h-full bg-vscode-editor text-vscode-text"
>
  <div class="flex flex-col items-center gap-6 max-w-md text-center">
    <!-- 아이콘 -->
    <div class="p-6 bg-vscode-sidebar rounded-lg">
      <Icon size={64} class="text-vscode-text-muted" />
    </div>

    <!-- 파일 정보 -->
    <div class="space-y-2">
      <h2 class="text-xl font-semibold">{tab.name}</h2>
      <div class="flex items-center gap-4 text-sm text-vscode-text-muted">
        <span class="uppercase">{ext} 파일</span>
        {#if tab.size}
          <span>•</span>
          <span>{formatFileSize(tab.size)}</span>
        {/if}
      </div>
    </div>

    <!-- 설명 -->
    <p class="text-vscode-text-muted">
      이 파일은 바이너리 파일이므로 미리보기를 제공하지 않습니다.
    </p>

    <!-- 다운로드 버튼 -->
    <button
      onclick={downloadFile}
      class="flex items-center gap-2 px-6 py-3 bg-vscode-accent hover:bg-blue-600 text-white rounded-lg transition-colors"
    >
      <Download size={20} />
      <span>파일 다운로드</span>
    </button>
  </div>
</div>
