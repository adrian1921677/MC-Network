package dev.nexus.showcase;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.TextDisplay;

import java.util.List;

/**
 * Der Führer: eine Schaufensterpuppe mit der Haut des Verkäufers und eine Sprechblase
 * über ihrem Kopf.
 *
 * Warum keine echte NPC-Figur? Eine Puppe kann nicht laufen — aber sie muss auch nicht.
 * Zwischen zwei Haltestellen gleitet der Besucher, und der Führer steht am Ziel schon
 * bereit. Das sieht man nie, und dafür gibt es keine zappelnde Lauf-Animation, keine KI
 * und nichts, was wegläuft.
 */
public final class Guide {

    /** Höhe der Sprechblase über den Füßen der Puppe. */
    private static final double BUBBLE_HEIGHT = 2.45;

    private final Mannequin body;
    private final TextDisplay bubble;
    private final Component name;

    public Guide(Location at, PlayerProfile skin, Component name) {
        this.name = name;
        this.body = at.getWorld().spawn(at, Mannequin.class, mannequin -> {
            if (skin != null) {
                mannequin.setProfile(ResolvableProfile.resolvableProfile(skin));
            }
            mannequin.setImmovable(true);
            mannequin.setInvulnerable(true);
            mannequin.setPersistent(false);
            mannequin.setSilent(true);
            mannequin.setGravity(false);
            mannequin.setCollidable(false);
            // Sonst schwebt das Wort "Schaufensterpuppe" über ihm; der Name steht in der Sprechblase
            mannequin.setDescription(null);
        });
        this.bubble = at.getWorld().spawn(bubbleSpot(at), TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setPersistent(false);
            display.setSeeThrough(true);
            display.setLineWidth(260);
            display.setBackgroundColor(Color.fromARGB(170, 12, 12, 16));
            display.text(name);
        });
    }

    private static Location bubbleSpot(Location at) {
        return at.clone().add(0, BUBBLE_HEIGHT, 0);
    }

    /** Stellt den Führer an die nächste Haltestelle. Der Besucher ist dann noch unterwegs. */
    public void moveTo(Location at) {
        body.teleport(at);
        bubble.teleport(bubbleSpot(at));
    }

    public void say(Component line) {
        bubble.text(name.append(Component.newline()).append(line));
    }

    /** Nichts zu sagen: nur der Name bleibt stehen, sonst wirkt die Blase wie eingefroren. */
    public void silent(Component idle) {
        bubble.text(name.append(Component.newline()).append(idle));
    }

    /** Beide Teile, damit der Aufrufer sie vor allen anderen Spielern verstecken kann. */
    public List<Entity> entities() {
        return List.of(body, bubble);
    }

    public void remove() {
        body.remove();
        bubble.remove();
    }
}
