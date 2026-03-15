plugins {
    id("dev.slne.surf.surfapi.gradle.core")
}

surfCoreApi {
    withSurfDatabaseR2dbc("1.3.0", "libs.database")
    withCoreCommon()
}

dependencies {
    api(projects.surfStatsApi)
    compileOnlyApi(libs.surf.clan.api)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("net.kyori:adventure-api:4.26.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}