plugins {
    id("java")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_6
    targetCompatibility = JavaVersion.VERSION_1_6
}

dependencies {
    compileOnly("org.jetbrains:annotations:20.1.0")
    implementation(project(":core"))
    implementation("junit:junit:4.13.1")
}
