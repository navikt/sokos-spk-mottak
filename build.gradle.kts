import kotlinx.kover.gradle.plugin.dsl.tasks.KoverReport

import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    id("com.expediagroup.graphql") version "10.0.1"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.8"

    application
}

group = "no.nav.sokos"

repositories {
    mavenCentral()

    val githubToken = System.getenv("GITHUB_TOKEN")
    if (githubToken.isNullOrEmpty()) {
        maven {
            name = "external-mirror-github-navikt"
            url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        }
    } else {
        maven {
            name = "github-package-registry-navikt"
            url = uri("https://maven.pkg.github.com/navikt/maven-release")
            credentials {
                username = "token"
                password = githubToken
            }
        }
    }
}

val ktorVersion = "3.4.3"
val jschVersion = "2.28.3"
val logbackVersion = "1.5.34"
val logstashVersion = "9.0"
val micrometerVersion = "1.17.0"
val kotlinLoggingVersion = "3.0.5"
val janinoVersion = "3.1.12"
val natpryceVersion = "1.6.10.0"
val kotestVersion = "6.2.1"
val wiremockVersion = "3.13.2"
val kotlinxSerializationVersion = "1.11.0"
val kotlinxDatetimeVersion = "0.8.0-0.6.x-compat"
val mockOAuth2ServerVersion = "4.0.1"
val mockkVersion = "1.14.11"
val hikariVersion = "7.1.0"
val db2JccVersion = "12.1.4.0"
val kotliqueryVersion = "2.0.8"
val testcontainersVersion = "1.21.4"
val h2Version = "2.4.240"
val flywayVersion = "12.9.0"
val postgresVersion = "42.7.11"
val dbSchedulerVersion = "16.12.0"
val vaultVersion = "1.3.10"
val tjenestespesifikasjonVersion = "1.0_20260311085905_5544925"
val ibmmqVersion = "10.0.0.0"
val activemqVersion = "2.54.0"
val graphqlClientVersion = "10.0.1"
val jaxbVersion = "4.0.9"
val opentelemetryVersion = "2.29.0-alpha"

dependencies {

    // Ktor server
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    // Ktor client
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-apache5-jvm:$ktorVersion")

    // Security
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")

    // Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$kotlinxSerializationVersion")
    implementation("org.glassfish.jaxb:jaxb-runtime:$jaxbVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")

    // Monitorering
    implementation("io.ktor:ktor-server-metrics-micrometer-jvm:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")
    runtimeOnly("org.codehaus.janino:janino:$janinoVersion")
    runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    // SFTP
    implementation("com.github.mwiede:jsch:$jschVersion")

    // Database
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("com.ibm.db2:jcc:$db2JccVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("no.nav:kotliquery:$kotliqueryVersion")

    implementation("org.flywaydb:flyway-core:$flywayVersion")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    // Config
    implementation("com.natpryce:konfig:$natpryceVersion")
    implementation("no.nav:vault-jdbc:$vaultVersion")

    // Scheduler
    implementation("com.github.kagkarlsson:db-scheduler:$dbSchedulerVersion")

    // MQ
    implementation("com.ibm.mq:com.ibm.mq.jakarta.client:$ibmmqVersion")

    // Oppdrag
    implementation("no.nav.sokos.tjenestespesifikasjoner:nav-virksomhet-oppdragsbehandling-v1-meldingsdefinisjon:$tjenestespesifikasjonVersion")
    implementation("no.nav.sokos.tjenestespesifikasjoner:avstemming-v1-tjenestespesifikasjon:$tjenestespesifikasjonVersion")

    // GraphQL
    implementation("com.expediagroup:graphql-kotlin-ktor-client:$graphqlClientVersion") {
        exclude("com.expediagroup:graphql-kotlin-client-jackson")
    }

    // Opentelemetry
    implementation("io.opentelemetry.instrumentation:opentelemetry-ktor-3.0:$opentelemetryVersion")

    // Test
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-now:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("com.h2database:h2:$h2Version")
    testImplementation("org.apache.activemq:artemis-jakarta-server:$activemqVersion")
    testImplementation("org.wiremock:wiremock:$wiremockVersion")
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-core") {
                useVersion("2.21.1")
                because("jackson-core: Number Length Constraint Bypass in Async Parser Leads to Potential DoS Condition. Affected version >= 2.19.0, < 2.21.1")
            }
            if (requested.group == "io.opentelemetry" && requested.name == "opentelemetry-api") {
                useVersion("1.62.0")
                because("OpenTelemetry Java SDK has Unbounded Memory Allocation in W3C Baggage Propagation. Affected version <= 1.61.0")
            }
            if (requested.group == "io.netty") {
                useVersion("4.2.15.Final")
                because("Netty CVE remediation: CVE-2026-45536, CVE-2026-44249, CVE-2026-45416 and CVE-2026-48043")
            }
            if (requested.group == "org.bouncycastle" && requested.name == "bcprov-jdk18on") {
                useVersion("1.84")
                because("Bouncy Castle Has Covert Timing Channel Vulnerability. Affected version >= 1.71, < 1.84")
            }
            if (requested.group == "org.bouncycastle" && requested.name == "bcpkix-jdk18on") {
                useVersion("1.84")
                because("CVE-2026-5588: >= 1.71, < 1.84")
            }
        }
    }
}

application {
    mainClass.set("no.nav.sokos.spk.mottak.ApplicationKt")
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
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks {
    named("runKtlintCheckOverMainSourceSet").configure {
        dependsOn("graphqlGenerateClient")
    }

    named("runKtlintFormatOverMainSourceSet").configure {
        dependsOn("graphqlGenerateClient")
    }

    withType<KotlinCompile>().configureEach {
        dependsOn("ktlintFormat")
        dependsOn("graphqlGenerateClient")
    }

    withType<KoverReport>().configureEach {
        kover {
            reports {
                filters {
                    excludes {
                        // exclusion rules - classes to exclude from report
                        classes("no.nav.pdl.*")
                    }
                }
            }
        }
    }

    withType<GraphQLGenerateClientTask>().configureEach {
        packageName.set("no.nav.pdl")
        schemaFile.set(file("$projectDir/src/main/resources/graphql/schema.graphql"))
        queryFileDirectory.set(file("$projectDir/src/main/resources/graphql"))
        serializer = GraphQLSerializer.KOTLINX
    }

    withType<Test>().configureEach {
        jvmArgs("--add-opens", "java.base/java.time=ALL-UNNAMED")
        useJUnitPlatform()

        testLogging {
            showExceptions = true
            showStackTraces = true
            exceptionFormat = FULL
            events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }

        reports.forEach { report -> report.required.value(false) }

        finalizedBy(koverHtmlReport)
    }

    ("build") {
        dependsOn("copyPreCommitHook")
    }

    register<Copy>("copyPreCommitHook") {
        from(".scripts/pre-commit")
        into(".git/hooks")
        filePermissions {
            user {
                execute = true
            }
        }
        doFirst {
            println("Installing git hooks...")
        }
        doLast {
            println("Git hooks installed successfully.")
        }
        description = "Copy pre-commit hook to .git/hooks"
        group = "git hooks"
        outputs.upToDateWhen { false }
    }
}
