package com.datasight.prompts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val serviceTitles = listOf(
    Bi("Assistance administrative", "Administrative support"),
    Bi("Relation client", "Customer support"),
    Bi("Administration RH", "HR administration"),
    Bi("Gestion de l’information", "Information management"),
    Bi("Administration financière", "Financial administration")
)
private val serviceSubtitles = listOf(
    Bi("Agendas, e-mails et documents", "Calendars, emails and documents"),
    Bi("Échanges et suivi des demandes", "Communication and inquiry follow-up"),
    Bi("Intégration et dossiers du personnel", "Onboarding and personnel records"),
    Bi("Saisie, CRM et archives numériques", "Data entry, CRM and digital archives"),
    Bi("Factures et suivi des paiements", "Invoices and payment tracking")
)
private val serviceIcons = listOf(Icons.Outlined.CalendarMonth, Icons.Outlined.HeadsetMic,
    Icons.Outlined.PeopleOutline, Icons.Outlined.Storage, Icons.Outlined.Description)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompanyScreen(m: AppModel, selected: Int?, openService: (Int) -> Unit = {}) {
    val preferences by m.prefs.collectAsStateWithLifecycle()
    val form by m.form.collectAsStateWithLifecycle()
    val language = preferences.language
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < .5f
    val largeFont = LocalDensity.current.fontScale >= 1.35f || LocalConfiguration.current.screenWidthDp < 380
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var service by rememberSaveable { mutableStateOf(selected?.takeIf { it in Company.services.indices } ?: 0) }
    var inquiry by rememberSaveable { mutableStateOf(false) }
    var includePrompt by rememberSaveable { mutableStateOf(false) }
    var preview by rememberSaveable { mutableStateOf(false) }
    var about by rememberSaveable { mutableStateOf(false) }
    var task by rememberSaveable { mutableStateOf(if (selected != null) form.title.ifBlank { form.task } else "") }
    fun external(intent: Intent) {
        launchExternal(context, intent) {
            m.message.value = tr(language, "Aucune application compatible. Vous pouvez copier les coordonnées.",
                "No compatible app. You can copy the contact details.")
        }
    }
    val body = buildString {
        append(tr(language, "Bonjour DATASIGHT,\n\nJe souhaite des informations sur : ",
            "Hello DATASIGHT,\n\nI would like information about: "))
        append(Company.services[service].first.at(language))
        if (task.isNotBlank()) append("\n" + tr(language, "Tâche : ", "Task: ") + task)
        if (includePrompt && form.output.isNotBlank()) append("\n\n" + form.output)
    }

    if (selected != null && selected in Company.services.indices) {
        ServiceDetailScreen(language, selected) { service = selected; includePrompt = false; inquiry = true }
    } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 840.dp).fillMaxWidth(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "company_intro") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(tr(language, "À PROPOS DE DATASIGHT", "ABOUT DATASIGHT"), color = colors.primary,
                        fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                    Box(Modifier.fillMaxWidth()) {
                        if (!largeFont) Box(Modifier.matchParentSize(), contentAlignment = Alignment.TopEnd) {
                            CompanyFolder(Modifier.padding(top = 4.dp).size(116.dp, 146.dp), dark)
                        }
                        Column(Modifier.fillMaxWidth().padding(end = if (largeFont) 0.dp else 128.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(tr(language, "Votre quotidien,\nplus simple.", "Everyday work,\nmade simpler."),
                                fontSize = 27.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.6).sp)
                            Text(tr(language, "Nous accompagnons les startups et les PME dans leurs tâches administratives et opérationnelles.",
                                "We support startups and small businesses with their administrative and operational tasks."),
                                fontSize = 14.sp, lineHeight = 19.sp, color = colors.onSurfaceVariant)
                        }
                    }
                    CenteredButton({ includePrompt = false; inquiry = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(tr(language, "Parlons de vos besoins", "Let’s talk about your needs"), Modifier.weight(1f, fill = false), fontSize = 14.sp)
                        Spacer(Modifier.width(12.dp)); Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(20.dp))
                    }
                    Text(tr(language, "Nos domaines d’accompagnement", "What we can help with"), fontSize = 20.sp,
                        lineHeight = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.4).sp)
                }
            }
            items(Company.services.size, key = { "company_service_$it" }) { index ->
                val accents = listOf(0xFF8E49D0, 0xFF008C79, 0xFFBD5C25, 0xFF167BB4, 0xFF6352D8)
                val lightInks = listOf(0xFFD3AFF2, 0xFF7BD9C1, 0xFFFFBD92, 0xFF8DC5FF, 0xFFB8BAFF)
                val ink = Color(if (dark) lightInks[index] else accents[index])
                Card(onClick = { openService(index) }, modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                    shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .75f))) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = ink.copy(alpha = if (dark) .14f else .11f), shape = RoundedCornerShape(12.dp)) {
                            Icon(serviceIcons[index], null, Modifier.padding(9.dp).size(23.dp), tint = ink)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(serviceTitles[index].at(language), fontSize = 16.sp, lineHeight = 20.sp,
                                letterSpacing = (-.3).sp, fontWeight = FontWeight.Bold)
                            Text(serviceSubtitles[index].at(language), fontSize = 12.sp, lineHeight = 17.sp,
                                color = colors.onSurfaceVariant)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp), tint = colors.onSurfaceVariant)
                    }
                }
            }
            item(key = "company_contacts") {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface), border = BorderStroke(1.dp, colors.outlineVariant)) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(tr(language, "Échangeons sur votre projet", "Let’s discuss your project"), fontSize = 19.sp,
                            lineHeight = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        ContactRow(Icons.Outlined.Email, Company.email, colors.primary, tr(language, "Préparer un email", "Prepare an email")) { includePrompt = false; inquiry = true }
                        ContactRow(Icons.Outlined.Phone, Company.phone, if (dark) Color(0xFF8DC5FF) else Color(0xFF167BB4),
                            tr(language, "Appeler", "Call")) {
                            external(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Company.phone}")))
                        }
                        ContactRow(Icons.Outlined.LocationOn, Company.address, colors.onSurfaceVariant,
                            tr(language, "Ouvrir la carte", "Open map")) {
                            external(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(Company.address))))
                        }
                    }
                }
            }
            item(key = "company_information") {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton({ about = true }) { Text(tr(language, "À propos de DATASIGHT", "About DATASIGHT"), fontSize = 12.sp) }
                    SecondaryButton({
                        clipboard.setText(AnnotatedString("${Company.email}\n${Company.phone}\n${Company.address}"))
                        m.message.value = tr(language, "Coordonnées copiées", "Contact details copied")
                    }) { Text(tr(language, "Copier les coordonnées", "Copy contact details"), fontSize = 12.sp) }
                }
            }
        }
    }

    if (about) AlertDialog(onDismissRequest = { about = false }, title = { Text("DATASIGHT") },
        text = { Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(Company.description.at(language))
            Text(tr(language, "Comment nous contacter", "How to get in touch"), fontWeight = FontWeight.Bold)
            Text(tr(language, "1. Décrivez la tâche et le service souhaité.\n2. Vérifiez les informations à partager.\n3. Ouvrez votre email pour échanger sur le périmètre et les modalités.",
                "1. Describe the task and desired service.\n2. Review what you want to share.\n3. Open your email to discuss scope and arrangements."))
        } }, confirmButton = { SecondaryButton({ about = false }) { Text(tr(language, "Fermer", "Close")) } })
    if (inquiry) AlertDialog(onDismissRequest = { inquiry = false }, title = { Text(tr(language, "Votre demande", "Your inquiry")) },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr(language, "Choisissez un domaine", "Choose a service"), fontWeight = FontWeight.SemiBold)
                serviceTitles.forEachIndexed { index, label ->
                    Surface(onClick = { service = index }, color = if (service == index) colors.primaryContainer else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(service == index, { service = index }); Text(label.at(language), fontSize = 14.sp, lineHeight = 18.sp)
                        }
                    }
                }
                Input(tr(language, "Nom de la tâche (facultatif)", "Task name (optional)"), task, { task = it }, minLines = 1)
                if (form.output.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(includePrompt, { includePrompt = it })
                    Text(tr(language, "Joindre le prompt complet", "Include the full prompt"), fontSize = 14.sp)
                }
                Text(tr(language, "Seuls le nom de la tâche et le service sont inclus par défaut. Aucun envoi automatique.",
                    "Only the task name and service are included by default. Nothing is sent automatically."),
                    fontSize = 13.sp, lineHeight = 18.sp, color = colors.onSurfaceVariant)
            }
        }, confirmButton = { SecondaryButton({ inquiry = false; preview = true }) { Text(tr(language, "Vérifier l’email", "Review email")) } },
        dismissButton = { SecondaryButton({ inquiry = false }) { Text(tr(language, "Annuler", "Cancel")) } })
    if (preview) AlertDialog(onDismissRequest = { preview = false; inquiry = true },
        title = { Text(tr(language, "Vérifiez votre email", "Review your email")) },
        text = { Text("${Company.email}\n\n$body", Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) },
        confirmButton = { SecondaryButton({
            external(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Company.email}?subject=" + Uri.encode("DATASIGHT — " + task) + "&body=" + Uri.encode(body))))
            preview = false
        }) { Text(tr(language, "Ouvrir l’application email", "Open email app")) } },
        dismissButton = { SecondaryButton({ preview = false; inquiry = true }) { Text(tr(language, "Retour", "Back")) } })

}

