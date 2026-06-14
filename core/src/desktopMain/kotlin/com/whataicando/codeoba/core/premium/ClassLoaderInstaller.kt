package com.whataicando.codeoba.core.premium

import com.whataicando.codeoba.core.domain.parser.Summarizer
import java.io.File
import java.net.URLClassLoader

private class FilteredParentClassLoader(private val delegate: ClassLoader) : ClassLoader(null) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // 1. Allow JDK/Standard classes (java.*, javax.*, sun.*, etc.)
        if (name.startsWith("java.") || 
            name.startsWith("javax.") || 
            name.startsWith("sun.") || 
            name.startsWith("com.sun.") ||
            name.startsWith("jdk.") ||
            name.startsWith("org.w3c.dom.") ||
            name.startsWith("org.xml.sax.")) {
            return super.loadClass(name, resolve)
        }
        
        // 2. Allow only classes from the API contract package and basic Kotlin runtime / serialization classes
        if (name.startsWith("com.whataicando.codeoba.core.domain.parser.") ||
            name.startsWith("com.whataicando.codeoba.core.domain.model.") ||
            name.startsWith("kotlin.") ||
            name.startsWith("kotlinx.serialization.")) {
            val clazz = delegate.loadClass(name)
            if (resolve) {
                resolveClass(clazz)
            }
            return clazz
        }

        // 3. Deny access to anything else (SettingsManager, SecureStorage, etc.)
        throw ClassNotFoundException("Access violation: Class $name is outside the API contract.")
    }
}

object ClassLoaderInstaller {
    fun install(jarFile: File, entrypointClass: String): Summarizer {
        require(entrypointClass.startsWith("com.whataicando.codeoba.premium.") || 
                entrypointClass.startsWith("com.whataicando.codeoba.core.premium.")) {
            "Security violation: entrypoint class must reside in the com.whataicando.codeoba.premium package."
        }
        val contractLoader = Summarizer::class.java.classLoader
        val parent = FilteredParentClassLoader(contractLoader)
        val classLoader = URLClassLoader(arrayOf(jarFile.toURI().toURL()), parent)
        val clazz = Class.forName(entrypointClass, true, classLoader)
        val instance = clazz.getDeclaredConstructor().newInstance()
        return instance as Summarizer
    }
}
