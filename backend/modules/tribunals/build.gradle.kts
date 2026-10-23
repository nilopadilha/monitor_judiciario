plugins {
    id("mj.module-conventions")
}

dependencies {
    implementation(project(":modules:djen"))
    implementation("org.springframework.boot:spring-boot-starter-cache")
}
