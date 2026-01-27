package com.atsoft.jira.plugin.logviewer.condition;

import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractWebCondition;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import jakarta.inject.Inject; // Jira 10 (Platform 7) compatible
// import javax.inject.Inject; // Legacy

public class IsAdminCondition extends AbstractWebCondition {

    private final GlobalPermissionManager globalPermissionManager;

    @Inject
    public IsAdminCondition(@ComponentImport GlobalPermissionManager globalPermissionManager) {
        this.globalPermissionManager = globalPermissionManager;
    }

    @Override
    public boolean shouldDisplay(ApplicationUser user,
            com.atlassian.jira.plugin.webfragment.model.JiraHelper jiraHelper) {
        // 1. Check if user is logged in
        if (user == null) {
            return false;
        }

        // 2. Check if user has SYSTEM_ADMIN permission
        return globalPermissionManager.hasPermission(GlobalPermissionKey.SYSTEM_ADMIN, user);
    }
}
