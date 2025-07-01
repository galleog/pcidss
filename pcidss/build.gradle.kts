plugins {
    id("ru.whyhappen.pcidss.gradle.kotlin")
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.kotest.bom))

    implementation(project(":iso8583"))
    implementation(project(":iso8583-autoconfigure"))
    implementation(project(":iso"))
    implementation(project(":bpc"))
    implementation(project(":way4"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.lettuce:lettuce-core")
    implementation("org.apache.commons:commons-pool2")
    implementation(libs.shedlock.spring)
    implementation(libs.shedlock.provider)
    implementation(libs.bc.fips)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation(libs.mockk)
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation(libs.mockwebserver)
    testImplementation(libs.jsonpathkt)
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("com.redis:testcontainers-redis")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
}