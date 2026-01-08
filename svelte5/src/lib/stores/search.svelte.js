import { fileSystemStore } from "./fileSystem.svelte";

class SearchStore {
  isFileSearchOpen = $state(false);
  searchQuery = $state("");
  searchResults = $state([]);
  selectedIndex = $state(0);

  openFileSearch() {
    this.isFileSearchOpen = true;
    this.searchQuery = "";
    this.searchResults = [];
    this.selectedIndex = 0;
  }

  closeFileSearch() {
    this.isFileSearchOpen = false;
    this.searchQuery = "";
    this.searchResults = [];
  }

  search(query) {
    this.searchQuery = query;
    if (!query.trim()) {
      this.searchResults = [];
      return;
    }

    this.searchResults = this.fuzzySearch(query.toLowerCase());
    this.selectedIndex = 0;
  }

  fuzzySearch(query) {
    const results = [];

    const searchInTree = (items, parentPath = "/") => {
      items.forEach((item) => {
        const fullPath = `${parentPath}${item.name}${
          item.type === "folder" ? "/" : ""
        }`;

        if (item.type === "file") {
          const fileName = item.name.toLowerCase();
          const score = this.calculateScore(fileName, query);

          if (score > 0) {
            results.push({
              name: item.name,
              path: fullPath,
              score: score,
              item: item,
            });
          }
        }

        if (item.type === "folder" && item.children) {
          searchInTree(item.children, fullPath);
        }
      });
    };

    searchInTree(fileSystemStore.fileTree.children);

    // 점수순으로 정렬
    return results.sort((a, b) => b.score - a.score).slice(0, 10);
  }

  calculateScore(text, query) {
    if (text.includes(query)) {
      // 정확한 매치는 높은 점수
      return 100 - text.indexOf(query);
    }

    // 퍼지 매칭
    let score = 0;
    let queryIndex = 0;

    for (let i = 0; i < text.length && queryIndex < query.length; i++) {
      if (text[i] === query[queryIndex]) {
        score += 10;
        queryIndex++;
      }
    }

    return queryIndex === query.length ? score : 0;
  }

  selectNext() {
    if (this.searchResults.length > 0) {
      this.selectedIndex = (this.selectedIndex + 1) % this.searchResults.length;
    }
  }

  selectPrevious() {
    if (this.searchResults.length > 0) {
      this.selectedIndex =
        this.selectedIndex === 0
          ? this.searchResults.length - 1
          : this.selectedIndex - 1;
    }
  }

  getSelectedResult() {
    return this.searchResults[this.selectedIndex];
  }
}

export const searchStore = new SearchStore();
