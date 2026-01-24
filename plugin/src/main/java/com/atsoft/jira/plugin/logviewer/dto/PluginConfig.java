package com.atsoft.jira.plugin.logviewer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean enabled;
    // Add more configuration fields here as needed
}