@Composable
private fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, accent: Color, action: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = colors.onSurfaceVariant)
        Text(text, Modifier.weight(1f), fontSize = 13.sp, lineHeight = 18.sp, color = colors.onSurfaceVariant)
        FilledTonalIconButton(onClick, Modifier.size(48.dp), shape = RoundedCornerShape(12.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = accent.copy(alpha = .11f), contentColor = accent)) {
            Icon(if (icon == Icons.Outlined.LocationOn) Icons.Outlined.OpenInNew else icon, action, Modifier.size(21.dp))
        }
    }
}

/** Small native vector illustration, decorative and independent of screen density. */
@Composable
private fun CompanyFolder(modifier: Modifier, dark: Boolean) {
    Canvas(modifier) {
        withTransform({ scale(size.width / 160f, size.height / 190f, Offset.Zero) }) {
            val pale = Color(if (dark) 0xFF303453 else 0xFFF0EDFF)
            drawRoundRect(pale, Offset(25f, 14f), Size(125f, 162f), CornerRadius(45f, 45f))
            val loop = Path().apply {
                moveTo(56f, 42f); cubicTo(65f, 19f, 142f, 27f, 143f, 60f)
                lineTo(143f, 112f); cubicTo(143f, 154f, 155f, 171f, 109f, 177f)
                cubicTo(78f, 181f, 19f, 166f, 15f, 145f); lineTo(15f, 104f)
                cubicTo(15f, 92f, 37f, 92f, 53f, 92f)
            }
            drawPath(loop, Color(if (dark) 0xFF9690CD else 0xFFA49CDE), style = Stroke(1.2f))
            drawRoundRect(Color(if (dark) 0xFF477A75 else 0xFFBEF0E6), Offset(26f, 14f), Size(44f, 28f), CornerRadius(12f,12f))
            drawCircle(Color(0xFFC9A4F0), 6f, Offset(124f, 31f))
            val back = Path().apply {
                moveTo(38f, 71f); quadraticTo(40f, 67f, 48f, 67f); lineTo(125f, 72f)
                quadraticTo(137f, 72f, 133f, 84f); lineTo(119f, 153f); lineTo(35f, 153f); close()
            }
            drawPath(back, Brush.linearGradient(listOf(Color(0xFF5156D8), Color(0xFF7C71E8))))
            withTransform({ rotate(6f, Offset(84f, 94f)) }) {
                drawRoundRect(Color(if (dark) 0xFF8480B9 else 0xFFD2CCFC), Offset(53f,51f), Size(72f,95f), CornerRadius(10f,10f))
                drawRoundRect(Color(if (dark) 0xFF444A6A else 0xFFFAF9FF), Offset(57f,55f), Size(64f,87f), CornerRadius(7f,7f))
                repeat(3) { index -> drawRoundRect(Color(if (dark) 0xFFB8BAFF else 0xFFD9D2FF),
                    Offset(65f,65f + index * 11f), Size(if (index == 2) 35f else 44f,4f), CornerRadius(2f,2f)) }
            }
            val front = Path().apply {
                moveTo(36f, 81f); lineTo(61f, 82f); quadraticTo(67f, 81f, 69f, 88f)
                lineTo(113f, 90f); quadraticTo(120f, 90f, 119f, 99f); lineTo(116f, 145f)
                quadraticTo(115f, 153f, 107f, 153f); lineTo(43f, 153f)
                quadraticTo(34f, 153f, 33f, 144f); lineTo(29f, 92f); quadraticTo(28f, 81f, 36f, 81f); close()
            }
            drawPath(front, Brush.linearGradient(listOf(Color(0xFFB1A5F2), Color(0xFF9790E9)), Offset(32f,85f), Offset(118f,155f)))
            drawRoundRect(Color(if (dark) 0xFF444A6A else 0xFFF6F4FF), Offset(49f,114f), Size(50f,27f), CornerRadius(6f,6f))
            drawRoundRect(Color(0xFFD8D1FF), Offset(56f,123f), Size(36f,3f), CornerRadius(2f,2f))
            drawRoundRect(Color(0xFFD8D1FF), Offset(56f,130f), Size(21f,3f), CornerRadius(2f,2f))
            drawCircle(Color(0xFF8FDDD0),6f,Offset(15f,114f))
            drawRoundRect(Color(if (dark) 0xFFB87F66 else 0xFFFFD2BC), Offset(126f,124f), Size(32f,30f), CornerRadius(12f,12f))
            drawCircle(Color(0xFFFFBD9F),6f,Offset(110f,172f))
        }
    }
}
