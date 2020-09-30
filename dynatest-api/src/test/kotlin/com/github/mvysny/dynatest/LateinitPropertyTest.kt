package com.github.mvysny.dynatest

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.expect

class LateinitPropertyTest {
    // can't use dynatest yet :-D
    @Test
    fun testFailsIfNoValue() {
        val file: File by late()
        expectThrows(RuntimeException::class, "LateinitProperty(name=file, value=null): not initialized") {
            file.name
        }
    }

    @Test
    fun testSimple() {
        var file: File by late()
        file = File("foo.txt")
        expect("foo.txt") { file.name }
    }
}
