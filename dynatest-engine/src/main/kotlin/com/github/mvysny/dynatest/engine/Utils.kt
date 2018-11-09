package com.github.mvysny.dynatest.engine

import java.io.File
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Paths

internal val isRunningInsideGradle: Boolean get() {
    val jars = (Thread.currentThread().contextClassLoader as URLClassLoader).urLs.toList()
    return (jars.any { it.toString().contains("gradle-worker.jar") })
}

internal fun URI.toFile(): File? {
    try {
        return Paths.get(this).toFile()
    } catch (e: FileSystemNotFoundException) {
        return null
    }
}
internal fun URL.toFile(): File? = toURI().toFile()
