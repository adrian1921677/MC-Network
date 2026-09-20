package dev.nexus.cosmetics.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.nexus.cosmetics.config.StorageSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Profil-Speicher für Netzwerke: eine Zeile pro Spieler in MySQL oder MariaDB.
 *
 * Drei Dinge, die ein Netzwerk braucht und die diese Klasse löst:
 *
 * 1. Übergabe zwischen Servern. Jede Zeile merkt sich in "online_on", auf welchem Server der
 *    Spieler gerade ist. Beim Verlassen wird das Feld geleert. Der nächste Server wartet beim
 *    Laden kurz darauf, damit er keine veralteten Daten liest.
 *
 * 2. Änderungen für Offline-Spieler. {@link #modify} liest, ändert und schreibt in einer
 *    Transaktion mit Zeilensperre. Zwei Server können sich dabei nicht gegenseitig überschreiben.
 *
 * 3. Änderungen für Spieler, die gerade woanders online sind. Solche Zeilen werden als
 *    "needs_reload" markiert; jeder Server fragt regelmäßig seine eigenen Spieler ab und lädt
 *    betroffene Profile nach.
 *
 * Alle Datenbankzugriffe laufen im Hintergrund. Pro Spieler werden sie aneinandergereiht, damit
 * ein Laden nie einen laufenden Speichervorgang überholt.
 */
public final class MySqlCosmeticStorage implements CosmeticStorage {

    /** Wie lange beim Laden höchstens gewartet wird, bis der vorherige Server das Profil freigibt. */
    private static final int HANDOVER_ATTEMPTS = 5;
    private static final long HANDOVER_DELAY_MILLIS = 120;

    private final HikariDataSource dataSource;
    private final String table;
    private final String serverName;
    private final Logger logger;
    private final ExecutorService executor;
    private final ScheduledExecutorService sync;
    /** Pro Spieler die zuletzt eingereihte Aufgabe, damit Zugriffe in der richtigen Reihenfolge laufen. */
    private final ConcurrentHashMap<UUID, CompletableFuture<Void>> chains = new ConcurrentHashMap<>();
    private volatile Consumer<UUID> externalListener;

    public MySqlCosmeticStorage(StorageSettings settings, Logger logger) throws SQLException {
        this.logger = logger;
        this.table = settings.tablePrefix() + "profiles";
        this.serverName = settings.serverName().isBlank()
                ? "server-" + Integer.toHexString(System.identityHashCode(this))
                : trim(settings.serverName(), 64);

        HikariConfig config = new HikariConfig();
        config.setPoolName("NexusCosmetics");
        config.setJdbcUrl(settings.jdbcUrl());
        config.setUsername(settings.user());
        config.setPassword(settings.password());
        // Eine Verbindung mehr als Arbeits-Threads: der Abgleich-Thread braucht auch eine,
        // sonst muss er warten, wenn gerade alle Threads beschäftigt sind.
        config.setMaximumPoolSize(settings.poolSize() + 1);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(10));
        config.setMaxLifetime(TimeUnit.MINUTES.toMillis(25));
        this.dataSource = new HikariDataSource(config);

        this.executor = Executors.newFixedThreadPool(settings.poolSize(),
                runnable -> new Thread(runnable, "NexusCosmetics-Storage"));
        this.sync = Executors.newSingleThreadScheduledExecutor(
                runnable -> new Thread(runnable, "NexusCosmetics-Sync"));

        try {
            createTable();
            clearOwnServer();
        } catch (SQLException | RuntimeException exception) {
            // Sonst blieben Pool und Threads offen, während das Plugin auf YAML zurückfällt
            shutdownResources();
            throw exception;
        }

        if (settings.syncIntervalSeconds() > 0) {
            long interval = settings.syncIntervalSeconds();
            sync.scheduleWithFixedDelay(this::pollExternalChanges, interval, interval, TimeUnit.SECONDS);
        }
    }

    // ------------------------------------------------------------------ Schema

    private void createTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS `%s` (
                  `uuid`         CHAR(36)     NOT NULL,
                  `equipped`     TEXT         NULL,
                  `owned`        MEDIUMTEXT   NULL,
                  `crate_keys`   TEXT         NULL,
                  `favorites`    TEXT         NULL,
                  `outfits`      TEXT         NULL,
                  `online_on`    VARCHAR(64)  NULL,
                  `needs_reload` TINYINT(1)   NOT NULL DEFAULT 0,
                  `updated_at`   BIGINT       NOT NULL DEFAULT 0,
                  PRIMARY KEY (`uuid`),
                  KEY `idx_nexus_sync` (`online_on`, `needs_reload`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """.formatted(table);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
        // Tabellen aus einer aelteren Version nachruesten
        addColumnIfMissing("favorites", "TEXT NULL");
        addColumnIfMissing("outfits", "TEXT NULL");
    }

    /**
     * Haengt eine Spalte an, falls sie noch fehlt.
     *
     * MySQL kennt kein "ADD COLUMN IF NOT EXISTS", deshalb fragen wir vorher den Katalog.
     */
    private void addColumnIfMissing(String column, String definition) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
                statement.setString(1, table);
                statement.setString(2, column);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next() && result.getInt(1) > 0) {
                        return;
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition)) {
                statement.execute();
                logger.info("Spalte '" + column + "' wurde nachtraeglich angelegt.");
            }
        } catch (SQLException exception) {
            logger.warning("Spalte '" + column + "' konnte nicht angelegt werden: " + exception.getMessage());
        }
    }

    /** Nach einem Absturz können noch Spieler auf uns eingetragen sein. Beim Start aufräumen. */
    private void clearOwnServer() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE `" + table + "` SET `online_on` = NULL WHERE `online_on` = ?")) {
            statement.setString(1, serverName);
            statement.executeUpdate();
        } catch (SQLException exception) {
            logger.warning("Alte Server-Markierungen konnten nicht aufgeräumt werden: " + exception.getMessage());
        }
    }

    // ------------------------------------------------------------------ Lesen und Schreiben

    @Override
    public CompletableFuture<PlayerProfile> load(UUID player) {
        return submit(player, "Laden", new PlayerProfile(), connection -> {
            PlayerProfile profile = readWithHandover(connection, player);
            claim(connection, player);
            return profile;
        });
    }

    @Override
    public CompletableFuture<Void> save(UUID player, PlayerProfile profile) {
        PlayerProfile snapshot = profile.copy();
        return submit(player, "Speichern", null, connection -> {
            write(connection, player, snapshot, false);
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> modify(UUID player, Consumer<PlayerProfile> change) {
        return submit(player, "Ändern", null, connection -> {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                PlayerProfile profile = read(connection, player, true);
                change.accept(profile);
                write(connection, player, profile, true);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
            return null;
        });
    }

    @Override
    public void release(UUID player) {
        submit(player, "Freigeben", null, connection -> {
            try (PreparedStatement statement = connection.prepareStatement("UPDATE `" + table
                    + "` SET `online_on` = NULL, `needs_reload` = 0 WHERE `uuid` = ? AND `online_on` = ?")) {
                statement.setString(1, player.toString());
                statement.setString(2, serverName);
                statement.executeUpdate();
            }
            return null;
        });
    }

    /**
     * Liest das Profil. Steht die Zeile noch auf einem anderen Server, warten wir kurz: der
     * Spieler hat den vorherigen Server gerade erst verlassen und dessen letzter Schreibvorgang
     * könnte noch unterwegs sein.
     */
    private PlayerProfile readWithHandover(Connection connection, UUID player) throws SQLException {
        for (int attempt = 0; attempt < HANDOVER_ATTEMPTS; attempt++) {
            if (!heldByOtherServer(connection, player)) {
                break;
            }
            try {
                Thread.sleep(HANDOVER_DELAY_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return read(connection, player, false);
    }

    private boolean heldByOtherServer(Connection connection, UUID player) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT `online_on` FROM `" + table + "` WHERE `uuid` = ?")) {
            statement.setString(1, player.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return false;
                }
                String owner = result.getString("online_on");
                return owner != null && !owner.equals(serverName);
            }
        }
    }

    private PlayerProfile read(Connection connection, UUID player, boolean lockRow) throws SQLException {
        String sql = "SELECT `equipped`, `owned`, `crate_keys`, `favorites`, `outfits` FROM `" + table + "` WHERE `uuid` = ?"
                + (lockRow ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return new PlayerProfile();
                }
                return ProfileCodec.fromJson(result.getString("equipped"), result.getString("owned"),
                        result.getString("crate_keys"), result.getString("favorites"), result.getString("outfits"));
            }
        }
    }

    /**
     * Schreibt das Profil. {@code flagReload} setzt die Nachlade-Markierung, falls der Spieler
     * gerade auf einem anderen Server online ist — der holt sich die Änderung dann selbst ab.
     */
    private void write(Connection connection, UUID player, PlayerProfile profile, boolean flagReload)
            throws SQLException {
        String reload = flagReload
                ? "CASE WHEN `online_on` IS NULL OR `online_on` = ? THEN `needs_reload` ELSE 1 END"
                : "`needs_reload`";
        String sql = "INSERT INTO `" + table
                + "` (`uuid`, `equipped`, `owned`, `crate_keys`, `favorites`, `outfits`, `updated_at`) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                + "`equipped` = ?, `owned` = ?, `crate_keys` = ?, `favorites` = ?, `outfits` = ?, "
                + "`updated_at` = ?, `needs_reload` = " + reload;
        String equipped = ProfileCodec.equippedToJson(profile);
        String owned = ProfileCodec.ownedToJson(profile);
        String keys = ProfileCodec.keysToJson(profile);
        String favorites = ProfileCodec.favoritesToJson(profile);
        String outfits = ProfileCodec.outfitsToJson(profile);
        long now = System.currentTimeMillis();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            statement.setString(index++, player.toString());
            for (int pass = 0; pass < 2; pass++) {
                statement.setString(index++, equipped);
                statement.setString(index++, owned);
                statement.setString(index++, keys);
                statement.setString(index++, favorites);
                statement.setString(index++, outfits);
                statement.setLong(index++, now);
            }
            if (flagReload) {
                statement.setString(index, serverName);
            }
            statement.executeUpdate();
        }
    }

    /** Trägt uns als aktuellen Server ein und löscht eine eventuell offene Nachlade-Markierung. */
    private void claim(Connection connection, UUID player) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO `" + table
                + "` (`uuid`, `online_on`, `needs_reload`, `updated_at`) VALUES (?, ?, 0, ?) "
                + "ON DUPLICATE KEY UPDATE `online_on` = ?, `needs_reload` = 0")) {
            statement.setString(1, player.toString());
            statement.setString(2, serverName);
            statement.setLong(3, System.currentTimeMillis());
            statement.setString(4, serverName);
            statement.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ Änderungen anderer Server

    @Override
    public void onExternalChange(Consumer<UUID> listener) {
        this.externalListener = listener;
    }

    /**
     * Sucht Profile, die ein anderer Server verändert hat, während der Spieler bei uns online ist.
     * Die Markierung wird sofort zurückgesetzt, damit dieselbe Änderung nicht doppelt gemeldet wird.
     */
    private void pollExternalChanges() {
        Consumer<UUID> listener = externalListener;
        if (listener == null) {
            return;
        }
        List<UUID> changed = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT `uuid` FROM `" + table
                    + "` WHERE `online_on` = ? AND `needs_reload` = 1")) {
                statement.setString(1, serverName);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        try {
                            changed.add(UUID.fromString(result.getString("uuid")));
                        } catch (IllegalArgumentException ignored) {
                            // keine gültige UUID in der Zeile: überspringen
                        }
                    }
                }
            }
            if (changed.isEmpty()) {
                return;
            }
            try (PreparedStatement statement = connection.prepareStatement("UPDATE `" + table
                    + "` SET `needs_reload` = 0 WHERE `uuid` = ? AND `online_on` = ?")) {
                for (UUID player : changed) {
                    statement.setString(1, player.toString());
                    statement.setString(2, serverName);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (SQLException exception) {
            logger.warning("Abgleich mit den anderen Servern fehlgeschlagen: " + exception.getMessage());
            return;
        }
        changed.forEach(listener);
    }

    // ------------------------------------------------------------------ Ablauf und Aufräumen

    @FunctionalInterface
    private interface SqlTask<T> {
        T run(Connection connection) throws SQLException;
    }

    /**
     * Reiht eine Aufgabe hinter der vorherigen desselben Spielers ein.
     *
     * Schlägt sie fehl, wird geloggt und {@code fallback} zurückgegeben — ein kurzzeitig nicht
     * erreichbarer Datenbankserver darf den Login nicht hängen lassen.
     */
    private <T> CompletableFuture<T> submit(UUID player, String what, T fallback, SqlTask<T> task) {
        CompletableFuture<T> result = new CompletableFuture<>();
        AtomicReference<CompletableFuture<Void>> queued = new AtomicReference<>();
        chains.compute(player, (uuid, previous) -> {
            CompletableFuture<Void> base = previous != null ? previous : CompletableFuture.completedFuture(null);
            CompletableFuture<Void> next = base.handleAsync((ignored, error) -> {
                try (Connection connection = dataSource.getConnection()) {
                    result.complete(task.run(connection));
                } catch (Exception exception) {
                    logger.warning(what + " des Profils von " + uuid + " fehlgeschlagen: " + exception.getMessage());
                    result.complete(fallback);
                }
                return null;
            }, executor);
            queued.set(next);
            return next;
        });
        // Die Kette wieder entfernen, sobald nichts mehr für diesen Spieler ansteht
        queued.get().whenComplete((ignored, error) -> chains.remove(player, queued.get()));
        return result;
    }

    @Override
    public void close() {
        sync.shutdownNow();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warning("Nicht alle Profile konnten rechtzeitig gespeichert werden.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        clearOwnServer();
        dataSource.close();
    }

    /** Gibt Pool und Threads frei, ohne noch einmal auf die Datenbank zuzugreifen. */
    private void shutdownResources() {
        sync.shutdownNow();
        executor.shutdownNow();
        dataSource.close();
    }

    @Override
    public String describe() {
        return "MySQL (" + dataSource.getUsername() + "@" + hostOf(dataSource.getJdbcUrl())
                + ", Server '" + serverName + "')";
    }

    private static String hostOf(String jdbcUrl) {
        int start = jdbcUrl.indexOf("//");
        if (start < 0) {
            return jdbcUrl;
        }
        int end = jdbcUrl.indexOf('?', start);
        return end < 0 ? jdbcUrl.substring(start + 2) : jdbcUrl.substring(start + 2, end);
    }

    private static String trim(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
