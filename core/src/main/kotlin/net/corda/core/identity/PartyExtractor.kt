package net.corda.core.identity

/**
 * Created by rossnicoll on 09/05/2017.
 */
interface PartyExtractor<T> {
    fun extractParties(spider: AnonymousPartySpider, obj: Any, written: MutableSet<Any>): Set<AnonymousParty>
}