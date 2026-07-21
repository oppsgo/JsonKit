plugins {
    alias(libs.plugins.animalsniffer) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

group = "com.github.oppsgo"
version = "1.0.4"

subprojects {
    group = rootProject.group
    version = rootProject.version

    // Benchmark is a local JMH harness — do not publish or Animal-Sniffer it.
    if (name == "benchmark") {
        return@subprojects
    }

    pluginManager.withPlugin("java") {
        // Kotlin support references kotlin.* (compileOnly); skip Animal Sniffer.
        if (name != "json-kotlin") {
            pluginManager.apply(libs.plugins.animalsniffer.get().pluginId)
        }
        pluginManager.apply("maven-publish")

        extensions.configure<JavaPluginExtension> {
            withSourcesJar()
        }

        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                }
            }
        }

        // JitPack rewrites *.module and points sourcesElements at the main jar
        // (wrong filename). Disable GMM so IDEs resolve -sources.jar via Maven POM.
        tasks.withType<GenerateModuleMetadata>().configureEach {
            enabled = false
        }

        tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.add("-Xlint:-options")
        }

        // Gradle 9 removes automatic JUnit Platform launcher loading.
        dependencies {
            "testRuntimeOnly"(platform(libs.junit.bom))
            "testRuntimeOnly"(libs.junit.platform.launcher)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("PASSED", "SKIPPED", "FAILED")
        }
    }

    pluginManager.withPlugin("ru.vyarus.animalsniffer") {
        dependencies {
            // Animal Sniffer Android API 19 signatures (includes common D8 desugared APIs)
            "signature"(libs.gummy.bears.api19) {
                artifact {
                    type = "signature"
                    extension = "signature"
                }
            }
        }
    }
}
