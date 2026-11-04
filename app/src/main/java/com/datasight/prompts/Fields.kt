package com.datasight.prompts

fun specificFields(c:Category)=when(c){
 Category.DEVELOP->listOf(Field("stack",Bi("Langage et stack","Language and stack"),Bi("Kotlin, Android, Compose","Kotlin, Android, Compose")),Field("io",Bi("Entrées et sorties","Inputs and outputs"),Bi("Une liste d’IDs → IDs uniques","List of IDs → unique IDs")),Field("environment",Bi("Limites de l’environnement","Environment constraints"),Bi("Hors ligne, Android 8+","Offline, Android 8+")),Field("checks",Bi("Vérifications attendues","Expected checks"),Bi("Cas vide, doublons et erreurs","Empty input, duplicates and errors")))
 Category.ANALYZE->listOf(Field("criteria",Bi("Questions ou critères","Questions or criteria"),Bi("Coût, risques et dépendances","Cost, risks and dependencies")),Field("comparison",Bi("Avec quoi comparer ?","What to compare against?"),Bi("Offre actuelle ou second scénario","Current proposal or second scenario")),Field("conclusions",Bi("Présentation des conclusions","Conclusions layout"),Bi("Constat, preuve, limite, action","Finding, evidence, limitation, action")))
 Category.DOCUMENT->listOf(Field("sections",Bi("Sections obligatoires","Required sections"),Bi("Objectif, étapes, contrôles","Purpose, steps, checks")),Field("purpose",Bi("But du document","Document purpose"),Bi("Transmettre le dossier sans perdre les actions","Hand over a case while preserving actions")))
 Category.IDEAS->listOf(Field("count",Bi("Nombre de propositions","Number of options"),Bi("8","8")),Field("selection",Bi("Critères de sélection","Selection criteria"),Bi("Facile à tester, sans nouvel outil","Easy to test, no new tools")))
 else->emptyList()
}
