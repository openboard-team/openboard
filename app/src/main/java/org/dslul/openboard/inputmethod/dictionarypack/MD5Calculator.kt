/**
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.dslul.openboard.inputmethod.dictionarypack

import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object MD5Calculator {
    @Throws(IOException::class)
    fun checksum(`in`: InputStream): String? { // This code from the Android documentation for MessageDigest. Nearly verbatim.
        val digester: MessageDigest
        digester = try {
            MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            return null // Platform does not support MD5 : can't check, so return null
        }
        val bytes = ByteArray(8192)
        var byteCount: Int
        while (`in`.read(bytes).also { byteCount = it } > 0) {
            digester.update(bytes, 0, byteCount)
        }
        val digest = digester.digest()
        val s = StringBuilder()
        for (i in digest.indices) {
            s.append(String.format("%1$02x", digest[i]))
        }
        return s.toString()
    }
}