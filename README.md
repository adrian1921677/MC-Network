# NexusCosmetics

3D-Cosmetics für Minecraft-Server und -Netzwerke. Installieren, fertig: Das Plugin liefert sein Resource Pack selbst an die Spieler aus.

> Status: frühe Entwicklung (v0.1) · Minecraft Java **26.3** · Paper

## Features

- **Hüte** mit echten 3D-Modellen (Zylinder, Krone)
- **Capes mit Stoff-Physik**: schwingen beim Laufen, flattern im Fahrtwind, schwingen beim Drehen zur Seite
- **Animierte Cosmetics** (z. B. Galaxie-Cape mit funkelnden Sternen, leuchtet im Dunkeln)
- **Automatisches Resource Pack**: eingebauter Download-Server, kein manuelles Hochladen nötig
- **Freischaltung per Permission**: ideal für Ränge und Shop-Käufe
- Capes existieren nur als Netzwerk-Pakete und stören keine anderen Plugins (Teleports, Weltwechsel)

## Befehle & Rechte

| Befehl | Beschreibung | Permission |
|---|---|---|
| `/cosmetics` | Öffnet das Menü | `nexuscosmetics.use` (Standard: alle) |
| `/cosmetics off` | Legt alle Cosmetics ab | `nexuscosmetics.use` |

Einzelne Cosmetics: `nexuscosmetics.cosmetic.<id>` (z. B. `nexuscosmetics.cosmetic.crown`), alle: `nexuscosmetics.cosmetic.*`

## Konfiguration

In `plugins/NexusCosmetics/config.yml` die öffentliche Adresse des Servers bei `resource-pack.host` eintragen und den Port (Standard `8163`) freigeben.

## Entwicklung

Benötigt Java 25.

```bash
./gradlew build       # Plugin bauen -> build/libs/
./gradlew runServer   # Testserver mit Plugin starten
python tools/generate_assets.py   # 3D-Modelle und Texturen neu erzeugen
```

Die Modelle in `src/main/resources/pack/assets/nexus/models/item/` lassen sich in Blockbench öffnen.
