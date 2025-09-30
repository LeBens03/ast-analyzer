# TP - Analyse Statistique et Graphe d'Appel pour Applications Orientées Objet

## Description
Ce projet analyse le code source Java d'une application orientée objet pour extraire des statistiques et construire le graphe d'appel des méthodes. Il répond aux consignes suivantes :

### Exercice 1 : Calcul statistique
L'application calcule :
1. **Nombre de classes** : Compte toutes les classes non interfaces.
2. **Nombre de lignes de code** : Additionne les lignes de toutes les méthodes.
3. **Nombre total de méthodes** : Compte toutes les méthodes dans toutes les classes.
4. **Nombre total de packages** : Compte les packages distincts.
5. **Nombre moyen de méthodes par classe** : Moyenne des méthodes par classe.
6. **Nombre moyen de lignes de code par méthode** : Moyenne des lignes par méthode.
7. **Nombre moyen d’attributs par classe** : Moyenne des attributs par classe.
8. **Top 10% des classes par nombre de méthodes** : Liste les classes avec le plus de méthodes.
9. **Top 10% des classes par nombre d’attributs** : Liste les classes avec le plus d’attributs.
10. **Classes dans les deux catégories précédentes** : Intersection des deux tops.
11. **Classes avec plus de X méthodes** : Liste les classes ayant plus de X méthodes (X configurable dans le code).
12. **Top 10% des méthodes par nombre de lignes (par classe)** : Méthodes les plus longues par classe.
13. **Nombre maximal de paramètres dans toutes les méthodes**.

### Exercice 2 : Graphe d'appel
- **Construction du graphe d'appel** : Affiche le graphe d'appel des méthodes (textuel, DOT pour GraphViz, HTML interactif).
- **Interface graphique (optionnelle)** : Affiche les statistiques dans une fenêtre Swing.

## Structure du projet
- `Analyzer.java` : Point d'entrée, analyse le code et calcule les statistiques.
- `Utils.java` : Définitions des structures `ClassInfo` et `MethodInfo`.
- `AppStatsGUI.java` : Interface graphique Swing pour afficher les statistiques.
- `CallGraphBuilder.java` : Génère et affiche le graphe d'appel (console, DOT, HTML).

## Installation

1. **Prérequis** :
   - Java 11 ou supérieur
   - Maven (pour la compilation)
   - Dépendance : JDT Core (org.eclipse.jdt.core) pour l'analyse AST

2. **Compilation** :
   Dans le dossier du projet, exécutez :
   ```bash
   mvn clean package
   ```

3. **Exécution** :
   Pour analyser un fichier ou dossier Java :
   ```bash
   java -cp target/classes analyzer.Analyzer <chemin-fichier-ou-dossier>
   ```
   Exemple :
   ```bash
   java -cp target/classes analyzer.Analyzer src/main/java/
   ```

## Utilisation
- Les statistiques sont affichées dans la console et dans une fenêtre graphique (Swing).
- Le graphe d'appel est affiché dans la console, exporté au format DOT (`callgraph.dot`) et HTML interactif (`callgraph.html`).
- Pour visualiser le graphe DOT :
  ```bash
  dot -Tpng callgraph.dot -o callgraph.png
  ```
- Ouvrez `callgraph.html` dans un navigateur pour une visualisation interactive.

## Correspondance code/consignes
- **Statistiques** : Calculées dans `Analyzer.java`, affichées via `AppStatsGUI.java`.
- **Graphe d'appel** : Construit et exporté par `CallGraphBuilder.java`.
- **Structures de données** : `Utils.java` (classes et méthodes).
- **Interface graphique** : `AppStatsGUI.java` (Swing).