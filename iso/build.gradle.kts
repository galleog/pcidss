plugins {
    id("ru.whyhappen.pcidss.gradle.library")
}

dependencies {
    implementation(platform(libs.spring.boot.bom))

    implementation(project(":iso8583-api"))
    implementation(project(":iso8583-autoconfigure"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}