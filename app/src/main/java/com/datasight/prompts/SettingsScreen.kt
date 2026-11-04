package com.datasight.prompts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Settings(m: AppModel, privacy: () -> Unit, onCleared: () -> Unit) {
    val preferences by m.prefs.collectAsStateWithLifecycle()
    val records by m.records.collectAsStateWithLifecycle()
    val language = preferences.language
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < .5f
    val largeFont = LocalDensity.current.fontScale >= 1.5f
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var incoming by remember { mutableStateOf<Backup?>(null) }
    var exportPreview by remember { mutableStateOf<String?>(null) }
    val exportCache = remember { context.cacheDir.resolve(BACKUP_EXPORT_CACHE) }
    var busy by remember { mutableStateOf(false) }
    var clear by rememberSaveable { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            m.attempt {
                withContext(Dispatchers.IO) {
                    check(exportCache.exists() && exportCache.length() > 0)
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        exportCache.inputStream().use { input -> input.copyTo(output) }
                    } ?: error("Cannot write backup")
                }
                m.message.value = tr(language, "Sauvegarde exportée", "Backup exported")
            }
            exportCache.delete()
            busy = false
        } else exportCache.delete()
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                incoming = withContext(Dispatchers.IO) {
                    val raw = context.contentResolver.openInputStream(uri)?.use { it.readBackupLimited() } ?: error("Cannot read backup")
                    BackupValidator.decodeUtf8(raw)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                m.message.value = tr(language, "Fichier invalide ou illisible. Aucune donnée modifiée.",
                    "Invalid or unreadable file. No data changed.")
            } finally { busy = false }
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 840.dp).fillMaxWidth(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "settings_heading") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tr(language, "Paramètres", "Settings"), fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                    Text(tr(language, "Personnalisez votre expérience.", "Make it work for you."), color = colors.onSurfaceVariant,
                        fontSize = 15.sp, lineHeight = 21.sp)
                    Spacer(Modifier.height(4.dp))
                }
            }
            item(key = "settings_language") {
                SettingsPanel {
                    SettingsTitle(Icons.Outlined.Language, tr(language, "Langue de l’application", "App language"),
                        if (dark) Color(0xFFB8BAFF) else Color(0xFF6352D8))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth().selectableGroup()) {
                        listOf("fr" to "Français", "en" to "English").forEach { (id, title) ->
                            val active = preferences.language == id
                            Surface(color = if (active) colors.primary else colors.surface,
                                contentColor = if (active) colors.onPrimary else colors.onSurfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = if (active) null else BorderStroke(1.dp, colors.outline),
                                modifier = Modifier.selectable(active, enabled = !busy, role = Role.RadioButton,
                                    onClick = { m.settings(preferences.copy(language = id)) })) {
                                Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (active) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
                                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    Text(tr(language, "Langue de réponse distincte dans le constructeur.",
                        "Choose the prompt language in the builder."), color = colors.onSurfaceVariant,
                        fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
            item(key = "settings_theme") {
                SettingsPanel {
                    SettingsTitle(Icons.Outlined.Brush, tr(language, "Apparence", "Appearance"),
                        if (dark) Color(0xFFFFBD92) else Color(0xFFB65D2A))
                    if (largeFont) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
                            listOf("system", "light", "dark").forEach { id ->
                                ThemeOption(id, language, preferences.theme == id, Modifier.fillMaxWidth(), !busy, true) {
                                    m.settings(preferences.copy(theme = id))
                                }
                            }
                        }
                    } else Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system", "light", "dark").forEach { id ->
                            ThemeOption(id, language, preferences.theme == id, Modifier.weight(1f), !busy, false) {
                                m.settings(preferences.copy(theme = id))
                            }
                        }
                    }
                }
            }
            item(key = "settings_privacy") {
                SettingsPanel {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsIcon(Icons.Outlined.Shield, if (dark) Color(0xFF7BD9C1) else Color(0xFF008C79))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(tr(language, "Confidentialité", "Privacy"), fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold)
                            Text(tr(language, "Vos prompts sont créés et conservés sur cet appareil.",
                                "Your prompts are created and stored on this device."), fontSize = 14.sp, lineHeight = 20.sp,
                                color = colors.onSurfaceVariant)
                            Text(tr(language, "Sans compte, publicité ni analyse.", "No account, ads or analytics."),
                                color = colors.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                    SecondaryButton(privacy) {
                        Text(tr(language, "En savoir plus", "Learn more"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp))
                    }
                }
            }
            item(key = "settings_backup") {
                SettingsPanel {
                    SettingsTitle(Icons.Outlined.FolderOpen, tr(language, "Sauvegarde et restauration", "Backup and restore"),
                        if (dark) Color(0xFF8DC5FF) else Color(0xFF167BB4))
                    SettingsAction(Icons.Outlined.Upload, tr(language, "Exporter une sauvegarde", "Export backup"), !busy) {
                        scope.launch { busy = true; m.attempt { exportPreview = m.exportBackup() }; busy = false }
                    }
                    HorizontalDivider(color = colors.outlineVariant)
                    SettingsAction(Icons.Outlined.Download, tr(language, "Importer une sauvegarde", "Import backup"), !busy) {
                        importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                    }
                    Text(tr(language, "Le fichier JSON contient vos données et n’est pas chiffré.",
                        "The JSON file contains your data and is not encrypted."), fontSize = 12.sp, lineHeight = 17.sp,
                        color = colors.onSurfaceVariant)
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            item(key = "settings_clear") {
                Card(onClick = { clear = true }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .75f))) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = colors.errorContainer.copy(alpha = .5f), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Outlined.DeleteOutline, null, Modifier.padding(10.dp).size(22.dp), tint = colors.error)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(tr(language, "Effacer les données locales", "Clear local data"), fontSize = 16.sp,
                                lineHeight = 21.sp, fontWeight = FontWeight.Bold)
                            Text(tr(language, "Une confirmation vous sera demandée.", "You will be asked to confirm."),
                                fontSize = 12.sp, lineHeight = 17.sp, color = colors.onSurfaceVariant)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp), tint = colors.onSurfaceVariant)
                    }
                }
            }
            item(key = "settings_onboarding") {
                SecondaryButton({ m.replayOnboarding() }) { Text(tr(language, "Revoir la présentation", "View introduction again")) }
            }
            item(key = "settings_footer") {
                Text("DATASIGHT · Prompt Builder · 1.0", Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 12.sp, color = colors.onSurfaceVariant)
            }
        }
    }
    exportPreview?.let { raw ->
        val backup = codec.decodeFromString<Backup>(raw)
        AlertDialog(onDismissRequest = { exportPreview = null }, title = { Text(tr(language, "Exporter vos données", "Export your data")) },
            text = { Text("${backup.records.count { !it.draft }} " + tr(language, "prompts, ", "prompts, ") +
                "${backup.records.count { it.draft }} " + tr(language, "brouillons et les réglages. Choisissez un emplacement sûr.",
                "drafts and settings. Choose a safe location.")) },
            confirmButton = { SecondaryButton({ scope.launch {
                busy = true
                m.attempt {
                    withContext(Dispatchers.IO) { exportCache.writeText(raw, Charsets.UTF_8) }
                    exportPreview = null; exporter.launch("datasight-backup.json")
                }
                busy = false
            } }, enabled = !busy) {
                Text(tr(language, "Choisir le fichier", "Choose file"))
            } }, dismissButton = { SecondaryButton({ exportPreview = null }) { Text(tr(language, "Annuler", "Cancel")) } })
    }
    incoming?.let { backup ->
        AlertDialog(onDismissRequest = { if (!busy) incoming = null },
            title = { Text(tr(language, "Remplacer les données locales ?", "Replace local data?")) },
            text = { Text("${backup.records.count { !it.draft }} " + tr(language, "prompts et ", "prompts and ") +
                "${backup.records.count { it.draft }} " + tr(language, "brouillons valides. Les données et réglages actuels seront remplacés.",
                "valid drafts. Current data and settings will be replaced.")) },
            confirmButton = { SecondaryButton({ scope.launch { busy = true; m.attempt {
                m.importBackup(backup); incoming = null; m.message.value = m.text("Import terminé", "Import complete")
            }; busy = false } }, enabled = !busy) { Text(tr(language, "Remplacer", "Replace")) } },
            dismissButton = { SecondaryButton({ incoming = null }, enabled = !busy) { Text(tr(language, "Annuler", "Cancel")) } })
    }
    if (clear) AlertDialog(onDismissRequest = { if (!busy) clear = false },
        title = { Text(tr(language, "Effacer les données locales ?", "Clear local data?")) },
        text = { Text("${records.count { !it.draft }} " + tr(language, "prompts et ", "prompts and ") +
            "${records.count { it.draft }} " + tr(language, "brouillons seront supprimés. Les favoris et réglages seront réinitialisés (français, thème système). Les fichiers exportés et le presse-papiers ne sont pas effacés. Cette action ne peut pas être annulée.",
            "drafts will be deleted. Favorites and settings will reset to French and system theme. Exported files and the clipboard are not cleared. This action cannot be undone.")) },
        confirmButton = { SecondaryButton({ scope.launch { busy = true; m.attempt {
            m.clearLocalData(); clear = false; m.message.value = tr(language, "Données locales effacées", "Local data cleared"); onCleared()
        }; busy = false } }, enabled = !busy, colors = ButtonDefaults.textButtonColors(contentColor = colors.error)) {
            Text(tr(language, "Tout effacer", "Clear all"))
        } }, dismissButton = { SecondaryButton({ clear = false }, enabled = !busy) { Text(tr(language, "Annuler", "Cancel")) } })
}

