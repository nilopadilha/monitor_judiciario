plugins {
    id("mj.module-conventions")
}

dependencies {
    implementation(project(":modules:processes"))
    implementation(project(":modules:djen"))
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("io.micrometer:micrometer-core")
}
