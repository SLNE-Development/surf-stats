plugins {
	id("dev.slne.java-common-conventions")
	id("dev.slne.java-shadow-conventions")
	id("xyz.jpenilla.run-paper") version "2.2.2"
}

dependencies {
	implementation(project(":surf-stats-core"))

	compileOnly("io.papermc.paper:paper-api:1.20.2-R0.1-SNAPSHOT")
}

tasks {
	runServer {
		// Configure the Minecraft version for our task.
		// This is the only required configuration besides applying the plugin.
		// Your plugin's jar (or shadowJar if present) will be used automatically.
		minecraftVersion("1.20.2")
	}
}