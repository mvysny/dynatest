package com.github.mvysny.dynatest.engine

import java.io.File
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Paths

internal val isRunningInsideGradle: Boolean get() {
    // if this function fails, Gradle will freeze. What the fuck!
    try {
        val classLoader = Thread.currentThread().contextClassLoader
        if (classLoader is URLClassLoader) {
            // JDK 8 and older
            val jars: List<URL> = classLoader.urLs.toList()
            return (jars.any { it.toString().contains("gradle-worker.jar") })
        }
        // JDK 9+ uses AppClassLoader
        val methodGetUrls = classLoader::class.java.getDeclaredMethod("getURLs")
        val jars = methodGetUrls.invoke(classLoader) as Array<URL>
        return (jars.any { it.toString().contains("gradle-worker.jar") })
    } catch (t: Throwable) {
        // give up, just pretend that we're inside of Gradle
        t.printStackTrace()
        return true
    }
}

internal fun URI.toFile(): File? {
    try {
        return Paths.get(this).toFile()
    } catch (e: FileSystemNotFoundException) {
        return null
    }
}
internal fun URL.toFile(): File? = toURI().toFile()
