package net.cubizor.cubiloc.color

import net.cubizor.cubicolor.text.MessageRole
import net.cubizor.cubicolor.text.MessageTheme
import net.cubizor.cubicolor.text.TextDecoration as CubiDecoration
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object MessageThemeTagResolver {

    fun of(theme: MessageTheme): TagResolver = TagResolver.resolver(
        MessageRole.entries.map { role ->
            TagResolver.resolver(role.name.lowercase()) { _, _ ->
                val style = theme.getStyle(role).orElse(null)
                    ?: return@resolver Tag.styling()

                val color = style.color
                val textColor = TextColor.color(color.red, color.green, color.blue)

                // Optional text shadow (Minecraft 1.21.4+): ARGB, so #00000000 disables the vanilla
                // shadow — lets bright/neon theme roles read cleanly. Null = leave the shadow unset.
                val shadow = style.shadow?.let { ShadowColor.shadowColor(it.toARGB()) }

                val decorations = style.decorations.mapNotNull { dec ->
                    when (dec) {
                        CubiDecoration.BOLD -> TextDecoration.BOLD
                        CubiDecoration.ITALIC -> TextDecoration.ITALIC
                        CubiDecoration.UNDERLINED -> TextDecoration.UNDERLINED
                        CubiDecoration.STRIKETHROUGH -> TextDecoration.STRIKETHROUGH
                        CubiDecoration.OBFUSCATED -> TextDecoration.OBFUSCATED
                        else -> null
                    }
                }

                Tag.styling { builder ->
                    builder.color(textColor)
                    shadow?.let { builder.shadowColor(it) }
                    decorations.forEach { builder.decoration(it, true) }
                }
            }
        }
    )
}
