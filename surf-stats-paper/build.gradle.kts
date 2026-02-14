plugins {
    id("dev.slne.surf.surfapi.gradle.paper-plugin")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.stats.paper.SurfStatsPlugin")
    authors.add("SLNE Dev Team")
    generateLibraryLoader(false)
    foliaSupported(true)
    withCorePaper()
}

dependencies {
    api(project(":surf-stats-core"))
}

tasks.processResources {
    expand("version" to project.version)
}
