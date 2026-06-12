package com.whataicando.codeoba.core.premium

import com.whataicando.codeoba.core.domain.parser.Summarizer
import java.io.File
import java.net.URLClassLoader

object ClassLoaderInstaller {
    fun install(jarFile: File, entrypointClass: String): Summarizer {
        require(entrypointClass.startsWith("com.whataicando.codeoba.premium.") || 
                entrypointClass.startsWith("com.whataicando.codeoba.core.premium.")) {
            "Security violation: entrypoint class must reside in the com.whataicando.codeoba.premium package."
        }
        val parent = Summarizer::class.java.classLoader
        val classLoader = URLClassLoader(arrayOf(jarFile.toURI().toURL()), parent)
        val clazz = Class.forName(entrypointClass, true, classLoader)
        val instance = clazz.getDeclaredConstructor().newInstance()
        return instance as Summarizer
    }
}
