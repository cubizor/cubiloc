package net.cubizor.cubiloc.tag

import net.kyori.adventure.text.format.Style

/**
 * Supplies the style applied to the root of every deserialized message component.
 *
 * Register with [net.cubizor.cubiloc.I18n.registerDefaultStyle]. The style is merged with
 * [Style.Merge.Strategy.IF_ABSENT_ON_TARGET], so it only fills in what the message itself did
 * not specify — a message that sets its own color, shadow or decoration always wins.
 *
 * Implementations must be thread-safe. Return [Style.empty] to contribute nothing.
 */
fun interface DefaultStyleSource {

    /**
     * Returns the fallback style for [receiver].
     *
     * @param receiver the receiver of the active [net.cubizor.cubiloc.context.I18nContext], or
     *   `null` when a message is rendered outside any context (console, background tasks).
     */
    fun style(receiver: Any?): Style
}
