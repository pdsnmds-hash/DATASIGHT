package com.datasight.prompts

import kotlinx.serialization.Serializable

@Serializable data class PrivacySection(val id: String, val title: Bi, val text: Bi)
@Serializable data class PrivacyDocument(val version: Int, val updated: Bi, val sections: List<PrivacySection>) {
    fun asText(language: String): String = buildString {
        append(tr(language, "Politique de confidentialité", "Privacy policy"))
        append("\nDATASIGHT Prompt Builder\n")
        append(tr(language, "Mise à jour : ", "Last updated: ") + updated.at(language))
        sections.forEach { append("\n\n${it.title.at(language)}\n${it.text.at(language)}") }
        append("\n\nDATASIGHT\n${Company.email}\n${Company.phone}\n${Company.address}")
    }
}
