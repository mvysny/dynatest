package com.github.mvysny.dynatest

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

class FileTestUtilsTest {
    @Nested
    @DisplayName("FileTestUtilsTest / expectExists()")
    class ExpectExistsTest {
        @Test
        fun `passes on existing file`() {
            File.createTempFile("foooo", "bar").expectExists()
        }

        @Test
        fun `passes on existing dir`() {
            createTempDir().expectExists()
        }

        @Test
        fun `fails on nonexisting file`() {
            expectThrows(AssertionError::class, "${slash}non${slash}existing does not exist") {
                File("/non/existing").expectExists()
            }
        }
    }

    @Nested
    @DisplayName("FileTestUtilsTest / expectFile()")
    class ExpectFileTests {
        @Test
        fun `passes on existing file`() {
            File.createTempFile("foooo", "bar").expectFile()
        }

        @Test
        fun `fails on existing dir`() {
            expectThrows(AssertionError::class, ".tmp is not a file") {
                createTempDir().expectFile()
            }
        }

        @Test
        fun `fails on nonexisting file`() {
            expectThrows(AssertionError::class, "${slash}non${slash}existing does not exist") {
                File("/non/existing").expectFile()
            }
        }
    }

    @Nested
    @DisplayName("FileTestUtilsTest / expectDirectory()")
    class ExpectDirectoryTest {
        @Test
        fun `fails on existing file`() {
            expectThrows(AssertionError::class, "bar is not a directory") {
                File.createTempFile("foooo", "bar").expectDirectory()
            }
        }

        @Test
        fun `passes on existing dir`() {
            createTempDir().expectDirectory()
        }

        @Test
        fun `fails on nonexisting file`() {
            expectThrows(AssertionError::class, "${slash}non${slash}existing does not exist") {
                File("/non/existing").expectDirectory()
            }
        }
    }

    @Nested
    @DisplayName("FileTestUtilsTest / expectReadableFile()")
    class ExpectReadableFileTest {
        @Test
        fun `passes on existing file`() {
            File.createTempFile("foooo", "bar").expectReadableFile()
        }

        @Test
        fun `fails on existing dir`() {
            expectThrows(AssertionError::class, ".tmp is not a file") {
                createTempDir().expectReadableFile()
            }
        }

        @Test
        fun `fails on nonexisting file`() {
            expectThrows(AssertionError::class, "${slash}non${slash}existing does not exist") {
                File("/non/existing").expectReadableFile()
            }
        }
    }
}
