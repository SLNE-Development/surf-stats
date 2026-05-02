import dev.slne.surf.surfapi.gradle.util.registerRequired
import dev.slne.surf.surfapi.gradle.util.registerSoft
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("dev.slne.surf.surfapi.gradle.paper-plugin")
}

group = "dev.slne.surf"

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

publishing {
    repositories {
        maven("https://reposilite.slne.dev/releases/") {
            name = "slne-repository-releases"
            credentials {
                username = System.getenv("SLNE_RELEASES_REPO_USERNAME")
                password = System.getenv("SLNE_RELEASES_REPO_PASSWORD")
            }
        }
    }

    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}
