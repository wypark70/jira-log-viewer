package com.atsoft.jira.plugin.logviewer.service;

import com.atsoft.jira.plugin.logviewer.dto.PluginConfig;

public interface ConfigManagerService {
    PluginConfig loadConfig();

    void saveConfig(PluginConfig config);
}
