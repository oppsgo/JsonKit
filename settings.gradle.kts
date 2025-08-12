@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        maven("https://www.jitpack.io")
        google()
        mavenCentral()
    }
}

rootProject.name = "JsonKit"
include(":core")
include(":adapter-fastjson")
include(":adapter-gson")

include(":test-core")