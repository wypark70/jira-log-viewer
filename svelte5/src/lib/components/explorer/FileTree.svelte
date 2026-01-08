<script>
  import FileTreeNode from "./FileTreeNode.svelte";

  let { items = [], parentPath = "/", level = 0 } = $props();
</script>

<div class="space-y-0.5">
  {#each items as item}
    <FileTreeNode {item} {parentPath} {level} />
  {/each}
</div>
