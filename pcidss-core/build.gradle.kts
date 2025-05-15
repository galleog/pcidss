plugins {
    id("ru.whyhappen.pcidss.gradle.library")
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.kotest.bom))

    api(project(":iso8583-api"))
    api("org.springframework:spring-webflux")
    api("org.springframework.data:spring-data-redis")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api(libs.bc.fips)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation(libs.mockk)
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation(libs.mockwebserver)
    testImplementation(libs.jsonpathkt)
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("com.redis:testcontainers-redis")
}