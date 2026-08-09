import dev.slne.surf.microservice.gradle.plugin.rabbit.RabbitModule

plugins {
    id("dev.slne.surf.api.gradle.core")
    id("dev.slne.surf.microservice")
}

surfCoreApi {
    withCoreCommon()
}

surfMicroservice {
    withRabbitModule(RabbitModule.CLIENT_API)
}

dependencies {
    api(projects.surfStatsCore.surfStatsCoreCommon)
    compileOnlyApi(libs.surf.clan.api)
    implementation(libs.kotlinx.serialization.json)

    // The `dev.slne.surf.api.gradle.core` convention plugin puts surf-api-core on
    // `compileOnly` only, so it never reaches the test classpath. Tests need it for
    // `key()` and, at runtime, for `logger()`. Same coordinate and version spec the
    // convention plugin itself uses.
    testImplementation("dev.slne.surf.api:surf-api-core:+")

    testImplementation("com.google.flogger:flogger:0.9")
    testRuntimeOnly("com.google.flogger:flogger-system-backend:0.9")

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("net.kyori:adventure-api:4.26.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}