package com.kakarote.ai_crm.desktop;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import redis.embedded.RedisServer;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Desktop (local-first) bootstrap.
 *
 * <p>When the {@code desktop} profile is active, this starts an embedded PostgreSQL and an
 * embedded Redis (their binaries are bundled in the jar), creates/loads the CRM schema into a
 * local data directory on the user's machine, and injects the resulting connection settings into
 * the Spring {@link org.springframework.core.env.Environment} <em>before</em> the datasource,
 * Flyway and Redis are initialised. Together with {@code application-desktop.yml} (MinIO off →
 * local file storage, WeKnora off) this lets the backend run as a self-contained desktop process
 * with no external services — only the LLM API is remote.</p>
 *
 * <p>Registered via {@code META-INF/spring.factories}. Only compiled/packaged under the Maven
 * {@code desktop} profile, so the normal (Docker) build is unaffected.</p>
 */
public class DesktopEmbeddedServices implements EnvironmentPostProcessor {

    private static final String PROFILE = "desktop";
    private static final String DB_NAME = "wk_ai_crm";

    private static volatile boolean started = false;
    private static EmbeddedPostgres postgres;
    private static RedisServer redisServer;

    @Override
    public synchronized void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (started) {
            return;
        }
        if (!Arrays.asList(environment.getActiveProfiles()).contains(PROFILE)) {
            return;
        }
        try {
            Path dataHome = resolveDataHome(environment);
            Files.createDirectories(dataHome);
            Path pgData = dataHome.resolve("pgdata");

            postgres = EmbeddedPostgres.builder()
                    .setDataDirectory(pgData.toFile())
                    // reuse the existing cluster across restarts (don't wipe/re-initdb);
                    // an unclean prior shutdown is handled by Postgres crash recovery
                    .setCleanDataDirectory(false)
                    .start();
            ensureDatabaseAndSchema(postgres);

            int pgPort = postgres.getPort();
            int redisPort = freePort();
            redisServer = new RedisServer(redisPort);
            redisServer.start();

            Map<String, Object> props = new LinkedHashMap<>();
            props.put("spring.datasource.url", "jdbc:postgresql://127.0.0.1:" + pgPort + "/" + DB_NAME);
            props.put("spring.datasource.username", "postgres");
            props.put("spring.datasource.password", "postgres");
            props.put("spring.data.redis.host", "127.0.0.1");
            props.put("spring.data.redis.port", redisPort);
            props.put("spring.data.redis.password", "");
            props.put("spring.data.redis.database", 0);
            environment.getPropertySources().addFirst(new MapPropertySource("wkDesktopEmbedded", props));

            Runtime.getRuntime().addShutdownHook(new Thread(DesktopEmbeddedServices::stopAll, "wk-desktop-embedded-stop"));
            started = true;
            System.out.println("[desktop] embedded Postgres on port " + pgPort
                    + ", Redis on port " + redisPort + ", data dir " + dataHome);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start embedded desktop services", e);
        }
    }

    private Path resolveDataHome(ConfigurableEnvironment env) {
        String configured = env.getProperty("wk.desktop.data-dir");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("WK_DESKTOP_DATA_DIR");
        }
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured);
        }
        return Paths.get(System.getProperty("user.home"), ".wk-ai-crm");
    }

    private void ensureDatabaseAndSchema(EmbeddedPostgres pg) throws Exception {
        // 1) create the wk_ai_crm database if it does not exist (connect to the default 'postgres' db)
        DataSource adminDs = pg.getPostgresDatabase();
        try (Connection c = adminDs.getConnection(); Statement st = c.createStatement()) {
            boolean dbExists;
            try (ResultSet rs = st.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + DB_NAME + "'")) {
                dbExists = rs.next();
            }
            if (!dbExists) {
                st.execute("CREATE DATABASE " + DB_NAME);
            }
        }
        // 2) load the base schema into wk_ai_crm on first run (Flyway then applies V10+ increments)
        DataSource appDs = pg.getDatabase("postgres", DB_NAME);
        try (Connection c = appDs.getConnection(); Statement st = c.createStatement()) {
            boolean schemaExists;
            try (ResultSet rs = st.executeQuery("SELECT to_regclass('public.manager_user')")) {
                schemaExists = rs.next() && rs.getString(1) != null;
            }
            if (!schemaExists) {
                st.execute(sanitize(readResource("sql/desktop_base_schema.sql")));
            }
        }
    }

    /** Strip psql meta-commands and the CREATE DATABASE line so the script is JDBC-executable. */
    private String sanitize(String sql) {
        StringBuilder sb = new StringBuilder(sql.length());
        for (String line : sql.split("\n")) {
            String t = line.trim();
            if (t.startsWith("\\")) {
                continue;
            }
            if (t.toUpperCase(Locale.ROOT).startsWith("CREATE DATABASE")) {
                continue;
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private String readResource(String path) throws IOException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    private static void stopAll() {
        try {
            if (redisServer != null) {
                redisServer.stop();
            }
        } catch (Exception ignored) {
            // best effort
        }
        try {
            if (postgres != null) {
                postgres.close();
            }
        } catch (Exception ignored) {
            // best effort
        }
    }
}
