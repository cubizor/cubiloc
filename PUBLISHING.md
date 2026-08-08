# Publishing

Releases are fully automated. A push to `main` runs `.github/workflows/workflow.yml`, which runs the
tests and then semantic-release. When the commits since the last tag warrant a release,
semantic-release bumps `gradle.properties`, writes the changelog, tags, and invokes the Gradle
`publishRelease` task.

`publishRelease` publishes to two places:

- **Maven Central** (`net.cubizor.cubiloc:cubiloc`) — the canonical, anonymously readable location.
- **GitHub Packages** — kept in sync, but requires a token even for public packages.

## Why not plain `publish`

Gradle's `publish` only *stages* a Central deployment; `publishAndReleaseToMavenCentral` is what
actually releases it. `publishRelease` (defined in the root `build.gradle.kts`) depends on both that
task and the GitHub Packages task.

## Central requirements

Maven Central rejects a deployment unless it has all of:

- sources and javadoc jars — configured via `KotlinJvm(JavadocJar.Javadoc(), sourcesJar = true)`
- a PGP signature for every artifact — `signAllPublications()`
- POM `name`, `description`, `url`, `licenses`, `developers`, `scm`

The publish plugin produces the sources and javadoc jars itself, so the convention plugin must *not*
also call `withSourcesJar()` / `withJavadocJar()` — that yields duplicate artifacts.

Because the project has no Java sources, the `javadoc` task has nothing to process and the javadoc
jar ships empty. Central only checks that the jar is present, so this is valid. Switching to
`JavadocJar.Dokka("dokkaHtml")` would produce real Kotlin API docs if that is ever wanted.

## Plugin / Kotlin version coupling

`com.vanniktech.maven.publish` is pinned to **0.35.0** in `gradle/libs.versions.toml`. Version
0.36.0 and newer require Kotlin Gradle Plugin 2.2.0; this project is on Kotlin 2.1.20. Bump the two
together, never one alone.

## CI secrets

| Secret | Maps to | Purpose |
| --- | --- | --- |
| `MVN_CENTRAL_USERNAME` | `ORG_GRADLE_PROJECT_mavenCentralUsername` | Central Portal user token |
| `MVN_CENTRAL_PASSWORD` | `ORG_GRADLE_PROJECT_mavenCentralPassword` | Central Portal user token |
| `GPG_PRIVATE_KEY` | `ORG_GRADLE_PROJECT_signingInMemoryKey` | ASCII-armored private key |
| `GPG_PASSPHRASE` | `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword` | Key passphrase |

The secret names are arbitrary; the `ORG_GRADLE_PROJECT_*` names are not — that prefix is how Gradle
turns an environment variable into a project property, and the property names are fixed by the
publish plugin.

Portal tokens are generated at <https://central.sonatype.com> under Account → Generate User Token.

## Signing key

RSA 4096, no expiry, published to `keys.openpgp.org` and `keyserver.ubuntu.com`. It is shared by
every Cubizor repository, not specific to cubiloc — Central binds a key to nothing, it only checks
that signatures verify against a key published on a keyserver.

```
Cubizor (Maven Central signing key) <vulzen@vulzen.dev>
8E5D 6182 7D96 1D9D 818D  BCC5 38C4 B9CE E555 C362
```

The private key, passphrase, and revocation certificate are **not** in this repo. If they are lost,
generate a new key, publish it to both keyservers, and update the two GPG secrets — Central does not
care that the key changed, only that signatures verify against a published key.

## Publishing locally

```bash
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(cat private-key.asc)"
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword='...'
./gradlew publishToMavenLocal
```

This exercises signing, sources, and javadoc without touching Central.

## Republishing an existing version

The workflow also has a `workflow_dispatch` trigger that runs `publishAndReleaseToMavenCentral` for
whatever version `gradle.properties` currently holds, bypassing semantic-release. Use it to get an
already-tagged version onto Central — for example after this repository was first wired up, since
`build:` and `docs:` commits do not trigger a release.

It deliberately skips GitHub Packages: that registry rejects re-uploading a version it already has,
and a manual run is by definition republishing an existing version.
