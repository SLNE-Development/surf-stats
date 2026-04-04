rootProject.name = "surf-stats"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.slne.dev/repository/maven-public/") { name = "maven-public" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.slne.surf.api.gradle.settings") version "26+"
}

include("surf-stats-api")
include("surf-stats-core:surf-stats-core-common")
include("surf-stats-core:surf-stats-core-client")

include("surf-stats-paper")
include("surf-stats-microservice")