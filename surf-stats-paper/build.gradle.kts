plugins {
    id("dev.slne.surf.surfapi.gradle.paper-raw")
}

dependencies {
    implementation(project(":surf-stats-core"))
}

tasks.processResources {
    expand("version" to project.version)
}