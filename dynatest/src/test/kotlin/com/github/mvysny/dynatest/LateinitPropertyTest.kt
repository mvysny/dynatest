package com.github.mvysny.dynatest

import java.io.File
import kotlin.test.expect

class LateinitPropertyTest : DynaTest({
    test("testFailsIfNoValue") {
        val file: File by late()
        expectThrows(RuntimeException::class, "LateinitProperty(name=file, value=null): not initialized") {
            file.name
        }
    }

    test("simple") {
        var file: File by late()
        file = File("foo.txt")
        expect("foo.txt") { file.name }
    }
})
