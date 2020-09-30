package com.github.mvysny.dynatest

import org.junit.jupiter.api.Test
import kotlin.test.expect

class CloneBySerializationTest {
    @Test
    fun testSimpleObjects() {
        expect("a") { "a".cloneBySerialization() }
        expect("") { "".cloneBySerialization() }
        expect(25) { 25.cloneBySerialization() }
    }
}
