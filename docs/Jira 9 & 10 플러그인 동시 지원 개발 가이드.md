# 📘 Jira 9 & 10 플러그인 듀얼 호환성 개발 가이드

## 1. 개요 (Strategy)

Jira 10은 **Atlassian Platform 7**을 기반으로 하며, 기존 Java EE (`javax.*`)에서 Jakarta EE (`jakarta.*`)로의 대대적인 전환이 이루어졌습니다. 본 가이드는 **"소스 코드는 하나로 유지하되(Single Source), 빌드 결과물은 두 가지 버전으로 산출(Dual Build)"**하는 전략을 채택하여 유지보수 효율성을 극대화합니다.

### 🎯 목표

* **Jira 9 Artifact:** `javax.*`, Java 8/11 호환, Spring 5.x
* **Jira 10 Artifact:** `jakarta.*`, Java 17 필수, Spring 6.x

---

## 2. 프로젝트 구조 (Directory Structure)

컴파일 호환성을 위해 **소스 디렉토리를 물리적으로 분리**합니다.

| 경로 | 명칭 | 포함 내용 | 비중 |
| --- | --- | --- | --- |
| `src/main/java` | **Common** | 비즈니스 로직, DTO, 유틸리티, **인터페이스** | 90% |
| `src/main/jira9` | **Legacy Adapter** | `javax.*` 의존 코드, Jira 9 전용 API 구현체 | 5% |
| `src/main/jira10` | **Modern Adapter** | `jakarta.*` 의존 코드, Jira 10 전용 API 구현체 | 5% |

---

## 3. Maven 설정 (`pom.xml`)

프로파일을 이용하여 빌드 타겟에 따라 **소스 경로**와 **의존성**을 동적으로 교체합니다.

### 3.1 `build-helper-maven-plugin` 설정

```xml
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

```

### 3.2 프로파일 정의 (핵심)

```xml
<profiles>
    <profile>
        <id>jira9</id>
        <activation><activeByDefault>true</activeByDefault></activation>
        <properties>
            <jira.version>9.12.7</jira.version>
            <extra.source.dir>src/main/jira9</extra.source.dir>
            <maven.compiler.source>1.8</maven.compiler.source>
            <maven.compiler.target>1.8</maven.compiler.target>
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
        </dependencies>
    </profile>
</profiles>

```

---

## 4. 마이그레이션 체크리스트 (Code Changes)

소스 코드 작성 시 아래 항목이 포함되어 있다면 **반드시 분리(`jira9`/`jira10`)**해야 합니다.

### 4.1 필수 변경 항목 (Namespace)

가장 빈번하게 발생하는 컴파일 에러 원인입니다.

| 기능 | Jira 9 (javax) | Jira 10 (jakarta) | 조치 방법 |
| --- | --- | --- | --- |
| **Servlet** | `javax.servlet.*` | `jakarta.servlet.*` | Filter, Servlet 구현체 분리 |
| **REST** | `javax.ws.rs.*` | `jakarta.ws.rs.*` | REST Controller 전체 분리 |
| **Inject (DI)** | `javax.inject.*` | `jakarta.inject.*` | 서비스 구현체 분리 |
| **XML (JAXB)** | `javax.xml.bind.*` | `jakarta.xml.bind.*` | XML 파싱 로직 분리 |
| **Validation** | `javax.validation.*` | `jakarta.validation.*` | DTO 내 어노테이션 사용 시 분리 |

### 4.2 라이브러리 및 API 변경

Platform 7 도입으로 인해 제거되거나 변경된 라이브러리 목록입니다.

| 라이브러리 | 변경 사항 | 권장 수정 방안 |
| --- | --- | --- |
| **Atlassian Fugue** | **제거됨** | `Option`, `Either`, `Pair` 등을 `java.util.Optional` 등 표준 라이브러리로 교체 (공통 코드 수정 권장) |
| **Log4j 1.x** | **완전 제거** | `org.apache.log4j.*` 사용 금지 → `org.slf4j.Logger`로 전면 교체 |
| **StringUtil** | `commons-lang` 제거 | `org.apache.commons.lang3.StringUtils` 사용 (버전 업그레이드) |
| **Spring Framework** | v5.x → **v6.x** | Spring 자체 유틸리티 클래스 사용 시 호환성 주의 |
| **Velocity** | v1.7 → **v2.x** | `.vm` 템플릿 내 문법 확인 (Backend 로직 영향은 적음) |

---

## 5. 구현 패턴 가이드 (Code Pattern)

### 5.1 인터페이스 기반 추상화 (Bridge Pattern)

버전별 구현체를 분리하고, 공통 로직은 인터페이스를 의존합니다.

1. **Interface (`src/main/java`)**: `public interface MyService { ... }`
2. **Jira 9 Impl (`src/main/jira9`)**:
```java
import javax.inject.Named;
@Named
public class MyServiceJira9 implements MyService { ... }

```


3. **Jira 10 Impl (`src/main/jira10`)**:
```java
import jakarta.inject.Named;
@Named
public class MyServiceJira10 implements MyService { ... }

```


4. **Usage (`src/main/java`)**:
```java
// 생성자 주입 (패키지 import 없이 주입받음)
public MyRestResource(MyService myService) { ... }

```



---

## 6. 빌드 및 배포 명령어

### 6.1 Jira 9용 빌드

```bash
mvn clean package -Pjira9 -DskipTests
# 결과: target/my-plugin-1.0.0-jira9.obr

```

### 6.2 Jira 10용 빌드

```bash
mvn clean package -Pjira10 -DskipTests
# 결과: target/my-plugin-1.0.0-jira10.obr

```

### ⚠️ 주의사항

* **Java Version:** Jira 10 빌드(`-Pjira10`)는 반드시 **JDK 17 이상**이 설치된 환경에서 실행해야 합니다.
* **IDE 설정:** IntelliJ 사용 시, Maven 프로파일 창에서 `jira9`를 체크하면 Jira 9 기준으로, `jira10`을 체크하면 Jira 10 기준으로 인덱싱이 다시 일어납니다. (동시 체크 금지)

---

이 문서를 기반으로 마이그레이션을 진행하시면, 향후 Jira 11 등 추가적인 버전 변화에도 유연하게 대처할 수 있는 구조를 갖추게 됩니다.