package ut.com.atsoft.jira.plugin.logviewer.helper;

import com.atsoft.jira.plugin.logviewer.helper.IssueCopyHelper;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.SimpleErrorCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IssueCopyHelperTest {

    @Mock
    private IssueService issueService;
    @Mock
    private IssueManager issueManager;
    @Mock
    private FieldLayoutManager fieldLayoutManager;
    @Mock
    private CustomFieldManager customFieldManager;
    @Mock
    private ProjectManager projectManager;

    @Mock
    private ApplicationUser user;
    @Mock
    private MutableIssue sourceIssue;
    @Mock
    private Project targetProject;
    @Mock
    private FieldLayout fieldLayout;
    @Mock
    private IssueInputParameters issueInputParameters;
    @Mock
    private IssueService.CreateValidationResult validationResult;
    @Mock
    private IssueService.IssueResult issueResult;
    @Mock
    private MutableIssue createdIssue;

    private IssueCopyHelper issueCopyHelper;

    @BeforeEach
    public void setup() {
        // Initialize ComponentAccessor mocks
        new MockComponentWorker()
                .addMock(IssueService.class, issueService)
                .addMock(IssueManager.class, issueManager)
                .addMock(FieldLayoutManager.class, fieldLayoutManager)
                .addMock(CustomFieldManager.class, customFieldManager)
                .addMock(ProjectManager.class, projectManager)
                .init();

        // Common stubs
        when(issueService.newIssueInputParameters()).thenReturn(issueInputParameters);
        when(projectManager.getProjectObjByKey("TARGET")).thenReturn(targetProject);
        when(targetProject.getId()).thenReturn(100L);
        when(issueInputParameters.getActionParameters()).thenReturn(new HashMap<>()); // Ensure map is not null

        issueCopyHelper = new IssueCopyHelper();
    }

    @Test
    public void testCopyIssueSuccess() {
        // Given
        when(issueManager.getIssueObject("SOURCE-1")).thenReturn(sourceIssue);
        when(fieldLayoutManager.getFieldLayout(targetProject, "10000")).thenReturn(fieldLayout);
        when(fieldLayout.getFieldLayoutItems()).thenReturn(Collections.emptyList());

        when(sourceIssue.getSummary()).thenReturn("Test Summary");
        when(sourceIssue.getDescription()).thenReturn("Test Description");

        // System fields stubs
        Priority priority = mock(Priority.class);
        when(priority.getId()).thenReturn("1");
        when(sourceIssue.getPriority()).thenReturn(priority);

        when(issueService.validateCreate(any(ApplicationUser.class), any(IssueInputParameters.class)))
                .thenReturn(validationResult);
        when(validationResult.isValid()).thenReturn(true);
        when(issueService.create(any(ApplicationUser.class), any(IssueService.CreateValidationResult.class)))
                .thenReturn(issueResult);
        when(issueResult.isValid()).thenReturn(true);
        when(issueResult.getIssue()).thenReturn(createdIssue);
        when(createdIssue.getKey()).thenReturn("TARGET-1");

        // When
        com.atlassian.jira.issue.Issue result = issueCopyHelper.copyIssue(user, "SOURCE-1", "TARGET", "10000");

        // Then
        assertNotNull(result);
        assertEquals("TARGET-1", result.getKey());

        verify(issueInputParameters).setSummary("Test Summary");
        verify(issueInputParameters).setDescription("Test Description");
        verify(issueInputParameters).setPriorityId("1");
        verify(issueInputParameters).setProjectId(100L);
        verify(issueInputParameters).setIssueTypeId("10000");
    }

    @Test
    public void testCopyCustomField_Select() {
        // Given
        when(issueManager.getIssueObject("SOURCE-1")).thenReturn(sourceIssue);
        when(fieldLayoutManager.getFieldLayout(targetProject, "10000")).thenReturn(fieldLayout);

        // Required Custom Field Setup
        FieldLayoutItem item = mock(FieldLayoutItem.class);
        OrderableField<?> field = mock(OrderableField.class);
        when(item.getOrderableField()).thenReturn(field);
        when(field.getId()).thenReturn("customfield_10001");
        when(item.isRequired()).thenReturn(true);
        when(fieldLayout.getFieldLayoutItems()).thenReturn(Collections.singletonList(item));

        // Custom Field Definition
        CustomField cf = mock(CustomField.class);
        CustomFieldType<?, ?> cfType = mock(CustomFieldType.class);
        when(customFieldManager.getCustomFieldObject("customfield_10001")).thenReturn(cf);
        when(cf.getCustomFieldType()).thenReturn(cfType);
        when(cfType.getKey()).thenReturn("com.atlassian.jira.plugin.system.customfieldtypes:select");
        when(cf.getId()).thenReturn("customfield_10001");

        // Custom Field Value
        Option option = mock(Option.class);
        when(sourceIssue.getCustomFieldValue(cf)).thenReturn(option);
        when(option.getOptionId()).thenReturn(55L);

        // Fail validation to avoid full create flow (we just want to verify input
        // params)
        when(issueService.validateCreate(any(), any())).thenReturn(validationResult);
        when(validationResult.isValid()).thenReturn(false);
        when(validationResult.getErrorCollection()).thenReturn(new SimpleErrorCollection());

        // When
        issueCopyHelper.copyIssue(user, "SOURCE-1", "TARGET", "10000");

        // Then
        verify(issueInputParameters).addCustomFieldValue("customfield_10001", "55");
    }

    @Test
    public void testCopyCustomField_UserPicker() {
        // Given
        when(issueManager.getIssueObject("SOURCE-1")).thenReturn(sourceIssue);
        when(fieldLayoutManager.getFieldLayout(targetProject, "10000")).thenReturn(fieldLayout);

        // Required Custom Field Setup
        FieldLayoutItem item = mock(FieldLayoutItem.class);
        OrderableField<?> field = mock(OrderableField.class);
        when(item.getOrderableField()).thenReturn(field);
        when(field.getId()).thenReturn("customfield_10002");
        when(item.isRequired()).thenReturn(true);
        when(fieldLayout.getFieldLayoutItems()).thenReturn(Collections.singletonList(item));

        // Custom Field Definition
        CustomField cf = mock(CustomField.class);
        CustomFieldType<?, ?> cfType = mock(CustomFieldType.class);
        when(customFieldManager.getCustomFieldObject("customfield_10002")).thenReturn(cf);
        when(cf.getCustomFieldType()).thenReturn(cfType);
        when(cfType.getKey()).thenReturn("com.atlassian.jira.plugin.system.customfieldtypes:userpicker");
        when(cf.getId()).thenReturn("customfield_10002");

        // Custom Field Value
        ApplicationUser valueUser = mock(ApplicationUser.class);
        when(sourceIssue.getCustomFieldValue(cf)).thenReturn(valueUser);
        when(valueUser.getKey()).thenReturn("user_key_1");

        // Fail validation
        when(issueService.validateCreate(any(), any())).thenReturn(validationResult);
        when(validationResult.isValid()).thenReturn(false);
        when(validationResult.getErrorCollection()).thenReturn(new SimpleErrorCollection());

        // When
        issueCopyHelper.copyIssue(user, "SOURCE-1", "TARGET", "10000");

        // Then
        verify(issueInputParameters).addCustomFieldValue("customfield_10002", "user_key_1");
    }

    @Test
    public void testValidationFailure() {
        // Given
        when(issueManager.getIssueObject("SOURCE-1")).thenReturn(sourceIssue);
        when(fieldLayoutManager.getFieldLayout(targetProject, "10000")).thenReturn(fieldLayout);
        when(fieldLayout.getFieldLayoutItems()).thenReturn(Collections.emptyList());

        when(issueService.validateCreate(any(), any())).thenReturn(validationResult);
        when(validationResult.isValid()).thenReturn(false);
        SimpleErrorCollection errors = new SimpleErrorCollection();
        errors.addErrorMessage("Validation failed");
        when(validationResult.getErrorCollection()).thenReturn(errors);

        // When
        com.atlassian.jira.issue.Issue result = issueCopyHelper.copyIssue(user, "SOURCE-1", "TARGET", "10000");

        // Then
        assertNull(result);
    }
}
