package net.cubizor.cubiloc.tag

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * Supplies additional MiniMessage tags for the receiver a message is rendered for.
 *
 * Register with [net.cubizor.cubiloc.I18n.registerTagResolvers]. Sources rank below
 * [TagResolver.standard] and any per-call resolver, but above the color scheme / message theme
 * resolver — so a source may override theme tag names such as `<primary>`.
 *
 * Implementations must be thread-safe: resolution happens on whatever thread renders the message.
 */
fun interface TagResolverSource {

    /**
     * Returns the resolver to use for [receiver].
     *
     * @param receiver the receiver of the active [net.cubizor.cubiloc.context.I18nContext], or
     *   `null` when a message is rendered outside any context (console, background tasks).
     */
    fun resolve(receiver: Any?): TagResolver
}
