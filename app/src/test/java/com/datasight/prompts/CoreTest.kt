package com.datasight.prompts
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlinx.serialization.*
import java.io.File

class CoreTest {
 @Test fun reproduciblePreservesFactsAndCode(){
  val f=Form(category=Category.DEVELOP,task="Build F-42 for 240 EUR",materials="fun f(x: Int) = x * 2\nval name = \"Élodie\"",language="en",values=mapOf("stack" to "Kotlin 2.2"))
  val a=PromptComposer.compose(f,null)
  assertEquals(a,PromptComposer.compose(f,null));assertTrue(a.contains(f.task));assertTrue(a.contains(f.materials));assertTrue(a.contains("Kotlin 2.2"));assertTrue(a.contains("reproducible tests"));assertFalse(a.contains("## Context"))
 }
 @Test fun outputLanguageIndependent(){val f=Form(task="Mon contexte intact",language="en");val s=PromptComposer.compose(f,null);assertTrue(s.startsWith("## Task"));assertTrue(s.contains(f.task));assertTrue(s.contains("Response language: English"))}
 @Test fun originalPreserved(){val f=Form(category=Category.IMPROVE,task="Make concise",original="abc\n123 <> {}",language="fr");assertTrue(PromptComposer.compose(f,null).contains(f.original))}
 @Test fun validation(){assertTrue("task" in FormValidator.errors(Form(),null));assertTrue("original" in FormValidator.errors(Form(category=Category.IMPROVE,task="x"),null));assertTrue("customLength" in FormValidator.errors(Form(task="x",length="custom"),null));assertTrue(FormValidator.errors(Form(task="x"),null).isEmpty())}
 @Test fun backupRoundtrip(){val r=Record("r1","Title",Category.WRITE,"write_01",codec.encodeToString(Form(task="é 42",output="body")),"é code {}","fr",1,2);val b=Backup(records=listOf(r),settings=Preferences(theme="dark"));assertEquals(b,BackupValidator.decode(codec.encodeToString(b)))}
 @Test fun invalidBackup(){listOf("{}","{broken",codec.encodeToString(Backup(2,emptyList(),Preferences())),codec.encodeToString(Backup(records=emptyList(),settings=Preferences(language="ru")))) .forEach{raw->assertTrue(runCatching{BackupValidator.decode(raw)}.isFailure)}}
 @Test fun frenchDefaults(){assertEquals("fr",Preferences().language);assertEquals("system",Preferences().theme);assertEquals("fr",Form().language)}
 @Test fun jsonSchemaPreserved(){val schema="""{"id":"string","total":42}""";val s=PromptComposer.compose(Form(task="Export",format="json",jsonSchema=schema,language="en"),null);assertTrue(s.contains(schema));assertTrue(s.contains("Requested JSON structure"))}
 @Test fun emptyOptionalBlocksOmitted(){val s=PromptComposer.compose(Form(task="X"),null);listOf("## Contexte","## Matériaux","## Contraintes","## Exemple").forEach{assertFalse(s.contains(it))}}
 @Test fun invalidDuplicateIds(){val r=Record("same","T",Category.WRITE,"",codec.encodeToString(Form()),"","fr",1,1);assertTrue(runCatching{BackupValidator.decode(codec.encodeToString(Backup(records=listOf(r,r),settings=Preferences())))}.isFailure)}
 @Test fun invalidStructuredParameters(){val r=Record("x","T",Category.WRITE,"","not json","","fr",1,1);assertTrue(runCatching{BackupValidator.decode(codec.encodeToString(Backup(records=listOf(r),settings=Preferences())))}.isFailure)}
 @Test fun invalidFormLanguage(){val r=Record("x","T",Category.WRITE,"",codec.encodeToString(Form(language="ru")),"","fr",1,1);assertTrue(runCatching{BackupValidator.decode(codec.encodeToString(Backup(records=listOf(r),settings=Preferences())))}.isFailure)}
 @Test fun invalidWizardStep(){val r=Record("x","T",Category.WRITE,"",codec.encodeToString(Form(step=4)),"","fr",1,1);assertTrue(runCatching{BackupValidator.decode(codec.encodeToString(Backup(records=listOf(r),settings=Preferences())))}.isFailure)}
 @Test fun noDuplicates(){val t=loadTemplates();assertEquals(60,t.size);assertEquals(60,t.map{it.id}.distinct().size);Category.entries.forEach{c->assertEquals(10,t.count{it.category==c})}}
}
fun loadTemplates():List<Template>{val file=listOf(File("src/main/assets/templates.json"),File("app/src/main/assets/templates.json")).first{it.exists()};return codec.decodeFromString(file.readText())}
@RunWith(Parameterized::class) class TemplateTest(private val template:Template,private val language:String){
 companion object{@JvmStatic @Parameterized.Parameters(name="{index}: {1}")fun data():List<Array<Any>> = loadTemplates().flatMap{t->listOf("fr","en").map{arrayOf(t,it)}}}
 @Test fun translatedAndComplete(){val t=template;assertTrue(t.title.at(language).isNotBlank());assertTrue(t.description.at(language).isNotBlank());assertTrue(t.use.at(language).isNotBlank());assertTrue(t.example.at(language).isNotBlank());assertTrue(t.tips.size>=2);t.fields.forEach{assertTrue(it.label.at(language).isNotBlank());assertTrue(it.example.at(language).isNotBlank())};val f=Form(category=t.category,task=t.title.at(language),language=language,original=if(t.category==Category.IMPROVE)"Original 42" else "",values=t.fields.associate{it.id to it.example.at(language)});assertTrue(FormValidator.errors(f,t).isEmpty());val result=PromptComposer.compose(f,t);assertFalse(Regex("\\{\\{.*?}}|\\[INSERT|Lorem ipsum|insérez ici",RegexOption.IGNORE_CASE).containsMatchIn(result));assertFalse(Regex("## [^\\n]+\\n\\s*##").containsMatchIn(result))}
}
