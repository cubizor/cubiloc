plugins {
    id("cubiloc.library-conventions")
}

version = project.findProperty("version") as String? ?: "0.0.1-SNAPSHOT"

repositories {
    // Cubicolor is on Maven Central since 1.6.0, so no credentialed repository is needed.
    mavenCentral()
    maven("https://repo.okaeri.cloud/releases")
}

dependencies {
    // Core
    implementation(libs.snakeyaml)
    implementation(libs.okaeri.placeholders)

    // Adventure
    implementation(libs.bundles.adventure)

    // Cubicolor
    implementation(libs.bundles.cubicolor)

    // Testing
    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junit.launcher)
    testImplementation(libs.assertj)
}

// Message key generation
tasks.register<GenerateMessageKeysTask>("generateMessageKeys") {
    yamlFile.set(file("src/main/resources/messages/en_US.yml"))
    outputDir.set(layout.buildDirectory.dir("generated/sources/messageKeys"))
    packageName.set("net.cubizor.cubiloc")
    objectName.set("M")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir(tasks.named("generateMessageKeys").map {
            layout.buildDirectory.dir("generated/sources/messageKeys")
        })
    }
}

tasks.named("compileKotlin") {
    dependsOn("generateMessageKeys")
}

// Entry point for semantic-release (see .releaserc.json). Plain `publish` only stages the
// Central deployment; `publishAndReleaseToMavenCentral` is what actually releases it.
tasks.register("publishRelease") {
    group = "publishing"
    description = "Publishes to Maven Central and GitHub Packages."
    dependsOn("publishAndReleaseToMavenCentral")
    dependsOn("publishAllPublicationsToGitHubPackagesRepository")
}
