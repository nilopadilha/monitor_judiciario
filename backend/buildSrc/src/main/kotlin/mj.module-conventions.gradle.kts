// Convenção para módulos de negócio: dependem do kernel compartilhado e da base Spring.
plugins {
    id("mj.java-conventions")
    `java-library`
}

dependencies {
    api(project(":shared"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${Versions.springdoc}")
}
