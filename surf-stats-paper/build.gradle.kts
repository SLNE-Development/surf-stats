import dev.slne.surf.surfapi.gradle.util.registerRequired
import dev.slne.surf.surfapi.gradle.util.registerSoft
import net.minecrell.pluginyml.paper.PaperPluginDescription
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("dev.slne.surf.surfapi.gradle.paper-plugin")
    `maven-publish`
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
                username = providers.environmentVariable("SLNE_RELEASES_REPO_USERNAME").orNull
                password = providers.environmentVariable("SLNE_RELEASES_REPO_PASSWORD").orNull
            }
        }
    }

    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}
