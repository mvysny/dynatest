package com.github.mvysny.dynatest

import java.io.File
import java.nio.file.Files

class TestUtilsTest : DynaTest({
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
