package com.github.mvysny.dynatest.engine

import java.io.File
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Paths

internal var pretendIsRunningInsideGradle: Boolean? = null

internal val isRunningInsideGradle: Boolean get() {
    // testing purposes
    if (pretendIsRunningInsideGradle != null) return pretendIsRunningInsideGradle!!

    // if this function fails, Gradle will freeze. What the fuck!
    try {
        val classLoader: ClassLoader = Thread.currentThread().contextClassLoader
        if (classLoader is URLClassLoader) {
            // JDK 8 and older
            val jars: List<URL> = classLoader.urLs.toList()
            return (jars.any { it.toString().contains("gradle-worker.jar") })
        }
        // JDK 9+ uses AppClassLoader which doesn't provide a list of URLs for us.
        // we need to check in a different way whether there is `gradle-worker.jar` on the classpath.
        // we know that it contains the worker/org/gradle/api/JavaVersion.class
        val workerJar: URL? = classLoader.getResource("worker/org/gradle/api/JavaVersion.class")
        return workerJar.toString().contains("gradle-worker.jar")
    } catch (t: Throwable) {
        // give up, just pretend that we're inside of Gradle
        t.printStackTrace() // to see these stacktraces run Gradle with --info
        return true
    }
}

internal fun URI.toFile(): File? = try {
    Paths.get(this).toFile()
} catch (e: FileSystemNotFoundException) {
    null
}
internal fun URL.toFile(): File? = toURI().toFile()
