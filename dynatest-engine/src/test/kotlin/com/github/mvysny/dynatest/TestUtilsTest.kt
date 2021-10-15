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
    group("withTempDir()") {

        // a demo of a function which uses `withTempDir` and populates/inits the folder further.
        fun DynaNodeGroup.reusable(): ReadWriteProperty<Any?, File> =
            withTempDir("sources") { dir -> File(dir, "foo.txt").writeText("") }

        group("simple") {
            val tempDir: File by withTempDir()
            lateinit var file: File
            beforeEach {
                // expect that the folder already exists, so that we can e.g. copy stuff there
                tempDir.expectDirectory()
                file = File(tempDir, "foo.txt") // example contents
                file.writeText("")
            }
            test("temp dir checker") {
                tempDir.expectDirectory()
                tempDir.expectFiles("**/*.txt")
                file.expectReadableFile()
            }
        }
        // tests the 'reusable' approach where the developer doesn't call `withTempDir()` directly
        // but creates a reusable function.
        group("reusable") {
            val tempDir: File by reusable()
            test("txt file checker") {
                tempDir.expectFiles("**/*.txt")
            }
        }
        group("deletes temp folder afterwards") {
            val tempDir: File by withTempDir()
            afterEach {
                expect(false) { tempDir.exists() }
            }
            test("dummy") {}
        }
    }
}
