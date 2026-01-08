<script>
  import { FileCode } from "lucide-svelte";
</script>

<div
  class="flex flex-col items-center justify-center h-full text-vscode-text-muted"
>
  <FileCode size={64} class="mb-4 opacity-50" />
  <p class="text-lg">파일을 선택하여 열기</p>
  <p class="text-sm mt-2">왼쪽 탐색기에서 파일을 클릭하세요</p>
</div>
