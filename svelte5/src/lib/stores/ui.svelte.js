class UIStore {
  sidebarWidth = $state(250);
  isSidebarVisible = $state(true);
  minSidebarWidth = 200;
  maxSidebarWidth = 500;

  setSidebarWidth(width) {
    if (width >= this.minSidebarWidth && width <= this.maxSidebarWidth) {
      this.sidebarWidth = width;
    }
  }

  toggleSidebar() {
    this.isSidebarVisible = !this.isSidebarVisible;
  }

  showSidebar() {
    this.isSidebarVisible = true;
  }

  hideSidebar() {
    this.isSidebarVisible = false;
  }
}

export const uiStore = new UIStore();
