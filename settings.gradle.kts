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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":core")
include(":adapter:json-gson")
include(":adapter:json-fastjson")
include(":adapter:json-fastjson2")
include(":benchmark")
