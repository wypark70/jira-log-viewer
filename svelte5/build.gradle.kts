plugins {
    id("com.github.node-gradle.node") version "5.0.0"
}

description = "Frontend Svelte5 module - runs npm build via Gradle"

node {
    version.set("20.19.0")
    npmVersion.set("9.8.0")
    download.set(true)
    // workDir & nodeModulesDir default to project build dirs
}

// Register or configure npm tasks only if they are not already present
val npmInstallExisting = tasks.findByName("npmInstall")
if (npmInstallExisting == null) {
    tasks.register<com.github.gradle.node.npm.task.NpmTask>("npmInstall") {
        args.set(listOf("install"))
    }
} else {
    tasks.named<com.github.gradle.node.npm.task.NpmTask>("npmInstall") {
        args.set(listOf("install"))
    }
}

val npmBuildExisting = tasks.findByName("npmBuild")
if (npmBuildExisting == null) {
    tasks.register<com.github.gradle.node.npm.task.NpmTask>("npmBuild") {
        dependsOn("npmInstall")
        args.set(listOf("run", "build"))
    }
} else {
    tasks.named<com.github.gradle.node.npm.task.NpmTask>("npmBuild") {
        dependsOn("npmInstall")
        args.set(listOf("run", "build"))
    }
}

// Ensure assemble depends on npmBuild so plugin resources are produced
if (tasks.findByName("assemble") != null) {
    tasks.named("assemble") {
        dependsOn("npmBuild")
    }
}
