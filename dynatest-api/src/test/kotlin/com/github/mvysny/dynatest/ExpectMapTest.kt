package com.github.mvysny.dynatest

import org.junit.jupiter.api.Test
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

class ExpectMapTest {
    @Test
    fun emptyMap() {
        expectMap() { mapOf<Int, Int>() }
        expectMap() { mutableMapOf<Int, Int>() }
        expectMap() { LinkedHashMap<String, Boolean>() }
        expectMap() { ConcurrentHashMap<String, Boolean>() }
    }

    @Test
    fun trivialMap() {
        expectMap(25 to "a") { mapOf(25 to "a") }
    }

    @Test
    fun simpleMapOfStrings() {
        expectMap("a" to 1, "b" to 2, "c" to 3) { mutableMapOf("a" to 1, "b" to 2, "c" to 3) }
    }

    @Test
    fun comparisonFailure() {
        expectThrows(AssertionError::class) {
            expectMap() { mapOf("a" to 1, "b" to 2, "c" to 3) }
        }
        expectThrows(AssertionError::class) {
            expectMap("a" to 1, "b" to 2, "c" to 3) { mapOf<Any, Any>(1 to "a", 2 to "b", 3 to "c") }
        }
        expectThrows(AssertionError::class) {
            expectMap("a" to 1, "b" to 2, "c" to 3) { mapOf<Any, Any>() }
        }
    }
}
