package dev.nexus.cosmetics.render.cape;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Ein Teil, das hinten am Spieler "reitet" (Cape, Flügel). Gibt es in zwei Kopien:
 * für alle anderen und nur für den Träger selbst (siehe BackCosmetic).
 */
interface BackPiece {

    void show(Player viewer);

    void hide(Player viewer);

    void forget(UUID viewer);

    void destroy();

    void tick();

    int[] entityIds();
}
