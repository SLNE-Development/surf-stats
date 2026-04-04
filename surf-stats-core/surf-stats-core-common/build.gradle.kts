import dev.slne.surf.microservice.gradle.plugin.rabbit.RabbitModule

plugins {
    id("dev.slne.surf.api.gradle.core")
    id("dev.slne.surf.microservice")
}

surfMicroservice {
    withRabbitModule(RabbitModule.COMMON_API)
}

dependencies {
    api(projects.surfStatsApi)

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