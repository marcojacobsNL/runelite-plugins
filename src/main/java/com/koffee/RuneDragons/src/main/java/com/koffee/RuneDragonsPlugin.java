package com.koffee.RuneDragons.src.main.java.com.koffee;

import com.google.inject.Provides;
import com.koffee.EthanApiPlugin.Collections.Bank;
import com.koffee.EthanApiPlugin.Collections.NPCs;
import com.koffee.EthanApiPlugin.Collections.TileObjects;
import com.koffee.EthanApiPlugin.EthanApiPlugin;
import com.koffee.InteractionApi.BankInteraction;
import com.koffee.InteractionApi.InventoryInteraction;
import com.koffee.InteractionApi.NPCInteraction;
import com.koffee.InteractionApi.TileObjectInteraction;
import com.koffee.KoffeeUtils.src.main.java.com.plugins.API.InventoryUtil;
import com.koffee.KoffeeUtils.src.main.java.com.plugins.API.ObjectUtil;
import com.koffee.PacketUtils.PacketUtilsPlugin;
import com.koffee.RuneDragons.src.main.java.com.koffee.data.State;
import com.koffee.RuneDragons.src.main.java.com.koffee.utils.CalculationUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

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
    public static ArrayList<Integer> inventorySetup = new ArrayList<>();
    // Variables
    private boolean started;
    private Instant timer;
    private int timeout;
    private boolean deposited = false;
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
            sendDebugMessage("You must be set to resizable mode to use RuneDragons.");
            return;
        }
        state = getCurrentState();
        switch (state) {
            case TIMEOUT:
                timeout--;
                break;
            case FIND_BANK:
                interactWithBank();
                break;
            case WITHDRAW:
                withdrawItems();
                timeout = tickDelay();
                break;
            case DEPOSIT:
                depositItems();
                timeout = tickDelay();
                deposited = true;
                break;
            case TELEPORT_HOME:
                teleportHome();
                break;
            case TELEPORT_TO_EDGE:
                teleportEdge();
                break;
            case MOVING:
            case TELEPORT_LITH:
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
            if (shouldRestock() && !Bank.isOpen()) {
                return State.FIND_BANK;
            } else if (shouldRestock() && Bank.isOpen() && !InventoryUtil.isEmpty() && !deposited) {
                return State.DEPOSIT;
            } else if (shouldRestock() && Bank.isOpen() && deposited) {
                return State.WITHDRAW;
            }
        }

        if (inPOH()) {
            if (shouldRestock()) {
                return State.TELEPORT_TO_EDGE;
            } else if (client.getBoostedSkillLevel(Skill.PRAYER) < client.getRealSkillLevel(Skill.PRAYER) && config.usePOHpool()) {
                return State.DRINK_POOL;
            } else if (client.getBoostedSkillLevel(Skill.HITPOINTS) < client.getRealSkillLevel(Skill.HITPOINTS) && config.usePOHpool()) {
                return State.DRINK_POOL;
            } else if (config.usePOHdigsite() || InventoryUtil.hasItem(ItemID.DIGSITE_PENDANT_5)) {
                return State.TELEPORT_LITH;
            }
        } else {
            if (inDragons() && (shouldRestock() || client.getBoostedSkillLevel(Skill.HITPOINTS) <= config.eatMin() && !InventoryUtil.hasItem(config.foodID()))) {
                timeout = tickDelay();
                return State.TELEPORT_HOME;
            }
            if (inEdgeville() && !shouldRestock()) {
                timeout = tickDelay();
                return State.TELEPORT_HOME;
            }
            if (!inEdgeville() && shouldRestock()) {
                timeout = tickDelay();
                return State.TELEPORT_HOME;
            }
            if (!inEdgeville() && !inDragons() && !inLithkren()) {
                timeout = tickDelay();
                return State.TELEPORT_HOME;
            }
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
            initInventory();
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
        inventorySetup.clear();
        started = false;
        deposited = false;
    }

    private void initInventory() {
        inventorySetup.clear();
        if (config.useVengeance()) {
            inventorySetup.add(ItemID.RUNE_POUCH);
        }
        if (config.superantifire()) {
            inventorySetup.add(ItemID.EXTENDED_SUPER_ANTIFIRE4);
        }
        if (!config.superantifire()) {
            inventorySetup.add(ItemID.EXTENDED_ANTIFIRE4);
        }
        if (config.supercombats()) {
            inventorySetup.add(ItemID.DIVINE_SUPER_COMBAT_POTION4);
        }
        if (!config.supercombats()) {
            inventorySetup.add(ItemID.SUPER_COMBAT_POTION4);
        }
        if (!config.usePOHdigsite()) {
            inventorySetup.add(ItemID.DIGSITE_PENDANT_5);
        }
        if (config.useSpec()) {
            inventorySetup.add(config.specId());
        }
        inventorySetup.add(ItemID.PRAYER_POTION4);
        inventorySetup.add(ItemID.TELEPORT_TO_HOUSE);
        inventorySetup.add(config.foodID());
        log.info("required inventory items: {}", inventorySetup.toString());
    }

    protected long sleepDelay() {
        return calculationUtils.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
    }

    protected int tickDelay() {
        return (int) calculationUtils.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
    }

    protected boolean inPOH() {
        boolean status = Arrays.stream(client.getMapRegions()).anyMatch(HOME_REGIONS::contains);
        if (config.debugMode()) {
            sendDebugMessage("We are in POH - " + status);
        }
        return status;
    }

    protected boolean inDragons() {
        boolean status = client.getLocalPlayer().getWorldArea().intersectsWith(RUNE_DRAGONS);
        if (config.debugMode()) {
            sendDebugMessage("We are in dragons - " + status);
        }
        return status;
    }

    protected boolean inLithkren() {
        boolean status = Arrays.stream(client.getMapRegions()).anyMatch(LITH_REGIONS::contains);
        if (config.debugMode()) {
            sendDebugMessage("We are in lithkren - " + status);
        }
        return status;
    }

    protected boolean inEdgeville() {
        boolean status = client.getLocalPlayer().getWorldArea().intersectsWith(EDGEVILLE_TELE);
        if (config.debugMode()) {
            sendDebugMessage("We are in edgevile - " + status);
        }
        return status;
    }

    protected boolean shouldRestock() {
        if (!InventoryUtil.hasItem(config.foodID())) {
            if (config.debugMode()) {
                sendDebugMessage("We are missing food");
            }
            if (inDragons()) {
                return client.getBoostedSkillLevel(Skill.HITPOINTS) <= config.eatMin();
            } else {
                return true;
            }
        }
        if (config.superantifire() && !InventoryUtil.hasAnyItems(SUPER_EXTENDED_ANTIFIRE_POTS)) {
            if (config.debugMode()) {
                sendDebugMessage("We are missing super extended antifire");
            }
            if (inDragons() || inLithkren()) {
                return client.getVarbitValue(Varbits.SUPER_ANTIFIRE) == 0;
            } else {
                return true;
            }
        }
        if (!config.superantifire() && !InventoryUtil.hasAnyItems(EXTENDED_ANTIFIRE_POTS)) {
            if (config.debugMode()) {
                sendDebugMessage("We are missing extended antifire");
            }
            if (inDragons() || inLithkren()) {
                return client.getVarbitValue(3981) == 0;
            } else {
                return true;
            }
        }
        if (config.supercombats() && !InventoryUtil.hasAnyItems(DIVINE_SUPER_COMBAT_POTS)) {
            if (config.debugMode()) {
                sendDebugMessage("We are missing divine super combat");
            }
            if (inDragons() || inLithkren()) {
                return client.getBoostedSkillLevel(Skill.STRENGTH) <= config.combatMin();
            } else {
                return true;
            }
        }
        if (!config.supercombats() && !InventoryUtil.hasAnyItems(SUPER_COMBAT_POTS)) {
            if (config.debugMode()) {
                sendDebugMessage("We are missing super combat");
            }
            if (inDragons() || inLithkren()) {
                return client.getBoostedSkillLevel(Skill.STRENGTH) <= config.combatMin();
            } else {
                return true;
            }
        }
        if (!config.usePOHdigsite() && !InventoryUtil.hasAnyItems(DIGSITE_PENDANTS)) {
            if (inPOH() && !inDragons()) {
                return true;
            }
        }
        if (inDragons()) {
            return !InventoryUtil.hasAnyItems(PRAYER_POTS) && client.getBoostedSkillLevel(Skill.PRAYER) <= config.prayerMin();
        } else {
            if (!InventoryUtil.hasItem(ItemID.TELEPORT_TO_HOUSE)) {
                return true;
            }
            return !InventoryUtil.hasAnyItems(PRAYER_POTS);
        }
    }

    protected void interactWithBank() {
        Optional<NPC> banker = NPCs.search().withAction("Bank").nearestToPlayer();
        Optional<TileObject> bank = TileObjects.search().withAction("Bank").nearestToPlayer();
        if (banker.isPresent()) {
            NPCInteraction.interact(banker.get(), "Bank");
        } else if (bank.isPresent()) {
            TileObjectInteraction.interact(bank.get(), "Bank");
        } else {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "couldn't find bank or banker", null);
            resetPlugin();
        }
    }

    protected void depositItems() {
        if (config.debugMode()) {
            sendDebugMessage("We are depositing our items");
        }
        BankInteraction.depositInventory();
    }

    protected void withdrawItems() {
        if (config.debugMode()) {
            sendDebugMessage("We are withdrawing our items");
        }
        Optional<Widget> house = Bank.search().withId(ItemID.TELEPORT_TO_HOUSE).first();
        Optional<Widget> superCombat = Bank.search().withId(ItemID.SUPER_COMBAT_POTION4).first();
        Optional<Widget> divineSuperCombat = Bank.search().withId(ItemID.DIVINE_SUPER_COMBAT_POTION4).first();
        Optional<Widget> extended = Bank.search().withId(ItemID.EXTENDED_ANTIFIRE4).first();
        Optional<Widget> superExtended = Bank.search().withId(ItemID.EXTENDED_SUPER_ANTIFIRE4).first();
        Optional<Widget> prayerPot = Bank.search().withId(ItemID.PRAYER_POTION4).first();
        Optional<Widget> food = Bank.search().withId(config.foodID()).first();
        Optional<Widget> pendant = Bank.search().withId(ItemID.DIGSITE_PENDANT_5).first();

        if (house.isEmpty() || food.isEmpty() || prayerPot.isEmpty() || superCombat.isEmpty() && !config.supercombats() || divineSuperCombat.isEmpty() && config.supercombats() || extended.isEmpty() && !config.superantifire() || superExtended.isEmpty() && config.superantifire()) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Missing required items. Stopping.", null);
            resetPlugin();
            return;
        }

        if (!InventoryUtil.hasItem(ItemID.TELEPORT_TO_HOUSE)) {
            BankInteraction.withdrawX(house.get(), 10);
            return;
        }
        if (superCombat.isPresent() && !InventoryUtil.hasItem(ItemID.SUPER_COMBAT_POTION4) && !config.supercombats()) {
            BankInteraction.withdrawX(superCombat.get(), 1);
            return;
        }
        if (divineSuperCombat.isPresent() && !InventoryUtil.hasItem(ItemID.DIVINE_SUPER_COMBAT_POTION4) && config.supercombats()) {
            BankInteraction.withdrawX(divineSuperCombat.get(), 1);
            return;
        }
        if (extended.isPresent() && !InventoryUtil.hasItem(ItemID.EXTENDED_ANTIFIRE4) && !config.superantifire()) {
            BankInteraction.withdrawX(extended.get(), 1);
            return;
        }
        if (superExtended.isPresent() && !InventoryUtil.hasItem(ItemID.EXTENDED_SUPER_ANTIFIRE4) && config.superantifire()) {
            BankInteraction.withdrawX(superExtended.get(), 1);
            return;
        }
        if (InventoryUtil.getItemAmount(ItemID.PRAYER_POTION4) < config.praypotAmount()) {
            BankInteraction.withdrawX(prayerPot.get(), config.praypotAmount() - InventoryUtil.getItemAmount(ItemID.PRAYER_POTION4));
            return;
        }
        if (InventoryUtil.getItemAmount(config.foodID()) < config.foodAmount()) {
            BankInteraction.withdrawX(food.get(), config.foodAmount() - InventoryUtil.getItemAmount(config.foodID()));
            return;
        }
        if (pendant.isPresent() && !InventoryUtil.hasItem(ItemID.DIGSITE_PENDANT_5) && !config.usePOHdigsite()) {
            BankInteraction.withdrawX(pendant.get(), 1);
        }
    }

    private void teleportHome() {
        InventoryInteraction.useItem(ItemID.TELEPORT_TO_HOUSE, "Break");
    }

    private void teleportEdge() {
        Optional<TileObject> glory = ObjectUtil.getNearest(13523);
        glory.ifPresent(tileObject -> TileObjectInteraction.interact(tileObject, "Edgeville"));
    }

    private void teleportLith() {
        if (config.usePOHdigsite()) {
            Optional<TileObject> glory = ObjectUtil.getNearest(33418);
            glory.ifPresent(tileObject -> TileObjectInteraction.interact(tileObject, "Lithkren"));
        }
    }

    private void sendDebugMessage(String message) {
        String msg = new ChatMessageBuilder().append(Color.RED, message).build();
        messageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(msg)
                .build());
    }
}
