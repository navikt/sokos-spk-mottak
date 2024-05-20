import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    jacoco
}

group = "no.nav.sokos"

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

val ktorVersion = "2.3.11"
val jschVersion = "0.2.17"
val logbackVersion = "1.5.6"
val logstashVersion = "7.4"
val jacksonVersion = "2.15.3"
val prometheusVersion = "1.13.0"
val kotlinLoggingVersion = "3.0.5"
val janionVersion = "3.1.12"
val natpryceVersion = "1.6.10.0"
val kotestVersion = "5.9.0"
val kotlinxSerializationVersion = "1.6.3"
val mockOAuth2ServerVersion = "2.1.5"
val mockkVersion = "1.13.11"
val hikariVersion = "5.1.0"
val db2JccVersion = "11.5.9.0"
val kotliqueryVersion = "1.9.0"
val testcontainersVersion = "1.19.8"
val h2Version = "2.2.224"
val kotestWiremockVersion = "3.0.1"
val flywayVersion = "10.13.0"
val postgresVersion = "42.7.3"
val dbSchedulerVersion = "14.0.0"
val vaultVersion = "1.3.10"

dependencies {

    // Ktor server
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")

    // Ktor client
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-apache-jvm:$ktorVersion")

    // Security
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")

    // Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$kotlinxSerializationVersion")

    // Monitorering
    implementation("io.ktor:ktor-server-metrics-micrometer-jvm:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$prometheusVersion")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")
    runtimeOnly("org.codehaus.janino:janino:$janionVersion")
    runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    // SFTP
    implementation("com.github.mwiede:jsch:$jschVersion")

    // Database
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("com.ibm.db2:jcc:$db2JccVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")

    // Config
    implementation("com.natpryce:konfig:$natpryceVersion")
    implementation("no.nav:vault-jdbc:$vaultVersion")

    // Scheduler
    implementation("com.github.kagkarlsson:db-scheduler:$dbSchedulerVersion")

    // Test
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("com.h2database:h2:$h2Version")
    testImplementation("io.kotest.extensions:kotest-extensions-wiremock:$kotestWiremockVersion")
}

sourceSets {
    main {
        java {
            srcDirs("${layout.buildDirectory.get()}/generated/src/main/kotlin")
        }
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {

    withType<KotlinCompile>().configureEach {
        dependsOn("ktlintFormat")
    }

    withType<ShadowJar>().configureEach {
        enabled = true
        archiveFileName.set("app.jar")
        manifest {
            attributes["Main-Class"] = "no.nav.sokos.spk.mottak.ApplicationKt"
            attributes["Class-Path"] = "/var/run/secrets/db2license/db2jcc_license_cisuz.jar"
        }
        finalizedBy(jacocoTestReport)
    }

    ("jar") {
        enabled = false
    }

    withType<JacocoReport>().configureEach {
        dependsOn(test)
        reports {
            html.required.set(true)
        }
    }

    withType<Test>().configureEach {
        useJUnitPlatform()

        testLogging {
            showExceptions = true
            showStackTraces = true
            exceptionFormat = FULL
            events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }

        reports.forEach { report -> report.required.value(false) }
    }

    withType<Wrapper> {
        gradleVersion = "8.7"
    }
}
