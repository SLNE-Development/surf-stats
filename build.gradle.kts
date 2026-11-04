buildscript {
    repositories {
        gradlePluginPortal()
        maven("https://reposilite.slne.dev/releases")
    }
    dependencies {
        classpath("dev.slne.surf.api:surf-api-gradle-plugin:+")
        classpath("dev.slne.surf.microservice:surf-microservice-gradle-plugin:2.+")
    }
}

allprojects {
    group = "dev.slne.surf.stats"
    version = findProperty("version") as String
}