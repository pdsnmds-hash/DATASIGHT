package com.datasight.prompts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import java.text.Collator
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Saved(m: AppModel, open: (Record) -> Unit, snack: SnackbarHostState, create: (Category) -> Unit) {
    val preferences by m.prefs.collectAsStateWithLifecycle()
    val records by m.records.collectAsStateWithLifecycle()
    val language = preferences.language
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < .5f
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("all") }
    var drafts by rememberSaveable { mutableStateOf(false) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf("recent") }
    var sorting by remember { mutableStateOf(false) }
    var choosingCategory by rememberSaveable { mutableStateOf(false) }
    val beginCreate = { choosingCategory = true }
    var exporting by remember { mutableStateOf<Record?>(null) }
    var exportText by rememberSaveable { mutableStateOf("") }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) scope.launch {
            m.attempt {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(exportText.toByteArray(Charsets.UTF_8)) }
                        ?: error("Cannot open output")
                }
                m.message.value = tr(language, "Fichier exporté", "File exported")
            }
        }
    }
    if (choosingCategory) AlertDialog(
        onDismissRequest = { choosingCategory = false },
        title = { Text(tr(language, "Créer un prompt", "Create a prompt")) },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr(language, "Choisissez le type de tâche.", "Choose the task category."))
                Category.entries.forEach { item ->
                    SecondaryButton({ choosingCategory = false; create(item) }) {
                        Icon(icon(item), null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(categoryName(item, language))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { SecondaryButton({ choosingCategory = false }) { Text(tr(language, "Annuler", "Cancel")) } }
    )
    val selectedRecords = records.filter { it.draft == drafts }
    val filtered = selectedRecords.filter {
        (category == "all" || it.category.name == category) && (!favoritesOnly || it.favorite) &&
            (query.isBlank() || it.title.contains(query.trim(), true) || it.text.contains(query.trim(), true) ||
                (it.draft && draftExcerpt(it, language).contains(query.trim(), true)))
    }.let { list ->
        when (sort) {
            "oldest" -> list.sortedWith(compareBy<Record> { it.updated }.thenBy { it.id })
            "title" -> {
                val collator = Collator.getInstance(Locale.forLanguageTag(language))
                list.sortedWith { first, second -> collator.compare(first.title, second.title).takeIf { it != 0 } ?: first.id.compareTo(second.id) }
            }
            else -> list.sortedWith(compareByDescending<Record> { it.updated }.thenBy { it.id })
        }
    }
    fun delete(record: Record) {
        m.delete(record) { scope.launch {
            if (snack.showSnackbar(tr(language, "Prompt supprimé", "Prompt deleted"),
                    tr(language, "Annuler", "Undo"), duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                m.restore(record)
            }
        } }
    }
    val sortOptions = listOf("recent" to tr(language, "Plus récents", "Newest first"),
        "oldest" to tr(language, "Plus anciens", "Oldest first"), "title" to tr(language, "Titre A–Z", "Title A–Z"))

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 840.dp).fillMaxWidth(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "saved_controls") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr(language, "VOTRE ESPACE", "YOUR WORKSPACE"), color = colors.primary,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(tr(language, "Mes prompts", "My prompts"), fontSize = 28.sp,
                                lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                            Text(tr(language, "Retrouvez vos textes et vos idées.", "Your texts and ideas, in one place."),
                                color = colors.onSurfaceVariant, fontSize = 14.sp, lineHeight = 19.sp)
                        }
                        FilledIconButton(beginCreate, Modifier.size(48.dp), shape = RoundedCornerShape(15.dp)) {
                            Icon(Icons.Outlined.Add, tr(language, "Créer un prompt", "Create a prompt"))
                        }
                    }
                    Surface(color = colors.primaryContainer.copy(alpha = if (dark) .55f else .6f), shape = RoundedCornerShape(15.dp)) {
                        Row(Modifier.fillMaxWidth().padding(4.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(false to tr(language, "Enregistrés", "Saved"), true to tr(language, "Brouillons", "Drafts")).forEach { (isDraft, title) ->
                                val active = drafts == isDraft
                                Surface(color = if (active) colors.surface else Color.Transparent, shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).selectable(active, role = Role.Tab, onClick = { drafts = isDraft })) {
                                    FlowRow(Modifier.heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                                        Text(title, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold,
                                            color = if (active) colors.primary else colors.onSurfaceVariant)
                                        Surface(color = if (active) colors.primaryContainer else colors.surfaceVariant,
                                            shape = RoundedCornerShape(7.dp)) {
                                            Text(records.count { it.draft == isDraft }.toString(), Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 11.sp, lineHeight = 14.sp, color = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text(tr(language, "Rechercher dans mes prompts", "Search my prompts"), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Outlined.Search, tr(language, "Rechercher", "Search")) },
                        trailingIcon = if (query.isNotEmpty()) {{ IconButton({ query = "" }) {
                            Icon(Icons.Outlined.Close, tr(language, "Effacer la recherche", "Clear search"))
                        } }} else null, shape = RoundedCornerShape(16.dp))
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                        CategoryFilter(tr(language, "Tout", "All"), null, category == "all", dark) { category = "all" }
                        Category.entries.forEach { item ->
                            CategoryFilter(categoryName(item, language), item, category == item.name, dark) { category = item.name }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        CenteredOutlinedButton({ favoritesOnly = !favoritesOnly },
                            Modifier.heightIn(min = 48.dp).semantics { selected = favoritesOnly }, shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = if (favoritesOnly) colors.primaryContainer else Color.Transparent)) {
                            Icon(if (favoritesOnly) Icons.Outlined.Star else Icons.Outlined.StarOutline, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp)); Text(tr(language, "Favoris", "Favorites"), fontSize = 13.sp)
                        }
                        Box {
                            CenteredOutlinedButton({ sorting = true }, Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)) {
                                Icon(Icons.Outlined.Sort, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp))
                                Text(sortOptions.first { it.first == sort }.second, fontSize = 13.sp)
                                Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
                            }
                            DropdownMenu(sorting, { sorting = false }) {
                                sortOptions.forEach { (id, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = { sort = id; sorting = false },
                                        leadingIcon = { if (sort == id) Icon(Icons.Outlined.Check, null) })
                                }
                            }
                        }
                    }
                    if (filtered.isNotEmpty()) Text("${filtered.size} " + when {
                        drafts -> tr(language, if (filtered.size == 1) "brouillon" else "brouillons", if (filtered.size == 1) "draft" else "drafts")
                        else -> tr(language, if (filtered.size == 1) "prompt" else "prompts", if (filtered.size == 1) "prompt" else "prompts")
                    }, color = colors.onSurfaceVariant, fontSize = 13.sp)
                }
            }
            if (filtered.isEmpty()) item(key = "saved_empty") {
                Surface(shape = RoundedCornerShape(20.dp), color = colors.surface,
                    border = BorderStroke(1.dp, colors.outlineVariant)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val noRecords = selectedRecords.isEmpty()
                        Text(if (noRecords) tr(language, if (drafts) "Aucun brouillon pour le moment" else "Votre premier prompt commence ici",
                            if (drafts) "No drafts yet" else "Your first prompt starts here")
                            else tr(language, "Aucun résultat", "No results"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(if (noRecords && drafts) tr(language, "Commencez une tâche : votre progression sera enregistrée ici.", "Start a task. Your progress is saved here.") else if (noRecords) tr(language, "Choisissez une tâche, puis enregistrez votre texte ici.",
                            "Choose a task, then save your text here.") else tr(language, "Changez votre recherche ou vos filtres.",
                            "Change your search or filters."), color = colors.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
                        if (noRecords) CenteredButton(beginCreate, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Icon(Icons.Outlined.Add, null, Modifier.size(19.dp)); Spacer(Modifier.width(6.dp))
                            Text(if (drafts) tr(language, "Créer un brouillon", "Start a draft") else tr(language, "Créer mon premier prompt", "Create my first prompt"))
                        } else SecondaryButton({ query = ""; category = "all"; favoritesOnly = false }) {
                            Text(tr(language, "Réinitialiser les filtres", "Reset filters"))
                        }
                    }
                }
            }
            items(filtered, key = { it.id }) { record ->
                SavedRecordCard(record, language, dark, { open(record) }, { m.favorite(record) }, {
                    clipboard.setText(AnnotatedString(record.text)); m.message.value = tr(language, "Prompt copié", "Prompt copied")
                }, { m.duplicate(record) }, { exporting = record }, { delete(record) })
            }
        }
    }
    exporting?.let { record ->
        PreviewDialog(language, record.text, { exporting = null }) {
            exportText = record.text; exporting = null
            exporter.launch(record.title.replace(Regex("[^\\p{L}\\p{N} _-]"), "_").take(70) + ".txt")
        }
    }
}

