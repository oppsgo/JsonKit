plugins {
    java
    alias(libs.plugins.jmh)
}

java {
    // Match library modules; use whatever JDK runs Gradle (8+). No toolchain download.
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    jmh(projects.core)
    jmh(projects.adapter.jsonFastjson2)
    jmh(projects.adapter.jsonFastjson)
    jmh(projects.adapter.jsonGson)
    jmh(libs.fastjson2) // bare JSON.* vs JsonKit in Fastjson2NativeVsJsonKitBenchmark
    jmh(libs.jmh.core)
    jmhAnnotationProcessor(libs.jmh.generator.annprocess)
}

jmh {
    jmhVersion.set(libs.versions.jmh.get())
    warmupIterations.set(3)
    iterations.set(5)
    fork.set(1)
    benchmarkMode.set(listOf("AverageTime"))
    timeUnit.set("ns")
    failOnError.set(true)
    duplicateClassesStrategy.set(DuplicatesStrategy.WARN)
    // e.g. ./gradlew :benchmark:jmh -PjmhInclude=GsonRoundTrip
    val include = findProperty("jmhInclude") as String?
    if (include != null) {
        includes.set(listOf(".*$include.*"))
    }
}
