import org.asciidoctor.gradle.AsciidoctorTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("plugin.jpa") version "1.3.61"
    id("org.springframework.boot") version "2.2.2.RELEASE"
    id("io.spring.dependency-management") version "1.0.8.RELEASE"
    kotlin("jvm") version "1.3.61"
    kotlin("plugin.spring") version "1.3.61"
    kotlin("plugin.allopen") version "1.3.61"
    kotlin("kapt") version "1.3.61"
    id("org.asciidoctor.convert") version "1.5.9.2"
}

group = "snc.openchargingnetwork.node"
version = "1.0.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

val snippetsDir = "build/generated-snippets"

val developmentOnly by configurations.creating
configurations {
    runtimeClasspath {
        extendsFrom(developmentOnly)
    }
}

repositories {
    jcenter()
}

dependencies {
    implementation("shareandcharge.openchargingnetwork:notary:0.4.0-beta1")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("khttp:khttp:1.0.0")
    implementation("org.web3j:core:4.5.5")
    implementation("org.postgresql:postgresql:42.2.6")
    runtimeOnly("com.h2database:h2")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    asciidoctor("org.springframework.restdocs:spring-restdocs-asciidoctor:2.0.3.RELEASE")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit")
        exclude(module = "junit-vintage-engine")
        exclude(module = "mockito-core")
        exclude(module = "android-json")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("com.ninja-squad:springmockk:1.1.2")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc:2.0.3.RELEASE")
    testImplementation("io.javalin:javalin:3.7.0")
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.Embeddable")
    annotation("javax.persistence.MappedSuperClass")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

val test: Test by tasks
test.apply {
    useJUnitPlatform()
    outputs.dir(snippetsDir)
    exclude("**/integration/**")
}

tasks.register<Exec>("ganache") {
    commandLine(listOf("/usr/bin/env", "npm", "install", "-g", "ganache-cli"))
    commandLine(listOf(
            "/usr/bin/env",
            "ganache-cli",
            "-m=candy maple cake sugar pudding cream honey rich smooth crumble sweet treat",
            "--port=8544",
            "--accounts=20",
            "--networkId=9",
            "--gasLimit=10000000"))
}

tasks.register<Test>("integrationTest") {
    useJUnitPlatform()
    include("**/integration/**")
}


val asciidoctor by tasks.getting(AsciidoctorTask::class) {
    inputs.dir(snippetsDir)
    dependsOn(test)
}

tasks {
    "bootJar"(BootJar::class) {
        dependsOn(asciidoctor)
        from("${asciidoctor.get().outputDir}/html5") {
            into("static/docs")
        }
    }
}

(tasks.getByName("processResources") as ProcessResources).apply {
    val profile: String by project
    include("**/application.$profile.properties")
    rename {
        "application.properties"
    }
}