package com.datasight.prompts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.brand_logo), null, modifier)
}

@Composable
fun OnboardingScreen(m: AppModel) {
    val preferences by m.prefs.collectAsStateWithLifecycle()
    val l = preferences.language
    var step by rememberSaveable { mutableIntStateOf(0) }
    var privacy by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()
    LaunchedEffect(step) { scroll.scrollTo(0) }
    BackHandler(privacy || step > 0) { if (privacy) privacy = false else step-- }
    val titles = listOf(
        Bi("Une idée. Un prompt clair.", "One idea. One clear prompt."),
        Bi("Le bon cadre pour votre tâche", "The right structure for your task"),
        Bi("Vos prompts, à portée de main", "Your prompts, always at hand")
    )
    val descriptions = listOf(
        Bi("Choisissez parmi six types de tâches ou partez d’un des 60 modèles : rédiger, analyser, préparer un document, trouver des idées, développer ou améliorer un prompt.", "Choose from six task categories or start with one of 60 templates: write, analyze, prepare a document, generate ideas, develop or improve a prompt."),
        Bi("Décrivez votre objectif, ajoutez le contexte utile et choisissez le format et la langue de réponse. DATASIGHT assemble le prompt sur votre appareil à partir de règles et de modèles, sans appel à une IA.", "Describe your goal, add useful context and choose the response format and language. DATASIGHT assembles the prompt on your device using rules and templates, without calling an AI."),
        Bi("Relisez et modifiez le résultat, puis copiez-le ou enregistrez-le. Vos brouillons sont conservés sur cet appareil. Retrouvez aussi les services administratifs et opérationnels de DATASIGHT.", "Review and edit the result, then copy or save it. Your drafts stay on this device. You can also explore DATASIGHT’s administrative and operational services.")
    )
    if (privacy) {
        Scaffold(topBar = { SecondaryButton({ privacy = false }, Modifier.statusBarsPadding().padding(horizontal = 24.dp)) { Text(tr(l,"Retour à la présentation","Back to introduction")) } }) { padding ->
            Box(Modifier.padding(padding)) { PrivacyScreen(m) }
        }
        return
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxSize().verticalScroll(scroll).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrandMark(Modifier.size(40.dp))
                    Text("DATASIGHT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Choices(listOf("fr" to "Français", "en" to "English"), l) { m.settings(preferences.copy(language = it)) }
                if (step == 0) {
                    Image(painterResource(R.drawable.brand_splash), null,
                        Modifier.fillMaxWidth().heightIn(max = 280.dp).aspectRatio(1.5f).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
                } else {
                    Surface(Modifier.fillMaxWidth().height(180.dp), color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(24.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(if (step == 1) Icons.Outlined.Tune else Icons.Outlined.Bookmarks, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Text(tr(l, "DÉCOUVRIR · ${step + 1} / 3", "GET STARTED · ${step + 1} / 3"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                LinearProgressIndicator(progress = { (step + 1) / 3f }, modifier = Modifier.fillMaxWidth())
                Heading(titles[step].at(l), descriptions[step].at(l))
                if (step == 2) Panel {
                    Text(tr(l, "Local et sans compte", "Local, with no account"), fontWeight = FontWeight.Bold)
                    Text(tr(l, "Sans publicité ni analyse d’usage. Aucune transmission automatique. Si vous copiez ou partagez un prompt avec un service tiers, ses règles s’appliquent.", "No ads or usage analytics. Nothing is transmitted automatically. If you copy or share a prompt with a third-party service, its rules apply."))
                }
                Action(if (step == 2) tr(l,"Commencer","Get started") else tr(l,"Continuer","Continue")) { if (step < 2) step++ else m.finishOnboarding() }
                if (step > 0) SecondaryButton({ step-- }) { Text(tr(l,"Précédent","Previous")) }
                if (step < 2) SecondaryButton({ m.finishOnboarding() }) { Text(tr(l,"Passer la présentation","Skip introduction")) }
                SecondaryButton({ privacy = true }) { Text(tr(l,"Politique de confidentialité","Privacy policy")) }
            }
        }
    }
}