@Composable
private fun SettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .75f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun SettingsTitle(icon: ImageVector, title: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsIcon(icon, accent)
        Text(title, Modifier.weight(1f), fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector, accent: Color) {
    Surface(color = accent.copy(alpha = .12f), shape = RoundedCornerShape(12.dp)) {
        Icon(icon, null, Modifier.padding(10.dp).size(22.dp), tint = accent)
    }
}

@Composable
private fun SettingsAction(icon: ImageVector, label: String, enabled: Boolean, action: () -> Unit) {
    SecondaryButton(action, Modifier.fillMaxWidth(), enabled = enabled) {
        Icon(icon, null, Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun ThemeOption(id: String, language: String, selected: Boolean, modifier: Modifier, enabled: Boolean, wide: Boolean, action: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val label = when (id) { "light" -> tr(language, "Clair", "Light"); "dark" -> tr(language, "Sombre", "Dark"); else -> tr(language, "Système", "System") }
    Surface(modifier = modifier.selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = action),
        color = colors.surface, shape = RoundedCornerShape(13.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant)) {
        Box {
            if (wide) Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ThemeMiniature(id, Modifier.size(50.dp, 44.dp)); Text(label, Modifier.weight(1f), fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                if (selected) Icon(Icons.Outlined.CheckCircle, null, tint = colors.primary)
            } else Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                ThemeMiniature(id, Modifier.size(54.dp, 48.dp))
                Text(label, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
            if (selected && !wide) Icon(Icons.Outlined.CheckCircle, null, Modifier.align(Alignment.TopEnd).padding(3.dp).size(18.dp), tint = colors.primary)
        }
    }
}

@Composable
private fun ThemeMiniature(id: String, modifier: Modifier) {
    Canvas(modifier) {
        val radius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        val light = Color(0xFFFCFCFF); val dark = Color(0xFF232A46)
        drawRoundRect(if (id == "dark") dark else light, cornerRadius = radius)
        if (id == "system") drawPath(androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * .62f, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height)
            lineTo(size.width * .36f, size.height); close()
        }, dark)
        drawRoundRect(Color(0xFFA4A0C6), cornerRadius = radius, style = Stroke(1.dp.toPx()))
        val dot = 4.dp.toPx()
        drawCircle(Color(0xFFAAA0F3), dot, Offset(size.width * .22f, size.height * .22f))
        repeat(3) { index ->
            drawRoundRect(if (index == 0) Color(0xFF9186EB) else Color(0xFFACAFCA).copy(alpha = .55f),
                Offset(size.width * .14f, size.height * (.42f + .19f * index)),
                Size(size.width * (if (index == 2) .5f else .7f), size.height * .1f), CornerRadius(dot, dot))
        }
    }
}

private fun java.io.InputStream.readBackupLimited(): ByteArray {
    val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer); if (count < 0) break
        require(output.size() + count <= 20_000_000)
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
