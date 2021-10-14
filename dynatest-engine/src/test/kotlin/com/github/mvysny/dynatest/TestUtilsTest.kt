package com.github.mvysny.dynatest

import com.github.mvysny.dynatest.engine.DynaNodeGroupImpl
import com.github.mvysny.dynatest.engine.isRunningInsideGradle
import com.github.mvysny.dynatest.engine.toTestSource
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.FileSource
import java.io.File
import java.io.IOException
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
    test("throwing expected exception succeeds") {
        expectThrows(RuntimeException::class) { throw RuntimeException("Expected") }
    }

    test("fails if block completes successfully") {
        try {
            expectThrows(RuntimeException::class) {} // expected to be failed with AssertionError
            throw RuntimeException("Should have failed")
        } catch (e: AssertionError) {
            // okay
            expect("Expected to fail with java.lang.RuntimeException but completed successfully") { e.message }
            expect(null) { e.cause }
        }
    }

    test("fails if block throws something else") {
        try {
            // this should fail with AssertionError since some other exception has been thrown
            expectThrows(RuntimeException::class) {
                throw IOException("simulated")
            }
            throw RuntimeException("Should have failed")
        } catch (e: AssertionError) {
            // okay
            expect("Expected to fail with java.lang.RuntimeException but failed with java.io.IOException: simulated") { e.message }
            expect<Class<*>?>(IOException::class.java) { e.cause?.javaClass }
        }
    }

    group("message") {
        test("throwing expected exception succeeds") {
            expectThrows(RuntimeException::class, "Expected") { throw RuntimeException("Expected") }
        }

        test("fails if the message is different") {
            try {
                expectThrows(RuntimeException::class, "foo") { throw RuntimeException("actual") }
                throw RuntimeException("Should have failed")
            } catch (e: AssertionError) {
                // expected
                expect("java.lang.RuntimeException message: Expected 'foo' but was 'actual'") { e.message }
            }
        }

        test("fails if block completes successfully") {
            try {
                expectThrows(RuntimeException::class, "foo") {} // expected to be failed with AssertionError
                throw RuntimeException("Should have failed")
            } catch (e: AssertionError) {
                // okay
                expect("Expected to fail with java.lang.RuntimeException but completed successfully") { e.message }
            }
        }

        test("fails if block throws something else") {
            expectThrows(AssertionError::class, "Expected to fail with java.lang.RuntimeException but failed with java.io.IOException: simulated") {
                // this should fail with AssertionError since some other exception has been thrown
                expectThrows(RuntimeException::class, "simulated") {
                    throw IOException("simulated")
                }
            }
        }

        test("thrown exception attached as cause to the AssertionError") {
            try {
                expectThrows(IOException::class, "foo") {
                    throw IOException("simulated")
                }
                throw RuntimeException("Should have failed")
            } catch (e: AssertionError) {
                // okay
                expect("java.io.IOException message: Expected 'foo' but was 'simulated'") { e.message }
                expect<Class<*>>(IOException::class.java) { e.cause!!.javaClass }
            }
        }
    }

    group("AssertionError not handled specially") {
        test("throwing expected exception succeeds") {
            expectThrows(AssertionError::class) { throw AssertionError("Expected") }
        }

        test("fails if block completes successfully") {
            try {
                expectThrows(AssertionError::class) {}
                throw RuntimeException("Should have failed")
            } catch (e: AssertionError) { /*okay*/ }
        }

        test("fails if block throws something else") {
            try {
                // this should fail with AssertionError since some other exception has been thrown
                expectThrows(AssertionError::class) {
                    throw IOException("simulated")
                }
                throw RuntimeException("Should have failed")
            } catch (e: AssertionError) { /*okay*/ }
        }

        test("fails on unexpected message") {
            try {
                // this should fail with AssertionError since some other exception has been thrown
                expectThrows(IOException::class, "expected") {
                    throw IOException("simulated")
                }
                throw RuntimeException("Should have failed")
            } catch (e: AssertionError) { /*okay*/ }
        }
    }
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

    test("jvmVersion") {
        // test that the JVM version parsing doesn't throw
        jvmVersion
    }
}
