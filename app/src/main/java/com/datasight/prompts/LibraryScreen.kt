package com.datasight.prompts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

internal fun categoryName(category: Category, language: String): String = when (category) {
    Category.DOCUMENT -> tr(language, "Documents", "Documents")
    Category.IDEAS -> tr(language, "Idées", "Ideas")
    Category.IMPROVE -> tr(language, "Améliorer", "Improve")
    else -> category.label.at(language)
}

internal fun categoryColors(category: Category, dark: Boolean): Pair<Color, Color> {
    val ink = Color(when (category) {
        Category.WRITE -> if (dark) 0xFFB8BAFF else 0xFF5853DA
        Category.ANALYZE -> if (dark) 0xFF7BD4D2 else 0xFF148287
        Category.DOCUMENT -> if (dark) 0xFF8DC5FF else 0xFF287BC1
        Category.IDEAS -> if (dark) 0xFFFFBD92 else 0xFFAF591F
        Category.DEVELOP -> if (dark) 0xFFD3AFF2 else 0xFF8453B2
        Category.IMPROVE -> if (dark) 0xFFF3ABBF else 0xFFB15170
    })
    val background = if (dark) ink.copy(alpha = .13f) else Color(when (category) {
        Category.WRITE -> 0xFFEDEAFF
        Category.ANALYZE -> 0xFFE5F3F2
        Category.DOCUMENT -> 0xFFE4F0FF
        Category.IDEAS -> 0xFFFFEBE3
        Category.DEVELOP -> 0xFFF0E9FA
        Category.IMPROVE -> 0xFFFFE7EC
    })
    return ink to background
}

// Only the listing copy is shortened; previews retain each template's complete content.
private fun listingTitle(t: Template, language: String): String = when (t.id) {
    "write_02" -> tr(language, "E-mail de clarification", "Clarification email")
    "document_02" -> tr(language, "Préparer un brief", "Prepare a brief")
    "analyze_01" -> tr(language, "Comparer des options", "Compare options")
    else -> t.title.at(language)
}
private fun listingDescription(t: Template, language: String): String = when (t.id) {
    "write_01" -> tr(language, "Une réponse claire, professionnelle et adaptée.", "A clear, professional response that fits.")
    "write_02" -> tr(language, "Posez les bonnes questions, sans ambiguïté.", "Ask the right questions, without ambiguity.")
    "document_02" -> tr(language, "Cadrez les objectifs et les livrables.", "Define objectives and deliverables.")
    "analyze_01" -> tr(language, "Évaluez les avantages et les limites.", "Weigh the benefits and limitations.")
    else -> t.description.at(language)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Library(m: AppModel, initial: Category?, open: (String) -> Unit) {
    val preferences by m.prefs.collectAsStateWithLifecycle()
    val language = preferences.language
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < .5f
    val keyboard = LocalSoftwareKeyboardController.current
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(initial?.name ?: "all") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    val featuredOrder = listOf("write_01", "write_02", "document_02", "analyze_01")
    val filtered = m.templates.filter { template ->
        (category == "all" || template.category.name == category) &&
            (!favoritesOnly || template.id in preferences.favorites) &&
            (query.isBlank() || buildList {
                add(template.title.at(language)); add(template.description.at(language))
                add(listingTitle(template, language)); add(categoryName(template.category, language))
                addAll(template.tags.map { it.at(language) })
                addAll(template.fields.map { it.label.at(language) })
            }.any { it.contains(query.trim(), ignoreCase = true) })
    }.sortedWith(compareBy<Template> { featuredOrder.indexOf(it.id).let { index -> if (index < 0) 4 else index } })

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 840.dp).fillMaxWidth(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "library_header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tr(language, "BIBLIOTHÈQUE DE PROMPTS", "PROMPT LIBRARY"),
                        color = colors.primary, fontSize = 13.sp, lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                    Text(tr(language, "La bonne structure, dès\nle départ.", "The right structure,\nfrom the start."),
                        Modifier.semantics { heading() },
                        fontSize = 26.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = (-.6).sp)
                    Text(tr(language, "Choisissez un modèle. Adaptez-le à votre besoin.",
                        "Choose a template. Make it fit your task."),
                        color = colors.onSurfaceVariant, fontSize = 14.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = tr(language, "Rechercher un modèle", "Search templates")
                        },
                        placeholder = { Text(tr(language, "Rechercher un modèle", "Search templates"), fontSize = 15.sp) },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(22.dp)) },
                        trailingIcon = if (query.isNotEmpty()) {{
                            IconButton({ query = "" }) { Icon(Icons.Outlined.Close, tr(language, "Effacer la recherche", "Clear search")) }
                        }} else null,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = colors.outline.copy(alpha = .7f))
                    )
                    Spacer(Modifier.height(2.dp))
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                        CategoryFilter(tr(language, "Tout", "All"), null, category == "all", dark) { category = "all" }
                        Category.entries.forEach { item ->
                            CategoryFilter(categoryName(item, language), item, category == item.name, dark) { category = item.name }
                        }
                    }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${filtered.size} " + tr(language, if (filtered.size == 1) "modèle" else "modèles", if (filtered.size == 1) "template" else "templates"),
                            fontSize = 14.sp, color = colors.onSurfaceVariant)
                        CenteredOutlinedButton(
                            onClick = { favoritesOnly = !favoritesOnly },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { selected = favoritesOnly },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (favoritesOnly) colors.primary else colors.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (favoritesOnly) colors.primary else colors.onSurfaceVariant,
                                containerColor = if (favoritesOnly) colors.primaryContainer else Color.Transparent)
                        ) {
                            Icon(if (favoritesOnly) Icons.Outlined.Star else Icons.Outlined.StarOutline, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tr(language, "Favoris", "Favorites"), fontSize = 13.sp)
                        }
                    }
                }
            }
            if (filtered.isEmpty()) item(key = "empty_results") {
                Panel {
                    Text(tr(language, "Aucun modèle trouvé", "No templates found"), fontWeight = FontWeight.Bold)
                    Text(tr(language, "Essayez un autre mot ou retirez un filtre.", "Try another word or remove a filter."), color = colors.onSurfaceVariant)
                    SecondaryButton({ query = ""; category = "all"; favoritesOnly = false }) {
                        Text(tr(language, "Réinitialiser les filtres", "Reset filters"))
                    }
                }
            }
            items(filtered, key = { it.id }) { template ->
                TemplateListingCard(template, language, dark, template.id in preferences.favorites,
                    open = { open(template.id) }, toggleFavorite = {
                        m.settings(preferences.copy(favorites = if (template.id in preferences.favorites)
                            preferences.favorites - template.id else preferences.favorites + template.id))
                    })
            }
        }
    }
}

