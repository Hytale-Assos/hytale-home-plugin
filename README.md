# Hytale Homes

Plugin de homes nommés pour serveur Hytale, développé par l'association étudiante
**HytaleAssos**.

Les joueurs peuvent enregistrer plusieurs points de téléportation et s'y rendre
avec `/home`, y compris vers un autre monde. Les homes sont conservés dans le
dossier de données du plugin et survivent aux redémarrages.

## Commandes

| Commande | Description |
|---|---|
| `/sethome` | Crée ou remplace le home `home` à la position actuelle. |
| `/sethome <nom>` | Crée ou remplace le home nommé. |
| `/home` | Téléporte vers `home`, ou vers l'unique home du joueur si `home` n'existe pas. |
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

Le home par défaut est `home`. `/home` sans argument utilise `home` ; si ce home
n'existe pas et que le joueur n'en a qu'un seul, il utilise celui-là.

Les commandes utilisent une syntaxe positionnelle, par exemple `/home spawn`,
et non `--name=spawn`. `/homes` affiche une liste numérotée :

```text
Your homes (2/5):
 1. home (default) [default] (12.5, 64.0, -30.2)
 2. base [nether] (5.0, 70.0, 12.0)
Use /home <number> or /home <name>.
```

`/home 2` et `/delhome 2` fonctionnent donc aussi bien avec l'index qu'avec le
nom. L'autocomplétion des noms de homes dans le client Hytale n'est pas
disponible : le client ne demande pas de valeurs personnalisées au serveur.

## Configuration

Le fichier `homes_config.json` est créé au premier lancement dans le dossier de
données du plugin.

| Clé | Défaut | Description |
|---|---|---|
| `MaxHomes` | `5` | Nombre maximal de homes par joueur. |
| `DefaultHomeName` | `home` | Nom utilisé quand aucun nom n'est fourni. |
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

L'écriture est atomique : le fichier est écrit dans un fichier temporaire puis
renommé. En cas de JSON invalide au chargement, le plugin journalise l'erreur et
démarre avec un stockage vide.

## Téléportation

La téléportation utilise le composant natif `Teleport` de Hytale. Elle fonctionne
donc entre les mondes chargés. Si le monde cible n'est pas chargé, la commande
affiche une erreur et ne déplace pas le joueur.

Le plugin est indépendant de `/tp home` (commande native liée au point de
respawn). `/sethome` ne modifie pas le point de respawn après une mort.

## Construction et tests

```bash
./gradlew test     # tests unitaires JUnit 5
./gradlew build    # build complet + validation du manifest
./gradlew runServer
```

Le jar est généré dans `build/libs/`.

Les tests unitaires couvrent `HomeService` et `HomeStorage`. L'intégration
serveur (commandes et téléportation réelle) se vérifie manuellement avec
`./gradlew runServer`.

## Compatibilité

- Java 25
- Serveur Hytale `>=0.7.2 <0.8.0`
- Plugin Gradle `com.azuredoom.hytale-tools`
