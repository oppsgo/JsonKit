plugins {
    id("java")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_6
    targetCompatibility = JavaVersion.VERSION_1_6
}

dependencies {
    compileOnly("org.jetbrains:annotations:20.1.0")
    compileOnly("com.google.code.gson:gson:2.10.1")
    implementation(project(":core"))
}

