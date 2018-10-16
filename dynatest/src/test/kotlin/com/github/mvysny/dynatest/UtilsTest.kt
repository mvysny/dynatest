package com.github.mvysny.dynatest

import com.github.mvysny.dynatest.engine.toFile
import java.io.File
import java.net.URI
import java.net.URL
import kotlin.test.expect

class UtilsTest : DynaTest({
    group("toFile") {
        test("file:///tmp") {
            expect(File("/tmp")) { URL("file:///tmp").toFile() }
            expect(File("/tmp")) { URI("file:///tmp").toFile() }
        }
        test("http:///foo.fi") {
            expect(null) { URL("http:///foo.fi").toFile() }
            expect(null) { URI("http:///foo.fi").toFile() }
        }
    }
})
