package com.atsoft.jira.plugin.logviewer.helper;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 이슈 복사를 위한 헬퍼 클래스입니다. (시스템 필드 및 커스텀 필드 포함)
 * 가독성과 확장성을 높이기 위해 전략 패턴(Strategy Pattern)을 사용하여 리팩토링되었습니다.
 */
public class IssueCopyHelper {

    private static final Logger log = LoggerFactory.getLogger(IssueCopyHelper.class);
    private static final String DATE_FORMAT = "d/MMM/yy";
    private static final String DATETIME_FORMAT = "d/MMM/yy h:mm a";

    private final IssueService issueService;
    private final IssueManager issueManager;
    private final FieldLayoutManager fieldLayoutManager;
    private final CustomFieldManager customFieldManager;

    private final Map<String, BiConsumer<IssueInputParameters, Issue>> systemFieldHandlers = new HashMap<>();
    private final List<CustomFieldTypeHandler> customFieldTypeHandlers = new ArrayList<>();

    public IssueCopyHelper() {
        this.issueService = ComponentAccessor.getIssueService();
        this.issueManager = ComponentAccessor.getIssueManager();
        this.fieldLayoutManager = ComponentAccessor.getFieldLayoutManager();
        this.customFieldManager = ComponentAccessor.getCustomFieldManager();

        initializeHandlers();
    }

    private void initializeHandlers() {
        // --- 시스템 필드 핸들러 ---
        systemFieldHandlers.put("summary", (params, issue) -> params.setSummary(issue.getSummary()));
        systemFieldHandlers.put("description", (params, issue) -> params.setDescription(issue.getDescription()));

        systemFieldHandlers.put("priority", (params, issue) -> {
            Priority priority = issue.getPriority();
            if (priority != null)
                params.setPriorityId(priority.getId());
        });

        systemFieldHandlers.put("assignee", (params, issue) -> {
            if (issue.getAssignee() != null)
                params.setAssigneeId(issue.getAssignee().getUsername());
        });

        systemFieldHandlers.put("reporter", (params, issue) -> {
            if (issue.getReporter() != null)
                params.setReporterId(issue.getReporter().getUsername());
        });

        systemFieldHandlers.put("duedate", (params, issue) -> {
            if (issue.getDueDate() != null)
                params.setDueDate(issue.getDueDate().toString());
        });

        systemFieldHandlers.put("labels", (params, issue) -> {
            if (!issue.getLabels().isEmpty()) {
                Set<String> labels = issue.getLabels().stream()
                        .map(Label::getLabel)
                        .collect(Collectors.toSet());
                params.getActionParameters().put("labels", labels.toArray(new String[0]));
            }
        });

        // --- 커스텀 필드 핸들러 ---

        // 1. 단일 선택 (Select List) / 라디오 버튼 (Radio Buttons)
        customFieldTypeHandlers.add(new CustomFieldTypeHandler(
                key -> key.endsWith(":select") || key.endsWith(":radiobuttons"),
                (params, cfId, value) -> {
                    if (value instanceof Option) {
                        Long optionId = ((Option) value).getOptionId();
                        if (optionId != null) {
                            params.addCustomFieldValue(cfId, optionId.toString());
                        }
                    }
                }));

        // 2. 다중 선택 (Multi-Select) / 체크박스 (Checkboxes)
        customFieldTypeHandlers.add(new CustomFieldTypeHandler(
                key -> key.endsWith(":multiselect") || key.endsWith(":multicheckboxes"),
                (params, cfId, value) -> {
                    if (value instanceof Collection) {
                        String[] optionIds = ((Collection<?>) value).stream()
                                .filter(o -> o instanceof Option)
                                .map(o -> ((Option) o).getOptionId())
                                .filter(Objects::nonNull)
                                .map(Object::toString)
                                .toArray(String[]::new);
                        params.addCustomFieldValue(cfId, optionIds);
                    }
                }));

        // 3. 사용자 선택 (User Picker)
        customFieldTypeHandlers.add(new CustomFieldTypeHandler(
                key -> key.endsWith(":userpicker"),
                (params, cfId, value) -> {
                    if (value instanceof ApplicationUser) {
                        params.addCustomFieldValue(cfId, ((ApplicationUser) value).getKey());
                    }
                }));

        // 4. 다중 사용자 선택 (Multi-User Picker)
        customFieldTypeHandlers.add(new CustomFieldTypeHandler(
                key -> key.endsWith(":multiuserpicker"),
                (params, cfId, value) -> {
                    if (value instanceof Collection) {
                        String[] userKeys = ((Collection<?>) value).stream()
                                .filter(u -> u instanceof ApplicationUser)
                                .map(u -> ((ApplicationUser) u).getKey())
                                .toArray(String[]::new);
                        params.addCustomFieldValue(cfId, userKeys);
                    }
                }));

        // 5. 날짜 선택 (Date Picker)
        customFieldTypeHandlers.add(new CustomFieldTypeHandler(
                key -> key.endsWith(":datepicker"),
                (params, cfId, value) -> {
                    if (value instanceof Date || value instanceof Timestamp) {
                        params.addCustomFieldValue(cfId, new SimpleDateFormat(DATE_FORMAT).format((Date) value));
                    }
                }));

        // 6. 날짜/시간 선택 (DateTime Picker)
        customFieldTypeHandlers.add(new CustomFieldTypeHandler(
                key -> key.endsWith(":datetime"),
                (params, cfId, value) -> {
                    if (value instanceof Date || value instanceof Timestamp) {
                        params.addCustomFieldValue(cfId, new SimpleDateFormat(DATETIME_FORMAT).format((Date) value));
                    }
                }));

        // 기본값 / 텍스트 / 숫자 등 (Catch-all)
        customFieldTypeHandlers.add(new CustomFieldTypeHandler(
                key -> true,
                (params, cfId, value) -> params.addCustomFieldValue(cfId, value.toString())));
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
        // populateFields에서 필수 필드가 아니어서 건너뛰어졌을 수 있지만, 일반적으로 이 필드들은 복사하는 것이 좋습니다.
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

        return null;
    }

