package com.atsoft.jira.plugin.logviewer.service.impl;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atsoft.jira.plugin.logviewer.service.CachedPluginSettingsService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of CachedPluginSettingsService.
 *
 * <p>
 * Why this is better than simple PluginConfigurationService:
 * </p>
 * <ul>
 * <li><strong>Performance:</strong> Uses {@link com.atlassian.cache.Cache} to
 * store settings in memory.
 * Repeated reads do not hit the database (PluginSettingsFactory).</li>
 * <li><strong>Cluster Safety:</strong> usage of
 * {@code settingsCache.remove(key)} (invalidation) ensures that
 * when a setting is updated on one node, the cache is invalidated across the
 * Jira Data Center cluster.
 * Other nodes will fetch the fresh value from the DB on their next access.</li>
 * <li><strong>Thread Safety:</strong> Atlassian Cache API handles concurrency
 * internally.</li>
 * </ul>
 */
@Component
@ExportAsService
@SuppressWarnings("null")
public class CachedPluginSettingsServiceImpl implements CachedPluginSettingsService, InitializingBean {

    private static final String PLUGIN_STORAGE_KEY = "com.atsoft.jira.plugin.logviewer";
    private static final String CACHE_NAME = "com.atsoft.jira.plugin.logviewer.settingsCache";

    @ComponentImport
    private final CacheManager cacheManager;
    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    // Cache<Key, Optional<Value>> to safely handle nulls (settings not present)
    private Cache<String, Optional<String>> settingsCache;

    @Inject
    public CachedPluginSettingsServiceImpl(
            @ComponentImport CacheManager cacheManager,
            @ComponentImport PluginSettingsFactory pluginSettingsFactory) {
        this.cacheManager = cacheManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    @Override
    public void afterPropertiesSet() {
        this.settingsCache = cacheManager.getCache(CACHE_NAME,
                new CacheLoader<String, Optional<String>>() {
                    @Nonnull
                    @Override
                    public Optional<String> load(@Nonnull String key) {
                        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
                        Object val = settings.get(PLUGIN_STORAGE_KEY + "." + key);
                        return Optional.ofNullable(val != null ? val.toString() : null);
                    }
                },
                new CacheSettingsBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS) // Safety net to refresh occasionally
                        .remote() // Critical for Data Center: enables distributed cache features
                        .replicateViaInvalidation() // Best practice for DB-backed caches: broadcast "remove"
                        .build());
    }

    @Override
    public String getSetting(String key) {
        // Retrieve from cache. If missing, CacheLoader is called.
        if (settingsCache == null) {
            return null;
        }
        return settingsCache.get(key).orElse(null);
    }

    @Override
    public void updateSetting(String key, String value) {
        // 1. Persist to Database (Source of Truth)
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        settings.put(PLUGIN_STORAGE_KEY + "." + key, value);

        // 2. Invalidate Cache (Cluster-wide)
        /*
         * We do NOT use put(key, value) here.
         * In a clustered environment (Data Center), simple local put() might lead to
         * stale data on other nodes,
         * or race conditions.
         * Calling remove(key) broadcasts an invalidation message.
         * All nodes (including this one) will just drop the cached value.
         * Next read will trigger the CacheLoader to fetch the fresh, committed value
         * from the DB.
         */
        if (settingsCache != null) {
            settingsCache.remove(key);
        }
    }
}
