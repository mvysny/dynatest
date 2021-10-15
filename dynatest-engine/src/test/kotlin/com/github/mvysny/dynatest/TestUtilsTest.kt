package com.github.mvysny.dynatest

import com.github.mvysny.dynatest.engine.DynaNodeGroupImpl
import com.github.mvysny.dynatest.engine.isRunningInsideGradle
import com.github.mvysny.dynatest.engine.toTestSource
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.FileSource
import java.io.File
import kotlin.properties.ReadWriteProperty
import kotlin.test.expect

class TestUtilsTest : DynaTest({
    group("expectThrows()") {
        expectThrowsTestBatch()
    }

    group("tests for StackTraceElement.toTestSource()") {
        test("this class resolves to FileSource") {
            val e: StackTraceElement = DynaNodeGroupImpl.computeTestSource()!!
            if (isRunningInsideGradle) {
                val src: ClassSource = e.toTestSource() as ClassSource
                expect(src.className) { src.className }
                expect(e.lineNumber) { src.position.get().line }
            } else {
                val src = e.toTestSource() as FileSource
                expect(
                    true,
                    src.file.absolutePath
                ) { src.file.absolutePath.endsWith("src/test/kotlin/com/github/mvysny/dynatest/TestUtilsTest.kt") }
                expect(e.lineNumber) { src.position.get().line }
            }
        }
    }
})

private fun DynaNodeGroup.expectThrowsTestBatch() {
}
