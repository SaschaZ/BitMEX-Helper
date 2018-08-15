package com.gapps.utils

import java.io.File
import java.net.URLDecoder

object JarLocation {

    val directory: String
        get() = JarLocation::class.java.protectionDomain.codeSource.location.path.let { path ->
            val decoded = URLDecoder.decode(path, "UTF-8")
            decoded.substring(0, decoded.lastIndexOf(File.separator))
        }

    fun fileInSameDir(filename: String) = File("${JarLocation.directory}${File.separator}$filename")
}