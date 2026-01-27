package com.atsoft.jira.plugin.logviewer.service.impl;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atsoft.jira.plugin.logviewer.dto.PluginConfig;
import com.atsoft.jira.plugin.logviewer.event.PluginConfigUpdatedEvent;
import com.atsoft.jira.plugin.logviewer.service.ConfigCacheService;
import com.atsoft.jira.plugin.logviewer.service.ConfigManagerService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ConfigCacheServiceImpl implements ConfigCacheService, InitializingBean, DisposableBean {

    private final EventPublisher eventPublisher;
    private final ConfigManagerService configManagerService;

    // Thread-safe cache
    private final AtomicReference<PluginConfig> cache = new AtomicReference<>();

    @Inject
    public ConfigCacheServiceImpl(@ComponentImport EventPublisher eventPublisher,
            ConfigManagerService configManagerService) {
        this.eventPublisher = eventPublisher;
        this.configManagerService = configManagerService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        eventPublisher.register(this);
        reloadCache();
    }

    @Override
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }

    @Override
    public PluginConfig getConfig() {
        PluginConfig config = cache.get();
        if (config == null) {
            reloadCache();
            return cache.get();
        }
        return config;
    }

    @EventListener
    public void onPluginConfigUpdated(PluginConfigUpdatedEvent event) {
        cache.set(event.getNewConfig());
    }

    private void reloadCache() {
        PluginConfig config = configManagerService.loadConfig();
        cache.set(config);
    }
}
