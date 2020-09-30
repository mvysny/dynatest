package com.github.mvysny.dynatest

import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class ExpectListTest {
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
