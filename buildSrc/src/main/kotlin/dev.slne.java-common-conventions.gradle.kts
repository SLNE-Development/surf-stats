plugins {
	// Apply the java Plugin to add support for Java.
	java

	id("net.linguica.maven-settings")
}

repositories {
	mavenCentral()
	mavenLocal()
	gradlePluginPortal()

	maven("https://repo.papermc.io/repository/maven-public/")

	maven {
		name = "space-maven"
		url = uri("https://packages.slne.dev/maven/p/surf/maven")
	}
}

dependencies {

}

// Apply a specific Java toolchain to ease working on different environments.
java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}
