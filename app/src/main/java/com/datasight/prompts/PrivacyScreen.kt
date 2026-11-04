package com.datasight.prompts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PrivacyScreen(m: AppModel) {
    val preferences by m.prefs.collectAsStateWithLifecycle()
    val language = preferences.language
    val document = m.privacy
    val clipboard = LocalClipboardManager.current
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 840.dp).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item(key = "policy_heading") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr(language, "Politique de confidentialité", "Privacy policy"), Modifier.semantics { heading() },
                        fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                    Text("DATASIGHT Prompt Builder", color = colors.primary, fontWeight = FontWeight.SemiBold)
                    Text(tr(language, "Mise à jour : ", "Last updated: ") + document.updated.at(language),
                        fontSize = 13.sp, color = colors.onSurfaceVariant)
                    CenteredOutlinedButton({
                        clipboard.setText(AnnotatedString(document.asText(language)))
                        m.message.value = tr(language, "Politique copiée", "Policy copied")
                    }, Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                        Text(tr(language, "Copier la politique", "Copy policy"))
                    }
                }
            }
            items(document.sections, key = { it.id }) { section ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(section.title.at(language), Modifier.semantics { heading() }, fontSize = 18.sp, lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold)
                    SelectionContainer {
                        Text(section.text.at(language), fontSize = 16.sp, lineHeight = 25.sp, color = colors.onSurfaceVariant)
                    }
                }
            }
            item(key = "policy_contacts") {
                Panel {
                    Text(tr(language, "Contacter DATASIGHT", "Contact DATASIGHT"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    SelectionContainer { Text("${Company.email}\n${Company.phone}\n${Company.address}", fontSize = 15.sp, lineHeight = 23.sp) }
                }
            }
        }
    }
}
