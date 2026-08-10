# Cubiloc

Kotlin i18n library with [Cubicolor](https://github.com/cubizor/Cubicolor) semantic colors and [Kyori Adventure](https://docs.advntr.dev/) component support.

YAML messages as the single source of truth, compile-time key safety via auto-generated constants, ThreadLocal context system, and per-user theme switching.

## Installation

```kotlin
repositories {
    mavenCentral()
    // okaeri-placeholders is a transitive dependency and is not on Maven Central.
    maven("https://repo.okaeri.cloud/releases")
}

dependencies {
    implementation("net.cubizor.cubiloc:cubiloc:2.2.0")
}
```

Releases are also mirrored to GitHub Packages. See [PUBLISHING.md](PUBLISHING.md) for the release
process.

## Quick Start

**1. Define messages in YAML** (`messages/en_US.yml`):

```yaml
welcome: "<success>Welcome {player}!</success>"
errors:
  notFound: "<error>Item '{item}' not found!</error>"
```

**2. Use in code:**

```kotlin
val i18n = I18n(Locale.US)
i18n.loadMessages("messages", dataFolder)
i18n.loadColorSchemeFromClasspath("dark", "themes/dark.json")
i18n.defaultScheme("dark")

// Use with context
i18n.context(player).use {
    val msg = i18n.message(M.WELCOME)
        .with("player" to player.name)
        .component()
    player.sendMessage(msg)
}
```

## Custom Tags & Default Style

Tags can also be supplied from outside the library, which lets one plugin own the palette for a
whole network instead of every plugin shipping its own theme files:

```kotlin
i18n.registerTagResolvers { receiver -> networkPalette.resolverFor(receiver) }
i18n.registerDefaultStyle { Style.style(NamedTextColor.WHITE) }
```

`registerTagResolvers` may be called more than once; sources are tried in registration order.
They rank below `TagResolver.standard()` and any per-call resolver, but above the color scheme /
message theme resolver, so a source can override tags such as `<primary>`.

`registerDefaultStyle` keeps a single source. Its style is merged into the root of every rendered
component with `IF_ABSENT_ON_TARGET`, so it only fills in what the message itself did not set.

## Documentation

See the [Wiki](https://github.com/cubizor/cubiloc/wiki) for full documentation.

## Dependencies

- [Cubicolor](https://github.com/cubizor/Cubicolor) — Semantic color system
- [Kyori Adventure](https://docs.advntr.dev/) — MiniMessage component rendering
- [okaeri-placeholders](https://github.com/OkaeriPoland/okaeri-placeholders) — Placeholder resolution
- [SnakeYAML](https://github.com/snakeyaml/snakeyaml) — YAML parsing

## License

MIT License — see [LICENSE](LICENSE)
