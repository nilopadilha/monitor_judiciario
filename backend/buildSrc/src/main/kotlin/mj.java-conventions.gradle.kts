plugins {
    java
    jacoco
}

group = "br.com.monitorjudicial"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

val boms = listOf(
    "org.springframework.boot:spring-boot-dependencies:${Versions.springBoot}",
    "org.springframework.cloud:spring-cloud-dependencies:${Versions.springCloud}",
)

dependencies {
    boms.forEach { bom ->
        implementation(platform(bom))
        annotationProcessor(platform(bom))
        testImplementation(platform(bom))
        compileOnly(platform(bom))
        testCompileOnly(platform(bom))
        testAnnotationProcessor(platform(bom))
    }

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:${Versions.lombokMapstructBinding}")
    implementation("org.mapstruct:mapstruct:${Versions.mapstruct}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${Versions.mapstruct}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(
        listOf(
            "-parameters",
            "-Amapstruct.defaultComponentModel=spring",
            "-Amapstruct.unmappedTargetPolicy=ERROR",
        ),
    )
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off")
    systemProperty("user.timezone", "UTC")
}
