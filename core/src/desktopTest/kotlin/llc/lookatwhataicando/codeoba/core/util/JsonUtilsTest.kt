package llc.lookatwhataicando.codeoba.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonUtilsTest {

    @Test
    fun testSerializeAndDeserializeList() {
        val original = listOf("/Users/pv/Dev", "/Users/pv/Dev,GitHub", "")
        val serialized = JsonUtils.serializeList(original)
        
        // Assert it serialized to a JSON array format
        assertTrue(serialized.startsWith("["))
        assertTrue(serialized.endsWith("]"))
        assertTrue(serialized.contains("/Users/pv/Dev,GitHub")) // Comma is preserved inside JSON string

        val deserialized = JsonUtils.deserializeList(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun testDeserializeEmptyAndMalformed() {
        // Empty inputs
        assertTrue(JsonUtils.deserializeList("").isEmpty())
        assertTrue(JsonUtils.deserializeList("[]").isEmpty())
        
        // Malformed inputs should fallback gracefully to empty list
        assertTrue(JsonUtils.deserializeList("{invalid}").isEmpty())
        assertTrue(JsonUtils.deserializeList("[invalid").isEmpty())
    }
}
