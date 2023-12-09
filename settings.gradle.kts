plugins {
	// Apply the foojay-resolver plugin to allow automatic download of JDKs
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "surf-stats"
include("surf-stats-api")
include("surf-stats-core")
include("surf-stats-bukkit")
