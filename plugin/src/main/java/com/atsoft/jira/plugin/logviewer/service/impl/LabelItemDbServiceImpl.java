package com.atsoft.jira.plugin.logviewer.service.impl;

import com.atsoft.jira.plugin.logviewer.dto.LabelItemDto;
import com.atsoft.jira.plugin.logviewer.repository.LabelItemRepository;
import com.atsoft.jira.plugin.logviewer.service.LabelItemDbService;

import lombok.RequiredArgsConstructor;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabelItemDbServiceImpl implements LabelItemDbService {
    @ComponentImport
    private final TransactionTemplate transactionTemplate;
    private final LabelItemRepository repository;

    @Override
    public List<LabelItemDto> getAll() {
        return transactionTemplate.execute(() -> repository.getAll());
    }

    @Override
    public LabelItemDto getById(int id) {
        return transactionTemplate.execute(() -> repository.getById(id));
    }

    @Override
    public LabelItemDto create(String customFieldId, String name, String projectId) {
        return transactionTemplate.execute(() -> repository.create(customFieldId, name, projectId));
    }

    @Override
    public LabelItemDto update(int id, String customFieldId, String name, String projectId) {
        return transactionTemplate.execute(() -> repository.update(id, customFieldId, name, projectId));
    }

    @Override
    public void delete(int id) {
        transactionTemplate.execute(() -> {
            repository.delete(id);
            return null;
        });
    }
}