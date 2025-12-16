package com.atsoft.jira.plugin.logviewer.helper;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.label.Label;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

public class IssueCopyHelper {

    private static final Logger log = LoggerFactory.getLogger(IssueCopyHelper.class);

    private final IssueService issueService;
    private final IssueManager issueManager;
    private final FieldLayoutManager fieldLayoutManager;
    private final CustomFieldManager customFieldManager;

    public IssueCopyHelper() {
        this.issueService = ComponentAccessor.getIssueService();
        this.issueManager = ComponentAccessor.getIssueManager();
        this.fieldLayoutManager = ComponentAccessor.getFieldLayoutManager();
        this.customFieldManager = ComponentAccessor.getCustomFieldManager();
    }

    /**
     * 외부 이슈를 현재 프로젝트로 복사합니다.
     *
     * @param user              작업을 수행하는 사용자
     * @param sourceIssueKey    원본 이슈 키 (예: "EXT-123")
     * @param targetProjectKey  복사될 대상 프로젝트 키 (예: "CUR-1")
     * @param targetIssueTypeId 대상 이슈 타입 ID (예: "10001" - Task)
     * @return 생성된 이슈 객체 (실패 시 null)
     */
    public Issue copyIssue(ApplicationUser user, String sourceIssueKey, String targetProjectKey,
            String targetIssueTypeId) {
        // 1. 원본 데이터 조회
        Issue sourceIssue = issueManager.getIssueObject(sourceIssueKey);
        Project targetProject = ComponentAccessor.getProjectManager().getProjectObjByKey(targetProjectKey);

        if (sourceIssue == null || targetProject == null) {
            log.error("Source issue or Target project not found.");
            return null;
        }

        // 2. 파라미터 컨테이너 생성 및 기본 설정
        IssueInputParameters params = issueService.newIssueInputParameters();
        params.setProjectId(targetProject.getId());
        params.setIssueTypeId(targetIssueTypeId);

        // 3. 필수 필드 검사 및 값 복사 (시스템 필드 + 커스텀 필드)
        populateFields(params, sourceIssue, targetProject, targetIssueTypeId);

        // 4. (옵션) 필수는 아니지만 항상 복사하고 싶은 필드 강제 설정
        // 시스템 필드: Summary, Description 등은 필수가 아니더라도 복사하는 것이 일반적
        if (params.getSummary() == null)
            params.setSummary(sourceIssue.getSummary());
        if (params.getDescription() == null)
            params.setDescription(sourceIssue.getDescription());

        // 5. 유효성 검사 및 생성
        IssueService.CreateValidationResult validationResult = issueService.validateCreate(user, params);

        if (validationResult.isValid()) {
            IssueService.IssueResult createResult = issueService.create(user, validationResult);
            if (createResult.isValid()) {
                log.info("Issue copied successfully: " + createResult.getIssue().getKey());
                return createResult.getIssue();
            } else {
                logErrors(createResult.getErrorCollection());
            }
        } else {
            logErrors(validationResult.getErrorCollection());
        }

        return null; // 실패
    }

    /**
     * 대상 프로젝트/이슈타입의 Field Configuration을 분석하여 필요한 필드 값을 원본에서 찾아 채웁니다.
     */
    private void populateFields(IssueInputParameters params, Issue sourceIssue, Project targetProject,
            String targetIssueTypeId) {
        // 대상 문맥의 필드 레이아웃 가져오기
        FieldLayout fieldLayout = fieldLayoutManager.getFieldLayout(targetProject, targetIssueTypeId);
        List<FieldLayoutItem> items = fieldLayout.getFieldLayoutItems();

        for (FieldLayoutItem item : items) {
            String fieldId = item.getOrderableField().getId();

            // 이미 설정된 필드(Project, IssueType 등)는 패스
            if (params.getActionParameters().containsKey(fieldId))
                continue;

            // 필수 필드이거나, 값을 반드시 복사해야 하는 경우 처리
            // (여기서는 '필수'인 경우에만 우선 복사하도록 구현)
            if (item.isRequired()) {
                copyField(params, sourceIssue, fieldId);
            }
        }
    }

    /**
     * 개별 필드 복사 로직 (System Fields + Custom Fields)
     */
    private void copyField(IssueInputParameters params, Issue sourceIssue, String fieldId) {
        // 1. 시스템 필드 처리
        switch (fieldId) {
            case "summary":
                params.setSummary(sourceIssue.getSummary());
                break;
            case "description":
                params.setDescription(sourceIssue.getDescription());
                break;
            case "priority":
                Priority priority = sourceIssue.getPriority();
                if (priority != null)
                    params.setPriorityId(priority.getId());
                break;
            case "assignee":
                if (sourceIssue.getAssignee() != null)
                    params.setAssigneeId(sourceIssue.getAssignee().getUsername());
                break;
            case "reporter":
                if (sourceIssue.getReporter() != null)
                    params.setReporterId(sourceIssue.getReporter().getUsername());
                break;
            case "duedate":
                if (sourceIssue.getDueDate() != null) {
                    // 날짜 변환 로직 필요 (String 포맷)
                    params.setDueDate(sourceIssue.getDueDate().toString());
                }
                break;
            case "labels":
                if (!sourceIssue.getLabels().isEmpty()) {
                    Set<String> labels = sourceIssue.getLabels().stream()
                            .map(Label::getLabel)
                            .collect(Collectors.toSet());
                    params.getActionParameters().put("labels", labels.toArray(new String[0]));
                }
                break;

            // 2. 커스텀 필드 처리
            default:
                if (fieldId.startsWith("customfield_")) {
                    CustomField cf = customFieldManager.getCustomFieldObject(fieldId);
                    if (cf != null) {
                        Object value = sourceIssue.getCustomFieldValue(cf);
                        if (value != null) {
                            // 주의: 커스텀 필드 타입에 따라 String 변환 방식이 다름
                            // 단순 텍스트/숫자라고 가정하고 toString() 사용.
                            // Select List인 경우 Option ID가 필요하므로 추가 로직 필요.
                            params.addCustomFieldValue(cf.getId(), value.toString());
                        }
                    }
                }
                break;
        }
    }

    private void logErrors(ErrorCollection errors) {
        errors.getErrorMessages().forEach(msg -> log.error("Error: " + msg));
        errors.getErrors().forEach((field, msg) -> log.error("Field Error [" + field + "]: " + msg));
    }
}