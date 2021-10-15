package com.github.mvysny.dynatest

import java.io.File
import java.nio.file.Path
import kotlin.test.expect

/**
 * Expects that this file or directory exists on the file system.
 */
public fun File.expectExists() {
    expect(true, "file $absoluteFile does not exist") { exists() }
}

/**
 * Expects that this file or directory exists on the file system.
 */
public fun Path.expectExists() {
    toFile().expectExists()
}

/**
 * Expects that this file or directory does not exist on the file system.
 */
public fun File.expectNotExists() {
    expect(false, "file/dir $absoluteFile exists") { exists() }
}

/**
 * Expects that this file or directory does not exist on the file system.
 */
public fun Path.expectNotExists() {
    toFile().expectNotExists()
}

/**
 * Expects that this file or directory [expectExists] and is a directory.
 */
public fun File.expectDirectory() {
    expectExists()
    expect(true, "file $absoluteFile is not a directory") { isDirectory }
}

/**
 * Expects that this file or directory [expectExists] and is a directory.
 */
public fun Path.expectDirectory() {
    toFile().expectDirectory()
}

/**
 * Expects that this file or directory [expectExists] and is a file.
 */
public fun File.expectFile() {
    expectExists()
    expect(true, "file $absoluteFile is not a file") { isFile }
}

/**
 * Expects that this file or directory [expectExists] and is a file.
 */
public fun Path.expectFile() {
    toFile().expectFile()
}

/**
 * Expects that this file or directory is a file [expectFile] and is readable ([File.canRead]).
 */
public fun File.expectReadableFile() {
    expectFile()
    expect(true, "file $absoluteFile is not readable") { canRead() }
}

/**
 * Expects that this file or directory is a file [expectFile] and is readable ([File.canRead]).
 */
public fun Path.expectReadableFile() {
    toFile().expectReadableFile()
}

/**
 * Expects that this file or directory is a file [expectFile] and is readable ([File.canRead]).
 */
public fun File.expectWritableFile() {
    expectFile()
    expect(true, "file $absoluteFile is not readable") { canWrite() }
}

/**
 * Expects that this file or directory is a file [expectFile] and is readable ([File.canRead]).
 */
public fun Path.expectWritableFile() {
    toFile().expectWritableFile()
}
