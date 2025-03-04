plugins {
    id("ru.whyhappen.pcidss.gradle.kotlin")
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(platform(libs.spring.boot.bom))

    implementation(project(":iso"))
    implementation(project(":bpc"))
    implementation(project(":way4"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation(libs.jreactive8583)
    implementation(libs.bc.fips)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}