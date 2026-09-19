plugins {
    java
    application
    jacoco
    id("com.diffplug.spotless") version "8.10.2"
    id("org.sonarqube") version "7.4.0.8496"
}

group = "com.washflow"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Javalin Core & Servidor Jetty embutido
    implementation("io.javalin:javalin:7.0.0")

    // JDBI Core e suporte a Interfaces (SqlObject) (CORRIGIDO: Parênteses e aspas duplas)
    implementation("org.jdbi:jdbi3-core:3.45.1")
    implementation("org.jdbi:jdbi3-sqlobject:3.45.1")

    // Driver do Banco de Dados (CORRIGIDO: Parênteses e aspas duplas)
    implementation("com.h2database:h2:2.2.224")
    implementation("org.postgresql:postgresql:42.7.13")

    // Pool de conexões (o mesmo papel que o Spring Boot cobre "de graça" via
    // spring-boot-starter-jdbc) usado pelo JdbiFactory.
    implementation("com.zaxxer:HikariCP:7.1.0")

    // Migrações de schema (o mesmo papel do Flyway/Liquibase auto-configurados
    // pelo Spring Boot) - roda contra o mesmo DataSource do JdbiFactory.
    implementation("org.flywaydb:flyway-core:11.8.2")
    implementation("org.flywaydb:flyway-database-postgresql:11.8.2")

    // Suporte a JSON usando Jackson
    implementation("io.javalin:javalin-rendering:7.0.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.0")

    // OpenAPI/Swagger: @OpenApi-annotated routes get compiled (no reflection)
    // into a spec served at /openapi, with Swagger UI at /swagger. Pinned to
    // the same version as io.javalin:javalin above - the plugins call
    // Javalin's internal router API directly, so a newer plugin version than
    // the Javalin version in use fails at startup with a NoSuchMethodError.
    implementation("io.javalin.community.openapi:javalin-openapi-plugin:7.0.0")
    implementation("io.javalin.community.openapi:javalin-swagger-plugin:7.0.0")
    annotationProcessor("io.javalin.community.openapi:openapi-annotation-processor:7.0.0")

    // Logs
    implementation("org.slf4j:slf4j-simple:2.0.13")
    
    // Testes
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.18.0")
}

application {
    mainClass.set("com.washflow.main.Main")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

sourceSets {
    main {
        java.srcDirs("src")
        java.exclude("test/**")
    }
    test {
        java.srcDirs("src/test")
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.19.2")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

sonar {
    properties {
        property("sonar.projectKey", "mayconaraujosantos_washflow-app")
        property("sonar.organization", "mayconaraujosantos")
        property("sonar.host.url", "https://sonarcloud.io")
        // Both the backend ("src") and the frontend ("web/src") are analyzed
        // as one SonarCloud project - it's a single deployable app, and
        // SonarCloud's JS/TS analyzer picks up .ts/.tsx files automatically.
        property("sonar.sources", "src,web/src")
        property("sonar.tests", "src/test")
        property("sonar.exclusions", "web/dist/**,web/node_modules/**,web/coverage/**")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
    }
}

val frontendDir = file("web")
val frontendDistDir = file("${frontendDir}/dist")

tasks.register<Delete>("cleanFrontend") {
    delete(frontendDistDir)
}

tasks.register<Exec>("bunInstall") {
    dependsOn("cleanFrontend")
    workingDir(frontendDir)
    commandLine("bun", "install", "--frozen-lockfile")
}

tasks.register<Exec>("bunBuild") {
    dependsOn("bunInstall")
    workingDir(frontendDir)
    commandLine("bun", "run", "build")
}

tasks.processResources {
    dependsOn("bunBuild")
    from(frontendDistDir) {
        into("static")
    }
}
