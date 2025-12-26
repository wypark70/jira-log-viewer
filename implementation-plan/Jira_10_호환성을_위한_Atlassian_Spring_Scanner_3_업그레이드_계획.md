# Jira 10 호환성을 위한 Atlassian Spring Scanner 3 업그레이드 계획

현재 프로젝트는 Jira 10.7.4 버전을 사용하고 있지만, Atlassian Spring Scanner 버전은 2.2.4로 설정되어 있습니다. Jira 10(Platform 7)은 Spring 6와 Jakarta EE를 사용하므로 Spring Scanner 3.x 버전이 필요합니다.

`cvc-complex-type.2.4.c` 오류는 XML 검증기가 `atlassian-scanner:scan-indexes` 요소의 선언을 찾지 못해 발생하며, 이는 주로 버전 불일치나 런타임 종속성 누락으로 인해 발생합니다.

## 제안된 변경 사항

### [MODIFY] [pom.xml](file:///d:/DevHome/projects/jira-log-viewer/plugin/pom.xml)

- `atlassian.spring.scanner.version`을 `3.0.4`로 업데이트합니다.
- `atlassian-spring-scanner-annotation`의 범위를 `provided`로 설정합니다.
- `atlassian-spring-scanner-runtime` 종속성을 제거합니다 (Jira 10에서 기본 제공됨).

### [MODIFY] [plugin-context.xml](file:///d:/DevHome/projects/jira-log-viewer/plugin/src/main/resources/META-INF/spring/plugin-context.xml)

- 네임스페이스와 스키마 위치가 최신 버전에 맞는지 확인합니다.
- 프로젝트 빌드 시 스캔 인덱스가 올바르게 생성되도록 설정을 확인합니다.

## 검증 계획

### 자동화 테스트

- `mvn clean compile`을 실행하여 프로젝트가 새로운 종속성과 함께 성공적으로 컴파일되는지 확인합니다.

### 수동 검증

- Jira 10 인스턴스에 플러그인을 배포하여 컴포넌트 스캔 및 주입이 정상적으로 작동하는지 확인합니다.
- Jira 로그에서 Spring 초기화 관련 오류가 없는지 확인합니다.
