# OSGi 종속성(com.pty4j) 누락 및 플러그인 로드 오류 수정 계획

Jira 10 기동 로그 분석 결과, `jira-log-viewer-plugin`이 로드되지 않는 주요 원인은 `com.pty4j` 패키지에 대한 OSGi 종속성을 해결하지 못했기 때문입니다.

## 원인 분석

1. **에러 메시지**: `missing requirement ... osgi.wiring.package; (osgi.wiring.package=com.pty4j)`
2. **원인**: `pty4j` 라이브러리는 일반적인 Java 라이브러리이며 OSGi 번들이 아닙니다. 플러그인 빌드 시 이 라이브러리가 포함되더라도, OSGi 설정에서 이 패키지를 외부에서 찾으려고 시도(Import-Package)하다가 실패하여 플러그인이 활성화되지 않습니다.
3. **해결 방법**: `pom.xml`의 `<Import-Package>` 설정에서 `com.pty4j` 패키지를 제외(`!`)하거나 선택 사항(`optional`)으로 표시하여, 플러그인 내부에 포함된(Bundled) 라이브러리를 사용하도록 강제해야 합니다.

## 제안된 변경 사항

### [MODIFY] [pom.xml](file:///d:/DevHome/projects/jira-log-viewer/plugin/pom.xml)

- `<Import-Package>` 섹션에 `!com.pty4j.*` 및 `!org.jetbrains.pty4j.*`를 추가하여 외부 임포트를 방지합니다.
- `pty4j`가 사용하는 다른 패키지들이 있을 경우를 대비하여 범위를 넓게 설정합니다.

## 검증 계획

### 자동화 테스트

- `atlas-package` 또는 `atlas-mvn package`를 실행하여 빌드 성공 확인.

### 수동 검증

- `atlas-run`을 실행하여 Jira가 기동된 후 'Jira Log Viewer Plugin'이 `ENABLED` 상태인지 확인.
- Jira 로그에서 더 이상 `com.pty4j` 관련 `missing requirement` 오류가 없는지 확인.
