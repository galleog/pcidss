plugins {
    id("ru.whyhappen.pcidss.gradle.library")
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.kotest.bom))

    implementation(project(":iso8583"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("io.micrometer:micrometer-observation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
}