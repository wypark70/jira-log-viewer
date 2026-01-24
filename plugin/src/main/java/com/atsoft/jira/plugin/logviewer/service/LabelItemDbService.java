package com.atsoft.jira.plugin.logviewer.service;

import com.atsoft.jira.plugin.logviewer.dto.LabelItemDto;
import java.util.List;

public interface LabelItemDbService {
    List<LabelItemDto> getAll();
    LabelItemDto getById(int id);
    LabelItemDto create(String customFieldId, String name, String projectId);
    LabelItemDto update(int id, String customFieldId, String name, String projectId);
    void delete(int id);
}