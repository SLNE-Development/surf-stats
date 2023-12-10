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
	withSourcesJar()
	withJavadocJar()
}

tasks {
	compileJava {
		options.encoding = Charsets.UTF_8.name()
		options.compilerArgs.add("-parameters")
	}
	javadoc {
		options.encoding = Charsets.UTF_8.name()
	}
}
