# NexusCosmetics

3D-Cosmetics und Emotes für Minecraft-Server und -Netzwerke. Installieren, fertig: Das Plugin liefert sein Resource Pack selbst an die Spieler aus.

> Status: frühe Entwicklung (v0.1) · Minecraft Java **26.3** · Paper

## Features

**174 Cosmetics** (rund 59 Hüte, 57 Rückenteile, 56 Haustiere) und 13 Emotes, darunter **20 Element-Welten** als Sammel-Sets mit animierten, leuchtenden Texturen und Partikel-Auren.


- **14 Hüte** mit echten 3D-Modellen, inklusive **sprechendem Hut** (Sprechblase, Mundbewegung, Team-Zuteilung) und schwebendem **Heiligenschein**
- **Capes und Zauberumhänge mit Stoff-Physik**: schwingen beim Laufen, flattern im Fahrtwind, schwingen beim Drehen zur Seite
- **Flügel** (Engel, Dämon, Schmetterling, Drache, Fee): schlagen, breiten sich beim Fallen aus, falten sich beim Schleichen
- **12 Haustiere mit eigener Persönlichkeit**: Drache, Geist, Pinguin, Kätzchen, Bienchen, Pilzchen, Eule, Fuchs, Axolotl, Panda, Schleim, Baby-Phönix
- **Emotes**: animierte 3D-Emojis über dem Kopf und Körper-Posen (Sitzen, Liegen, Tanzen …)
- **Automatisches Resource Pack**: eingebauter Download-Server, eigene Modelle werden automatisch eingebaut
- **Cosmetic-Truhen** mit Öffnungs-Animation, Seltenheiten und Shop-Befehlen
- **ULTRA-Stufe**: riesige, animierte Cosmetics mit Partikel-Aura (Phönixschwingen, Himmelsdrache, Sternenkrone, Aurora-Umhang) und eigener Truhen-Show
- **Menü-Item** in der Hotbar (Geschenk-Modell), Rechtsklick öffnet das Menü
- **Voll konfigurierbar**: Cosmetics und Emotes an/aus, Namen, freie oder Premium-Inhalte, eigene Cosmetics, Deutsch und Englisch
- Paket-basierte Cosmetics stören keine anderen Plugins (Teleports, Weltwechsel, Minigames)

## Befehle & Rechte

| Befehl | Beschreibung | Permission |
|---|---|---|
| `/cosmetics` | Öffnet das Menü | `nexuscosmetics.use` (Standard: alle) |
| `/cosmetics off` | Legt alle Cosmetics ab | `nexuscosmetics.use` |
| `/cosmetics item` | Gibt das Menü-Item zurück | `nexuscosmetics.use` |
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
python tools/generate_assets.py   # 3D-Modelle und Texturen neu erzeugen (inkl. tools/themes.py)
```

Die Modelle in `src/main/resources/pack/assets/nexus/models/item/` lassen sich in Blockbench öffnen.
