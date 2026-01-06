좋습니다! 개발을 바로 시작하실 수 있도록 프로젝트 생성부터 컴포넌트 등록, 빌드까지의 전체 워크플로우를 단계별로 정리해 드릴게요.
1. 프로젝트 생성 (Terminal)
먼저 Atlassian SDK가 설치된 환경에서 아래 명령어를 입력하여 기본 뼈대를 만듭니다.
atlas-create-jira-plugin

 * groupId: com.example.jira
 * artifactId: impersonate-plugin
 * version: 1.0.0
 * package: com.example.jira
2. 의존성 설정 (pom.xml)
상태 변경(IssueService)과 REST API(JAX-RS) 기능을 사용하기 위해 pom.xml에 필요한 라이브러리가 포함되어 있는지 확인합니다. 보통 SDK가 기본으로 넣어주지만, provided 범위로 설정되어야 합니다.
<dependency>
    <groupId>com.atlassian.jira</groupId>
    <artifactId>jira-api</artifactId>
    <version>${jira.version}</version>
    <scope>provided</scope>
</dependency>

3. REST 엔드포인트 등록 (atlassian-plugin.xml)
Java 코드를 작성하기 전, 지라가 이 클래스를 REST API로 인식하도록 src/main/resources/atlassian-plugin.xml에 등록해야 합니다.
<rest key="impersonate-rest" path="/impersonate" version="1.0">
    <description>사용자 위임 상태 변경 API</description>
</rest>

4. 최종 코드 구현 (src/main/java/...)
앞서 설명드린 User Impersonation 로직을 담은 최종 클래스 구조입니다.
package com.example.jira.rest;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/action")
public class ImpersonateAction {

    @POST
    public Response changeStatus(@QueryParam("issueKey") String issueKey, 
                                 @QueryParam("userKey") String userKey, 
                                 @QueryParam("actionId") int actionId) {
        
        // 1. 서비스 및 유저 확보
        IssueService issueService = ComponentAccessor.getIssueService();
        JiraAuthenticationContext authContext = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser targetUser = ComponentAccessor.getUserManager().getUserByKey(userKey);
        Issue issue = ComponentAccessor.getIssueManager().getIssueObject(issueKey);
        ApplicationUser originalUser = authContext.getLoggedInUser();

        // 2. 권한 위임 (Impersonation)
        authContext.setLoggedInUser(targetUser);

        try {
            IssueInputParameters params = issueService.newIssueInputParameters();
            IssueService.TransitionValidationResult result = 
                issueService.validateTransition(targetUser, issue.getId(), actionId, params);

            if (result.isValid()) {
                issueService.transition(targetUser, result);
                return Response.ok("History recorded as: " + targetUser.getDisplayName()).build();
            }
            return Response.status(400).entity(result.getErrorCollection().toString()).build();
        } finally {
            // 3. 복구 (중요)
            authContext.setLoggedInUser(originalUser);
        }
    }
}

5. 빌드 및 배포 (Build & Deploy)
프로젝트 루트 디렉토리에서 아래 명령어를 실행합니다.
 * 컴파일 및 패키징: atlas-package (target 폴더에 .jar 파일 생성)
 * 지라 실행(테스트용): atlas-run (로컬에 테스트용 지라를 띄움)
 * 실제 지라 설치: 지라 관리자 페이지 -> 앱 관리 -> 앱 업로드 -> 생성된 .jar 선택
최종 확인 단계
이제 외부에서 아래와 같이 호출하면 모든 과정이 끝납니다!
 * URL: http://{Jira_URL}/rest/impersonate/1.0/action?issueKey=KEY-1&userKey=target_id&actionId=31
실제로 이 코드를 적용하시면서 빌드 에러가 나거나, 지라 버전 때문에 호환되지 않는 API가 있다면 바로 말씀해 주세요. 즉시 수정해 드리겠습니다. 시작해 보시겠어요?
