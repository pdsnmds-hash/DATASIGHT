package com.datasight.prompts

import org.junit.Assert.*
import org.junit.Test
import kotlinx.serialization.encodeToString

class BackupEncodingTest {
    @Test fun acceptsUtf8Backup() {
        val backup = Backup(records = emptyList(), settings = Preferences(favorites = setOf("write_01")))
        assertEquals(backup, BackupValidator.decodeUtf8(codec.encodeToString(backup).toByteArray(Charsets.UTF_8)))
    }
    @Test fun rejectsMalformedUtf8InsideOtherwiseValidJson() {
        val prefix = """{"schemaVersion":1,"records":[],"settings":{"language":"fr","theme":"system","favorites":["""".toByteArray()
        val suffix = """"]}}""".toByteArray()
        val bytes = prefix + byteArrayOf(0xC3.toByte(), 0x28) + suffix
        assertTrue(runCatching { BackupValidator.decode(bytes.toString(Charsets.UTF_8)) }.isSuccess)
        assertTrue(runCatching { BackupValidator.decodeUtf8(bytes) }.isFailure)
    }
}
