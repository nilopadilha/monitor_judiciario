plugins {
    id("mj.java-conventions")
    `java-library`
}

dependencies {
    api(project(":shared"))
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("io.micrometer:micrometer-core")

    testImplementation("org.wiremock:wiremock-standalone:${Versions.wiremock}")
}
