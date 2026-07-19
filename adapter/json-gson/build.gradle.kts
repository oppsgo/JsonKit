plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    api(projects.core)
    implementation(libs.gson)

    testImplementation(testFixtures(projects.core))
}
