package net.corda.core.identity

import net.corda.core.contracts.ContractState

/**
 * Spider utility for exploring a contract state object and extracting the anonymous parties contained within.
 * Uses [ClassPartyExtractor] to extract parties from objects by default, but custom extractors can be
 * added via [registerExtractor].
 */
class AnonymousPartySpider {
    private val defaultExtractor = ClassPartyExtractor()
    private val extractors: HashMap<Class<*>, PartyExtractor<*>> = HashMap()

    init {
        registerExtractor(AnonymousParty::class.java, AnonymousPartyExtractor())
    }

    /**
     * Register a new extractor for the given class.
     */
    fun <T: Any> registerExtractor(clazz: Class<T>, extractor: PartyExtractor<T>) {
        extractors[clazz] = extractor
    }

    /**
     * Extract the anonymous parties from the supplied state.
     */
    fun extractParties(obj: ContractState) = extractParties(obj, HashSet<Any>())

    internal fun extractParties(obj: Any, written: MutableSet<Any>): Set<AnonymousParty> {
        val extractor = extractors[obj.javaClass] ?: defaultExtractor
        return extractor.extractParties(this, obj, written)
    }
}