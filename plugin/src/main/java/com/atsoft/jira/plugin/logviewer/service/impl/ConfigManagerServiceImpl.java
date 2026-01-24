package com.atsoft.jira.plugin.logviewer.service.impl;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atsoft.jira.plugin.logviewer.dto.PluginConfig;
import com.atsoft.jira.plugin.logviewer.event.PluginConfigUpdatedEvent;
import com.atsoft.jira.plugin.logviewer.service.ConfigManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigManagerServiceImpl implements ConfigManagerService {

    private static final String PLUGIN_STORAGE_KEY = "com.atsoft.jira.plugin.logviewer";
    private static final String KEY_ENABLED = "enabled";

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;
    @ComponentImport
    private final EventPublisher eventPublisher;

    @Override
    public PluginConfig loadConfig() {
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        String enabledStr = (String) settings.get(PLUGIN_STORAGE_KEY + "." + KEY_ENABLED);
        boolean enabled = Boolean.parseBoolean(enabledStr);
        return new PluginConfig(enabled);
    }

    @Override
    public void saveConfig(PluginConfig config) {
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        settings.put(PLUGIN_STORAGE_KEY + "." + KEY_ENABLED, String.valueOf(config.isEnabled()));

        // Publish event to notify other components (e.g., CacheService)
        eventPublisher.publish(new PluginConfigUpdatedEvent(config));
    }
}
