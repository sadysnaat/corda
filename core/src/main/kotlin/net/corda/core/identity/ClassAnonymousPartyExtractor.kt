package net.corda.core.identity

/**
 * Default extractor for parties from an object. Uses reflection to determine fields on a class, filters out any
 * synthetic or primitive
 */
class ClassPartyExtractor : PartyExtractor<Any> {
    override fun extractParties(spider: AnonymousPartySpider, obj: Any, written: MutableSet<Any>): Set<AnonymousParty> {
        val parties = HashSet<AnonymousParty>()
        obj.javaClass.fields
                .filter { !it.isSynthetic } // Ignore synthetic fields
                .filter { !it.type.isPrimitive } // Primitive types are never parties, nor can contain parties
                .map { field -> field.get(obj) }
                .filter { value -> !written.contains(value) }
                .forEach { value ->
                    written.add(value)
                    parties.addAll(spider.extractParties(value, written))
                }
        return parties
    }
}