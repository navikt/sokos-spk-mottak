pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            name = "github-package-registry-navikt-ktlint-gradle"
            url = uri("https://maven.pkg.github.com/navikt/ktlint-gradle")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull
                password =
                    System.getenv("READER_TOKEN")
                        ?: System.getenv("GITHUB_TOKEN")
                        ?: providers.gradleProperty("gpr.key").orNull
            }
        }
    }
}

rootProject.name = "sokos-spk-mottak"
