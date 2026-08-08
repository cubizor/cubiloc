import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-library`
    `maven-publish`
    id("com.vanniktech.maven.publish")
}

group = "net.cubizor.cubiloc"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    // Sources/javadoc jars come from the publish plugin; adding them here would duplicate them.
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        events("passed", "skipped", "failed")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

// ============================================================
// Publishing
// ============================================================

mavenPublishing {
    // Central rejects deployments without sources + javadoc jars.
    configure(KotlinJvm(javadocJar = JavadocJar.Javadoc(), sourcesJar = true))

    // Released without a manual portal click.
    publishToMavenCentral(automaticRelease = true)

    // Central rejects unsigned artifacts. Credentials come from
    // ORG_GRADLE_PROJECT_signingInMemoryKey / ...KeyPassword in CI.
    signAllPublications()

    pom {
        name.set("Cubiloc")
        description.set("Kotlin i18n library with Cubicolor semantic colors and Adventure components")
        url.set("https://github.com/cubizor/cubiloc")
        inceptionYear.set("2025")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("cubizor")
                name.set("Cubizor Team")
                email.set("dev@cubizor.net")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/cubizor/cubiloc.git")
            developerConnection.set("scm:git:ssh://github.com/cubizor/cubiloc.git")
            url.set("https://github.com/cubizor/cubiloc")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/cubizor/cubiloc")
            credentials {
                username = findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR") ?: ""
                password = findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
}
