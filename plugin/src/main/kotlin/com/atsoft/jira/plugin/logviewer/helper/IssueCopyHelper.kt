@file:Suppress("SpellCheckingInspection")

package com.atsoft.jira.plugin.logviewer.helper

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.customfields.option.Option
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager
import com.atlassian.jira.issue.label.Label
import com.atlassian.jira.project.Project
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.util.ErrorCollection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.stream.Collectors

/**
 * 이슈 복사를 위한 헬퍼 클래스입니다. (시스템 필드 및 커스텀 필드 포함)
 * 가독성과 확장성을 높이기 위해 전략 패턴(Strategy Pattern)을 사용하여 리팩토링되었습니다.
 */
class IssueCopyHelper {
    private val issueService: IssueService = ComponentAccessor.getIssueService()
    private val issueManager: IssueManager = ComponentAccessor.getIssueManager()
    private val fieldLayoutManager: FieldLayoutManager = ComponentAccessor.getFieldLayoutManager()
    private val customFieldManager: CustomFieldManager = ComponentAccessor.getCustomFieldManager()

    private val systemFieldHandlers: MutableMap<String, BiConsumer<IssueInputParameters?, Issue?>?> =
        HashMap<String, BiConsumer<IssueInputParameters?, Issue?>?>()
    private val customFieldTypeHandlers: MutableList<CustomFieldTypeHandler> = ArrayList<CustomFieldTypeHandler>()

    init {
        initializeHandlers()
    }

