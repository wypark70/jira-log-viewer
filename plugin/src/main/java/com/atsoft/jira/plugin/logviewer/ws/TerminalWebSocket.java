package com.atsoft.jira.plugin.logviewer.ws;

import com.atsoft.jira.plugin.logviewer.service.PtyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint("/ws/terminal")
public class TerminalWebSocket {
    private static final Logger log = LoggerFactory.getLogger(TerminalWebSocket.class);
    private final PtyManager ptyManager = PtyManager.getInstance();
    private String sessionId;

    @OnOpen
    public void onOpen(Session session) {
        this.sessionId = session.getId();
        log.info("WebSocket opened: " + sessionId);
        try {
            ptyManager.startTerminal(sessionId, output -> {
                try {
                    session.getBasicRemote().sendText(output);
                } catch (IOException e) {
                    log.error("Error sending to websocket", e);
                }
            });
        } catch (IOException e) {
            log.error("Failed to start terminal", e);
            try {
                session.close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // Simple protocol: If message starts with "resize:", handle resize. Else write
        // to terminal.
        try {
            if (message.startsWith("resize:")) {
                String[] parts = message.split(":");
                if (parts.length == 3) {
                    int cols = Integer.parseInt(parts[1]);
                    int rows = Integer.parseInt(parts[2]);
                    ptyManager.resize(sessionId, cols, rows);
                }
            } else {
                ptyManager.write(sessionId, message);
            }
        } catch (IOException e) {
            log.error("Error writing to terminal", e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        log.info("WebSocket closed: " + sessionId);
        ptyManager.kill(sessionId);
    }

    @OnError
    public void onError(Session session, Throwable t) {
        log.error("WebSocket error", t);
        ptyManager.kill(sessionId);
    }
}
