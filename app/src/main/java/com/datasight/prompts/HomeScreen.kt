package com.datasight.prompts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private data class HomeCategory(val category: Category, val title: Bi, val subtitle: Bi)
private val homeCategories = listOf(
    HomeCategory(Category.WRITE, Bi("Rédiger", "Write"), Bi("E-mails et contenus", "Emails and content")),
    HomeCategory(Category.ANALYZE, Bi("Analyser", "Analyze"), Bi("Comparer et comprendre", "Compare and understand")),
    HomeCategory(Category.DOCUMENT, Bi("Documents", "Documents"), Bi("Briefs et rapports", "Briefs and reports")),
    HomeCategory(Category.IDEAS, Bi("Trouver des idées", "Generate ideas"), Bi("Explorer de nouvelles pistes", "Explore new possibilities")),
    HomeCategory(Category.DEVELOP, Bi("Développer", "Develop"), Bi("Code, specs et tests", "Code, specs and tests")),
    HomeCategory(Category.IMPROVE, Bi("Améliorer", "Improve"), Bi("Affiner un prompt", "Refine a prompt"))
)

@Composable
fun Home(
    m: AppModel,
    category: (Category) -> Unit,
    start: (Template) -> Unit,
    resume: (Record) -> Unit,
    templates: () -> Unit
) {
    val preferences by m.prefs.collectAsStateWithLifecycle()
    val records by m.records.collectAsStateWithLifecycle()
    val language = preferences.language
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < .5f
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Two equal columns: 2 × ((available width - 40 - 12) / 2) + 12 + 40.
        // A large system font switches to a single column instead of clipping labels.
        val columns = when {
            LocalDensity.current.fontScale >= 1.5f || maxWidth < 340.dp -> 1
            maxWidth >= 700.dp -> 3
            else -> 2
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.widthIn(max = 900.dp).fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HomeIntro(language, dark)
                Text(
                    tr(language, "Que souhaitez-vous faire ?", "What would you like to do?"),
                    modifier = Modifier.semantics { heading() },
                    fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = (-.3).sp
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    homeCategories.chunked(columns).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { item ->
                                CategoryTile(item, language, dark, Modifier.weight(1f).fillMaxHeight()) {
                                    category(item.category)
                                }
                            }
                        }
                    }
                }
                InspirationBanner(language, templates)
                records.firstOrNull { it.draft }?.let { record ->
                    Panel {
                        Text(tr(language, "Reprendre un brouillon", "Continue a draft"), style = MaterialTheme.typography.titleMedium)
                        Text(record.title)
                        Action(tr(language, "Continuer", "Continue")) { resume(record) }
                    }
                }
                val recent = records.filter { !it.draft }.take(3)
                if (recent.isNotEmpty()) {
                    Text(tr(language, "Récemment enregistrés", "Recently saved"), style = MaterialTheme.typography.titleLarge)
                    recent.forEach { record ->
                        CenteredOutlinedButton({ resume(record) }, Modifier.fillMaxWidth()) { Text(record.title) }
                    }
                }
                // Recommendations remain available below the reference's first-screen composition.
                Text(tr(language, "Modèles recommandés", "Recommended templates"), style = MaterialTheme.typography.titleMedium)
                m.templates.filter { it.id in listOf("write_01", "document_01", "analyze_01") }.forEach { template ->
                    SecondaryButton({ start(template) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(template.title.at(language)) }
                }
                Text(
                    tr(language, "Assemblage local par règles et modèles. Aucun appel à une IA.", "Assembled locally using rules and templates. No AI calls."),
                    color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HomeIntro(language: String, dark: Boolean) {
    val largeFont = LocalDensity.current.fontScale >= 1.35f || LocalConfiguration.current.screenWidthDp < 380
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 132.dp)) {
            if (!largeFont) PromptSheets(
                Modifier.align(Alignment.TopEnd).padding(top = 8.dp).size(112.dp, 126.dp), dark
            )
            Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text("PROMPT BUILDER", color = colors.primary, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    tr(language, "Une idée.\nUn prompt clair.", "One idea.\nOne clear prompt."),
                    Modifier.fillMaxWidth().padding(end = if (largeFont) 0.dp else 68.dp),
                    fontSize = 27.sp, lineHeight = 32.sp,
                    letterSpacing = (-.7).sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    tr(language, "Créez le bon prompt, simplement.", "Build the right prompt, simply."),
                    color = colors.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CategoryTile(item: HomeCategory, language: String, dark: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val accent = when (item.category) {
        Category.WRITE -> if (dark) Color(0xFFB8BAFF) else Color(0xFF6057DB)
        Category.ANALYZE -> if (dark) Color(0xFF7BD4D2) else Color(0xFF138186)
        Category.DOCUMENT -> if (dark) Color(0xFF8DC5FF) else Color(0xFF287BC1)
        Category.IDEAS -> if (dark) Color(0xFFFFBD92) else Color(0xFFAF591F)
        Category.DEVELOP -> if (dark) Color(0xFFD3AFF2) else Color(0xFF8453B2)
        Category.IMPROVE -> if (dark) Color(0xFFF3ABBF) else Color(0xFFB15170)
    }
    val tint = when (item.category) {
        Category.WRITE -> Color(0xFFEDEAFF)
        Category.ANALYZE -> Color(0xFFE5F2F2)
        Category.DOCUMENT -> Color(0xFFE4F0FF)
        Category.IDEAS -> Color(0xFFFFE9DF)
        Category.DEVELOP -> Color(0xFFF0E9FA)
        Category.IMPROVE -> Color(0xFFFFE7EC)
    }
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 124.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = if (dark) .9f else .75f))
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Surface(
                color = if (dark) accent.copy(alpha = .13f) else tint,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    if (item.category == Category.IMPROVE) Icons.Outlined.AutoAwesome else icon(item.category),
                    contentDescription = null, tint = accent,
                    modifier = Modifier.padding(9.dp).size(24.dp)
                )
            }
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(item.title.at(language), Modifier.weight(1f), fontSize = 16.sp,
                    lineHeight = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.4).sp)
                Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp), tint = colors.onSurface)
            }
            Spacer(Modifier.height(4.dp))
            Text(item.subtitle.at(language), fontSize = 14.sp, lineHeight = 18.sp,
                color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun InspirationBanner(language: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(28.dp), tint = colors.primary)
            Box(Modifier.width(1.dp).height(36.dp).background(colors.primary.copy(alpha = .25f)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(tr(language, "Besoin d’inspiration ?", "Need inspiration?"), fontSize = 17.sp,
                    lineHeight = 21.sp, fontWeight = FontWeight.Bold)
                Text(tr(language, "Explorez les modèles prêts à adapter.", "Explore templates ready to adapt."),
                    color = colors.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
            }
            Icon(Icons.Outlined.ChevronRight, null, Modifier.size(22.dp))
        }
    }
}

