# JIRA.Events 정리

Jira 플러그인 개발 시 자주 사용되는 주요 `JIRA.Events` 종류입니다.

## 1. `JIRA.Events.NEW_CONTENT_ADDED` (가장 중요)

Jira 페이지 내에서 **새로운 HTML 요소(DOM)가 추가될 때** 발생합니다.

- **발생 시점**: 다이얼로그(Dialog) 오픈, 인라인 편집 폼 로드, 페이지 일부 비동기 로딩 시 등.
- **주요 인자**:
  - `e`: 이벤트 객체
  - `context`: 새로 추가된 DOM 요소 (jQuery 객체 또는 Element)
  - `reason`: 발생 원인 (옵션)
- **활용 예시**: "이슈 생성" 팝업이 떴을 때 필드를 제어하거나, 특정 버튼에 이벤트를 바인딩할 때 사용합니다.
  ```javascript
  JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function (e, context, reason) {
    var $context = AJS.$(context);
    if ($context.find("#create-issue-dialog").length > 0) {
      // 이슈 생성 다이얼로그가 열렸을 때 로직
    }
  });
  ```

## 2. `JIRA.Events.ISSUE_REFRESHED`

이슈 상세 화면이나 특정 이슈 영역이 **새로고침(Refresh) 되었을 때** 발생합니다.

- **발생 시점**: 워크플로우 전환 후, 이슈 수정 완료 후, 코멘트 추가 후 등 화면이 갱신될 때.
- **활용 예시**: 이슈 상태가 변경된 후 UI를 다시 그려주거나 데이터를 갱신해야 할 때 사용합니다.

## 3. `JIRA.Events.PANEL_REFRESHED`

이슈 화면의 특정 **패널(Panel)이 갱신되었을 때** 발생합니다.

- **발생 시점**: 우측 사이드바(People, Dates 등)나 특정 탭 패널이 로드/갱신될 때.
- **활용 예시**: 사이드바에 커스텀 버튼을 추가했는데, 패널이 갱신되면서 버튼이 사라지는 것을 방지하기 위해 사용합니다.

## 4. `JIRA.Events.INLINE_EDIT_SAVE_COMPLETE`

인라인 편집(Inline Edit)으로 필드 값을 수정한 후 **저장이 완료되었을 때** 발생합니다.

- **활용 예시**: 인라인 편집으로 값이 바뀐 후 후속 작업을 처리할 때 유용합니다.

---

### 💡 팁: 브라우저에서 직접 확인하기

Jira 버전에 따라 사용 가능한 이벤트가 조금씩 다를 수 있습니다. 개발자 도구(F12) 콘솔(Console) 창에서 아래 명령어를 입력하면 현재 사용 가능한 모든 이벤트 상수를 확인할 수 있습니다.

```javascript
console.dir(JIRA.Events);
```
