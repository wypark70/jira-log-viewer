<script>
  import FileTree from "./FileTree.svelte";
  import { fileSystemStore } from "$lib/stores/fileSystem.svelte.js";
</script>

<div class="p-2">
  <FileTree items={fileSystemStore.fileTree.children} parentPath="/" />
</div>
