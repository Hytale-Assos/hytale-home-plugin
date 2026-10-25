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
- Cooldown et warmup configurables, warmup annulé en cas de déplacement ou de
  dégâts.
- Combat tag optionnel, désactivé par défaut.
- Vérification de destination sûre (pas dans un bloc, le vide ou un fluide).
- Liste de mondes interdits pour `/sethome` et la téléportation.
- Permissions configurables par commande.
- Persistance JSON dans `homes.json`, avec sauvegarde de secours et écriture
  atomique.
- Validation des données au chargement et rollback mémoire en cas d'échec de
  sauvegarde.

### Technique

- Java 25, serveur Hytale `>=0.7.2 <0.8.0`.
- Package `assos.hytale.home`.
- `HomeService` : règles métier (limites, noms, index, mondes bloqués).
- `HomeStorage` : stockage JSON thread-safe, validation, backup et version.
- `HomeTeleportService` : cooldown, warmup, combat tag, safe landing.
- `HomeSafety` : vérification de la destination.
- `CooldownTracker` : suivi des cooldowns par joueur.
- `HomeTeleporter` : utilisation du composant natif `Teleport`.
- `HomesPage` : page custom via `InteractiveCustomUIPage`.
- L'interface réutilise le layout natif de Hytale et envoie ses textes en markup
  inline ; aucun asset pack n'est nécessaire.
- Tests JUnit 5 : **64 tests** (48 `HomeService`, 11 `HomeStorage`, 5
  `CooldownTracker`).

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
 1. home (default) — world: default — 12.5, 64.0, -30.2
 2. base — world: nether — 5.0, 70.0, 12.0
Use /home <number> or /home <name>.
```

`/home 2` et `/delhome 2` fonctionnent donc aussi bien avec l'index qu'avec le
nom.

## Interface

`/home` sans argument ouvre une page custom :

- titre **YOUR HOMES** ;
- indication **Click a home to teleport.** ;
- un bouton par home avec son nom, son monde et ses coordonnées ;
- le home par défaut est marqué `(default)` ;
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
| `RequirePermissions` | `false` | Exige les permissions par commande. |
| `TeleportCooldownSeconds` | `30` | Délai minimal entre deux téléportations. `0` désactive le cooldown. |
| `TeleportWarmupSeconds` | `3` | Délai avant téléportation. `0` téléporte immédiatement. |
| `CombatTagEnabled` | `false` | Bloque la téléportation pendant le combat. |
| `CombatTagSeconds` | `10` | Durée du combat tag quand il est activé. |
| `SafeLanding` | `true` | Vérifie que la destination n'est pas dans un bloc, le vide ou un fluide. |
| `BlockedWorlds` | *(vide)* | Mondes interdits, séparés par des virgules. |

Le warmup est annulé si le joueur se déplace ou subit des dégâts. Le cooldown
peut être contourné avec la permission `hytale.homes.bypass.cooldown`.

## Permissions

Quand `RequirePermissions` est `false`, toutes les commandes sont ouvertes à
tous les joueurs.

Quand il est `true`, les nœuds suivants sont exigés :

| Commande | Permission |
|---|---|
| `/home` | `hytale.homes.home` |
| `/sethome` | `hytale.homes.sethome` |
| `/homes` | `hytale.homes.homes` |
| `/delhome` | `hytale.homes.delhome` |

Le changement de `RequirePermissions` nécessite un redémarrage du serveur, car
les permissions sont fixées à l'enregistrement des commandes.

## Téléportation

La téléportation utilise le composant natif `Teleport` de Hytale. Elle fonctionne
donc entre les mondes chargés.

Ordre des vérifications :

1. monde interdit ;
2. téléportation inter-monde autorisée ;
3. monde cible chargé ;
4. combat tag, si activé ;
5. destination sûre, si `SafeLanding` est activé ;
6. cooldown, sauf permission de bypass ;
7. warmup, puis téléportation.

Le warmup compare la position et les timestamps de dégâts/combat au moment de la
téléportation. Le cooldown est enregistré après une téléportation réussie.

Le plugin est indépendant de `/tp home` (commande native liée au point de
respawn). `/sethome` ne modifie pas le point de respawn après une mort.

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
- Le fichier précédent est conservé dans `homes.json.bak` avant remplacement.
- Une version de données supérieure à celle supportée est refusée.
- Les entrées invalides (UUID, monde, coordonnées) sont ignorées avec un
  avertissement dans les logs.
- En cas d'échec de sauvegarde, la modification est annulée en mémoire pour
  rester cohérente avec le disque.
- Les fichiers générés par le serveur de dev (`homes.json`, `homes.json.bak`,
  `homes_config.json`, `homes_config.json.bak`) sont gitignorés et exclus du
  jar.

## Limites connues

- L'autocomplétion des noms de homes dans le client Hytale n'est pas disponible :
  le client ne demande pas de valeurs personnalisées au serveur.
- Il n'existe pas de keybind officiel côté Hytale : le client n'est pas moddable
  et les plugins serveur ne peuvent pas enregistrer une nouvelle touche. `/home`
  sans argument sert donc de point d'entrée vers l'interface.
- La vérification de destination ne peut pas être fiable si le chunk cible n'est
  pas encore chargé ; dans ce cas la téléportation est laissée au moteur.
- Un monde cible doit être chargé pour une téléportation inter-monde.

## Construction et tests

```bash
./gradlew test     # tests unitaires JUnit 5
./gradlew build    # build complet + validation du manifest
./gradlew runServer
```

Le jar est généré dans `build/libs/`.

Les tests unitaires couvrent `HomeService`, `HomeStorage` et `CooldownTracker`.
L'intégration serveur (commandes, interface, cooldown/warmup et téléportation
réelle) se vérifie manuellement avec `./gradlew runServer`.

La CI GitHub Actions vérifie `develop` et `main`, et publie le jar sur les tags
`v*`.

## Structure

```text
src/main/java/assos/hytale/home/
├── HytaleHomePlugin.java
├── HomePermissions.java
├── commands/
│   ├── DelHomeCommand.java
│   ├── HomeCommand.java
│   ├── HomeSuggestions.java
│   ├── HomesCommand.java
│   └── SetHomeCommand.java
├── config/
│   └── HomesConfig.java
├── home/
│   ├── CooldownTracker.java
│   ├── Home.java
│   ├── HomeRepository.java
│   ├── HomeSafety.java
│   ├── HomeService.java
│   ├── HomeStorage.java
│   ├── HomeTeleportException.java
│   ├── HomeTeleportService.java
│   └── HomeTeleporter.java
└── ui/
    └── HomesPage.java
```

## Compatibilité

- Java 25
- Serveur Hytale `>=0.7.2 <0.8.0`
- Plugin Gradle `com.azuredoom.hytale-tools`
