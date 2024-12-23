import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("plugin.jpa") version "1.3.72"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.3.72"
    kotlin("plugin.allopen") version "1.3.72"
    kotlin("kapt") version "1.3.72"
    id("org.springframework.boot") version "2.2.6.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

group = "snc.openchargingnetwork.node"
version = "1.2.0-rc2"
java.sourceCompatibility = JavaVersion.VERSION_1_8

val snippetsDir = "build/generated-snippets"

val developmentOnly: Configuration by configurations.creating
configurations {
    runtimeClasspath {
        extendsFrom(developmentOnly)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter() // Use cautiously as it's deprecated
    gradlePluginPortal() // Added for plugin dependencies
    maven("https://jitpack.io") // Add this line
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.danilopianini:khttp:1.2.2")
    implementation("org.web3j:core:4.5.5")
    implementation("org.postgresql:postgresql:42.2.12")
    implementation("com.jayway.jsonpath:json-path:2.9.0")
    implementation("com.aayushatharva.brotli4j:brotli4j:1.7.0")
    runtimeOnly("com.h2database:h2")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit")
        exclude(module = "junit-vintage-engine")
        exclude(module = "mockito-core")
        exclude(module = "android-json")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("com.ninja-squad:springmockk:2.0.0")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc:2.0.4.RELEASE")
    testImplementation("io.javalin:javalin:3.8.0")
    testImplementation("org.awaitility:awaitility-kotlin:4.0.2")
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.Embeddable")
    annotation("javax.persistence.MappedSuperclass")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

val test: Test by tasks
test.apply {
    description = "Runs OCN Node unit and integration tests (note: integration tests depend on ganache-cli running)."
    dependsOn("unitTest", "integrationTest")
    outputs.dir(snippetsDir)
}

tasks.register<Test>("unitTest") {
    group = "verification"
    description = "Runs OCN Node unit tests."
    useJUnitPlatform()
    exclude("**/integration/**")
}

tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Runs OCN Node integration tests (note: depends on a ganache-cli process running)."
    useJUnitPlatform()
    include("**/integration/**")
}

tasks.register<Exec>("ganache") {
    group = "help"
    description = "Runs a ganache-cli instance for integration testing."
    commandLine(listOf(
        "/usr/bin/env",
        "ganache-cli",
        "-m=candy maple cake sugar pudding cream honey rich smooth crumble sweet treat",
        "--port=8544",
        "--accounts=20",
        "--networkId=9",
        "--gasLimit=10000000"
    ))
}

(tasks.getByName("processResources") as ProcessResources).apply {
    val profile: String by project
    println("Building using **/application.$profile.properties")
    include("**/application.$profile.properties")
    rename { "application.properties" }
}

tasks.register<Tar>("archive") {
    group = "build"
    description = "Assembles a tar archive with GZIP compression for distribution."
    archiveFileName.set("ocn-node-${project.version}.tar.gz")
    destinationDirectory.set(file("$buildDir/dist"))
    compression = Compression.GZIP

    into ("/ocn-node-${project.version}") {
        from(
            "$buildDir/libs/ocn-node-${project.version}.jar",
            "src/main/resources/application.dev.properties",
            "src/main/resources/application.prod.properties",
            "infra/ocn-node.service",
            "README.md",
            "CONFIGURATION.md",
            "CHANGELOG.md"
        )
    }
}