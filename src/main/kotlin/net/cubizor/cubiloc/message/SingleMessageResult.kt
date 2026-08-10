package net.cubizor.cubiloc.message

import eu.okaeri.placeholders.Placeholders
import net.cubizor.cubicolor.api.ColorScheme
import net.cubizor.cubicolor.text.MessageTheme
import net.cubizor.cubiloc.I18n
import net.cubizor.cubiloc.context.I18nContextHolder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * A single resolved message, rendered lazily against whatever [I18nContextHolder] context is
 * active at render time.
 *
 * Tag resolver sources and the default style registered on [I18n] are read through the [i18n]
 * reference. Instances built from a raw value without an [I18n] (the legacy constructor below)
 * have no way to reach them and therefore render with the standard, per-call and theme resolvers
 * only, and without any default root style.
 */
class SingleMessageResult internal constructor(
    private val i18n: I18n? = null,
    private val messageKey: String? = null,
    private val rawValueOverride: String? = null,
    private val placeholders: Map<String, Any> = emptyMap(),
    internal val globalPlaceholders: Placeholders? = null,
    private val colorScheme: ColorScheme? = null,
    private val messageTheme: MessageTheme? = null,
    internal val messageMap: Map<String, Any>? = null,
) {
    // Legacy constructor for callers that already have a resolved string. Pass `i18n` to keep
    // access to the registered tag resolver sources and default style; omitting it renders
    // without them.
    internal constructor(
        rawValue: String,
        i18n: I18n? = null,
        placeholders: Map<String, Any> = emptyMap(),
        globalPlaceholders: Placeholders? = null,
        colorScheme: ColorScheme? = null,
        messageTheme: MessageTheme? = null,
        messageMap: Map<String, Any>? = null,
    ) : this(
        i18n = i18n,
        messageKey = null,
        rawValueOverride = rawValue,
        placeholders = placeholders,
        globalPlaceholders = globalPlaceholders,
        colorScheme = colorScheme,
        messageTheme = messageTheme,
        messageMap = messageMap,
    )

    fun with(vararg pairs: Pair<String, Any>): SingleMessageResult =
        SingleMessageResult(i18n, messageKey, rawValueOverride, placeholders + pairs, globalPlaceholders, colorScheme, messageTheme, messageMap)

    fun withColorScheme(scheme: ColorScheme): SingleMessageResult =
        SingleMessageResult(i18n, messageKey, rawValueOverride, placeholders, globalPlaceholders, scheme, messageTheme, messageMap)

    fun withMessageTheme(theme: MessageTheme): SingleMessageResult =
        SingleMessageResult(i18n, messageKey, rawValueOverride, placeholders, globalPlaceholders, colorScheme, theme, messageMap)

    private fun resolveRawAndMap(): Pair<String, Map<String, Any>?> {
        if (rawValueOverride != null) {
            // Legacy / explicit raw value path. messageMap is whatever was passed in.
            return rawValueOverride to messageMap
        }
        if (i18n != null && messageKey != null) {
            // Lazy path: resolve against whatever context is active right now.
            val locale = i18n.currentLocaleStrInternal()
            val raw = i18n.resolveKey(messageKey, locale) as? String ?: "key not found: $messageKey"
            return raw to i18n.getMessageMapInternal(locale)
        }
        return ("" to messageMap)
    }

    private fun process(): String {
        val (rawValue, map) = resolveRawAndMap()
        var value = rawValue
        if (map != null) {
            value = MessageReference.resolve(value, map, placeholders)
        }
        return MessageResolver.resolvePlaceholders(value, placeholders, globalPlaceholders)
    }

    private fun miniMessage(additionalResolvers: List<TagResolver>): MiniMessage {
        val context = I18nContextHolder.get()
        return MessageResolver.buildMiniMessage(
            colorScheme = colorScheme ?: context.colorScheme,
            messageTheme = messageTheme ?: context.messageTheme,
            additionalResolvers = additionalResolvers,
            tagResolverSources = i18n?.tagResolverSourcesInternal().orEmpty(),
            defaultStyleSource = i18n?.defaultStyleSourceInternal(),
            receiver = context.receiver,
        )
    }

    fun component(): Component = miniMessage(emptyList()).deserialize(process())

    fun component(additionalResolver: TagResolver): Component =
        miniMessage(listOf(additionalResolver)).deserialize(process())

    fun componentLegacy(): Component =
        LegacyComponentSerializer.legacyAmpersand().deserialize(process())

    fun asString(): String = process()

    fun raw(): String = resolveRawAndMap().first
}
