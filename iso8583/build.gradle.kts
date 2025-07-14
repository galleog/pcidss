plugins {
    id("ru.whyhappen.pcidss.gradle.library")
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.kotest.bom))

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    api("io.netty:netty-handler")
    api("org.slf4j:slf4j-api")
    api("org.springframework:spring-core")

    implementation("org.springframework:spring-context")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.netty:reactor-netty-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("io.micrometer:micrometer-observation")
    implementation("io.micrometer:micrometer-tracing")

    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation(libs.mockk)
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation(libs.awaitility.kotlin)
    testImplementation("io.micrometer:micrometer-tracing-integration-test")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("ch.qos.logback:logback-classic")
}