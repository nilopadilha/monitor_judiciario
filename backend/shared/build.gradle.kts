plugins {
    id("mj.java-conventions")
    `java-library`
}

dependencies {
    api("org.springframework:spring-context")
    api("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.slf4j:slf4j-api")
}
