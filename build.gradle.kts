plugins {
    alias(libs.plugins.animalsniffer) apply false
}

version = "0.0.1"

subprojects {
    pluginManager.withPlugin("java") {
        pluginManager.apply(libs.plugins.animalsniffer.get().pluginId)

        tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.add("-Xlint:-options")
        }

        // Gradle 9 removes automatic JUnit Platform launcher loading.
        dependencies {
            "testRuntimeOnly"(platform(libs.junit.bom))
            "testRuntimeOnly"(libs.junit.platform.launcher)
        }

        tasks.withType<Test>() {
            useJUnitPlatform()
            testLogging {
                events("PASSED", "SKIPPED", "FAILED")
            }
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
