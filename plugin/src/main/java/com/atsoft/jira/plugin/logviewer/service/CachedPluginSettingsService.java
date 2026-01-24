package com.atsoft.jira.plugin.logviewer.service;

/**
 * Service for managing plugin settings with high-performance caching.
 * Uses Atlassian Cache API to reduce database load.
 */
public interface CachedPluginSettingsService {

    /**
     * Retrieves a setting value.
     * Uses internal cache for high performance.
     *
     * @param key The configuration key.
     * @return The configuration value, or null if not set.
     */
    String getSetting(String key);

    /**
     * Updates a setting value.
     * Persists to database and invalidates the cache to ensure consistency.
     *
     * @param key   The configuration key.
     * @param value The new value.
     */
    void updateSetting(String key, String value);
}
