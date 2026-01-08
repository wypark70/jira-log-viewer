# 📘 [Master Guide] Jira 9 & 10 플러그인 듀얼 호환성 개발 가이드 (v3.0)

## 1. 개요 (Overview)

### 1.1 배경 및 목적

Jira 10은 **Atlassian Platform 7**을 기반으로 하며, Java EE에서 **Jakarta EE**로의 생태계 전환이 이루어졌습니다. 이로 인해 단일 소스 코드로는 두 버전을 동시에 지원할 수 없습니다. 본 가이드는 **"소스 코드는 하나로 유지하되(Single Source), 빌드 결과물은 이원화(Dual Build)"**하여 유지보수 효율성을 극대화하는 것을 목적으로 합니다.

### 1.2 핵심 전략

* **Common (`src/main/java`):** 비즈니스 로직, DTO, 인터페이스 (전체 코드의 90% 이상)
* **Legacy (`src/main/jira9`):** `javax.*` 패키지 및 Jira 9 전용 API 사용
* **Modern (`src/main/jira10`):** `jakarta.*` 패키지 및 Jira 10 전용 API 사용

---

## 2. 주요 변경 사항 및 영향도 분석 (Key Changes) - ⭐ 추가 조사 반영

단순히 `javax`가 `jakarta`로 바뀌는 것 외에도, 아래 라이브러리와 기능들이 대거 변경되었습니다. 이 항목들을 **분리 대상**으로 식별해야 합니다.

### 2.1 패키지 네임스페이스 변경 (필수 분리)

| 기능 영역 | Jira 9 (Java EE 8) | Jira 10 (Jakarta EE 9/10) | 영향받는 주요 클래스 |
| --- | --- | --- | --- |
| **Dependency Injection** | `javax.inject` | `jakarta.inject` | `@Inject`, `@Named` |
| **REST API** | `javax.ws.rs` | `jakarta.ws.rs` | `@Path`, `@GET`, `Response` |
| **Servlet** | `javax.servlet` | `jakarta.servlet` | `HttpServletRequest`, `Filter` |
| **Bean Validation** | `javax.validation` | `jakarta.validation` | `@NotNull`, `@Valid` |
| **XML Binding (JAXB)** | `javax.xml.bind` | `jakarta.xml.bind` | `JAXBContext`, `@XmlRootElement` |
| **Mail (SMTP)** 🆕 | `javax.mail` | `jakarta.mail` | `Message`, `Transport`, `InternetAddress` |
| **Lifecycle** 🆕 | `javax.annotation` | `jakarta.annotation` | `@PostConstruct`, `@PreDestroy` |

### 2.2 라이브러리 및 프레임워크 업그레이드

| 라이브러리 | 변경 사항 | 주의사항 및 대처 |
| --- | --- | --- |
| **Java Version** | Java 8/11 → **Java 17** | Jira 10 빌드는 반드시 JDK 17+ 환경에서 수행 필요. |
| **Spring Framework** | v5.x → **v6.x** | Spring 자체 유틸리티 클래스 사용 시 호환성 체크 필요. |
| **Hibernate** | v5.x → **v6.x** | ORM 사용 시 Dialect 및 쿼리 생성 방식 차이 주의. |
| **Velocity Engine** 🆕 | v1.7 → **v2.x** | `.vm` 파일 문법이 엄격해짐. 존재하지 않는 속성 참조 시 에러 발생 가능성 높음. (Backend 로직보단 템플릿 수정 필요) |
| **Log4j** | 1.x **완전 제거** | `org.apache.log4j` 패키지 사용 금지. 무조건 `slf4j`로 교체. |
| **Atlassian Fugue** | **제거됨** | `Option`, `Either` 등을 Java 표준 `Optional`로 리팩토링 필수. |

---

## 3. 프로젝트 구조 및 Maven 설정

### 3.1 디렉토리 구조

```text
project-root/
├── src/main/java      (Common: Interface, DTO, Logic)
├── src/main/jira9     (Jira 9 Impl: javax.* imports)
├── src/main/jira10    (Jira 10 Impl: jakarta.* imports)
└── pom.xml

```

### 3.2 Maven `pom.xml` 설정 (최종)

프로파일을 통해 소스 폴더와 의존성을 동적으로 교체합니다.

