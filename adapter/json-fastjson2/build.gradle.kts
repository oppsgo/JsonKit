plugins {
    id("java")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    implementation(libs.fastjson2)
    implementation(projects.core)

    testImplementation(testFixtures(projects.core))
}
