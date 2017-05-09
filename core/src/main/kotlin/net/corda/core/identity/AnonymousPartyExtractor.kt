package net.corda.core.identity

/**
 * Extractor for when an [AnonymousParty] is encountered, returns the object directly.
 */
class AnonymousPartyExtractor : PartyExtractor<AnonymousParty> {
    override fun extractParties(spider: AnonymousPartySpider, obj: Any, written: MutableSet<Any>): Set<AnonymousParty> {
        return setOf(obj as AnonymousParty)
    }
}