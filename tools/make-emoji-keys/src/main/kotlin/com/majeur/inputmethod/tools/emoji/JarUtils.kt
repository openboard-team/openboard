/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.majeur.inputmethod.tools.emoji

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.HashMap
import java.util.jar.JarFile
import kotlin.RuntimeException

object JarUtils {

    fun getJarFile(mainClass: Class<*>): JarFile {
        val mainClassPath = "/${mainClass.name.replace('.', '/')}.class"
        val resUrl = mainClass.getResource(mainClassPath)
        if (resUrl?.protocol != "jar") {
            throw RuntimeException("Should run as jar and not as " + resUrl?.protocol)
        }
        val path = resUrl.path
        if (!path.startsWith("file:")) {
            throw RuntimeException("Unknown jar path: $path")
        }
        val jarPath = path.substring("file:".length, path.indexOf('!'))
        try {
            return JarFile(URLDecoder.decode(jarPath, "UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun openResource(name: String): InputStream {
        return javaClass.getResourceAsStream("/$name")
    }

    fun getLatestEmojiTestResource(jar: JarFile) : String {
        var latestUnicodeVersion = 0.0
        var name = ""
        jar.entries().iterator().forEach {
            if (it.name.endsWith("emoji-test.txt")) {
                val ver = it.name
                        .removeSuffix("/emoji-test.txt")
                        .substringAfterLast("/")
                        .toDouble()
                if (ver > latestUnicodeVersion) {
                    latestUnicodeVersion = ver
                    name = it.name
                }
            }
        }
        if (name.isEmpty())
            throw RuntimeException("No emoji specs provided in resources")
        return name
    }

    fun getAndroidResTemplateResource(jar: JarFile) : String {
        jar.entries().iterator().forEach {
            if (it.name.endsWith("emoji-categories.tmpl")) {
                return it.name
            }
        }
        throw RuntimeException("No template provided in resources")
    }

    fun close(stream: Closeable?) {
        try {
            stream?.close()
        } catch (e: IOException) {
        }
    }

    fun getEmojiSupportResource(jar: JarFile): String {
        jar.entries().iterator().forEach {
            if (it.name.endsWith("android-emoji-support.txt")) {
                return it.name
            }
        }
        throw RuntimeException("No emoji support file provided in resources")
    }
}