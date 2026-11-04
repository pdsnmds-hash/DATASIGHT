package com.datasight.prompts

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import java.util.UUID

@Serializable @Entity(tableName="records") data class Record(
 @PrimaryKey val id:String, val title:String, val category:Category, val subtype:String,
 val parameters:String, val text:String, val language:String, val created:Long, val updated:Long,
 val favorite:Boolean=false, val templateId:String?=null, val draft:Boolean=false
)
@Entity(tableName="pending_settings") data class PendingSettings(@PrimaryKey val id:Int=1,val payload:String)
class Converters { @TypeConverter fun from(c:Category)=c.name; @TypeConverter fun to(s:String)=Category.valueOf(s) }
@Dao interface RecordDao {
 @Query("SELECT * FROM records ORDER BY updated DESC") fun observe():Flow<List<Record>>
 @Query("SELECT * FROM records") suspend fun all():List<Record>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun put(record:Record)
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putAll(records:List<Record>)
 @Query("DELETE FROM records WHERE id=:id") suspend fun delete(id:String)
 @Query("DELETE FROM records") suspend fun clear()
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun journal(value:PendingSettings)
 @Query("SELECT * FROM pending_settings WHERE id=1") suspend fun pendingSettings():PendingSettings?
 @Query("DELETE FROM pending_settings") suspend fun clearJournal()
}
@Database(entities=[Record::class,PendingSettings::class],version=1,exportSchema=false) @TypeConverters(Converters::class)
abstract class AppDatabase:RoomDatabase() { abstract fun records():RecordDao }
const val BACKUP_EXPORT_CACHE = "datasight-export-pending.json"
val Context.settingsStore by preferencesDataStore("settings")
class AppModel(app:Application):AndroidViewModel(app) {
 val db=Room.databaseBuilder(app,AppDatabase::class.java,"datasight.db").build()
 val templates:List<Template> = codec.decodeFromString(app.assets.open("templates.json").bufferedReader().use{it.readText()})
 val privacy:PrivacyDocument = codec.decodeFromString(app.assets.open("privacy.json").bufferedReader().use{it.readText()})
 val records=db.records().observe().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 val prefs=app.settingsStore.data.map { p -> Preferences(p[stringPreferencesKey("language")]?:"fr",p[stringPreferencesKey("theme")]?:"system",p[stringSetPreferencesKey("favorites")]?:emptySet()) }.stateIn(viewModelScope,SharingStarted.Eagerly,Preferences())
 val onboardingCompleted = app.settingsStore.data.map { it[booleanPreferencesKey("onboarding_completed")] ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
 fun replayOnboarding() { viewModelScope.launch { attempt { flushDraft(); getApplication<Application>().settingsStore.edit { it[booleanPreferencesKey("onboarding_completed")] = false } } } }
 fun finishOnboarding() { viewModelScope.launch { attempt { getApplication<Application>().settingsStore.edit { it[booleanPreferencesKey("onboarding_completed")] = true } } } }
 val form=MutableStateFlow(Form())
 val message=MutableStateFlow<String?>(null)
 private var draftId=UUID.randomUUID().toString()
 private var draftCreated=System.currentTimeMillis()
 private var pending:Job?=null
 private var generation=0
 private var dirty=false
 init { viewModelScope.launch { attempt {
  db.records().pendingSettings()?.let { writeSettings(codec.decodeFromString<Preferences>(it.payload));db.records().clearJournal() }
  val r=db.records().all().filter{it.draft}.maxByOrNull{it.updated}
  if(r!=null && form.value==Form()) resume(r)
 } } }
 fun text(fr:String,en:String)=tr(prefs.value.language,fr,en)
 fun template()=templates.find{it.id==form.value.templateId}
 fun start(t:Template) { pending?.cancel(); generation++; draftId=UUID.randomUUID().toString(); draftCreated=System.currentTimeMillis(); form.value=Form(category=t.category,subtype=t.id,templateId=t.id,language=prefs.value.language); persist() }
 fun resume(r:Record) { dirty=r.draft;pending?.cancel();generation++; draftId=if(r.draft) r.id else UUID.randomUUID().toString(); draftCreated=if(r.draft) r.created else System.currentTimeMillis();form.value=codec.decodeFromString<Form>(r.parameters).copy(recordId=if(r.draft)null else r.id,output=r.text,title=r.title) }
 fun update(change:(Form)->Form) { form.value=change(form.value); persist() }
 fun flushDraft() {
  pending?.cancel(); if(!dirty)return; val f=form.value; if(f.task.isBlank() && f.original.isBlank() && f.values.isEmpty()) return
  val id=draftId; val created=draftCreated
  viewModelScope.launch { attempt { db.records().put(record(f,id,created,true)) } }
 }
 private fun persist() {
  dirty=true;pending?.cancel(); val f=form.value;val id=draftId;val created=draftCreated;val revision=generation
  pending=viewModelScope.launch { delay(180); if(revision==generation) attempt { db.records().put(record(f,id,created,true)) } }
 }
 private fun record(f:Form,id:String,created:Long,draft:Boolean)=Record(id,f.title.ifBlank{templates.find{it.id==f.templateId}?.title?.at(prefs.value.language)?:f.task.take(80)},f.category,f.subtype,codec.encodeToString(f),f.output,f.language,created,maxOf(created,System.currentTimeMillis()),templateId=f.templateId,draft=draft)
 fun generate() { val output=PromptComposer.compose(form.value,template()); update{it.copy(output=output,baseline=output)} }
 fun save(separate:Boolean=false,after:()->Unit={}) { viewModelScope.launch { attempt {
  val f=form.value;require(f.output.isNotBlank());require(f.title.isNotBlank())
  val old=records.value.find{it.id==f.recordId};val id=if(separate)UUID.randomUUID().toString() else f.recordId?:UUID.randomUUID().toString()
  db.records().put(record(f,id,if(separate)System.currentTimeMillis() else old?.created?:System.currentTimeMillis(),false).copy(favorite=if(separate)false else old?.favorite?:false))
  pending?.cancel(); generation++; db.records().delete(draftId); form.value=f.copy(recordId=id);dirty=false;message.value=text("Prompt enregistré","Prompt saved");after()
 } } }
 fun favorite(r:Record) { viewModelScope.launch{attempt{db.records().put(r.copy(favorite=!r.favorite,updated=System.currentTimeMillis()))}} }
 fun duplicate(r:Record) { viewModelScope.launch{attempt{db.records().put(r.copy(id=UUID.randomUUID().toString(),title=r.title+text(" — copie"," — copy"),created=System.currentTimeMillis(),updated=System.currentTimeMillis()))}} }
 fun delete(r:Record, after:()->Unit={}) { viewModelScope.launch{attempt{
  if(r.draft && r.id==draftId) { pending?.cancel();generation++;dirty=false;form.value=Form(language=prefs.value.language) }
  db.records().delete(r.id);after()
 }} }
 fun restore(r:Record) { viewModelScope.launch{attempt{db.records().put(r)}} }
 fun settings(p:Preferences) { viewModelScope.launch{attempt{writeSettings(p)}} }
 private suspend fun writeSettings(p:Preferences) { getApplication<Application>().settingsStore.edit { it[stringPreferencesKey("language")]=p.language;it[stringPreferencesKey("theme")]=p.theme;it[stringSetPreferencesKey("favorites")]=p.favorites } }
 suspend fun exportBackup():String {
  pending?.join()
  val raw=codec.encodeToString(Backup(records=db.records().all(),settings=prefs.value))
  require(raw.toByteArray(Charsets.UTF_8).size<=20_000_000)
  return raw
 }
 suspend fun importBackup(b:Backup) {
  pending?.cancelAndJoin();generation++
  db.withTransaction { db.records().clear(); db.records().putAll(b.records);db.records().journal(PendingSettings(payload=codec.encodeToString(b.settings))) }
  writeSettings(b.settings)
  db.records().clearJournal()
  dirty=false
  form.value=Form(language=b.settings.language)
 }
 suspend fun clearLocalData() {
  pending?.cancelAndJoin();generation++;dirty=false
  withContext(Dispatchers.IO) {
   val cache=getApplication<Application>().cacheDir.resolve(BACKUP_EXPORT_CACHE)
   if(cache.exists())check(cache.delete())
  }
  val defaults=Preferences()
  db.withTransaction {
   db.records().clear();db.records().clearJournal()
   db.records().journal(PendingSettings(payload=codec.encodeToString(defaults)))
  }
  writeSettings(defaults);db.records().clearJournal()
  form.value=Form(language=defaults.language)
  draftId=UUID.randomUUID().toString();draftCreated=System.currentTimeMillis()
 }
 suspend fun attempt(block:suspend()->Unit) { try{block()}catch(e:Exception){if(e is CancellationException)throw e; message.value=text("L’opération a échoué. Réessayez.","The operation failed. Please try again.")} }
}
