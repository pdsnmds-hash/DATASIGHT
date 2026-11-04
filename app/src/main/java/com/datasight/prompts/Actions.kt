package com.datasight.prompts

import android.content.*
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun launchExternal(ctx:Context,intent:Intent,onError:()->Unit) { try{ctx.startActivity(intent)}catch(e:ActivityNotFoundException){onError()}catch(e:SecurityException){onError()} }
@Composable fun Result(m:AppModel,edit:()->Unit,service:(Int)->Unit) {
 val p by m.prefs.collectAsStateWithLifecycle();val f by m.form.collectAsStateWithLifecycle();val l=p.language;val clip=LocalClipboardManager.current;val ctx=LocalContext.current;var structure by rememberSaveable{mutableStateOf(false)};var showOriginal by rememberSaveable{mutableStateOf(false)};var share by remember{mutableStateOf(false)};var save by remember{mutableStateOf(false)}
 Page{
 Heading(tr(l,"Votre prompt est prêt","Your prompt is ready"),tr(l,"Relisez-le et adaptez-le avant de l’utiliser.","Review and adapt it before use."))
 Input(tr(l,"Prompt modifiable","Editable prompt"),f.output,{v->m.update{it.copy(output=v)}},minLines=12)
 Text("${f.output.length} "+tr(l,"caractères","characters"),color=MaterialTheme.colorScheme.onSurfaceVariant)
 Action(tr(l,"Copier","Copy")){clip.setText(AnnotatedString(f.output));m.message.value=tr(l,"Prompt copié","Prompt copied")}
 Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){CenteredOutlinedButton({save=true},Modifier.weight(1f)){Text(tr(l,"Enregistrer","Save"))};CenteredOutlinedButton({share=true},Modifier.weight(1f)){Text(tr(l,"Partager","Share"))}}
 CenteredOutlinedButton(edit,Modifier.fillMaxWidth()){Text(tr(l,"Modifier les paramètres","Edit settings"))}
 SecondaryButton({structure=!structure}){Text(tr(l,"Comprendre la structure","Understand the structure"))}
 if(structure)Panel{Text(tr(l,"Le prompt regroupe la tâche, le contexte fourni, les matériaux, les exigences, le format et les critères de vérification. Seuls les blocs remplis sont inclus. Les séparateurs distinguent vos sources des instructions ; ils ne garantissent pas l’absence de prompt injection.","The prompt groups the task, supplied context, materials, requirements, format and verification criteria. Only filled blocks appear. Separators distinguish sources from instructions; they do not guarantee protection from prompt injection."))}
 val suggestions=buildList{if(f.context.isBlank())add(tr(l,"Préciser la situation de départ.","Add the starting situation."));if(f.materials.isBlank())add(tr(l,"Joindre des faits ou des sources disponibles.","Add available facts or sources."));if(f.constraints.isBlank())add(tr(l,"Définir les limites utiles à cette tâche.","Define useful boundaries for this task."))}
 if(suggestions.isNotEmpty())Panel{Text(tr(l,"Pour aller plus loin","What you can clarify"),style=MaterialTheme.typography.titleMedium);suggestions.forEach{Text("• $it")}}
 if(f.original.isNotBlank()){SecondaryButton({showOriginal=!showOriginal}){Text(tr(l,"Voir le prompt original","View original prompt"))};if(showOriginal)Panel{Text(f.original);CenteredOutlinedButton({m.update{it.copy(output=it.original)}},Modifier.fillMaxWidth()){Text(tr(l,"Restaurer comme résultat","Restore as result"))}}}
 m.template()?.service?.let{Help(l,it,service)}
 }
 if(save)AlertDialog(onDismissRequest={save=false},title={Text(tr(l,"Enregistrer le prompt","Save prompt"))},text={Input(tr(l,"Titre *","Title *"),f.title,{v->m.update{it.copy(title=v)}},minLines=1)},confirmButton={SecondaryButton({m.save{save=false}},enabled=f.title.isNotBlank()&&f.output.isNotBlank()){Text(tr(l,"Enregistrer","Save"))}},dismissButton={SecondaryButton({save=false}){Text(tr(l,"Annuler","Cancel"))}})
 if(share)PreviewDialog(l,f.output,{share=false}){launchExternal(ctx,Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,f.output)},tr(l,"Partager le prompt","Share prompt"))){m.message.value=tr(l,"Aucune application compatible.","No compatible app available.")};share=false}
}
@Composable fun PreviewDialog(l:String,text:String,cancel:()->Unit,confirm:()->Unit) { AlertDialog(onDismissRequest=cancel,title={Text(tr(l,"Aperçu avant de continuer","Preview before continuing"))},text={androidx.compose.foundation.lazy.LazyColumn{item{Text(text)}}},confirmButton={SecondaryButton(confirm){Text(tr(l,"Continuer","Continue"))}},dismissButton={SecondaryButton(cancel){Text(tr(l,"Annuler","Cancel"))}}) }
