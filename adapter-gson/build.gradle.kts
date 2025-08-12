plugins {
    id("java")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_6
    targetCompatibility = JavaVersion.VERSION_1_6
}

dependencies {
    compileOnly("org.jetbrains:annotations:20.1.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(project(":core"))

    testImplementation(project(":test-core"))
    testImplementation("junit:junit:4.13.1")
}

tasks.test {
    useJUnit()
    testLogging {
        events("PASSED", "SKIPPED", "FAILED")
    }
}
