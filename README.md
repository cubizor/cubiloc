# Cubiloc

Kotlin i18n library with [Cubicolor](https://github.com/cubizor/Cubicolor) semantic colors and [Kyori Adventure](https://docs.advntr.dev/) component support.

YAML messages as the single source of truth, compile-time key safety via auto-generated constants, ThreadLocal context system, and per-user theme switching.

## Installation

```kotlin
repositories {
    mavenCentral()
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

## Documentation

See the [Wiki](https://github.com/cubizor/cubiloc/wiki) for full documentation.

## Dependencies

- [Cubicolor](https://github.com/cubizor/Cubicolor) — Semantic color system
- [Kyori Adventure](https://docs.advntr.dev/) — MiniMessage component rendering
- [okaeri-placeholders](https://github.com/OkaeriPoland/okaeri-placeholders) — Placeholder resolution
- [SnakeYAML](https://github.com/snakeyaml/snakeyaml) — YAML parsing

## License

MIT License — see [LICENSE](LICENSE)
