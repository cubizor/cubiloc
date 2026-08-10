package net.cubizor.cubiloc.message

import eu.okaeri.placeholders.Placeholders
import net.cubizor.cubicolor.api.ColorScheme
import net.cubizor.cubicolor.text.MessageTheme
import net.cubizor.cubiloc.I18n
import net.cubizor.cubiloc.context.I18nContextHolder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * A resolved multi-line message.
 *
 * Tag resolver sources and the default style registered on [I18n] are read through the [i18n]
 * reference; instances built without one render with the standard and theme resolvers only.
 */
class ListMessageResult internal constructor(
    private val i18n: I18n? = null,
    private val rawValues: List<String>,
    private val placeholders: Map<String, Any> = emptyMap(),
    internal val globalPlaceholders: Placeholders? = null,
    private val colorScheme: ColorScheme? = null,
    private val messageTheme: MessageTheme? = null,
    internal val messageMap: Map<String, Any>? = null,
) {
    private var processedValues: List<String>? = null

    fun with(vararg pairs: Pair<String, Any>): ListMessageResult =
        ListMessageResult(i18n, rawValues, placeholders + pairs, globalPlaceholders, colorScheme, messageTheme, messageMap)

    fun withColorScheme(scheme: ColorScheme): ListMessageResult =
        ListMessageResult(i18n, rawValues, placeholders, globalPlaceholders, scheme, messageTheme, messageMap)

    fun withMessageTheme(theme: MessageTheme): ListMessageResult =
        ListMessageResult(i18n, rawValues, placeholders, globalPlaceholders, colorScheme, theme, messageMap)

    private fun process(): List<String> {
        processedValues?.let { return it }
        val result = rawValues.map { line ->
            var value = line
            if (messageMap != null) {
                value = MessageReference.resolve(value, messageMap, placeholders)
            }
            MessageResolver.resolvePlaceholders(value, placeholders, globalPlaceholders)
        }
        processedValues = result
        return result
    }

    private fun miniMessage(): MiniMessage {
        val context = I18nContextHolder.get()
        return MessageResolver.buildMiniMessage(
            colorScheme = colorScheme ?: context.colorScheme,
            messageTheme = messageTheme ?: context.messageTheme,
            tagResolverSources = i18n?.tagResolverSourcesInternal().orEmpty(),
            defaultStyleSource = i18n?.defaultStyleSourceInternal(),
            receiver = context.receiver,
        )
    }

    fun components(): List<Component> {
        val processed = process()
        val mm = miniMessage()
        return processed.map { mm.deserialize(it) }
    }

    fun component(): Component {
        val processed = process()
        if (processed.isEmpty()) return Component.empty()
        val mm = miniMessage()
        return processed.drop(1).fold(mm.deserialize(processed[0])) { acc, line ->
            acc.append(Component.newline()).append(mm.deserialize(line))
        }
    }

    fun componentLegacy(): Component {
        val processed = process()
        if (processed.isEmpty()) return Component.empty()
        val s = LegacyComponentSerializer.legacyAmpersand()
        return processed.drop(1).fold(s.deserialize(processed[0])) { acc, line ->
            acc.append(Component.newline()).append(s.deserialize(line))
        }
    }

    fun asString(): String = process().joinToString("\n")

    fun asList(): List<String> = process().toList()

    fun raw(): List<String> = rawValues.toList()
}
