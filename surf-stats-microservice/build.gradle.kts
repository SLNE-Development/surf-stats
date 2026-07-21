import dev.slne.surf.microservice.gradle.plugin.rabbit.RabbitModule

plugins {
    id("dev.slne.surf.api.gradle.standalone")
    id("dev.slne.surf.microservice")
}

surfStandaloneApi {
    withSurfDatabaseR2dbc("2.3.1", "dev.slne.surf.stats.microservice.libs.database")
}

surfMicroservice {
    withMicroserviceApi()
    withRabbitModule(RabbitModule.SERVER_API, true)
}

dependencies {
    api(projects.surfStatsCore.surfStatsCoreCommon)
}