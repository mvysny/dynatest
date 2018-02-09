package com.github.mvysny.dynatest

import org.junit.jupiter.api.Test
import kotlin.test.expect

class DynaTestTest {
    @Test
    fun simple() {
        var ran = false
        runTests {
            test("simple") {
                ran = true
            }
        }
        expect(true) { ran }
    }
}
