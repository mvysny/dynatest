package com.github.mvysny.dynatest.engine

import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Paths

internal val isRunningInsideGradle: Boolean get() {
    try {
        Class.forName("org.gradle.internal.remote.internal.hub.queue.EndPointQueue")
        return true
    } catch (e: ClassNotFoundException) {
        return false
    }
}

internal fun URI.toFile(): File? {
    try {
        val path = Paths.get(this)
        return path.toFile()
    } catch (e: FileSystemNotFoundException) {
        return null
    }
}
internal fun URL.toFile(): File? = toURI().toFile()
