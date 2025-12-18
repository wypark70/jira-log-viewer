package com.atsoft.jira.plugin.logviewer.service;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class PtyManager {
    private static final Logger log = LoggerFactory.getLogger(PtyManager.class);
    private static final PtyManager INSTANCE = new PtyManager();

    private final Map<String, PtyProcess> processes = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public static PtyManager getInstance() {
        return INSTANCE;
    }

    public void startTerminal(String sessionId, Consumer<String> onOutput) throws IOException {
        String[] cmd = isWindows() ? new String[] { "cmd.exe" } : new String[] { "/bin/bash", "-l" };
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("TERM", "xterm");

        PtyProcess process = new PtyProcessBuilder(cmd)
                .setEnvironment(env)
                .start();

        processes.put(sessionId, process);

        // Read output in a separate thread
        executor.submit(() -> {
            try (InputStream is = process.getInputStream()) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    onOutput.accept(new String(buffer, 0, read, StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                log.error("Error reading PTY output", e);
            } finally {
                kill(sessionId);
            }
        });
    }

    public void write(String sessionId, String command) throws IOException {
        PtyProcess process = processes.get(sessionId);
        if (process != null && process.isAlive()) {
            OutputStream os = process.getOutputStream();
            os.write(command.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    public void resize(String sessionId, int cols, int rows) {
        PtyProcess process = processes.get(sessionId);
        if (process != null && process.isAlive()) {
            process.setWinSize(new WinSize(cols, rows));
        }
    }

    public void kill(String sessionId) {
        PtyProcess process = processes.remove(sessionId);
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
