package com.koffee.RuneDragons.src.main.java.com.koffee;

import com.google.inject.Provides;
import com.koffee.EthanApiPlugin.EthanApiPlugin;
import com.koffee.PacketUtils.PacketUtilsPlugin;
import com.koffee.RuneDragons.src.main.java.com.koffee.data.State;
import com.koffee.RuneDragons.src.main.java.com.koffee.utils.CalculationUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import static com.koffee.RuneDragons.src.main.java.com.koffee.data.Constants.*;


@Getter
@PluginDependency(EthanApiPlugin.class)
@PluginDependency(PacketUtilsPlugin.class)
@PluginDescriptor(
        name = "Rune Dragons Killer",
        enabledByDefault = false,
        description = "This plugin will kill rune dragons for you",
        tags = {"rune", "dragon", "bot"}
)
@Slf4j
public class RuneDragonsPlugin extends Plugin {

    public static Player player;
    // Variables
    private boolean started;
    private Instant timer;
    private int timeout;
    @Getter
    private State state;
    // Injects
    @Inject
    private Client client;
    @Inject
    private RuneDragonsConfig config;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private RuneDragonsOverlay runeDragonsOverlay;
    @Inject
    private CalculationUtils calculationUtils;
    @Inject
    private ChatMessageManager messageManager;

    @Override
    protected void startUp() throws Exception {
    }

    @Override
    protected void shutDown() throws Exception {
    }

    @Provides
    private RuneDragonsConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(RuneDragonsConfig.class);
    }

    // Subscribes
    @Subscribe
    private void onGameTick(GameTick event) {
        if (!EthanApiPlugin.loggedIn() || !started) {
            // We do an early return if the user isn't logged in\
            return;
        }
        player = client.getLocalPlayer();
        if (player == null || !started) {
            return;
        }
        if (!client.isResized()) {
            ChatMessageBuilder builder = new ChatMessageBuilder();
            builder.append(Color.RED, "You must be set to resizable mode to use Void Agility.");
            String msg = builder.build();
            messageManager.queue(QueuedMessage.builder().name("RuneDragons").runeLiteFormattedMessage(msg).build());
            return;
        }
        state = getCurrentState();
        switch (state) {
            case TIMEOUT:
                timeout--;
                break;
            case MOVING:
            case TELEPORT_LITH:
            case DEPOSIT:
            case DRINK_POOL:
            case ENTER_DRAGONS:
            case ANIMATING:
                break;
        }
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged e) {
        if (e.getGroup().equals("RuneDragons") && e.getKey().equals("startButton")) {
            toggle();
        }
    }

    // Utils
    private State getCurrentState() {
        if (timeout > 0) {
            return State.TIMEOUT;
        }

        if (EthanApiPlugin.isMoving()) {
            timeout = tickDelay();
            return State.MOVING;
        }

        if (inEdgeville()) {
            timeout = tickDelay();
            return State.DEPOSIT;
        }

        if (inPOH()) {
            timeout = tickDelay();
            return State.DRINK_POOL;
        }

        if (inLithkren()) {
            timeout = tickDelay();
            if (inDragons()) {
                return State.ENTER_DRAGONS;
            }
            return State.TELEPORT_LITH;
        }

        return State.ANIMATING;
    }


    public void toggle() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        if (!started) {
            overlayManager.add(runeDragonsOverlay);
            timer = Instant.now();
            started = true;
        } else {
            resetPlugin();
            started = false;
        }
    }

    public String getElapsedTime() {
        Duration duration = Duration.between(timer, Instant.now());
        long durationInMillis = duration.toMillis();
        long second = (durationInMillis / 1000) % 60;
        long minute = (durationInMillis / (1000 * 60)) % 60;
        long hour = (durationInMillis / (1000 * 60 * 60)) % 24;

        return String.format("%02d:%02d:%02d", hour, minute, second);
    }

    private void resetPlugin() {
        overlayManager.remove(runeDragonsOverlay);
        timer = null;
        timeout = 0;
    }

    protected long sleepDelay() {
        return calculationUtils.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
    }

    protected int tickDelay() {
        return (int) calculationUtils.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
    }

    public boolean inPOH() {
        boolean status = Arrays.stream(client.getMapRegions()).anyMatch(HOME_REGIONS::contains);
        if (config.debugMode()) {
            sendDebugMessage("We are in POH - " + status);
        }
        return status;
    }

    public boolean inDragons() {
        boolean status = client.getLocalPlayer().getWorldArea().intersectsWith(RUNE_DRAGONS);
        if (config.debugMode()) {
            sendDebugMessage("We are in dragons - " + status);
        }
        return status;
    }

    public boolean inLithkren() {
        boolean status = Arrays.stream(client.getMapRegions()).anyMatch(LITH_REGIONS::contains);
        if (config.debugMode()) {
            sendDebugMessage("We are in lithkren - " + status);
        }
        return status;
    }

    public boolean inEdgeville() {
        boolean status = client.getLocalPlayer().getWorldArea().intersectsWith(EDGEVILLE_TELE);
        if (config.debugMode()) {
            sendDebugMessage("We are in edgevile - " + status);
        }
        return status;
    }

    private void sendDebugMessage(String message) {
        String msg = new ChatMessageBuilder().append(Color.RED, message).build();
        messageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(msg)
                .build());
    }
}
