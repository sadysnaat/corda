package net.corda.core.identity

import net.corda.core.utilities.ALICE
import net.corda.testing.ALICE_PUBKEY
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnonymousPartyExtractorTest {
    @Test
    fun `extracts an anonymous party`() {
        val expected = AnonymousParty(ALICE_PUBKEY)
        val extractor = AnonymousPartyExtractor()
        val spider = AnonymousPartySpider()
        val actual = extractor.extractParties(spider, expected, mutableSetOf()).single()
        assertEquals(expected, actual)
    }

    @Test
    fun `rejects anything else`() {
        val expected = ALICE.name
        val extractor = AnonymousPartyExtractor()
        val spider = AnonymousPartySpider()
        assertFailsWith<ClassCastException> { extractor.extractParties(spider, expected, mutableSetOf()).single() }
    }
}