private fun draftExcerpt(record: Record, language: String): String {
    if (record.text.isNotBlank()) return record.text
    val form = runCatching { codec.decodeFromString<Form>(record.parameters) }.getOrNull()
    return listOfNotNull(form?.task?.takeIf { it.isNotBlank() }, form?.context?.takeIf { it.isNotBlank() })
        .joinToString("\n").ifBlank { tr(language, "Continuez à préciser votre tâche dans le constructeur.", "Continue defining your task in the builder.") }
}

@Composable
private fun SavedRecordCard(record: Record, language: String, dark: Boolean, open: () -> Unit,
                            favorite: () -> Unit, copy: () -> Unit, duplicate: () -> Unit, export: () -> Unit, delete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (ink, tint) = categoryColors(record.category, dark)
    var menu by remember { mutableStateOf(false) }
    val date = remember(record.updated, language) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.forLanguageTag(language)).format(Date(record.updated))
    }
    Card(open, Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface), border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .75f))) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(color = tint, shape = RoundedCornerShape(9.dp)) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(icon(record.category), null, Modifier.size(17.dp), tint = ink)
                        Text(categoryName(record.category, language), color = ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(record.title, Modifier.padding(end = 24.dp), fontSize = 18.sp, lineHeight = 23.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = (-.3).sp)
                Text(if (record.draft) draftExcerpt(record, language) else record.text, fontSize = 14.sp, lineHeight = 20.sp,
                    color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(date, fontSize = 11.sp, color = colors.onSurfaceVariant)
                        Text((if (record.language == "fr") "FR" else "EN") + if (record.draft) " · " + tr(language, "Brouillon", "Draft") else "",
                            fontSize = 11.sp, color = colors.primary, fontWeight = FontWeight.SemiBold)
                    }
                    if (record.text.isNotBlank()) IconButton(copy) {
                        Icon(Icons.Outlined.ContentCopy, tr(language, "Copier le prompt", "Copy prompt"), Modifier.size(21.dp), tint = colors.primary)
                    }
                    Box {
                        IconButton({ menu = true }) { Icon(Icons.Outlined.MoreHoriz, tr(language, "Actions du prompt", "Prompt actions")) }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text(if (record.draft) tr(language, "Continuer le brouillon", "Continue draft") else tr(language, "Ouvrir et modifier", "Open and edit")) },
                                onClick = { menu = false; open() }, leadingIcon = { Icon(Icons.Outlined.Edit, null) })
                            DropdownMenuItem(text = { Text(tr(language, "Dupliquer", "Duplicate")) },
                                onClick = { menu = false; duplicate() }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) })
                            if (record.text.isNotBlank()) DropdownMenuItem(text = { Text(tr(language, "Exporter TXT", "Export TXT")) },
                                onClick = { menu = false; export() }, leadingIcon = { Icon(Icons.Outlined.FileDownload, null) })
                            DropdownMenuItem(text = { Text(tr(language, "Supprimer", "Delete"), color = colors.error) },
                                onClick = { menu = false; delete() }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = colors.error) })
                        }
                    }
                }
            }
            IconToggleButton(record.favorite, { favorite() }, Modifier.align(Alignment.TopEnd).size(48.dp)) {
                Icon(if (record.favorite) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                    tr(language, if (record.favorite) "Retirer des favoris" else "Ajouter aux favoris",
                        if (record.favorite) "Remove from favorites" else "Add to favorites"),
                    Modifier.size(22.dp), tint = if (record.favorite) colors.primary else colors.onSurfaceVariant)
            }
        }
    }
}