@Composable
internal fun CategoryFilter(label: String, category: Category?, selected: Boolean, dark: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (ink, background) = category?.let { categoryColors(it, dark) } ?: (colors.primary to colors.primaryContainer)
    // The visible pill is compact; the entire 48 dp outer area is selectable.
    Box(Modifier.heightIn(min = 48.dp).selectable(selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Surface(
            color = if (selected) colors.primary else background,
            contentColor = if (selected) colors.onPrimary else colors.onSurface,
            shape = RoundedCornerShape(12.dp),
            border = if (selected && category != null) BorderStroke(1.dp, colors.onPrimary.copy(alpha = .3f)) else null
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (category != null) Icon(if (category == Category.IMPROVE) Icons.Outlined.AutoAwesome else icon(category),
                    null, Modifier.size(18.dp), tint = if (selected) colors.onPrimary else ink)
                Text(label, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TemplateListingCard(template: Template, language: String, dark: Boolean, favorite: Boolean,
                                open: () -> Unit, toggleFavorite: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (ink, background) = categoryColors(template.category, dark)
    Card(onClick = open, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .75f))) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Surface(color = background, shape = RoundedCornerShape(9.dp)) {
                    Row(Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(icon(template.category), null, Modifier.size(16.dp), tint = ink)
                        Text(categoryName(template.category, language), fontSize = 11.sp, lineHeight = 14.sp,
                            fontWeight = FontWeight.SemiBold, color = ink)
                    }
                }
                Text(listingTitle(template, language), Modifier.padding(end = 28.dp),
                    fontSize = 17.sp, lineHeight = 21.sp, letterSpacing = (-.3).sp, fontWeight = FontWeight.Bold)
                Text(listingDescription(template, language), fontSize = 13.sp, lineHeight = 18.sp,
                    color = colors.onSurfaceVariant)
                SecondaryButton(open, Modifier.fillMaxWidth().padding(top = 7.dp)) {
                    Text(tr(language, "Personnaliser", "Customize"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp))
                }
            }
            IconToggleButton(checked = favorite, onCheckedChange = { toggleFavorite() },
                modifier = Modifier.align(Alignment.TopEnd).size(48.dp)) {
                Icon(if (favorite) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                    tr(language, if (favorite) "Retirer des favoris" else "Ajouter aux favoris",
                        if (favorite) "Remove from favorites" else "Add to favorites"),
                    Modifier.size(22.dp), tint = if (favorite) colors.primary else colors.onSurfaceVariant)
            }
        }
    }
}
