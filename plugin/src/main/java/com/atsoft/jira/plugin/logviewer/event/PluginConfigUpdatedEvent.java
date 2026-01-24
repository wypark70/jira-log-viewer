package com.atsoft.jira.plugin.logviewer.event;

import com.atsoft.jira.plugin.logviewer.dto.PluginConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Event published when the plugin configuration is updated.
 */
@Getter
@RequiredArgsConstructor
public class PluginConfigUpdatedEvent {
    private final PluginConfig newConfig;
}
