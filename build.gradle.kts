plugins {
    `java`
    `maven-publish`
    id("com.github.node-gradle.node") version "5.0.0" apply false
}

allprojects {
    group = "com.atsoft.jira.plugin"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://packages.atlassian.com/maven/repository/public") }
        maven { url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
