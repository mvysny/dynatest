package com.github.mvysny.dynatest

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.expect

class TestUtilsTest : DynaTest({
    group("expectThrows()") {
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

    group("cloneBySerialization()") {
        test("testSimpleObjects") {
            expect("a") { "a".cloneBySerialization() }
            expect("") { "".cloneBySerialization() }
            expect(25) { 25.cloneBySerialization() }
        }
    }

    group("expectList()") {
        test("emptyList") {
            expectList() { listOf<Int>() }
            expectList() { mutableListOf<Int>() }
            expectList() { LinkedList<Int>() }
            expectList() { CopyOnWriteArrayList<Int>() }
        }

        test("singleton list") {
            expectList(25) { listOf(25) }
        }

        test("simpleListOfStrings") {
            expectList("a", "b", "c") { listOf("a", "b", "c") }
        }

        test("comparisonFailure") {
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
        test("emptyMap") {
            expectMap() { mapOf<Int, Int>() }
            expectMap() { mutableMapOf<Int, Int>() }
            expectMap() { LinkedHashMap<String, Boolean>() }
            expectMap() { ConcurrentHashMap<String, Boolean>() }
        }

        test("singleton map") {
            expectMap(25 to "a") { mapOf(25 to "a") }
        }

        test("simpleMapOfStrings") {
            expectMap("a" to 1, "b" to 2, "c" to 3) { mutableMapOf("a" to 1, "b" to 2, "c" to 3) }
        }

        test("comparisonFailure") {
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

    group("deleteRecursively()") {
        test("simple file") {
            val f = File.createTempFile("foo", "bar")
            f.expectFile()
            f.toPath().deleteRecursively()
            f.expectNotExists()
        }
        test("empty folder") {
            val f = Files.createTempDirectory("tmp").toFile()
            f.expectDirectory()
            f.toPath().deleteRecursively()
            f.expectNotExists()
        }
        test("doesn't fail when the file doesn't exist") {
            val f = File.createTempFile("foo", "bar")
            f.delete()
            f.expectNotExists()
            f.toPath().deleteRecursively()
            f.expectNotExists()
        }
        test("non-empty folder") {
            val f = Files.createTempDirectory("tmp").toFile()
            val foo = File(f, "foo.txt")
            foo.writeText("foo")
            f.expectDirectory()
            f.toPath().deleteRecursively()
            foo.expectNotExists()
            f.expectNotExists()
        }
    }

    test("jvmVersion") {
        // test that the JVM version parsing doesn't throw
        jvmVersion
    }
})
