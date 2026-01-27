package com.atsoft.jira.plugin.logviewer.condition;

import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractWebCondition;
import com.atsoft.jira.plugin.logviewer.dto.LabelItemDto;
import com.atsoft.jira.plugin.logviewer.service.LabelItemDbService;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

public class CheckLabelCondition extends AbstractWebCondition {

    private final LabelItemDbService labelItemDbService;
    private String customFieldIdToCheck;

    @Inject
    public CheckLabelCondition(LabelItemDbService labelItemDbService) {
        this.labelItemDbService = labelItemDbService;
    }

    @Override
    public void init(Map<String, String> params) {
        // 'atlassian-plugin.xml'의 <param> 태그에서 값을 읽어옵니다.
        // 예: <param name="targetCustomFieldId">customfield_10000</param>
        this.customFieldIdToCheck = params.get("targetCustomFieldId");
    }

    @Override
    public boolean shouldDisplay(ApplicationUser user, JiraHelper jiraHelper) {
        // 파라미터가 설정되지 않았으면 기본적으로 숨김 처리 (또는 정책에 따라 true)
        if (customFieldIdToCheck == null || customFieldIdToCheck.isEmpty()) {
            return false;
        }

        // DB에서 데이터를 조회 (ActiveObjects / JDBC)
        List<LabelItemDto> allItems = labelItemDbService.getAll();

        // 조회된 데이터 중 조건에 맞는 항목이 있는지 검사
        for (LabelItemDto item : allItems) {
            if (customFieldIdToCheck.equals(item.getCustomFieldId())) {
                return true; // 조건에 맞는 데이터가 있으면 메뉴 표시
            }
        }

        return false; // 없으면 숨김
    }
}
