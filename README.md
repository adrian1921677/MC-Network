# NexusCosmetics

3D-Cosmetics und Emotes für Minecraft-Server und -Netzwerke. Installieren, fertig: Das Plugin liefert sein Resource Pack selbst an die Spieler aus.

> Status: frühe Entwicklung (v0.1) · Minecraft Java **26.3** · Paper

## Features

- **Hüte** mit echten 3D-Modellen, inklusive **sprechendem Hut** (Sprechblase, Mundbewegung, Team-Zuteilung) und schwebendem **Heiligenschein**
- **Capes und Zauberumhänge mit Stoff-Physik**: schwingen beim Laufen, flattern im Fahrtwind, schwingen beim Drehen zur Seite
- **Haustiere mit eigener Persönlichkeit**: Drache, Geist, Pinguin, Kätzchen, Bienchen, Pilzchen, Eule, jeweils mit besonderen Animationen
- **Emotes**: animierte 3D-Emojis über dem Kopf und Körper-Posen (Sitzen, Liegen, Tanzen …)
- **Automatisches Resource Pack**: eingebauter Download-Server, eigene Modelle werden automatisch eingebaut
- **Voll konfigurierbar**: Cosmetics und Emotes an/aus, Namen, freie oder Premium-Inhalte, eigene Cosmetics, Deutsch und Englisch
- Paket-basierte Cosmetics stören keine anderen Plugins (Teleports, Weltwechsel, Minigames)

## Befehle & Rechte

| Befehl | Beschreibung | Permission |
|---|---|---|
| `/cosmetics` | Öffnet das Menü | `nexuscosmetics.use` (Standard: alle) |
| `/cosmetics off` | Legt alle Cosmetics ab | `nexuscosmetics.use` |
| `/cosmetics reload` | Lädt alle Dateien neu | `nexuscosmetics.admin` (Standard: OP) |
| `/emote [name]` | Emote-Menü bzw. Emote abspielen (auch: Schleichen + F) | `nexuscosmetics.use` |

- Einzelne Cosmetics: `nexuscosmetics.cosmetic.<id>`, alle: `nexuscosmetics.cosmetic.*`
- Einzelne Emotes: `nexuscosmetics.emote.<id>`, alle: `nexuscosmetics.emote.*`
- Mit `unlocked-by-default: true` (Cosmetics) bzw. `free: true` (Emotes) braucht man keine Permission.

## Konfiguration

Alle Dateien liegen in `plugins/NexusCosmetics/`:

| Datei | Inhalt |
|---|---|
| `config.yml` | Sprache, Resource Pack (Adresse/Port), Emote-Cooldown, Sprechpausen des Huts |
| `cosmetics.yml` | Alle Cosmetics: an/aus, Name, Slot, Verhalten, Leuchten, frei/Premium, eigenes Modell |
| `emotes.yml` | Alle Emotes: an/aus, Name, Typ, frei/Premium |
| `lang/messages_de.yml`, `lang/messages_en.yml` | Alle Texte (MiniMessage-Farben), auch die Sprüche des Huts |
| `pack/` | Eigene Resource-Pack-Dateien, werden automatisch ins Pack eingebaut |

Für einen echten Server: In der `config.yml` bei `resource-pack.host` die öffentliche Adresse eintragen und den Port (Standard `8163`) freigeben.

## Entwicklung

Benötigt Java 25.

```bash
./gradlew build       # Plugin bauen -> build/libs/
./gradlew runServer   # Testserver mit Plugin starten
python tools/generate_assets.py   # 3D-Modelle und Texturen neu erzeugen
```

Die Modelle in `src/main/resources/pack/assets/nexus/models/item/` lassen sich in Blockbench öffnen.
