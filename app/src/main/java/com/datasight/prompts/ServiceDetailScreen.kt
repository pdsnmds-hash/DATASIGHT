package com.datasight.prompts

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal data class ServiceContent(val image: Int, val intro: Bi, val tasks: List<Bi>, val prepare: Bi, val outcome: Bi)
internal val serviceContents = listOf(
    ServiceContent(R.drawable.service_administrative,
        Bi("Libérez du temps pour votre activité en organisant les tâches de bureau qui rythment votre quotidien.", "Make room for your core work by organizing the office tasks that shape your day."),
        listOf(Bi("Agendas et réunions : rassembler les disponibilités, préparer l’ordre du jour et organiser les documents utiles.", "Calendars and meetings: collect availability, prepare an agenda and organize useful documents."),
            Bi("Correspondance : préparer des emails, mettre en forme des courriers et suivre les réponses attendues.", "Correspondence: draft emails, format letters and track expected replies."),
            Bi("Documents et recherche : structurer un dossier, harmoniser sa présentation et rassembler les informations demandées.", "Documents and research: structure a file, standardize its presentation and gather requested information.")),
        Bi("Précisez la tâche, les documents disponibles, vos priorités et les règles de présentation. Pour un agenda, indiquez les contraintes horaires.", "Describe the task, available documents, priorities and presentation rules. For scheduling, include availability constraints."),
        Bi("Un périmètre peut inclure un dossier organisé, un document prêt à relire ou une liste de suivi. Le contenu et les validations sont convenus avec vous.", "The scope can include an organized file, a document ready for review or a tracking list. Content and approvals are agreed with you.")),
    ServiceContent(R.drawable.service_customer,
        Bi("Apportez de la continuité aux échanges avec vos clients grâce à une communication claire et un suivi organisé.", "Bring continuity to customer conversations through clear communication and organized follow-up."),
        listOf(Bi("Emails et chat : préparer les réponses à partir de vos informations et de vos consignes de communication.", "Email and chat: prepare replies using your information and communication guidelines."),
            Bi("Demandes entrantes : classer les sujets, noter les éléments manquants et orienter les questions vers le bon interlocuteur.", "Incoming inquiries: categorize topics, record missing details and route questions to the right person."),
            Bi("Suivi téléphonique : organiser les points à aborder et consigner les prochaines actions selon le cadre défini.", "Phone follow-up: organize discussion points and record next actions within the agreed scope.")),
        Bi("Présentez vos canaux, les demandes habituelles, les réponses autorisées et les cas à vous transmettre. Ne joignez que les informations utiles au premier échange.", "Describe your channels, common inquiries, approved responses and cases to escalate. Include only useful information in the first exchange."),
        Bi("Exemples de supports : trames de réponse, tableau de suivi des demandes et récapitulatif des actions à poursuivre. Aucun engagement commercial n’est pris sans vos consignes.", "Example materials include response outlines, an inquiry tracker and a summary of next actions. Commercial commitments require your instructions.")),
    ServiceContent(R.drawable.service_hr,
        Bi("Structurez les démarches administratives liées à vos collaborateurs, de leur arrivée au suivi des informations du personnel.", "Organize employee administration, from onboarding to maintaining personnel information."),
        listOf(Bi("Intégration : préparer une checklist d’accueil, rassembler les documents et coordonner les étapes pratiques.", "Onboarding: prepare a welcome checklist, gather documents and coordinate practical steps."),
            Bi("Dossiers RH : classer les pièces, repérer les documents manquants et tenir un suivi administratif.", "Personnel records: organize files, identify missing documents and maintain an administrative tracker."),
            Bi("Présences et paie : rassembler les éléments fournis et organiser leur transmission aux personnes responsables.", "Attendance and payroll: collect supplied information and organize its handover to the responsible people.")),
        Bi("Indiquez les étapes de votre processus, les responsables de validation et les catégories de documents. Les données personnelles sensibles ne sont pas nécessaires au premier contact.", "Describe process steps, approval owners and document categories. Sensitive personal information is not needed for the first contact."),
        Bi("Un dossier d’accueil structuré, une checklist ou un suivi des pièces peut être défini ensemble. Les décisions RH et la validation des éléments de paie restent auprès de vos responsables.", "An organized onboarding pack, checklist or document tracker can be defined together. HR decisions and payroll approval remain with your responsible staff.")),
    ServiceContent(R.drawable.service_information,
        Bi("Rendez vos informations plus faciles à retrouver et à maintenir grâce à une saisie soignée et une organisation cohérente.", "Make information easier to find and maintain through careful data entry and consistent organization."),
        listOf(Bi("Saisie et contrôle : reporter les données fournies, vérifier les champs et signaler les informations manquantes.", "Entry and checking: record supplied data, check fields and flag missing information."),
            Bi("CRM et bases : mettre à jour les fiches selon vos règles et organiser les éléments à vérifier.", "CRM and databases: update records according to your rules and organize items needing review."),
            Bi("Archives numériques : définir un classement, harmoniser les noms de fichiers et organiser les dossiers.", "Digital archives: define a filing structure, standardize filenames and organize folders.")),
        Bi("Décrivez la source, les champs attendus, le format de sortie et les règles de contrôle. Un exemple anonymisé suffit pour expliquer la structure.", "Describe the source, expected fields, output format and validation rules. An anonymized sample is enough to explain the structure."),
        Bi("Le résultat peut prendre la forme d’un fichier structuré, de fiches actualisées ou d’un plan de classement. Les valeurs inconnues sont signalées plutôt qu’inventées.", "The result can be a structured file, updated records or a filing plan. Unknown values are flagged rather than invented.")),
    ServiceContent(R.drawable.service_financial,
        Bi("Gardez une vue organisée sur vos factures et les pièces nécessaires au suivi administratif des paiements.", "Keep an organized view of invoices and the documents needed for administrative payment tracking."),
        listOf(Bi("Facturation : préparer les documents à partir des références, montants et conditions que vous fournissez.", "Invoicing: prepare documents using the references, amounts and terms you provide."),
            Bi("Suivi des paiements : tenir un tableau des échéances et préparer les points à vérifier.", "Payment tracking: maintain a due-date tracker and prepare items to check."),
            Bi("Relances et pièces : rédiger une demande de suivi selon vos consignes et classer les justificatifs.", "Reminders and records: draft a follow-up request according to your instructions and organize supporting documents.")),
        Bi("Précisez les documents concernés, la période, les références et le circuit de validation. N’envoyez pas d’identifiants bancaires ou de mots de passe.", "Specify the documents, period, references and approval process. Do not send banking credentials or passwords."),
        Bi("Exemples : tableau de suivi, dossier de pièces classées ou brouillon de relance à valider. Le périmètre porte sur l’administration financière, sans promesse de conseil fiscal ou de tenue comptable.", "Examples include a tracking sheet, organized supporting documents or a reminder draft for approval. The scope concerns financial administration, without promising tax advice or bookkeeping."))
)

