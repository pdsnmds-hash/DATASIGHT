package com.datasight.prompts

import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable data class Bi(val fr: String, val en: String) { fun at(lang: String) = if(lang == "en") en else fr }
fun tr(lang: String, fr: String, en: String) = if(lang == "en") en else fr
val codec = Json { encodeDefaults = true; ignoreUnknownKeys = false }
@Serializable enum class Category(val label: Bi, val description: Bi) {
 WRITE(Bi("Rédiger", "Write"),Bi("Des mots justes pour votre quotidien.","Clear writing for everyday work.")),
 ANALYZE(Bi("Analyser","Analyze"),Bi("Comparer, comprendre et décider.","Compare, understand and decide.")),
 DOCUMENT(Bi("Préparer un document","Prepare a document"),Bi("Des documents utiles et organisés.","Useful, organized documents.")),
 IDEAS(Bi("Trouver des idées","Generate ideas"),Bi("Explorer de nouvelles possibilités.","Explore new possibilities.")),
 DEVELOP(Bi("Développer","Develop"),Bi("Décrire, expliquer et vérifier du code.","Describe, explain and verify code.")),
 IMPROVE(Bi("Améliorer un prompt","Improve a prompt"),Bi("Clarifier une instruction existante.","Clarify an existing instruction."))
}
@Serializable data class Field(val id: String, val label: Bi, val example: Bi, val required: Boolean = false)
@Serializable data class Template(val id: String, val category: Category, val title: Bi, val description: Bi, val use: Bi, val fields: List<Field>, val instruction: Bi, val example: Bi, val tips: List<Bi>, val service: Int? = null, val tags: List<Bi> = emptyList())
@Serializable data class Form(
 val category: Category = Category.WRITE, val subtype: String = "", val templateId: String? = null,
 val task: String = "", val values: Map<String,String> = emptyMap(), val audience: String = "", val context: String = "", val materials: String = "",
 val requirements: String = "", val format: String = "text", val language: String = "fr", val length: String = "short", val customLength: String = "", val tone: String = "neutral",
 val role: String = "", val constraints: String = "", val exclude: String = "", val example: String = "", val questions: Boolean = true, val unknown: Boolean = true,
 val jsonSchema:String = "", val original: String = "", val improvements: Set<String> = setOf("goal","context","format","constraints","criteria"), val step: Int = 0,
 val output: String = "", val baseline: String = "", val title: String = "", val recordId: String? = null
)
object FormValidator {
 fun errors(f: Form, template: Template?): List<String> = buildList {
  if(f.task.isBlank()) add("task")
  if(f.category == Category.IMPROVE && f.original.isBlank()) add("original")
  template?.fields?.filter { it.required && f.values[it.id].isNullOrBlank() }?.forEach { add(it.id) }
  if(f.length == "custom" && f.customLength.isBlank()) add("customLength")
 }
}
object PromptComposer {
 val formats = listOf("text","list","table","steps","json","code")
 fun format(id:String, lang:String) = when(id) {
  "list" -> tr(lang,"Liste","List"); "table" -> tr(lang,"Tableau","Table"); "steps" -> tr(lang,"Étapes numérotées","Numbered steps"); "json" -> "JSON"; "code" -> "Code"; else -> tr(lang,"Texte","Text")
 }
 fun compose(f: Form, t: Template?): String {
  val l = f.language
  val sections = mutableListOf<String>()
  fun block(fr:String,en:String,text:String) { if(text.isNotBlank()) sections += "## ${tr(l,fr,en)}\n${text.trim()}" }
  block("Tâche","Task", listOfNotNull(f.role.takeIf { it.isNotBlank() }?.let { tr(l,"Rôle : ","Role: ")+it },t?.instruction?.at(l), f.task).distinct().filter{it.isNotBlank()}.joinToString("\n"))
  block("Contexte","Context",listOf(f.context, f.audience.takeIf{it.isNotBlank()}?.let{tr(l,"Destinataire : ","Audience: ")+it}.orEmpty()).filter{it.isNotBlank()}.joinToString("\n"))
  if(f.original.isNotBlank()) block("Prompt original — texte conservé","Original prompt — preserved text","--- ORIGINAL ---\n${f.original}\n--- FIN / END ---")
  if(f.materials.isNotBlank()) block("Matériaux fournis","Source materials","--- SOURCE ---\n${f.materials}\n--- FIN / END ---")
  val values = (t?.fields.orEmpty()+specificFields(f.category)).distinctBy{it.id}.mapNotNull { field -> f.values[field.id]?.takeIf{it.isNotBlank()}?.let { "${field.label.at(l)} : $it" } }
  block("Exigences","Requirements",(values+listOf(f.requirements)).filter{it.isNotBlank()}.joinToString("\n"))
  block("Format de réponse","Response format",buildList {
   add(format(f.format,l)); if(f.format=="json" && f.jsonSchema.isNotBlank())add(tr(l,"Structure JSON demandée :\n","Requested JSON structure:\n")+f.jsonSchema); add(tr(l,"Langue de réponse : ","Response language: ")+if(f.language=="fr") "Français" else "English")
   add(when(f.length){"custom" -> f.customLength; "detailed" -> tr(l,"Détaillé, avec explications utiles.","Detailed, with useful explanations."); else -> tr(l,"Concis, sans omettre les points essentiels.","Concise, retaining essential points.")})
   if(f.category==Category.WRITE) add(tr(l,"Ton : ","Tone: ")+when(f.tone){"warm"->tr(l,"chaleureux","warm");"formal"->tr(l,"formel","formal");else->tr(l,"neutre et professionnel","neutral and professional")})
  }.joinToString("\n"))
  if(f.category==Category.IMPROVE && f.improvements.isNotEmpty()) block("Points à préciser","Clarification focus", f.improvements.sorted().joinToString(", "){when(it){"goal"->tr(l,"objectif","goal");"context"->tr(l,"contexte","context");"format"->tr(l,"format","format");"constraints"->tr(l,"contraintes","constraints");else->tr(l,"critères","criteria")}})
  block("Contraintes","Constraints",listOf(f.constraints,f.exclude.takeIf{it.isNotBlank()}?.let{tr(l,"À exclure : ","Exclude: ")+it}.orEmpty()).filter{it.isNotBlank()}.joinToString("\n"))
  block("Exemple attendu","Desired example", f.example)
  block("Critères de vérification","Verification criteria",buildList {
   add(tr(l,"Conserver les faits, nombres, noms et le code fournis. Ne pas inventer de faits.","Preserve supplied facts, numbers, names and code. Do not invent facts."))
   if(f.unknown) add(tr(l,"Signaler les données inconnues et distinguer les hypothèses des faits.","Identify unknown data and distinguish assumptions from facts."))
   if(f.questions) add(tr(l,"Si une information essentielle manque, poser des questions ciblées avant de conclure.","If essential information is missing, ask focused questions before concluding."))
   if(f.category==Category.DEVELOP) add(tr(l,"Vérifier les entrées, sorties et cas limites. Fournir des tests reproductibles et préciser ce qui n'a pas été exécuté.","Verify inputs, outputs and edge cases. Provide reproducible tests and state what was not executed."))
  }.distinct().joinToString("\n"))
  return sections.joinToString("\n\n")
 }
}
@Serializable data class Preferences(val language:String="fr", val theme:String="system", val favorites:Set<String> = emptySet())
@Serializable data class Backup(val schemaVersion:Int=1, val records:List<Record>, val settings:Preferences)
object BackupValidator {
 fun decodeUtf8(bytes:ByteArray):Backup {
  require(bytes.size<=20_000_000)
  val decoder=Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
   .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
  return decode(decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString())
 }
 fun decode(raw:String):Backup {
  require(raw.toByteArray().size<=20_000_000) { "size" }
  val b=codec.decodeFromString<Backup>(raw)
  require(b.schemaVersion==1 && b.records.size<=10000)
  require(b.settings.language in listOf("fr","en") && b.settings.theme in listOf("system","light","dark"))
  require(b.records.map{it.id}.distinct().size==b.records.size)
  b.records.forEach { require(it.id.isNotBlank() && it.language in listOf("fr","en") && it.created>0 && it.updated>=it.created); val f=codec.decodeFromString<Form>(it.parameters)
   require(f.language in listOf("fr","en") && f.step in 0..2 && f.format in PromptComposer.formats && f.length in listOf("short","detailed","custom"))
   require(f.category==it.category && f.language==it.language)
  }
  return b
 }
}
object Company {
 const val phone="+12086711146"; const val email="gfretyyjh@gmail.com"; const val address="229 RUE SAINT-HONORE, 75001 PARIS, France"
 val description=Bi("DATASIGHT aide les entreprises à simplifier leurs processus administratifs et opérationnels quotidiens. Nous accompagnons les startups, les PME et les organisations établies dans les tâches qui demandent précision, organisation et attention aux détails.","DATASIGHT helps businesses simplify everyday administrative and operational processes. We support startups, small and medium businesses and established organizations with tasks requiring precision, organization and attention to detail.")
 val services=listOf(
  Bi("Assistance administrative et bureautique","Administrative and office support") to Bi("Planification, calendriers, emails, préparation de documents, correspondance et recherche d'informations. Exemple : organiser un dossier de réunion et préparer son ordre du jour.","Scheduling, calendars, email, document preparation, correspondence and information research. Example: organize meeting materials and prepare an agenda."),
  Bi("Service client et assistance","Customer service and support") to Bi("Communication avec les clients par email, téléphone et chat. Exemple : préparer une réponse claire à une demande et organiser son suivi.","Customer communication by email, phone and chat. Example: prepare a clear response to an inquiry and organize follow-up."),
  Bi("Assistance RH et administration de la paie","HR support and payroll administration") to Bi("Intégration, documents RH, suivi des présences et coordination administrative. Exemple : préparer une liste d'accueil et rassembler les documents nécessaires.","Onboarding, HR documents, attendance and administrative coordination. Example: prepare a welcome checklist and collect required documents."),
  Bi("Saisie de données et gestion de l'information","Data entry and information management") to Bi("Saisie et vérification, mise à jour CRM, bases de données et archives numériques. Exemple : structurer un fichier clients et repérer les champs manquants.","Data entry and verification, CRM updates, databases and digital archives. Example: organize a customer file and identify missing fields."),
  Bi("Facturation et administration financière","Invoicing and financial administration") to Bi("Préparation des factures, suivi des paiements et organisation des pièces financières. Exemple : préparer une relance à partir des références de facture fournies.","Invoices, payment tracking and organization of financial documents. Example: prepare a payment reminder using supplied invoice references.")
 )
}
