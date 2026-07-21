plugins {
    `java-library`
    `java-test-fixtures`
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_6)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_6)
    }
}

configurations {
    // Single compileOnly declaration of kotlin-stdlib/reflect is reused by:
    // - module tests
    // - testFixtures compile + adapter modules that inherit KotlinDataClassContractTest
    testImplementation.get().extendsFrom(compileOnly.get())
    testFixturesApi.get().extendsFrom(compileOnly.get())
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    api(projects.core)

    // Consumers supply versions aligned with their Kotlin toolchain (min 1.6.21).
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlin.reflect)

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter)

    testImplementation(testFixtures(projects.core))
}
