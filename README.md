# Hytale Homes

Plugin de homes nommés pour serveur Hytale, développé par l'association étudiante
**HytaleAssos**.

Les joueurs peuvent enregistrer plusieurs points de téléportation, les consulter
dans une interface et s'y téléporter, y compris vers un autre monde. Les homes
sont conservés dans le dossier de données du plugin et survivent aux
redémarrages.

## Résumé de l'implémentation

### Fonctionnalités

- Homes nommés multiples, 5 par défaut.
- `/sethome [nom]` : création ou remplacement d'un home.
- `/home` : ouverture de l'interface listant les homes.
- `/home <nom>` et `/home <numéro>` : téléportation directe.
- `/homes` : liste numérotée dans le chat.
- `/delhome [nom|numéro]` : suppression d'un home.
- Interface avec un bouton par home, nom, monde et coordonnées.
- Téléportation multi-monde si le monde cible est chargé.
- Persistance JSON dans `homes.json`, avec écriture atomique.
- Aucune permission requise en v1.
- Pas de cooldown ni de warmup en v1.

### Technique

- Java 25, serveur Hytale `>=0.7.2 <0.8.0`.
- Package `assos.hytale.home`.
- `HomeService` : règles métier (limites, noms, index, résolution).
- `HomeStorage` : stockage JSON thread-safe et sauvegarde atomique.
- `HomeTeleporter` : utilisation du composant natif `Teleport`.
- `HomesPage` : page custom via `InteractiveCustomUIPage`.
- L'interface réutilise le layout natif de Hytale et envoie ses textes en markup
  inline ; aucun asset pack n'est nécessaire.
- Tests JUnit 5 : 43 tests pour `HomeService`, 8 pour `HomeStorage`, soit
  **51 tests**.

## Commandes

| Commande | Description |
|---|---|
| `/sethome` | Crée ou remplace le home `home` à la position actuelle. |
| `/sethome <nom>` | Crée ou remplace le home nommé. |
| `/home` | Ouvre l'interface listant les homes. |
| `/home <nom>` | Téléporte vers le home nommé. |
| `/home <numéro>` | Téléporte vers le home correspondant à l'index affiché par `/homes`. |
| `/homes` | Liste les homes numérotés avec leur monde et leurs coordonnées. |
| `/delhome` | Supprime le home `home`. |
| `/delhome <nom>` | Supprime le home nommé. |
| `/delhome <numéro>` | Supprime le home correspondant à l'index affiché par `/homes`. |

Règles des noms :

- les noms sont convertis en minuscules et les espaces autour sont ignorés ;
- caractères autorisés : lettres, chiffres, `_` et `-` ;
- les noms purement numériques (`1`, `42`, `007`) sont refusés pour ne pas
  entrer en conflit avec les index de `/homes` ;
- longueur maximale configurable (24 par défaut).

Le home par défaut est `home`. `/sethome` et `/delhome` sans argument utilisent
ce nom. `/home` sans argument ouvre l'interface ; un clic sur un home téléporte
et ferme la page.

Les commandes utilisent une syntaxe positionnelle, par exemple `/home spawn`,
et non `--name=spawn`. `/homes` affiche une liste numérotée :

```text
Your homes (2/5):
 1. home (default) [default] (12.5, 64.0, -30.2)
 2. base [nether] (5.0, 70.0, 12.0)
Use /home <number> or /home <name>.
```

`/home 2` et `/delhome 2` fonctionnent donc aussi bien avec l'index qu'avec le
nom.

## Interface

`/home` sans argument ouvre une page custom :

- titre **YOUR HOMES** ;
- indication **Click a home to teleport.** ;
- un bouton par home avec son nom, son monde et ses coordonnées ;
- un clic téléporte le joueur puis ferme la page ;
- si le joueur n'a aucun home, un message l'invite à utiliser `/sethome`.

La page est construite à partir du layout natif
`Pages/SelectOverrideRespawnPointPage.ui` et du bouton natif
`Pages/OverrideRespawnPointButton.ui`. Le titre et le contenu sont remplacés par
du markup inline, donc il n'y a pas de layout client à installer.

## Configuration

Le fichier `homes_config.json` est créé au premier lancement dans le dossier de
données du plugin.

| Clé | Défaut | Description |
|---|---|---|
| `MaxHomes` | `5` | Nombre maximal de homes par joueur. |
| `DefaultHomeName` | `home` | Nom utilisé par `/sethome` et `/delhome` quand aucun nom n'est fourni. |
| `AllowCrossWorld` | `true` | Autorise les téléportations vers un autre monde. |
| `MaxHomeNameLength` | `24` | Longueur maximale d'un nom de home. |

## Données

Les homes sont stockés dans `homes.json`, dans le dossier de données du plugin :

```json
{
  "version": 1,
  "players": {
    "<uuid>": {
      "home": {
        "name": "home",
        "world": "default",
        "x": 12.5,
        "y": 64.0,
        "z": -30.2,
        "pitch": 0.0,
        "yaw": 90.0,
        "roll": 0.0,
        "createdAt": 1760000000000
      }
    }
  }
}
```

- L'écriture est atomique : fichier temporaire puis renommage.
- En cas de JSON invalide au chargement, le plugin journalise l'erreur et
  démarre avec un stockage vide.
- Les fichiers générés par le serveur de dev (`homes.json`,
  `homes_config.json`, `homes_config.json.bak`) sont gitignorés et exclus du
  jar.

## Téléportation

La téléportation utilise le composant natif `Teleport` de Hytale. Elle fonctionne
donc entre les mondes chargés. Si le monde cible n'est pas chargé, la commande
affiche une erreur et ne déplace pas le joueur.

Le plugin est indépendant de `/tp home` (commande native liée au point de
respawn). `/sethome` ne modifie pas le point de respawn après une mort.

## Limites connues

- L'autocomplétion des noms de homes dans le client Hytale n'est pas disponible :
  le client ne demande pas de valeurs personnalisées au serveur.
- Il n'existe pas de keybind officiel côté Hytale : le client n'est pas moddable
  et les plugins serveur ne peuvent pas enregistrer une nouvelle touche. `/home`
  sans argument sert donc de point d'entrée vers l'interface.
- Aucun système de cooldown ou de warmup en v1.
- Un monde cible doit être chargé pour une téléportation inter-monde.

## Construction et tests

```bash
./gradlew test     # tests unitaires JUnit 5
./gradlew build    # build complet + validation du manifest
./gradlew runServer
```

Le jar est généré dans `build/libs/`.

Les tests unitaires couvrent `HomeService` et `HomeStorage`. L'intégration
serveur (commandes, interface et téléportation réelle) se vérifie manuellement
avec `./gradlew runServer`.

## Structure

```text
src/main/java/assos/hytale/home/
├── HytaleHomePlugin.java
├── commands/
│   ├── DelHomeCommand.java
│   ├── HomeCommand.java
│   ├── HomeSuggestions.java
│   ├── HomesCommand.java
│   └── SetHomeCommand.java
├── config/
│   └── HomesConfig.java
├── home/
│   ├── Home.java
│   ├── HomeRepository.java
│   ├── HomeService.java
│   ├── HomeStorage.java
│   ├── HomeTeleportException.java
│   └── HomeTeleporter.java
└── ui/
    └── HomesPage.java
```

## Compatibilité

- Java 25
- Serveur Hytale `>=0.7.2 <0.8.0`
- Plugin Gradle `com.azuredoom.hytale-tools`