    private fun initializeHandlers() {
        // --- 시스템 필드 핸들러 ---
        systemFieldHandlers["summary"] = BiConsumer { params: IssueInputParameters?, issue: Issue? ->
            params!!.summary = issue!!.summary
        }
        systemFieldHandlers["description"] = BiConsumer { params: IssueInputParameters?, issue: Issue? ->
            params!!.description = issue!!.description
        }

        systemFieldHandlers["priority"] = BiConsumer { params: IssueInputParameters?, issue: Issue? ->
            val priority = issue!!.priority
            if (priority != null) params!!.priorityId = priority.id
        }

        systemFieldHandlers["assignee"] = BiConsumer { params: IssueInputParameters?, issue: Issue? ->
            if (issue!!.assignee != null) params!!.assigneeId = issue.assignee.username
        }

        systemFieldHandlers["reporter"] = BiConsumer { params: IssueInputParameters?, issue: Issue? ->
            if (issue!!.reporter != null) params!!.reporterId = issue.reporter.username
        }

        systemFieldHandlers["duedate"] = BiConsumer { params: IssueInputParameters?, issue: Issue? ->
            if (issue!!.dueDate != null) params!!.dueDate = issue.dueDate.toString()
        }

        systemFieldHandlers["labels"] = BiConsumer { params: IssueInputParameters?, issue: Issue? ->
            if (!issue!!.labels.isEmpty()) {
                val labels = issue.labels.stream()
                    .map { obj: Label? -> obj!!.label }
                    .collect(Collectors.toSet())
                params!!.actionParameters["labels"] = labels.toTypedArray<String?>()
            }
        }

        // --- 커스텀 필드 핸들러 ---

        // 1. 단일 선택 (Select List) / 라디오 버튼 (Radio Buttons)
        customFieldTypeHandlers.add(
            CustomFieldTypeHandler(
                { key: String? -> key!!.endsWith(":select") || key.endsWith(":radiobuttons") },
                { params: IssueInputParameters?, cfId: String?, value: Any? ->
                    if (value is Option) {
                        val optionId = value.optionId
                        if (optionId != null) {
                            params!!.addCustomFieldValue(cfId, optionId.toString())
                        }
                    }
                })
        )

        // 2. 다중 선택 (Multi-Select) / 체크박스 (Checkboxes)
        customFieldTypeHandlers.add(
            CustomFieldTypeHandler(
                { key: String? -> key!!.endsWith(":multiselect") || key.endsWith(":multicheckboxes") },
                { params: IssueInputParameters?, cfId: String?, value: Any? ->
                    if (value is MutableCollection<*>) {
                        val optionIds: Array<String> = value
                            .filterIsInstance<Option>()       // Option 타입만 필터링
                            .mapNotNull { it.optionId }       // optionId(Long?) 추출 후 null 제거
                            .map(Long::toString)    // Long → String 변환 (메서드 참조 활용)
                            .toTypedArray()                   // Array<String>으로 변환
                        params!!.addCustomFieldValue(cfId, *optionIds)
                    }
                })
        )

        // 3. 사용자 선택 (User Picker)
        customFieldTypeHandlers.add(
            CustomFieldTypeHandler(
                { key: String? -> key!!.endsWith(":userpicker") },
                { params: IssueInputParameters?, cfId: String?, value: Any? ->
                    if (value is ApplicationUser) {
                        params!!.addCustomFieldValue(cfId, value.key)
                    }
                })
        )

        // 4. 다중 사용자 선택 (Multi-User Picker)
        customFieldTypeHandlers.add(
            CustomFieldTypeHandler(
                { key: String? -> key!!.endsWith(":multiuserpicker") },
                { params: IssueInputParameters?, cfId: String?, value: Any? ->
                    if (value is MutableCollection<*>) {
                        val userKeys: Array<String?> = value
                            .filterIsInstance<ApplicationUser>()   // ApplicationUser 타입만 필터링
                            .map { it.key }                        // 각 객체의 key 추출
                            .toTypedArray()                        // Array<String?>로 변환
                        params!!.addCustomFieldValue(cfId, *userKeys)
                    }
                })
        )

        // 5. 날짜 선택 (Date Picker)
        customFieldTypeHandlers.add(
            CustomFieldTypeHandler(
                { key: String? -> key!!.endsWith(":datepicker") },
                { params: IssueInputParameters?, cfId: String?, value: Any? ->
                    if (value is Date) {
                        params!!.addCustomFieldValue(cfId, SimpleDateFormat(DATE_FORMAT).format(value))
                    }
                })
        )

        // 6. 날짜/시간 선택 (DateTime Picker)
        customFieldTypeHandlers.add(
            CustomFieldTypeHandler(
                { key: String? -> key!!.endsWith(":datetime") },
                { params: IssueInputParameters?, cfId: String?, value: Any? ->
                    if (value is Date) {
                        params!!.addCustomFieldValue(cfId, SimpleDateFormat(DATETIME_FORMAT).format(value))
                    }
                })
        )

        // 기본값 / 텍스트 / 숫자 등 (Catch-all)
        customFieldTypeHandlers.add(
            CustomFieldTypeHandler(
                { _: String? -> true },
                { params: IssueInputParameters?, cfId: String?, value: Any? ->
                    params!!.addCustomFieldValue(
                        cfId,
                        value.toString()
                    )
                })
        )
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
    fun copyIssue(
        user: ApplicationUser?, sourceIssueKey: String?, targetProjectKey: String?,
        targetIssueTypeId: String?
    ): Issue? {
        // 1. 원본 데이터 조회

        val sourceIssue: Issue? = issueManager.getIssueObject(sourceIssueKey)
        val targetProject = ComponentAccessor.getProjectManager().getProjectObjByKey(targetProjectKey)

        if (sourceIssue == null || targetProject == null) {
            log.error("Source issue or Target project not found.")
            return null
        }

        // 2. 파라미터 컨테이너 생성 및 기본 설정
        val params = issueService.newIssueInputParameters()
        params.projectId = targetProject.id
        params.issueTypeId = targetIssueTypeId

        // 3. 필수 필드 검사 및 값 복사 (시스템 필드 + 커스텀 필드)
        populateFields(params, sourceIssue, targetProject, targetIssueTypeId)

        // 4. (옵션) 필수는 아니지만 항상 복사하고 싶은 필드는
        // populateFields에서 이미 복사되도록 처리되어 있으므로 여기서는 중복 설정을 피합니다.

        // 5. 유효성 검사 및 생성
        val validationResult = issueService.validateCreate(user, params)

        if (validationResult.isValid) {
            val createResult = issueService.create(user, validationResult)
            if (createResult.isValid) {
                log.info("Issue copied successfully: " + createResult.issue.key)
                return createResult.issue
            } else {
                logErrors(createResult.errorCollection)
            }
        } else {
            logErrors(validationResult.errorCollection)
        }

        return null
    }

    private fun populateFields(
        params: IssueInputParameters, sourceIssue: Issue, targetProject: Project?,
        targetIssueTypeId: String?
    ) {
        val fieldLayout = fieldLayoutManager.getFieldLayout(targetProject, targetIssueTypeId)
        val items = fieldLayout.fieldLayoutItems
        // 먼저 시스템 필드(요약, 설명, 우선순위 등)를 기본적으로 복사합니다.
        // 일부 환경에서는 필드 레이아웃에 '필수'로 표시되어 있지 않아도 시스템 필드는 항상 복사하는 것이 기대됩니다.
        for (systemFieldId in systemFieldHandlers.keys) {
            copyField(params, sourceIssue, systemFieldId)
        }

        for (item in items) {
            val fieldId = item.orderableField.id

            // 이미 설정된 필드(Project, IssueType 등)는 패스
            if (params.actionParameters.containsKey(fieldId)) continue

            // 필수 필드인 경우 복사
            if (item.isRequired) {
                copyField(params, sourceIssue, fieldId)
            }
        }
    }

    private fun copyField(params: IssueInputParameters?, sourceIssue: Issue, fieldId: String) {
        if (systemFieldHandlers.containsKey(fieldId)) {
            systemFieldHandlers[fieldId]!!.accept(params, sourceIssue)
        } else {
            copyCustomField(params, sourceIssue, fieldId)
        }
    }

    private fun copyCustomField(params: IssueInputParameters?, sourceIssue: Issue, fieldId: String) {
        if (!fieldId.startsWith("customfield_")) return

        val cf = customFieldManager.getCustomFieldObject(fieldId) ?: return
        val value = sourceIssue.getCustomFieldValue(cf) ?: return
        val key = cf.customFieldType.key

        // 핸들러를 순회하며 첫 번째로 일치하는 핸들러를 찾아 실행
        for (handler in customFieldTypeHandlers) {
            if (handler.predicate.test(key)) {
                handler.copier.accept(params, cf.id, value)
                return
            }
        }
    }

    private fun logErrors(errors: ErrorCollection) {
        errors.errorMessages.forEach(Consumer { msg: String? -> log.error("Error: $msg") })
        errors.errors.forEach { (field: String?, msg: String?) -> log.error("Field Error [$field]: $msg") }
    }

    // --- 내부 헬퍼 클래스/인터페이스 ---
    private class CustomFieldTypeHandler(
        val predicate: Predicate<String?>,
        val copier: TripleConsumer<IssueInputParameters?, String?, Any?>
    )

    private fun interface TripleConsumer<T, U, V> {
        fun accept(t: T?, u: U?, v: V?)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(IssueCopyHelper::class.java)
        private const val DATE_FORMAT = "d/MMM/yy"
        private const val DATETIME_FORMAT = "d/MMM/yy h:mm a"
    }
}