<script>
  import {
    Folder,
    FolderOpen,
    File,
    FileCode,
    FileJson,
    FileText,
    Image,
  } from "lucide-svelte";

  let { item, isOpen = false } = $props();

  const iconMap = {
    js: FileCode,
    ts: FileCode,
    jsx: FileCode,
    tsx: FileCode,
    svelte: FileCode,
    json: FileJson,
    md: FileText,
    txt: FileText,
    png: Image,
    jpg: Image,
    jpeg: Image,
    gif: Image,
    svg: Image,
    webp: Image,
    default: File,
  };

  const getIcon = (item) => {
    if (item.type === "folder") return isOpen ? FolderOpen : Folder;
    const ext = item.name.split(".").pop()?.toLowerCase();
    return iconMap[ext] || iconMap.default;
  };

  const Icon = $derived(getIcon(item));
  const color = $derived(item.type === "folder" ? "#dcb67a" : "#858585");
</script>

<span class="flex-shrink-0" style="color: {color}">
  <Icon size={16} />
</span>
