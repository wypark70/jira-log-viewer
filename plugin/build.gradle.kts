plugins {
    `java`
    kotlin("jvm") version "2.2.20"
}

description = "Jira Log Viewer plugin module (converted to Gradle Kotlin DSL)"

repositories {
    mavenCentral()
    maven("https://packages.atlassian.com/maven/repository/public")
}

java {
    toolchain {
        languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(21))
    }
}

dependencies {
    // Provided / compileOnly (from pom.xml)
    compileOnly("com.atlassian.jira:jira-api:10.7.4")
    compileOnly("com.atlassian.plugin:atlassian-spring-scanner-annotation:3.0.4")
    compileOnly("jakarta.inject:jakarta.inject-api:2.0.0")
    compileOnly("org.slf4j:slf4j-api:2.0.16")
    compileOnly("org.projectlombok:lombok:1.18.42")

    // Runtime / implementation
    implementation("com.sigpwned:chardet4j:77.1.0")
    implementation("com.github.albfernandez:juniversalchardet:2.4.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.20")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // Test dependencies (mapped from pom.xml)
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testImplementation("com.atlassian.jira:jira-tests:10.7.4")
    testImplementation("com.atlassian.plugins:atlassian-plugins-osgi-testrunner:2.0.16")
    testImplementation("ch.qos.logback:logback-classic:1.5.23")
    testImplementation("org.apache.lucene:lucene-core:8.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.2.20")
    testImplementation("org.projectlombok:lombok:1.18.42")

    // Ensure provided dependencies are available on the test classpath
    testImplementation("com.atlassian.jira:jira-api:10.7.4")
    testImplementation("com.atlassian.plugin:atlassian-spring-scanner-annotation:3.0.4")
    testImplementation("jakarta.inject:jakarta.inject-api:2.0.0")
    testImplementation("org.slf4j:slf4j-api:2.0.16")
}

tasks.test {
    useJUnitPlatform()
    // Exclude integration / wired tests that require Atlassian test runner environment
    // Keep only unit tests under package 'ut' to allow Gradle build to proceed
    include("**/ut/**")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.addAll(listOf("-Xnested-type-aliases"))
    }
}

// Note: Atlassian-specific packaging and OBR generation (jira-maven-plugin) is not directly mapped.
// Consider adding custom Gradle tasks or keeping the Maven packaging step for OBR creation.
