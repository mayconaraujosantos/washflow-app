plugins {
    java
    application
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

    // Suporte a JSON usando Jackson
    implementation("io.javalin:javalin-rendering:7.0.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    
    // Logs
    implementation("org.slf4j:slf4j-simple:2.0.13")
    
    // Testes
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
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
