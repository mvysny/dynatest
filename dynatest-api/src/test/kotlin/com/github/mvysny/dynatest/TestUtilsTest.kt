package com.github.mvysny.dynatest

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.expect

// can't use dynatest yet :-D
class TestUtilsTest {

    // tests for expectThrows() are in dynatest-engine. I was too lazy to port them here.

    @Nested
    @DisplayName("cloneBySerialization()")
    inner class CloneBySerialization {
        @Test
        fun testSimpleObjects() {
            expect("a") { "a".cloneBySerialization() }
            expect("") { "".cloneBySerialization() }
            expect(25) { 25.cloneBySerialization() }
        }
    }

    @Nested
    @DisplayName("expectList()")
    inner class ExpectList {
        @Test
        fun emptyList() {
            expectList() { listOf<Int>() }
            expectList() { mutableListOf<Int>() }
            expectList() { LinkedList<Int>() }
            expectList() { CopyOnWriteArrayList<Int>() }
        }

        @Test
        fun trivialList() {
            expectList(25) { listOf(25) }
        }

        @Test
        fun simpleListOfStrings() {
            expectList("a", "b", "c") { listOf("a", "b", "c") }
        }

        @Test
        fun comparisonFailure() {
            expectThrows(AssertionError::class) {
                expectList() { listOf("a", "b", "c") }
            }
            expectThrows(AssertionError::class) {
                expectList(1, 2, 3) { listOf("a", "b", "c") }
            }
            expectThrows(AssertionError::class) {
                expectList(1, 2, 3) { listOf() }
            }
        }
    }

    @Nested
    @DisplayName("expectMap()")
    inner class ExpectMap {
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
}
