plugins {
    id("mj.module-conventions")
}

dependencies {
    implementation(project(":modules:clients"))
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("io.jsonwebtoken:jjwt-api:${Versions.jjwt}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${Versions.jjwt}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${Versions.jjwt}")
}
