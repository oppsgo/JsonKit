plugins {
    `java-library`
    `java-test-fixtures`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    compileOnly(libs.jetbrains.annotations)

    // Exported to adapter modules via testFixtures(project(":core"));
    // core's own test source set also depends on testFixtures.
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter)
}
