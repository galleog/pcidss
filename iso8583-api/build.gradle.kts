plugins {
    id("ru.whyhappen.pcidss.gradle.library")
}

dependencies {
    implementation(platform(libs.spring.boot.bom))

    api("org.springframework:spring-context")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api(libs.jreactive8583)
}