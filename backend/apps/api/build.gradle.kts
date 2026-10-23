plugins {
    id("mj.java-conventions")
    id("org.springframework.boot")
}

val businessModules = listOf(
    ":shared",
    ":modules:authentication",
    ":modules:clients",
    ":modules:tribunals",
    ":modules:processes",
    ":modules:djen",
    ":modules:monitoring",
    ":modules:notifications",
    ":modules:audit",
    ":modules:dashboard",
)

dependencies {
    businessModules.forEach { implementation(project(it)) }

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${Versions.springdoc}")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:rabbitmq")
    testImplementation("org.awaitility:awaitility")
    testImplementation("com.tngtech.archunit:archunit-junit5:${Versions.archunit}")
    testImplementation("org.wiremock:wiremock-standalone:${Versions.wiremock}")
}

springBoot {
    mainClass.set("br.com.monitorjudicial.MonitorJudicialApplication")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("monitor-judicial-api.jar")
}

// ---- Cobertura agregada de todos os módulos (meta do PDF: ≥ 90 %) ----
val coverageExclusions = listOf(
    "**/*Entity.class",
    "**/*Application.class",
    "**/config/**",
    "**/*Configuration.class",
    "**/*Properties.class",
    "**/*Request.class",
)

val coveredProjects = businessModules.map { project(it) } + project
coveredProjects.filter { it != project }.forEach { evaluationDependsOn(it.path) }

val aggregatedSources = files(coveredProjects.map { it.layout.projectDirectory.dir("src/main/java") })
val aggregatedClasses = files(coveredProjects.map { p ->
    p.layout.buildDirectory.dir("classes/java/main").map { dir -> fileTree(dir) { exclude(coverageExclusions) } }
})
val aggregatedExecutionData = files(coveredProjects.map { it.layout.buildDirectory.file("jacoco/test.exec") })

val coverageReport by tasks.registering(JacocoReport::class) {
    group = "verification"
    description = "Relatório de cobertura agregado do backend."
    dependsOn(coveredProjects.map { "${it.path}:test" })
    sourceDirectories.from(aggregatedSources)
    classDirectories.from(aggregatedClasses)
    executionData.from(aggregatedExecutionData.filter { it.exists() })
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

val coverageVerification by tasks.registering(JacocoCoverageVerification::class) {
    group = "verification"
    description = "Falha o build se a cobertura de linhas ficar abaixo de 90 %."
    dependsOn(coverageReport)
    sourceDirectories.from(aggregatedSources)
    classDirectories.from(aggregatedClasses)
    executionData.from(aggregatedExecutionData.filter { it.exists() })
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(coverageVerification)
}
