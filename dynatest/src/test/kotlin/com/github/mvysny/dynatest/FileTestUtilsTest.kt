package com.github.mvysny.dynatest

import java.io.File
import java.nio.file.Files
import kotlin.properties.ReadWriteProperty
import kotlin.test.expect

class FileTestUtilsTest : DynaTest({
    group("expectExists()") {
        test("passes on existing file") {
            File.createTempFile("foooo", "bar").expectExists()
        }

        test("passes on existing dir") {
            Files.createTempDirectory("foo").expectExists()
        }

        test("fails on nonexisting file") {
            expectThrows(AssertionError::class, "${slash}non${slash}existing does not exist") {
                File("/non/existing").expectExists()
            }
        }
    }

    group("expectNotExists()") {
        test("fails on existing file") {
            expectThrows(AssertionError::class, "asdasd") {
                File.createTempFile("foooo", "bar").expectNotExists()
            }
        }

        test("fails on existing dir") {
            expectThrows(AssertionError::class, "asdasd") {
                Files.createTempDirectory("foo").expectNotExists()
            }
        }

        test("passes on nonexisting file") {
            File("/non/existing").expectNotExists()
        }
    }

    group("expectFile()") {
        test("passes on existing file") {
            File.createTempFile("foooo", "bar").expectFile()
        }

        test("fails on existing dir") {
            expectThrows(AssertionError::class, " is not a file") {
                Files.createTempDirectory("foo").expectFile()
            }
        }

        test("fails on nonexisting file") {
            expectThrows(AssertionError::class, "${slash}non${slash}existing does not exist") {
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
            Files.createTempDirectory("foo").expectDirectory()
        }

        test("fails on nonexisting file") {
            expectThrows(AssertionError::class, "${slash}non${slash}existing does not exist") {
                File("/non/existing").expectDirectory()
            }
        }
    }

    group("expectReadableFile()") {
        test("passes on existing file") {
            File.createTempFile("foooo", "bar").expectReadableFile()
        }

        test("fails on existing dir") {
            expectThrows(AssertionError::class, " is not a file") {
                Files.createTempDirectory("foo").expectReadableFile()
            }
        }

        test("fails on nonexisting file") {
            expectThrows(AssertionError::class, "${slash}non${slash}existing does not exist") {
                File("/non/existing").expectReadableFile()
            }
        }

        test("fails on unreadable file") {
            expectThrows(AssertionError::class, " is not readable") {
                val f = File.createTempFile("foooo", "bar")
                f.setReadable(false)
                f.expectReadableFile()
            }
        }

        test("succeeds on read-only file") {
            val f = File.createTempFile("foooo", "bar")
            f.setWritable(false)
            f.expectReadableFile()
        }
    }

    group("expectWritableFile()") {
        test("passes on existing file") {
            File.createTempFile("foooo", "bar").expectWritableFile()
        }

        test("fails on existing dir") {
            expectThrows(AssertionError::class, " is not a file") {
                Files.createTempDirectory("foo").expectWritableFile()
            }
        }

        test("fails on nonexisting file") {
            expectThrows(AssertionError::class, "${slash}non${slash}existing does not exist") {
                File("/non/existing").expectWritableFile()
            }
        }

        test("succeeds on unreadable file") {
                val f = File.createTempFile("foooo", "bar")
                f.setReadable(false)
                f.expectWritableFile()
        }

        test("fails on read-only file") {
            expectThrows(AssertionError::class, " is not writable") {
                val f = File.createTempFile("foooo", "bar")
                f.setWritable(false)
                f.expectWritableFile()
            }
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
})

val slash: Char = File.separatorChar