```xml
<profiles>
    <profile>
        <id>jira9</id>
        <activation><activeByDefault>true</activeByDefault></activation>
        <properties>
            <jira.version>9.12.7</jira.version>
            <extra.source.dir>src/main/jira9</extra.source.dir>
        </properties>
        <dependencies>
            <dependency>
                <groupId>javax.servlet</groupId>
                <artifactId>javax.servlet-api</artifactId>
                <version>4.0.1</version>
                <scope>provided</scope>
            </dependency>
            <dependency>
                <groupId>javax.inject</groupId>
                <artifactId>javax.inject</artifactId>
                <version>1</version>
                <scope>provided</scope>
            </dependency>
            <dependency>
                <groupId>javax.ws.rs</groupId>
                <artifactId>jsr311-api</artifactId>
                <version>1.1.1</version>
                <scope>provided</scope>
            </dependency>
             <dependency>
                <groupId>com.sun.mail</groupId>
                <artifactId>javax.mail</artifactId>
                <version>1.6.2</version>
                <scope>provided</scope>
            </dependency>
        </dependencies>
    </profile>

    <profile>
        <id>jira10</id>
        <properties>
            <jira.version>10.0.0</jira.version>
            <extra.source.dir>src/main/jira10</extra.source.dir>
            <maven.compiler.source>17</maven.compiler.source>
            <maven.compiler.target>17</maven.compiler.target>
        </properties>
        <dependencies>
            <dependency>
                <groupId>jakarta.servlet</groupId>
                <artifactId>jakarta.servlet-api</artifactId>
                <version>6.0.0</version>
                <scope>provided</scope>
            </dependency>
            <dependency>
                <groupId>jakarta.inject</groupId>
                <artifactId>jakarta.inject-api</artifactId>
                <version>2.0.1</version>
                <scope>provided</scope>
            </dependency>
            <dependency>
                <groupId>jakarta.ws.rs</groupId>
                <artifactId>jakarta.ws.rs-api</artifactId>
                <version>3.1.0</version>
                <scope>provided</scope>
            </dependency>
            <dependency>
                <groupId>jakarta.mail</groupId>
                <artifactId>jakarta.mail-api</artifactId>
                <version>2.1.2</version>
                <scope>provided</scope>
            </dependency>
        </dependencies>
    </profile>
</profiles>

<build>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>build-helper-maven-plugin</artifactId>
            <executions>
                <execution>
                    <id>add-source</id>
                    <phase>generate-sources</phase>
                    <goals><goal>add-source</goal></goals>
                    <configuration>
                        <sources>
                            <source>${extra.source.dir}</source>
                        </sources>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>

```

---

## 4. 구현 패턴: Bridge Pattern

### Step 1. Interface (Common)

```java
// src/main/java/com/myplugin/service/MailService.java
// javax나 jakarta 패키지를 절대 import하지 않습니다.
public interface MailService {
    void sendEmail(String to, String subject, String body);
}

```

### Step 2. Jira 9 Impl (Legacy)

```java
// src/main/jira9/com/myplugin/service/impl/MailServiceImpl.java
import javax.inject.Named;
import javax.mail.internet.MimeMessage; // javax.mail 사용

@Named
public class MailServiceImpl implements MailService {
    @Override
    public void sendEmail(...) {
        // Jira 9 방식 (javax.mail) 구현
    }
}

```

### Step 3. Jira 10 Impl (Modern)

```java
// src/main/jira10/com/myplugin/service/impl/MailServiceImpl.java
import jakarta.inject.Named;
import jakarta.mail.internet.MimeMessage; // jakarta.mail 사용

@Named
public class MailServiceImpl implements MailService {
    @Override
    public void sendEmail(...) {
        // Jira 10 방식 (jakarta.mail) 구현
    }
}

```

---

## 5. 🚨 자주 겪는 에러 및 해결 (Troubleshooting)

### Case 1. `@PostConstruct`, `@PreDestroy` 컴파일 에러

* **원인:** 라이프사이클 어노테이션도 패키지가 변경됨 (`javax.annotation` vs `jakarta.annotation`).
* **해결:** 해당 어노테이션을 사용하는 클래스는 반드시 `src/main/jira9`와 `src/main/jira10`으로 분리해야 합니다.

### Case 2. Velocity 템플릿 렌더링 에러 (화면이 깨짐)

* **증상:** Backend 에러 로그 없이 화면에 `$issue.getKey()` 같은 텍스트가 그대로 노출됨.
* **원인:** Jira 10의 Velocity 2.x는 문법이 더 엄격합니다. 객체가 null일 때 메서드를 호출하면 예외를 던지거나 렌더링을 멈출 수 있습니다.
* **해결:** `.vm` 파일에서 `$!issue.getKey()`와 같이 `!`(Silent Reference)를 붙이거나, Java 코드에서 null 체크를 강화해야 합니다.

### Case 3. IDE에서 "Cannot resolve symbol 'jakarta'"

* **해결:** IntelliJ Maven 패널 → Profiles → `jira10` 체크 → Reload. (한 번에 하나의 프로파일만 활성화해야 함)

### Case 4. `AbstractMethodError` (런타임)

* **원인:** 인터페이스 시그니처는 같은데, 컴파일된 바이트코드의 참조 패키지가 달라서 발생. (예: `javax.servlet.http.HttpServletRequest`를 인자로 받는 메서드)
* **해결:** 공통 인터페이스(`Common`)에서는 가능한 `javax`나 `jakarta` 타입 자체를 인자로 쓰지 않도록 설계해야 합니다. (Wrapper 클래스나 DTO 사용 권장)

### Case 5. `ClassNotFoundException: org.apache.log4j.Logger`

* **원인:** 레거시 코드에 Log4j 1.x 의존성이 남아있음.
* **해결:** `import org.apache.log4j`를 찾아 `import org.slf4j`로 모두 변경.

---

## 6. 결론 (Conclusion)

이 가이드를 준수하면 다음과 같은 효과를 얻을 수 있습니다.

1. **완벽한 호환성:** 하나의 프로젝트로 Jira 9와 10 OBR을 각각 생성.
2. **안전한 전환:** `javax`와 `jakarta`의 혼용으로 인한 런타임 충돌 원천 차단.
3. **유지보수 효율:** 비즈니스 로직(90%) 수정 시 두 버전에 자동 반영.

이 문서를 팀 내 개발 표준으로 배포하여 마이그레이션을 진행하시기 바랍니다.