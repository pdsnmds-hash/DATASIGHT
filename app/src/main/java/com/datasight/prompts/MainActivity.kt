package com.datasight.prompts

import android.content.*
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import kotlinx.coroutines.launch

class MainActivity:ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) { setTheme(R.style.AppTheme);super.onCreate(savedInstanceState);enableEdgeToEdge();setContent { val m:AppModel=viewModel();val p by m.prefs.collectAsStateWithLifecycle();val darkTheme=p.theme=="dark" || p.theme=="system"&&isSystemInDarkTheme();SideEffect { WindowCompat.getInsetsController(window,window.decorView).apply { isAppearanceLightStatusBars=!darkTheme;isAppearanceLightNavigationBars=!darkTheme } };var appSession by remember { mutableIntStateOf(0) };key(appSession){AppTheme(p.theme){App(m){appSession++}}} } }
}
private val light=lightColorScheme(primary=Color(0xFF5156D8),onPrimary=Color.White,primaryContainer=Color(0xFFECECFF),onPrimaryContainer=Color(0xFF23265A),background=Color(0xFFF7F8FC),surface=Color.White,onBackground=Color(0xFF1D2438),onSurface=Color(0xFF1D2438),onSurfaceVariant=Color(0xFF5E687D),outline=Color(0xFF5E687D),outlineVariant=Color(0xFFDDE2ED))
private val dark=darkColorScheme(primary=Color(0xFFB8BAFF),onPrimary=Color(0xFF23265A),primaryContainer=Color(0xFF303759),onPrimaryContainer=Color(0xFFF0F3FA),background=Color(0xFF111522),surface=Color(0xFF1B2234),surfaceVariant=Color(0xFF252E44),onBackground=Color(0xFFF0F3FA),onSurface=Color(0xFFF0F3FA),onSurfaceVariant=Color(0xFFB5BED0),outline=Color(0xFFB5BED0),outlineVariant=Color(0xFF36415A))
@Composable fun AppTheme(theme:String,content:@Composable ()->Unit) { val deviceDensity=LocalDensity.current; val width=LocalConfiguration.current.screenWidthDp; val textFactor=when { width<=340 -> .9f; width<400 -> .96f; else -> 1f }; CompositionLocalProvider(LocalDensity provides Density(deviceDensity.density,deviceDensity.fontScale*textFactor)) { MaterialTheme(colorScheme=if(theme=="dark" || theme=="system"&&isSystemInDarkTheme())dark else light,shapes=Shapes(medium=RoundedCornerShape(20.dp),large=RoundedCornerShape(24.dp)),content=content) } }
fun icon(c:Category):ImageVector=when(c){Category.WRITE->Icons.Outlined.Edit;Category.ANALYZE->Icons.Outlined.Analytics;Category.DOCUMENT->Icons.Outlined.Description;Category.IDEAS->Icons.Outlined.Lightbulb;Category.DEVELOP->Icons.Outlined.Code;Category.IMPROVE->Icons.Outlined.AutoFixHigh}
fun accent(c:Category)=Color(when(c){Category.WRITE->0xFF5156D8;Category.ANALYZE->0xFF167C80;Category.DOCUMENT->0xFF2876BE;Category.IDEAS->0xFFA45424;Category.DEVELOP->0xFF8453B2;Category.IMPROVE->0xFFB83A75})
@Composable fun Page(scroll:ScrollState=rememberScrollState(),content:@Composable ColumnScope.()->Unit) { Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal=if(LocalConfiguration.current.screenWidthDp<360)16.dp else 24.dp,vertical=20.dp).widthIn(max=840.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content) }
@Composable fun Heading(title:String,sub:String?=null) { Text(title,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);if(sub!=null)Text(sub,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyLarge) }
@Composable fun Panel(content:@Composable ColumnScope.()->Unit) { Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(if(LocalConfiguration.current.screenWidthDp<360)16.dp else 20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)} }
@Composable fun Action(label:String,onClick:()->Unit) { CenteredButton(onClick,Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(16.dp)){Text(label)} }
@Composable fun Input(label:String,value:String,onValue:(String)->Unit,example:String="",error:Boolean=false,minLines:Int=2) { OutlinedTextField(value,onValue,Modifier.fillMaxWidth(),label={Text(label)},placeholder=if(example.isBlank())null else {{Text(example)}},supportingText=if(error){{Text("⚠ "+label)}}else null,isError=error,minLines=minLines,shape=RoundedCornerShape(16.dp)) }
@Composable fun Choices(options:List<Pair<String,String>>,selected:String,onSelect:(String)->Unit) { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){options.forEach{(id,label)->FilterChip(selected==id,{onSelect(id)},label={Text(label)},modifier=Modifier.heightIn(min=48.dp))}} }
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun App(m:AppModel,onLocalDataCleared:()->Unit={}) {
 val onboardingCompleted by m.onboardingCompleted.collectAsStateWithLifecycle()
 if (onboardingCompleted == null) { Surface(Modifier.fillMaxSize()) {} ; return }
 if (onboardingCompleted == false) { OnboardingScreen(m); return }
 val lifecycle=LocalLifecycleOwner.current.lifecycle
 DisposableEffect(lifecycle,m) { val observer=LifecycleEventObserver { _, event -> if(event==Lifecycle.Event.ON_STOP) m.flushDraft() };lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer)} }
 val p by m.prefs.collectAsStateWithLifecycle();val l=p.language;val nav=rememberNavController();val entry by nav.currentBackStackEntryAsState();val route=entry?.destination?.route?:"create";val scope=rememberCoroutineScope();val snack=remember{SnackbarHostState()};val message by m.message.collectAsStateWithLifecycle()
 LaunchedEffect(message){message?.let{snack.showSnackbar(it);m.message.value=null}}
 val tabs=listOf("create" to tr(l,"Créer","Create"),"templates" to tr(l,"Modèles","Templates"),"saved" to tr(l,"Mes prompts","My prompts"),"company" to "DATASIGHT")
 Scaffold(snackbarHost={SnackbarHost(snack)},topBar={TopAppBar(colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background),title={Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){BrandMark(Modifier.size(28.dp));Text("DATASIGHT",fontWeight=FontWeight.Bold,fontSize=if(LocalDensity.current.fontScale>=1.5f)16.sp else 22.sp,letterSpacing=if(LocalDensity.current.fontScale>=1.5f)1.sp else 2.sp)}},navigationIcon={if(route !in tabs.map{it.first}) IconButton({if(route=="builder"&&m.form.value.step>0)m.update{it.copy(step=it.step-1)}else nav.popBackStack()}){Icon(Icons.AutoMirrored.Outlined.ArrowBack,tr(l,"Retour","Back"))}},actions={if(route!="settings"&&route!="privacy"){IconButton({nav.navigate("settings"){launchSingleTop=true}}){Icon(Icons.Outlined.Settings,tr(l,"Paramètres","Settings"))}}})},bottomBar={if(route in tabs.map{it.first})DatasightNavigation(tabs,route){id->if(id=="create")nav.popBackStack("create",false)else nav.navigate(id){popUpTo("create"){saveState=true};launchSingleTop=true;restoreState=true}}}) { padding ->
 Box(Modifier.fillMaxSize().padding(padding).imePadding()) { NavHost(nav,"create") {
  composable("create"){Home(m,{category->m.start(m.templates.first{it.category==category});nav.navigate("builder")},{t->m.start(t);nav.navigate("builder")},{r->m.resume(r);nav.navigate(if(r.draft)"builder" else "result")},{nav.navigate("templates"){launchSingleTop=true}})}
  composable("templates"){Library(m,null,{nav.navigate("preview/$it")})}
  composable("templates/category/{category}"){val c=it.arguments?.getString("category")?.let{value->runCatching{Category.valueOf(value)}.getOrNull()};Library(m,c,{id->nav.navigate("preview/$id")})}
  composable("preview/{id}"){backEntry->val t=m.templates.first{it.id==backEntry.arguments?.getString("id")};Preview(m,t,{m.start(t);nav.navigate("builder")},{nav.navigate("company/service/$it")})}
  composable("builder"){Builder(m,{nav.navigate("result")})}
  composable("result"){Result(m,{nav.navigate("builder"){launchSingleTop=true}},{nav.navigate("company/service/$it")})}
  composable("saved"){Saved(m,{m.resume(it);nav.navigate(if(it.draft)"builder" else "result")},snack,{category->m.flushDraft();m.start(m.templates.first{it.category==category});nav.navigate("builder")})}
  composable("company"){CompanyScreen(m,null,{nav.navigate("company/service/$it")})}
  composable("company/service/{service}"){CompanyScreen(m,it.arguments?.getString("service")?.toIntOrNull())}
  composable("settings"){Settings(m,{nav.navigate("privacy"){launchSingleTop=true}},onLocalDataCleared)}
  composable("privacy"){PrivacyScreen(m)}
 } }
 }
}
@Composable fun Preview(m:AppModel,t:Template,apply:()->Unit,service:(Int)->Unit) { val p by m.prefs.collectAsStateWithLifecycle();val l=p.language
 Page{Text(t.category.label.at(l),color=MaterialTheme.colorScheme.primary);Heading(t.title.at(l),t.description.at(l));Panel{Text(tr(l,"Quand l’utiliser","When to use"),style=MaterialTheme.typography.titleMedium);Text(t.use.at(l));t.fields.forEach{Text(it.label.at(l),fontWeight=FontWeight.Bold);Text(it.example.at(l))}};Panel{Text(tr(l,"Structure complète","Complete structure"),style=MaterialTheme.typography.titleMedium);Text(PromptComposer.compose(Form(category=t.category,task=t.title.at(l),language=l,values=t.fields.associate{it.id to it.example.at(l)}),t))};Panel{Text(tr(l,"Exemple rempli","Filled example"),style=MaterialTheme.typography.titleMedium);Text(t.example.at(l));t.tips.forEach{Text("• "+it.at(l))}};Action(tr(l,"Utiliser ce modèle","Use this template"),apply);t.service?.let{Help(l,it,service)}}
}
@Composable fun Help(l:String,id:Int,service:(Int)->Unit){Panel{Text(tr(l,"Besoin d’aide pour réaliser cette tâche ?","Need help with the task itself?"),style=MaterialTheme.typography.titleMedium);Text(Company.services[id].first.at(l));SecondaryButton({service(id)}){Text(tr(l,"Découvrir le service","Explore the service"))}}}