    private void populateFields(IssueInputParameters params, Issue sourceIssue, Project targetProject,
            String targetIssueTypeId) {
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

    private void copyField(IssueInputParameters params, Issue sourceIssue, String fieldId) {
        if (systemFieldHandlers.containsKey(fieldId)) {
            systemFieldHandlers.get(fieldId).accept(params, sourceIssue);
        } else {
            copyCustomField(params, sourceIssue, fieldId);
        }
    }

    private void copyCustomField(IssueInputParameters params, Issue sourceIssue, String fieldId) {
        if (!fieldId.startsWith("customfield_"))
            return;

        CustomField cf = customFieldManager.getCustomFieldObject(fieldId);
        if (cf == null)
            return;

        Object value = sourceIssue.getCustomFieldValue(cf);
        if (value == null)
            return;

        String key = cf.getCustomFieldType().getKey();

        // 핸들러를 순회하며 첫 번째로 일치하는 핸들러를 찾아 실행
        for (CustomFieldTypeHandler handler : customFieldTypeHandlers) {
            if (handler.predicate.test(key)) {
                handler.copier.accept(params, cf.getId(), value);
                return;
            }
        }
    }

    private void logErrors(ErrorCollection errors) {
        errors.getErrorMessages().forEach(msg -> log.error("Error: " + msg));
        errors.getErrors().forEach((field, msg) -> log.error("Field Error [" + field + "]: " + msg));
    }

    // --- 내부 헬퍼 클래스/인터페이스 ---

    private static class CustomFieldTypeHandler {
        final Predicate<String> predicate;
        final TripleConsumer<IssueInputParameters, String, Object> copier;

        CustomFieldTypeHandler(Predicate<String> predicate,
                TripleConsumer<IssueInputParameters, String, Object> copier) {
            this.predicate = predicate;
            this.copier = copier;
        }
    }

    @FunctionalInterface
    private interface TripleConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }
}