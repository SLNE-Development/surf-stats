import dev.slne.surf.surfapi.gradle.util.registerRequired
import dev.slne.surf.surfapi.gradle.util.registerSoft
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("dev.slne.surf.surfapi.gradle.paper-plugin")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.stats.paper.SurfStatsPlugin")
    authors.add("SLNE Dev Team")
    generateLibraryLoader(false)
    foliaSupported(true)

    serverDependencies {
        registerSoft(
            "surf-clan-paper",
            joinClassPath = true,
            loadOrder = PaperPluginDescription.RelativeLoadOrder.BEFORE
        )

        registerRequired("surf-rabbitmq-paper")
    }

    withCorePaper()
}

dependencies {
    api(projects.surfStatsCore.surfStatsCoreClient)
}