@Composable
fun ServiceDetailScreen(language: String, index: Int, inquire: () -> Unit) {
    val content = serviceContents[index]
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 840.dp).fillMaxWidth(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Text(tr(language, "LES SERVICES DATASIGHT", "DATASIGHT SERVICES"), color = colors.primary, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(12.dp))
                Heading(Company.services[index].first.at(language), content.intro.at(language))
            }
            item {
                Image(painterResource(content.image), tr(language, "Illustration de l’activité : ", "Activity illustration: ") + Company.services[index].first.at(language),
                    Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
            }
            item { Panel {
                Text(tr(language, "Des tâches concrètes", "Practical ways we can help"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                content.tasks.forEach { Text("• " + it.at(language), style = MaterialTheme.typography.bodyLarge) }
            } }
            item { Panel {
                Text(tr(language, "Pour préparer notre échange", "Preparing for our conversation"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(content.prepare.at(language), style = MaterialTheme.typography.bodyLarge)
            } }
            item { Panel {
                Text(tr(language, "Un résultat défini ensemble", "An outcome defined together"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(content.outcome.at(language), style = MaterialTheme.typography.bodyLarge)
            } }
            item { Panel {
                Text(tr(language, "Comment commencer", "How to get started"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(tr(language, "1. Décrivez votre besoin et vos priorités.\n\n2. Vérifiez les informations que vous souhaitez partager.\n\n3. Ouvrez votre application email pour discuter du périmètre et des modalités avec DATASIGHT.", "1. Describe your needs and priorities.\n\n2. Review the information you want to share.\n\n3. Open your email app to discuss the scope and arrangements with DATASIGHT."), style = MaterialTheme.typography.bodyLarge)
                Action(tr(language, "Préparer une demande", "Prepare an inquiry"), inquire)
                Text(tr(language, "Votre demande est préparée sur cet appareil. Rien n’est envoyé automatiquement.", "Your inquiry is prepared on this device. Nothing is sent automatically."), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            } }
        }
    }
}
