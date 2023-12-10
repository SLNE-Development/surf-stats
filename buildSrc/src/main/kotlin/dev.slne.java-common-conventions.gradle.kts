import gradle.kotlin.dsl.accessors._d803cb14c6fe14adae2bba009cf4d623.java

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
	maven("https://packages.slne.dev/maven/p/surf/maven") {
		name = "space-maven"
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
