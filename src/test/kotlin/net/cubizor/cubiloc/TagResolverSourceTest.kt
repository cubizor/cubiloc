package net.cubizor.cubiloc

import net.cubizor.cubiloc.color.ColorSchemeTagResolver
import net.cubizor.cubiloc.message.SingleMessageResult
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Locale

class TagResolverSourceTest {

    private lateinit var i18n: I18n
    private val player = I18nTest.TestPlayer("John", "en_US")

    @BeforeEach
    fun setUp() {
        i18n = I18n(Locale.US)
        i18n.registerLocaleProvider(I18nTest.TestPlayerLocaleProvider())
        i18n.loadMessages("messages", File("src/test/resources"))
        i18n.loadColorSchemeFromClasspath("dark", "themes/dark.json")
        i18n.loadColorSchemeFromClasspath("light", "themes/light.json")
        i18n.defaultScheme("dark")
    }

    // ==================== Resolver Precedence ====================

    @Test
    fun `registered source overrides theme resolver`() {
        val themed = i18n.context(player).use { firstColor(i18n.message("serverName").component()) }

        i18n.registerTagResolvers { stylingResolver("primary", PINK) }

        i18n.context(player).use {
            assertThat(firstColor(i18n.message("serverName").component())).isEqualTo(PINK)
        }
        assertThat(themed).isNotEqualTo(PINK)
    }

    @Test
    fun `standard tags are never overridden by a registered source`() {
        i18n.registerTagResolvers { stylingResolver("red", PINK) }

        i18n.context(player).use {
            assertThat(firstColor(rawMessage("<red>test</red>").component()))
                .isEqualTo(NamedTextColor.RED)
        }
    }

    @Test
    fun `sources are tried in registration order`() {
        i18n.registerTagResolvers { stylingResolver("primary", PINK) }
        i18n.registerTagResolvers { stylingResolver("primary", NamedTextColor.GREEN) }

        i18n.context(player).use {
            assertThat(firstColor(i18n.message("serverName").component())).isEqualTo(PINK)
        }
    }

    @Test
    fun `per-call resolver wins over a registered source`() {
        i18n.registerTagResolvers { stylingResolver("primary", PINK) }

        i18n.context(player).use {
            val component = i18n.message("serverName")
                .component(stylingResolver("primary", NamedTextColor.GREEN))
            assertThat(firstColor(component)).isEqualTo(NamedTextColor.GREEN)
        }
    }

    @Test
    fun `list messages honour registered sources`() {
        i18n.registerTagResolvers { stylingResolver("accent", PINK) }

        i18n.context(player).use {
            assertThat(firstColor(i18n.list("helpMenu").components().first())).isEqualTo(PINK)
        }
    }

    // ==================== Backwards Compatibility ====================

    @Test
    fun `rendering is unchanged when nothing is registered`() {
        // Mirrors the resolver chain as it was before sources existed.
        val legacy = MiniMessage.builder()
            .tags(TagResolver.resolver(TagResolver.standard(), ColorSchemeTagResolver.of(i18n.getColorScheme("dark")!!)))
            .postProcessor { it.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) }
            .build()

        i18n.context(player).use {
            val message = i18n.message("balance").with("amount" to 12, "currency" to "USD")
            assertThat(message.component()).isEqualTo(legacy.deserialize(message.asString()))
        }
    }

    // ==================== Default Style ====================

    @Test
    fun `default style fills in the root without overriding the message`() {
        i18n.registerDefaultStyle { Style.style(NamedTextColor.WHITE) }

        i18n.context(player).use {
            val component = rawMessage("plain <red>colored</red>").component()
            assertThat(component.color()).isEqualTo(NamedTextColor.WHITE)
            assertThat(colorOf(component, "plain ")).isEqualTo(NamedTextColor.WHITE)
            assertThat(colorOf(component, "colored")).isEqualTo(NamedTextColor.RED)
        }
    }

    @Test
    fun `default style applies shadow color and yields to an explicit one`() {
        i18n.registerDefaultStyle { Style.style().shadowColor(ShadowColor.none()).build() }

        i18n.context(player).use {
            assertThat(rawMessage("plain").component().shadowColor()).isEqualTo(ShadowColor.none())

            val explicit = ShadowColor.fromHexString("#ff0000ff")
            val component = rawMessage("<shadow:#ff0000ff>explicit</shadow>").component()
            assertThat(shadowOf(component, "explicit")).isEqualTo(explicit)
        }
    }

    @Test
    fun `default style does not touch the non-italic default`() {
        i18n.registerDefaultStyle { Style.style(NamedTextColor.WHITE, TextDecoration.ITALIC) }

        i18n.context(player).use {
            assertThat(rawMessage("plain").component().decoration(TextDecoration.ITALIC))
                .isEqualTo(TextDecoration.State.FALSE)
        }
    }

    @Test
    fun `latest registered default style wins`() {
        i18n.registerDefaultStyle { Style.style(NamedTextColor.WHITE) }
        i18n.registerDefaultStyle { Style.style(NamedTextColor.GREEN) }

        i18n.context(player).use {
            assertThat(rawMessage("plain").component().color()).isEqualTo(NamedTextColor.GREEN)
        }
    }

    // ==================== Receiver Propagation ====================

    @Test
    fun `context receiver is passed to both sources`() {
        var tagReceiver: Any? = "unset"
        var styleReceiver: Any? = "unset"
        i18n.registerTagResolvers { receiver -> tagReceiver = receiver; TagResolver.empty() }
        i18n.registerDefaultStyle { receiver -> styleReceiver = receiver; Style.empty() }

        i18n.context(player).use { i18n.message("welcome").with("player" to "John").component() }

        assertThat(tagReceiver).isSameAs(player)
        assertThat(styleReceiver).isSameAs(player)
    }

    @Test
    fun `receiver is null outside of a context`() {
        var tagReceiver: Any? = "unset"
        i18n.registerTagResolvers { receiver -> tagReceiver = receiver; TagResolver.empty() }

        i18n.message("welcome").with("player" to "John").component()

        assertThat(tagReceiver).isNull()
    }

    // ==================== Helpers ====================

    private fun rawMessage(raw: String): SingleMessageResult =
        SingleMessageResult(rawValue = raw, i18n = i18n, globalPlaceholders = i18n.placeholders)

    private fun stylingResolver(tag: String, color: TextColor): TagResolver =
        TagResolver.resolver(tag) { _, _ -> Tag.styling(color) }

    private fun firstColor(component: Component): TextColor? {
        component.color()?.let { return it }
        return component.children().firstNotNullOfOrNull { firstColor(it) }
    }

    /** Resolves the inherited style of the component rendering exactly [content]. */
    private fun styleOf(component: Component, content: String, inherited: Style = Style.empty()): Style? {
        val effective = inherited.merge(component.style(), Style.Merge.Strategy.ALWAYS)
        if (component is TextComponent && component.content() == content) return effective
        return component.children().firstNotNullOfOrNull { styleOf(it, content, effective) }
    }

    private fun colorOf(component: Component, content: String): TextColor? =
        styleOf(component, content)?.color()

    private fun shadowOf(component: Component, content: String): ShadowColor? =
        styleOf(component, content)?.shadowColor()

    private companion object {
        val PINK: TextColor = TextColor.color(0xFF00FF)
    }
}
