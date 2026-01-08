class SettingsStore {
  isPanelOpen = $state(false);

  // 에디터 설정
  fontSize = $state(14);
  fontFamily = $state("Consolas");
  showMinimap = $state(true);
  showLineNumbers = $state(true);
  wordWrap = $state("off");

  // 테마 설정
  editorTheme = $state("vs-dark");

  constructor() {
    this.loadSettings();
  }

  togglePanel() {
    this.isPanelOpen = !this.isPanelOpen;
  }

  openPanel() {
    this.isPanelOpen = true;
  }

  closePanel() {
    this.isPanelOpen = false;
  }

  // 설정 변경 메서드
  setFontSize(size) {
    this.fontSize = Math.max(10, Math.min(24, size));
    this.saveSettings();
  }

  setFontFamily(family) {
    this.fontFamily = family;
    this.saveSettings();
  }

  toggleMinimap() {
    this.showMinimap = !this.showMinimap;
    this.saveSettings();
  }

  toggleLineNumbers() {
    this.showLineNumbers = !this.showLineNumbers;
    this.saveSettings();
  }

  setWordWrap(wrap) {
    this.wordWrap = wrap;
    this.saveSettings();
  }

  setEditorTheme(theme) {
    this.editorTheme = theme;
    this.saveSettings();
  }

  // localStorage 관리
  loadSettings() {
    if (typeof window === "undefined") return;

    try {
      const saved = localStorage.getItem("editorSettings");
      if (saved) {
        const settings = JSON.parse(saved);
        this.fontSize = settings.fontSize ?? 14;
        this.fontFamily = settings.fontFamily ?? "Consolas";
        this.showMinimap = settings.showMinimap ?? true;
        this.showLineNumbers = settings.showLineNumbers ?? true;
        this.wordWrap = settings.wordWrap ?? "off";
        this.editorTheme = settings.editorTheme ?? "vs-dark";
      }
    } catch (error) {
      console.error("Failed to load settings:", error);
    }
  }

  saveSettings() {
    if (typeof window === "undefined") return;

    try {
      const settings = {
        fontSize: this.fontSize,
        fontFamily: this.fontFamily,
        showMinimap: this.showMinimap,
        showLineNumbers: this.showLineNumbers,
        wordWrap: this.wordWrap,
        editorTheme: this.editorTheme,
      };
      localStorage.setItem("editorSettings", JSON.stringify(settings));
    } catch (error) {
      console.error("Failed to save settings:", error);
    }
  }

  resetSettings() {
    this.fontSize = 14;
    this.fontFamily = "Consolas";
    this.showMinimap = true;
    this.showLineNumbers = true;
    this.wordWrap = "off";
    this.editorTheme = "vs-dark";
    this.saveSettings();
  }
}

export const settingsStore = new SettingsStore();
