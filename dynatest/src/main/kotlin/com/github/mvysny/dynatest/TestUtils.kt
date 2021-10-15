package com.github.mvysny.dynatest

import java.io.*
import java.nio.file.Files
import java.nio.file.Path

/**
 * Similar to [File.deleteRecursively] but throws informative [IOException] instead of
 * just returning false on error. uses Java 8 [Files.deleteIfExists] to delete files and folders.
 */
public fun Path.deleteRecursively() {
    toFile().walkBottomUp().forEach { Files.deleteIfExists(it.toPath()) }
}

/**
 * Returns the major JVM version of the current JRE, e.g. 6 for Java 1.6, 8 for Java 8, 11 for Java 11 etc.
 */
public val jvmVersion: Int get() = System.getProperty("java.version").parseJvmVersion()

private fun String.parseJvmVersion(): Int {
    // taken from https://stackoverflow.com/questions/2591083/getting-java-version-at-runtime
    val version: String = removePrefix("1.").takeWhile { it.isDigit() }
    return version.toInt()
}
