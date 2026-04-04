import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension

buildscript {
    repositories {
        gradlePluginPortal()
        maven("https://repo.slne.dev/repository/maven-public/") { name = "maven-public" }
    }
    dependencies {
        classpath("dev.slne.surf:surf-api-gradle-plugin:26+")
        classpath("dev.slne.surf.microservice:surf-microservice-gradle-plugin:26+")
    }
}

allprojects {
    group = "dev.slne.surf.stats"
    version = findProperty("version") as String
}

subprojects {
    afterEvaluate {
        extensions.findByType<KotlinJvmExtension>()?.apply {
            compilerOptions {
                optIn.add("dev.slne.surf.stats.api.utils.InternalStatsApi")
            }
        }
    }
}