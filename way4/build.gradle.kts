plugins {
    id("ru.whyhappen.pcidss.gradle.library")
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.kotest.bom))

    implementation(project(":iso8583"))
    implementation(project(":iso8583-autoconfigure"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("io.kotest:kotest-assertions-core")
}