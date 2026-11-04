package com.datasight.prompts

import org.junit.Assert.*
import org.junit.Test
import kotlinx.serialization.decodeFromString
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PrivacyTest {
    private fun source(path: String): File = listOf(File("src/main/$path"), File("app/src/main/$path")).first { it.exists() }
    private fun policy(): PrivacyDocument = codec.decodeFromString(source("assets/privacy.json").readText())

    @Test fun completeBilingualSections() {
        val document = policy()
        val required = setOf("scope", "local_data", "purposes", "sharing", "contact", "backup", "retention", "security", "control", "updates")
        assertEquals(required, document.sections.map { it.id }.toSet())
        assertEquals(required.size, document.sections.size)
        for (language in listOf("fr", "en")) {
            assertTrue(document.updated.at(language).isNotBlank())
            document.sections.forEach {
                assertTrue(it.title.at(language).isNotBlank())
                assertTrue(it.text.at(language).length > 100)
            }
            assertFalse(Regex("\\{\\{.*?}}|Lorem ipsum|TODO", RegexOption.IGNORE_CASE).containsMatchIn(document.asText(language)))
        }
    }

    @Test fun policyUsesCentralContacts() {
        for (language in listOf("fr", "en")) {
            val text = policy().asText(language)
            assertTrue(text.contains(Company.email))
            assertTrue(text.contains(Company.phone))
            assertTrue(text.contains(Company.address))
        }
    }

    @Test fun excludesWorkingFilesFromBackupAndDeviceTransfer() {
        val factory = DocumentBuilderFactory.newInstance()
        val document = factory.newDocumentBuilder().parse(source("res/xml/data_extraction_rules.xml"))
        for (transport in listOf("cloud-backup", "device-transfer")) {
            val elements = document.getElementsByTagName(transport).item(0).childNodes
            val excluded = (0 until elements.length).mapNotNull { index ->
                val node = elements.item(index)
                if (node.nodeName == "exclude" && node.attributes.getNamedItem("path").nodeValue == ".") node.attributes.getNamedItem("domain").nodeValue else null
            }.toSet()
            assertTrue(excluded.containsAll(setOf("root", "file", "database", "sharedpref", "external")))
        }
    }
}
