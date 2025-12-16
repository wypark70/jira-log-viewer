<script>
  let count = $state(0);
  let logLines = $state([
    { id: 1, time: '10:00:01', level: 'INFO', msg: 'Jira Log Viewer initialized' },
    { id: 2, time: '10:00:05', level: 'DEBUG', msg: 'Connecting to backend...' },
    { id: 3, time: '10:00:06', level: 'INFO', msg: 'Connection established' }
  ]);

  function addLog() {
    count += 1;
    logLines.push({
      id: Date.now(),
      time: new Date().toLocaleTimeString(),
      level: 'INFO',
      msg: `New log entry #${count}`
    });
  }
</script>

<main class="container">
  <header>
    <div class="logo">
      <span class="icon">⚡</span>
      <h1>Jira Log Viewer</h1>
    </div>
    <div class="status">
      <span class="badge online">Connected</span>
    </div>
  </header>

  <section class="hero glass-panel">
    <div class="hero-content">
      <h2 class="glow-text">Real-time Analysis</h2>
      <p>Monitor your Jira instance logs with modern precision.</p>
      <button class="btn-primary" onclick={addLog}>
        Generate Log Entry ({count})
      </button>
    </div>
  </section>

  <section class="log-container glass-panel">
    <div class="log-header">
      <span>Timestamp</span>
      <span>Level</span>
      <span>Message</span>
    </div>
    <div class="log-list">
      {#each logLines as line (line.id)}
        <div class="log-row">
          <span class="time">{line.time}</span>
          <span class="level" data-level={line.level}>{line.level}</span>
          <span class="msg">{line.msg}</span>
        </div>
      {/each}
    </div>
  </section>
</main>

<style>
  .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 2rem;
    width: 100%;
  }

  header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 3rem;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 1rem;
    font-size: 1.5rem;
  }

  .badge.online {
    background: rgba(16, 185, 129, 0.2);
    color: #34d399;
    padding: 0.4rem 0.8rem;
    border-radius: 20px;
    font-size: 0.85rem;
    border: 1px solid rgba(16, 185, 129, 0.3);
  }

  .hero {
    padding: 3rem;
    margin-bottom: 2rem;
    text-align: center;
    background: linear-gradient(180deg, rgba(30, 30, 35, 0.8) 0%, rgba(30, 30, 35, 0.4) 100%);
  }

  .hero h2 {
    font-size: 2.5rem;
    margin-bottom: 0.5rem;
  }

  .hero p {
    color: var(--text-muted);
    margin-bottom: 2rem;
    font-size: 1.1rem;
  }

  .log-container {
    padding: 1rem;
    min-height: 400px;
  }

  .log-header {
    display: grid;
    grid-template-columns: 100px 80px 1fr;
    padding: 1rem;
    border-bottom: 1px solid var(--glass-border);
    color: var(--text-muted);
    font-size: 0.9rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .log-list {
    max-height: 500px;
    overflow-y: auto;
  }

  .log-row {
    display: grid;
    grid-template-columns: 100px 80px 1fr;
    padding: 0.8rem 1rem;
    border-bottom: 1px solid rgba(255,255,255,0.03);
    transition: background 0.2s;
    font-family: 'JetBrains Mono', monospace;
    font-size: 0.9rem;
  }

  .log-row:hover {
    background: rgba(255,255,255,0.05);
  }

  .level[data-level="INFO"] { color: #60a5fa; }
  .level[data-level="DEBUG"] { color: #a1a1aa; }
  .level[data-level="ERROR"] { color: #f87171; }
</style>
