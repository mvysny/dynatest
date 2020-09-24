package com.github.mvysny.dynatest

import com.github.mvysny.dynatest.engine.DynaNodeGroupImpl
import com.github.mvysny.dynatest.engine.isRunningInsideGradle
import com.github.mvysny.dynatest.engine.toTestSource
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.FileSource
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.expect

class TestUtilsTest : DynaTest({
    group("expectThrows()") {
        expectThrowsTestBatch()
    }

    group("expectList()") {
        test("empty list") {
            expectList() { listOf<Int>() }
            expectList() { mutableListOf<Int>() }
            expectList() { LinkedList<Int>() }
            expectList() { CopyOnWriteArrayList<Int>() }
        }
        test("trivial list") {
            expectList(25) { listOf(25) }
        }
        test("simple list of strings") {
            expectList("a", "b", "c") { listOf("a", "b", "c") }
        }
        test("comparison failure") {
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

    group("expectMap()") {
        test("empty map") {
            expectMap() { mapOf<Int, Int>() }
            expectMap() { mutableMapOf<Int, Int>() }
            expectMap() { LinkedHashMap<String, Boolean>() }
            expectMap() { ConcurrentHashMap<String, Boolean>() }
        }
        test("trivial map") {
            expectMap(25 to "a") { mapOf(25 to "a") }
        }
        test("simple map of strings") {
            expectMap("a" to 1, "b" to 2, "c" to 3) { mutableMapOf("a" to 1, "b" to 2, "c" to 3) }
        }
        test("comparison failure") {
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

    group("cloneBySerialization()") {
        test("simple objects") {
            expect("a") { "a".cloneBySerialization() }
            expect("") { "".cloneBySerialization() }
            expect(25) { 25.cloneBySerialization() }
        }
    }

    group("tests for StackTraceElement.toTestSource()") {
        test("this class resolves to FileSource") {
            val e = DynaNodeGroupImpl.computeTestSource()!!
            if (isRunningInsideGradle) {
                val src = e.toTestSource() as ClassSource
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

    group("File-related") {
        fileTestBatch()
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
}

private fun DynaNodeGroup.fileTestBatch() {
    group("expectExists()") {
        test("passes on existing file") {
            File.createTempFile("foooo", "bar").expectExists()
        }
        test("passes on existing dir") {
            createTempDir().expectExists()
        }
        test("fails on nonexisting file") {
            expectThrows(AssertionError::class, "/non/existing does not exist") {
                File("/non/existing").expectExists()
            }
        }
    }
    group("expectFile()") {
        test("passes on existing file") {
            File.createTempFile("foooo", "bar").expectFile()
        }
        test("fails on existing dir") {
            expectThrows(AssertionError::class, ".tmp is not a file") {
                createTempDir().expectFile()
            }
        }
        test("fails on nonexisting file") {
            expectThrows(AssertionError::class, "/non/existing does not exist") {
                File("/non/existing").expectFile()
            }
        }
    }
    group("expectDirectory()") {
        test("fails on existing file") {
            expectThrows(AssertionError::class, "bar is not a directory") {
                File.createTempFile("foooo", "bar").expectDirectory()
            }
        }
        test("passes on existing dir") {
            createTempDir().expectDirectory()
        }
        test("fails on nonexisting file") {
            expectThrows(AssertionError::class, "/non/existing does not exist") {
                File("/non/existing").expectDirectory()
            }
        }
    }
    group("expectReadableFile()") {
        test("passes on existing file") {
            File.createTempFile("foooo", "bar").expectReadableFile()
        }
        test("fails on existing dir") {
            expectThrows(AssertionError::class, ".tmp is not a file") {
                createTempDir().expectReadableFile()
            }
        }
        test("fails on nonexisting file") {
            expectThrows(AssertionError::class, "/non/existing does not exist") {
                File("/non/existing").expectReadableFile()
            }
        }
    }
}
