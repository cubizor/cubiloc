package net.cubizor.cubiloc.message

import eu.okaeri.placeholders.Placeholders
import eu.okaeri.placeholders.message.CompiledMessage
import net.cubizor.cubicolor.api.ColorScheme
import net.cubizor.cubicolor.text.MessageTheme
import net.cubizor.cubiloc.color.ColorSchemeTagResolver
import net.cubizor.cubiloc.color.MessageThemeTagResolver
import net.cubizor.cubiloc.tag.DefaultStyleSource
import net.cubizor.cubiloc.tag.TagResolverSource
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

internal object MessageResolver {

    /**
     * Builds the MiniMessage instance used to deserialize a message.
     *
     * Tag precedence, highest first:
     * `TagResolver.standard()` → [additionalResolvers] → [tagResolverSources] → theme resolver.
     * Registered sources therefore win over theme tag names such as `<primary>`, while standard
     * MiniMessage tags can never be shadowed.
     *
     * The resolver list is assembled in the opposite order because Adventure gives priority to
     * the *last* resolver passed to [TagResolver.resolver].
     */
    fun buildMiniMessage(
        colorScheme: ColorScheme?,
        messageTheme: MessageTheme?,
        additionalResolvers: List<TagResolver> = emptyList(),
        tagResolverSources: List<TagResolverSource> = emptyList(),
        defaultStyleSource: DefaultStyleSource? = null,
        receiver: Any? = null,
    ): MiniMessage {
        val themeResolver = when {
            messageTheme != null -> MessageThemeTagResolver.of(messageTheme)
            colorScheme != null -> ColorSchemeTagResolver.of(colorScheme)
            else -> TagResolver.empty()
        }
        val resolvers = buildList {
            add(themeResolver)
            tagResolverSources.asReversed().mapTo(this) { it.resolve(receiver) }
            addAll(additionalResolvers.asReversed())
            add(TagResolver.standard())
        }
        val defaultStyle = defaultStyleSource?.style(receiver)?.takeIf { it != Style.empty() }
        return MiniMessage.builder()
            .tags(TagResolver.resolver(resolvers))
            .postProcessor { applyRootStyle(it, defaultStyle) }
            .build()
    }

    /** Applies the non-italic default plus the optional fallback style, never overriding the message. */
    private fun applyRootStyle(component: Component, defaultStyle: Style?): Component {
        val result = component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
        if (defaultStyle == null) return result
        return result.style(result.style().merge(defaultStyle, Style.Merge.Strategy.IF_ABSENT_ON_TARGET))
    }

    fun resolvePlaceholders(value: String, placeholders: Map<String, Any>, global: Placeholders?): String {
        val compiled = CompiledMessage.of(value)
        val instance = global ?: Placeholders.create()
        val ctx = instance.contextOf(compiled)

        for ((k, v) in expandDottedKeys(placeholders)) {
            ctx.with(k, v)
        }

        ctx.placeholders.fallbackResolver { parent, field, _ ->
            val name = field.unsafe().name
            when {
                parent is Map<*, *> -> parent[name]
                placeholders.containsKey(name) -> placeholders[name]
                else -> null
            }
        }

        return ctx.apply()
    }

    @Suppress("UNCHECKED_CAST")
    fun expandDottedKeys(source: Map<String, Any>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        for ((key, value) in source) {
            if ("." in key) {
                val parts = key.split(".")
                var current = result
                for (i in 0 until parts.size - 1) {
                    val next = current.getOrPut(parts[i]) { mutableMapOf<String, Any>() }
                    current = if (next is MutableMap<*, *>) {
                        next as MutableMap<String, Any>
                    } else {
                        mutableMapOf<String, Any>().also { current[parts[i]] = it }
                    }
                }
                current[parts.last()] = value
            } else {
                result[key] = value
            }
        }
        return result
    }
}
