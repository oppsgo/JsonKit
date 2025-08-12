plugins {
    id("java")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_6
    targetCompatibility = JavaVersion.VERSION_1_6
}

dependencies {
    compileOnly("org.jetbrains:annotations:20.1.0")
    implementation("com.alibaba:fastjson:1.2.83")
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