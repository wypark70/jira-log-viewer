<script>
  import { onMount, onDestroy } from "svelte";
  import { Terminal } from "xterm";
  import { FitAddon } from "xterm-addon-fit";
  import { WebLinksAddon } from "xterm-addon-web-links";
  import "xterm/css/xterm.css";

  let terminalContainer;
  let term;
  let socket;
  let fitAddon;

  onMount(() => {
    term = new Terminal({
      cursorBlink: true,
      theme: {
        background: "#1e1e1e",
        foreground: "#ffffff",
      },
      fontFamily: 'Menlo, Monaco, "Courier New", monospace',
      fontSize: 14,
    });

    fitAddon = new FitAddon();
    term.loadAddon(fitAddon);
    term.loadAddon(new WebLinksAddon());

    term.open(terminalContainer);
    fitAddon.fit();

    // Connect to WebSocket
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    // Adjust path as needed. Assuming /ws/terminal for now.
    const path = "/ws/terminal";
    // In a real plugin, this URL might need to be dynamic based on context path.
    // Assuming the proxy or server maps /ws/terminal correctly.

    // socket = new WebSocket(`${protocol}//${window.location.host}${path}`);
    socket = new WebSocket(`http://localhost:2990/jira/ws/terminal`);

    socket.onopen = () => {
      term.write("\r\n\x1b[32mConnected to Server Terminal\x1b[0m\r\n");
      // Send initial resize
      socket.send(`resize:${term.cols}:${term.rows}`);
    };

    socket.onmessage = (event) => {
      term.write(event.data);
    };

    socket.onclose = () => {
      term.write("\r\n\x1b[31mConnection closed\x1b[0m\r\n");
    };

    socket.onerror = (error) => {
      console.error("WebSocket error:", error);
      term.write("\r\n\x1b[31mConnection error\x1b[0m\r\n");
    };

    term.onData((data) => {
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(data);
      }
    });

    term.onResize((size) => {
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(`resize:${size.cols}:${size.rows}`);
      }
    });

    window.addEventListener("resize", onWindowResize);
  });

  onDestroy(() => {
    if (socket) {
      socket.close();
    }
    if (term) {
      term.dispose();
    }
    window.removeEventListener("resize", onWindowResize);
  });

  function onWindowResize() {
    if (fitAddon) {
      fitAddon.fit();
    }
  }
</script>

<div bind:this={terminalContainer} class="terminal-container"></div>

<style>
  .terminal-container {
    width: 100%;
    height: 100%;
    background-color: #1e1e1e;
    padding: 5px;
    box-sizing: border-box;
    overflow: hidden;
  }
</style>
