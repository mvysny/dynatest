package com.github.mvysny.dynatest

import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.expect

class TestUtilsTest : DynaTest({
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
