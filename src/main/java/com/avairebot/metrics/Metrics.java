package com.avairebot.metrics;

import ch.qos.logback.classic.LoggerContext;
import com.avairebot.AvaIre;
import com.avairebot.metrics.handlers.SparkExceptionHandler;
import com.avairebot.metrics.routes.GetMetrics;
import com.avairebot.metrics.routes.GetStats;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.logback.InstrumentedAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

public class Metrics {

    // ################################################################################
    // ##                              JDA Stats
    // ################################################################################

    public static final Counter jdaEvents = Counter.build()
        .name("avaire_jda_events_received_total")
        .help("All events that JDA provides us with by class")
        .labelNames("class") // GuildJoinedEvent, MessageReceivedEvent, ReconnectEvent etc
        .register();

    // ################################################################################
    // ##                             AvaIre Stats
    // ################################################################################

    public static final Gauge guilds = Gauge.build()
        .name("avaire_guilds_total")
        .help("Total number of guilds the bot is in")
        .register();

    public static final Gauge geoTracker = Gauge.build()
        .name("avaire_geo_tracker_total")
        .help("Total number of guilds split up by geographic location")
        .labelNames("region")
        .register();

    // Music

    public static final Counter searchRequests = Counter.build() //search requests issued by users
        .name("avaire_music_search_requests_total")
        .help("Total search requests")
        .register();

    public static final Counter tracksLoaded = Counter.build()
        .name("avaire_music_tracks_loaded_total")
        .help("Total tracks loaded by the audio loader")
        .register();

    public static final Counter trackLoadsFailed = Counter.build()
        .name("avaire_music_track_loads_failed_total")
        .help("Total failed track loads by the audio loader")
        .register();

    public static final Gauge musicPlaying = Gauge.build()
        .name("avaire_guild_music_playing_total")
        .help("Total number of guilds listening to music")
        .register();

    // Commands

    public static final Counter commandsRatelimited = Counter.build()
        .name("avaire_commands_ratelimited_total")
        .help("Total ratelimited commands")
        .labelNames("class") // use the simple name of the command class
        .register();

    public static final Counter slowmodeRatelimited = Counter.build()
        .name("avaire_slowmode_ratelimited_total")
        .help("Total ratelimited messages")
        .labelNames("channel")
        .register();

    public static final Counter commandsReceived = Counter.build()
        .name("avaire_commands_received_total")
        .help("Total received commands. Some of these might get ratelimited.")
        .labelNames("class") // use the simple name of the command class: PlayCommand, DanceCommand, ShardsCommand etc
        .register();

    public static final Counter commandsExecuted = Counter.build()
        .name("avaire_commands_executed_total")
        .help("Total executed commands by class")
        .labelNames("class") // use the simple name of the command class: PlayCommand, DanceCommand, ShardsCommand etc
        .register();

    public static final Histogram executionTime = Histogram.build() // commands execution time, excluding ratelimited ones
        .name("avaire_command_execution_duration_seconds")
        .help("Command execution time, excluding handling ratelimited commands.")
        .labelNames("class") // use the simple name of the command class: PlayCommand, DanceCommand, ShardsCommand etc
        .register();

    public static final Counter commandExceptions = Counter.build()
        .name("avaire_commands_exceptions_total")
        .help("Total uncaught exceptions thrown by command invocation")
        .labelNames("class") // class of the exception
        .register();

    // ################################################################################
    // ##                           Method Stuff
    // ################################################################################

    public static final Logger LOGGER = LoggerFactory.getLogger(Metrics.class);

    private static final int PORT = 1256;

    private static AvaIre avaire;
    private static boolean isSetup = false;

    public static void setup(AvaIre avaire) {
        if (isSetup) {
            throw new IllegalStateException("The metrics has already been setup!");
        }

        Metrics.avaire = avaire;

        final LoggerContext factory = (LoggerContext) LoggerFactory.getILoggerFactory();
        final ch.qos.logback.classic.Logger root = factory.getLogger(Logger.ROOT_LOGGER_NAME);
        final InstrumentedAppender prometheusAppender = new InstrumentedAppender();
        prometheusAppender.setContext(root.getLoggerContext());
        prometheusAppender.start();
        root.addAppender(prometheusAppender);

        // JVM (hotspot) metrics
        DefaultExports.initialize();

        LOGGER.info("Igniting Spark API on port: " + PORT);

        Spark.port(PORT);

        Spark.before(new HttpFilter());
        Spark.exception(Exception.class, new SparkExceptionHandler());

        Spark.get("/metrics", new GetMetrics(MetricsHolder.METRICS));
        Spark.get("/stats", new GetStats(MetricsHolder.METRICS));

        Metrics.isSetup = true;
    }

    public AvaIre getAvaire() {
        return avaire;
    }

    private static class MetricsHolder {
        private static final Metrics METRICS = new Metrics();
    }
}
