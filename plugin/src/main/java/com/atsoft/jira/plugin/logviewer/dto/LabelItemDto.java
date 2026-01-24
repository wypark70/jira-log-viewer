package com.atsoft.jira.plugin.logviewer.dto;

public class LabelItemDto {
    private int id;
    private String customFieldId;
    private String name;
    private String projectId;

    public LabelItemDto() {}
    public LabelItemDto(int id, String customFieldId, String name, String projectId) {
        this.id = id;
        this.customFieldId = customFieldId;
        this.name = name;
        this.projectId = projectId;
    }

    // 게터/세터
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCustomFieldId() { return customFieldId; }
    public void setCustomFieldId(String customFieldId) { this.customFieldId = customFieldId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
}