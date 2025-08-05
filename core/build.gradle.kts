plugins {
    id("java")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_6
    targetCompatibility = JavaVersion.VERSION_1_6
}

dependencies {
    compileOnly("org.jetbrains:annotations:20.1.0")

    testImplementation("junit:junit:4.13.1")

//    testImplementation(project(":adapter-gson"))
//    testImplementation("com.google.code.gson:gson:2.10.1")
}

tasks.test {
    useJUnit()
    testLogging {
        events("PASSED", "SKIPPED", "FAILED")
    }
}