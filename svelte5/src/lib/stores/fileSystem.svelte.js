import { mockFileSystem } from "$lib/utils/mockData";

class FileSystemStore {
  fileTree = $state(mockFileSystem);
  expandedFolders = $state(new Set(["/"]));

  toggleFolder(path) {
    if (this.expandedFolders.has(path)) {
      this.expandedFolders.delete(path);
    } else {
      this.expandedFolders.add(path);
    }
    // Set을 새로 생성하여 반응성 트리거
    this.expandedFolders = new Set(this.expandedFolders);
  }

  isExpanded(path) {
    return this.expandedFolders.has(path);
  }

  getItemByPath(path) {
    const parts = path.split("/").filter(Boolean);
    /** @type {any} */
    let current = this.fileTree;

    for (const part of parts) {
      current = current.children?.find((item) => item.name === part);
      if (!current) return null;
    }

    return current;
  }
}

export const fileSystemStore = new FileSystemStore();
