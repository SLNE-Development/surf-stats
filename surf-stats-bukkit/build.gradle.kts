plugins {
	id("dev.slne.java-common-conventions")
	id("dev.slne.java-shadow-conventions")
	alias(libs.plugins.run.paper.plugin)
}

dependencies {
	implementation(project(":surf-stats-core"))

	compileOnly(libs.paper.api)
}

tasks {
	runServer {
		// Configure the Minecraft version for our task.
		// This is the only required configuration besides applying the plugin.
		// Your plugin's jar (or shadowJar if present) will be used automatically.
		minecraftVersion(libs.versions.minecraft.server.version.get())
	}
}