/** Native vector illustration: three sheets turning an idea into a structured prompt. */
@Composable
private fun PromptSheets(modifier: Modifier, dark: Boolean) {
    Canvas(modifier) {
        withTransform({ scale(size.width / 140f, size.height / 160f, Offset.Zero) }) {
            fun sheet(y: Float): Path = Path().apply {
                moveTo(54f, y); quadraticTo(60f, y - 5f, 69f, y - 4f)
                lineTo(132f, y + 3f); quadraticTo(141f, y + 4f, 135f, y + 13f)
                lineTo(94f, y + 70f); quadraticTo(88f, y + 78f, 77f, y + 77f)
                lineTo(15f, y + 67f); quadraticTo(4f, y + 65f, 12f, y + 55f); close()
            }
            drawPath(sheet(71f), Color(if (dark) 0xFF4C497F else 0xFFD8D5FF))
            drawPath(sheet(51f), Brush.linearGradient(listOf(
                Color(if (dark) 0xFF6965A5 else 0xFFEAE8FF),
                Color(if (dark) 0xFF7773B3 else 0xFFBDB9FF)), Offset(20f,50f), Offset(125f,122f)))
            drawPath(sheet(28f), Brush.linearGradient(listOf(
                Color(if (dark) 0xFF414969 else 0xFFFFFFFF),
                Color(if (dark) 0xFF535B83 else 0xFFF5F5FF))))
            drawPath(sheet(28f), Color(if (dark) 0xFF9290C8 else 0xFFE2E0FF), style = Stroke(.6f))
            val line = Color(if (dark) 0xFFB8BAFF else 0xFFAEA9FF)
            drawLine(line, Offset(56f,52f), Offset(104f,57f), 4.5f, StrokeCap.Round)
            drawLine(line.copy(alpha = .8f), Offset(48f,64f), Offset(94f,69f), 4f, StrokeCap.Round)
            drawLine(line.copy(alpha = .6f), Offset(40f,76f), Offset(73f,80f), 4f, StrokeCap.Round)
            val star = Path().apply {
                moveTo(116f,1f); quadraticTo(119f,16f,131f,19f)
                quadraticTo(119f,22f,115f,36f); quadraticTo(111f,23f,101f,20f)
                quadraticTo(112f,16f,116f,1f); close()
            }
            drawPath(star, Brush.linearGradient(listOf(Color(0xFFD3CFFF), Color(0xFF9991EE))))
            drawLine(line.copy(alpha = .5f), Offset(48f,12f), Offset(51f,20f), 3f, StrokeCap.Round)
            drawLine(line.copy(alpha = .5f), Offset(33f,18f), Offset(39f,25f), 3f, StrokeCap.Round)
            drawLine(line.copy(alpha = .5f), Offset(24f,31f), Offset(33f,35f), 3f, StrokeCap.Round)
        }
    }
}

@Composable
fun DatasightNavigation(tabs: List<Pair<String, String>>, route: String, select: (String) -> Unit) {
    val largeFont = LocalDensity.current.fontScale >= 1.5f
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.background, tonalElevation = 0.dp) {
        Column {
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = .4f))
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 6.dp)
                    .selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val icons = listOf(Icons.Outlined.AddCircleOutline, Icons.Outlined.GridView,
                    Icons.Outlined.Bookmarks, Icons.Outlined.Business)
                tabs.forEachIndexed { index, (id, label) ->
                    val selected = route == id
                    Column(
                        Modifier.weight(1f).semantics { if (largeFont) contentDescription = label }.selectable(selected = selected, role = Role.Tab,
                            onClick = { select(id) })
                            .background(if (selected) colors.primary else Color.Transparent, RoundedCornerShape(17.dp))
                            .heightIn(min = 58.dp).padding(horizontal = 2.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val ink = if (selected) colors.onPrimary else colors.onSurfaceVariant
                        Icon(icons[index], null, Modifier.size(23.dp), tint = ink)
                        if (!largeFont) Text(label, color = ink, fontSize = 11.sp, lineHeight = 14.sp, textAlign = TextAlign.Center,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}
