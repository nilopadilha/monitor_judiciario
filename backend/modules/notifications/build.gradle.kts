plugins {
    id("mj.module-conventions")
}

dependencies {
    implementation(project(":modules:clients"))
    implementation(project(":modules:processes"))
    implementation(project(":modules:monitoring"))
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("io.micrometer:micrometer-core")
}
