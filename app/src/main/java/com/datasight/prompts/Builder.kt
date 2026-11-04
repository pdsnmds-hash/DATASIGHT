package com.datasight.prompts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable fun Builder(m:AppModel,done:()->Unit) {
 val p by m.prefs.collectAsStateWithLifecycle();val f by m.form.collectAsStateWithLifecycle();val l=p.language;val t=m.template();var validation by rememberSaveable{mutableStateOf(false)};var expanded by rememberSaveable{mutableStateOf(false)};var conflict by remember{mutableStateOf(false)}
 fun generate(){m.generate();done()}
 val fields=(t?.fields.orEmpty()+specificFields(f.category)).distinctBy{it.id}
 val errors=FormValidator.errors(f,t)
 androidx.activity.compose.BackHandler(enabled=f.step>0){m.update{it.copy(step=it.step-1)}}
 Page {
 Text(tr(l,"Étape ${f.step+1} sur 3","Step ${f.step+1} of 3"),color=MaterialTheme.colorScheme.primary)
 LinearProgressIndicator(progress={(f.step+1)/3f},modifier=Modifier.fillMaxWidth())
 Heading(when(f.step){0->tr(l,"Définissez votre tâche","Define your task");1->tr(l,"Ajoutez le bon contexte","Add useful context");else->tr(l,"Choisissez le résultat","Choose your output")},f.category.label.at(l))
 when(f.step){
 0->{
  Text(tr(l,"Type de tâche","Task type"),style=MaterialTheme.typography.titleMedium)
  Choices(m.templates.filter{it.category==f.category}.map{it.id to it.title.at(l)},f.templateId.orEmpty(),{id->m.update{it.copy(templateId=id,subtype=id)}})
  if(f.category==Category.IMPROVE){Input(tr(l,"Prompt original *","Original prompt *"),f.original,{v->m.update{it.copy(original=v)}},error=validation&&"original" in errors,minLines=5);Text(tr(l,"Que souhaitez-vous préciser ?","What would you like to clarify?"));val opts=listOf("goal" to tr(l,"Objectif","Goal"),"context" to tr(l,"Contexte","Context"),"format" to tr(l,"Format","Format"),"constraints" to tr(l,"Contraintes","Constraints"),"criteria" to tr(l,"Critères","Criteria"));opts.forEach{(id,label)->FilterChip(id in f.improvements,{m.update{it.copy(improvements=if(id in it.improvements)it.improvements-id else it.improvements+id)}},label={Text(label)})}}
  Input(tr(l,"Quel résultat souhaitez-vous ? *","What result do you need? *"),f.task,{v->m.update{it.copy(task=v)}},t?.title?.at(l).orEmpty(),validation&&"task" in errors)
  t?.fields?.forEach{field->Input(field.label.at(l)+if(field.required)" *" else "",f.values[field.id].orEmpty(),{v->m.update{it.copy(values=it.values+(field.id to v))}},field.example.at(l),validation&&field.id in errors,minLines=3)}
 }
 1->{
  if(f.category!=Category.DEVELOP)Input(tr(l,"Pour qui ?","Who is it for?"),f.audience,{v->m.update{it.copy(audience=v)}},tr(l,"Équipe interne, client, nouveau collègue…","Internal team, customer, new colleague…"),minLines=1)
  Input(tr(l,"Quelle est la situation ?","What is the situation?"),f.context,{v->m.update{it.copy(context=v)}},tr(l,"Ce qui a déjà eu lieu et le besoin actuel","What happened and what is needed now"))
  Input(if(f.category==Category.DEVELOP)tr(l,"Code existant (facultatif)","Existing code (optional)") else tr(l,"Faits ou matériaux disponibles","Available facts or materials"),f.materials,{v->m.update{it.copy(materials=v)}},minLines=4)
  specificFields(f.category).forEach{field->Input(field.label.at(l),f.values[field.id].orEmpty(),{v->m.update{it.copy(values=it.values+(field.id to v))}},field.example.at(l))}
  Input(tr(l,"Exigences particulières","Special requirements"),f.requirements,{v->m.update{it.copy(requirements=v)}})
 }
 else->{
  Text(tr(l,"Format de réponse","Response format"));Choices(PromptComposer.formats.map{it to PromptComposer.format(it,l)},f.format,{v->m.update{it.copy(format=v)}})
  if(f.format=="json")Input(tr(l,"Structure JSON (facultatif)","JSON structure (optional)"),f.jsonSchema,{v->m.update{it.copy(jsonSchema=v)}},"""{"summary": "string", "actions": []}""",minLines=3)
  Text(tr(l,"Langue de réponse","Response language"));Choices(listOf("fr" to "French / Français","en" to "English"),f.language,{v->m.update{it.copy(language=v)}})
  Text(tr(l,"Longueur","Length"));Choices(listOf("short" to tr(l,"Concis","Short"),"detailed" to tr(l,"Détaillé","Detailed"),"custom" to tr(l,"Personnalisé","Custom")),f.length,{v->m.update{it.copy(length=v)}})
  if(f.length=="custom")Input(tr(l,"Limite personnalisée *","Custom limit *"),f.customLength,{v->m.update{it.copy(customLength=v)}},tr(l,"200 mots maximum","At most 200 words"),validation&&"customLength" in errors,minLines=1)
  if(f.category==Category.WRITE){Text(tr(l,"Ton","Tone"));Choices(listOf("neutral" to tr(l,"Neutre","Neutral"),"warm" to tr(l,"Chaleureux","Warm"),"formal" to tr(l,"Formel","Formal")),f.tone,{v->m.update{it.copy(tone=v)}})}
  SecondaryButton({expanded=!expanded}){Text(tr(l,"Réglages supplémentaires","Additional settings")+if(expanded)" −" else " +")}
  if(expanded){Input(tr(l,"Rôle de l’exécutant","Performer role"),f.role,{v->m.update{it.copy(role=v)}},minLines=1);Input(tr(l,"Contraintes","Constraints"),f.constraints,{v->m.update{it.copy(constraints=v)}});Input(tr(l,"À exclure","Exclude"),f.exclude,{v->m.update{it.copy(exclude=v)}});Input(tr(l,"Exemple de résultat","Desired example"),f.example,{v->m.update{it.copy(example=v)}});Row{Checkbox(f.questions,{v->m.update{it.copy(questions=v)}});Text(tr(l,"Poser les questions nécessaires","Ask necessary clarification questions"),Modifier.padding(top=12.dp))};Row{Checkbox(f.unknown,{v->m.update{it.copy(unknown=v)}});Text(tr(l,"Signaler inconnues et hypothèses","Identify unknowns and assumptions"),Modifier.padding(top=12.dp))}}
 }
 }
 if(validation&&errors.isNotEmpty())Text(tr(l,"Complétez les champs obligatoires avant de continuer.","Complete required fields before continuing."),color=MaterialTheme.colorScheme.error)
 Action(if(f.step<2)tr(l,"Continuer","Continue") else tr(l,"Créer le prompt","Create prompt")){
  validation=true
  val invalid=if(f.step==0)errors.filter{it!="customLength"} else if(f.step==2)errors else emptyList()
  if(invalid.isEmpty()){validation=false;if(f.step<2)m.update{it.copy(step=it.step+1)} else if(f.output.isNotBlank()&&f.output!=f.baseline)conflict=true else generate()}
 }
 if(f.step>0)CenteredOutlinedButton({validation=false;m.update{it.copy(step=it.step-1)}},Modifier.fillMaxWidth()){Text(tr(l,"Étape précédente","Previous step"))}
 Text(tr(l,"Votre brouillon est enregistré automatiquement sur cet appareil.","Your draft is saved automatically on this device."),color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
 if(conflict)AlertDialog(onDismissRequest={conflict=false},title={Text(tr(l,"Conserver votre modification ?","Keep your edit?"))},text={Text(tr(l,"La nouvelle génération remplacera le texte modifié. Vous pouvez d’abord enregistrer cette version séparément.","Generating again will replace the edited text. You can save this version separately first."))},confirmButton={SecondaryButton({conflict=false;generate()}){Text(tr(l,"Remplacer","Replace"))}},dismissButton={Column{SecondaryButton({m.update{it.copy(title=it.title.ifBlank{it.task.take(80)})};m.save(true){conflict=false;generate()}}){Text(tr(l,"Enregistrer puis générer","Save then generate"))};SecondaryButton({conflict=false}){Text(tr(l,"Annuler","Cancel"))}}})
}
