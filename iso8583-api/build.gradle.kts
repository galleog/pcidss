plugins {
    id("ru.whyhappen.pcidss.gradle.library")
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.kotest.bom))

    api("org.springframework:spring-context")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api(libs.jreactive8583) {
        exclude("io.netty")
    }
    api("io.projectreactor.netty:reactor-netty-core")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    api("io.projectreactor.kotlin:reactor-kotlin-extensions")
    api("io.micrometer:micrometer-observation")
    api("io.micrometer:micrometer-tracing")

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