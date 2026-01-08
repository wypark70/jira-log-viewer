import { detectFileType, FileType } from "$lib/utils/fileTypeDetector";
import { getMonacoLanguage } from "$lib/utils/languageMapper";

class EditorStore {
  openTabs = $state([]);
  activeTabIndex = $state(0);

  get activeTab() {
    return this.openTabs[this.activeTabIndex];
  }

  openFile(file) {
    const existingIndex = this.openTabs.findIndex(
      (tab) => tab.path === file.path
    );

    if (existingIndex !== -1) {
      // 이미 열려있는 파일이면 해당 탭으로 전환
      this.activeTabIndex = existingIndex;
    } else {
      // 새 파일 열기
      const fileType = detectFileType(file.name);

      this.openTabs.push({
        path: file.path,
        name: file.name,
        content: file.content || "",
        fileType: fileType,
        language:
          fileType === FileType.TEXT ? getMonacoLanguage(file.name) : null,
        size: file.size || 0,
        isDirty: false,
      });
      this.activeTabIndex = this.openTabs.length - 1;
    }
  }

  closeTab(index) {
    this.openTabs.splice(index, 1);
    if (this.activeTabIndex >= this.openTabs.length) {
      this.activeTabIndex = Math.max(0, this.openTabs.length - 1);
    }
  }

  setActiveTab(index) {
    this.activeTabIndex = index;
  }

  updateContent(path, content) {
    const tab = this.openTabs.find((t) => t.path === path);
    if (tab) {
      tab.content = content;
      tab.isDirty = true;
    }
  }
}

export const editorStore = new EditorStore();
