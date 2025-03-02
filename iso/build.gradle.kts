plugins {
    id("ru.whyhappen.pcidss.gradle.library")
}

dependencies {
    implementation(platform(libs.spring.boot.bom))

    api(project(":iso8583-api"))
    api("org.springframework.boot:spring-boot-autoconfigure")
}