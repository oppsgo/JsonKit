plugins {
    alias(libs.plugins.animalsniffer) apply false
}

group = "com.github.oppsgo"
version = "1.0.1"

subprojects {
    group = rootProject.group
    version = rootProject.version

    pluginManager.withPlugin("java") {
        pluginManager.apply(libs.plugins.animalsniffer.get().pluginId)
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
