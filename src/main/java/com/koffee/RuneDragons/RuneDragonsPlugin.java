package com.koffee.RuneDragons;

import com.google.inject.Provides;
import com.koffee.EthanApiPlugin.Collections.Bank;
import com.koffee.EthanApiPlugin.Collections.ETileItem;
import com.koffee.EthanApiPlugin.Collections.NPCs;
import com.koffee.EthanApiPlugin.Collections.TileObjects;
import com.koffee.EthanApiPlugin.EthanApiPlugin;
import com.koffee.InteractionApi.BankInteraction;
import com.koffee.InteractionApi.InventoryInteraction;
import com.koffee.InteractionApi.NPCInteraction;
import com.koffee.InteractionApi.TileObjectInteraction;
import com.koffee.KoffeeUtils.BankUtil;
import com.koffee.KoffeeUtils.CalculationUtils;
import com.koffee.KoffeeUtils.InventoryUtil;
import com.koffee.KoffeeUtils.KoffeeUtilsPlugin;
import static com.koffee.KoffeeUtils.KoffeeUtilsPlugin.sleep;
import com.koffee.KoffeeUtils.NpcUtil;
import com.koffee.KoffeeUtils.ObjectUtil;
import com.koffee.KoffeeUtils.PrayerUtil;
import com.koffee.PacketUtils.PacketUtilsPlugin;
import com.koffee.PacketUtils.WidgetInfoExtended;
import com.koffee.Packets.MousePackets;
import com.koffee.Packets.MovementPackets;
import com.koffee.Packets.TileItemPackets;
import com.koffee.Packets.WidgetPackets;
import static com.koffee.RuneDragons.data.Constants.*;
import com.koffee.RuneDragons.data.State;
import com.koffee.RuneDragons.data.SubState;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.HitsplatID;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;


@PluginDependency(EthanApiPlugin.class)
@PluginDependency(PacketUtilsPlugin.class)
@PluginDependency(KoffeeUtilsPlugin.class)
@PluginDescriptor(name = "<html><font color=\"#d42020\">[Koffee]</font> Rune Dragons</html>",
	description = "AIO Rune dragons",
	tags = {"koffee", "ethan"})
@Slf4j
public class RuneDragonsPlugin extends Plugin
{


	// Variables
	// models
	public static Player player;
	public static NPC currentNPC;
	public static WorldPoint deathLocation;
	// lists
	public static List<Integer> inventorySetup = new ArrayList<>();
	public static List<ItemSpawned> itemsToLoot = new ArrayList<>();
	@Inject
	ItemManager itemManager;
	@Getter
	private int killCount;
	@Getter
	private int killsPerHour;
	@Getter
	private int totalLoot;
	@Getter
	private int lootPerHour;
	@Getter
	private SubState subState;
	@Getter
	private State state;
	// booleans
	@Getter
	private boolean started;
	private boolean deposited = false;
	// timers
	private Instant timer;
	private int timeout;
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
	private KoffeeUtilsPlugin koffeeUtils;

	@Override
	protected void startUp() throws Exception
	{
	}

	@Override
	protected void shutDown() throws Exception
	{
	}

	@Provides
	private RuneDragonsConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneDragonsConfig.class);
	}

	// Subscribes
	@Subscribe
	private void onGameTick(GameTick event)
	{
		if (!EthanApiPlugin.loggedIn() || !started)
		{
			// We do an early return if the user isn't logged in\
			return;
		}
		player = client.getLocalPlayer();
		if (player == null || !started)
		{
			return;
		}
		if (!client.isResized())
		{
			koffeeUtils.sendGameMessage("You must be set to resizable mode to use RuneDragons.");
			return;
		}
		if (client.getWidget(WidgetInfo.BANK_PIN_CONTAINER) != null)
		{
			log.info("Enter bank pin manually");
			return;
		}
		updateStats();
		state = getCurrentState();
		subState = getCurrentSubState();
		switch (state)
		{
			case TIMEOUT:
				timeout--;
				return;
			case LOGOUT:
				koffeeUtils.sendGameMessage("We are missing a teleport to house tab, stopping plugin.");
				resetPlugin();
				return;
			case ANIMATING:
				return;
		}
		switch (subState)
		{
			case ACTIVATE_PRAYER:
				togglePrayers(true);
				break;
			case DEACTIVATE_PRAYER:
				togglePrayers(false);
				break;
			case DRINK_POTIONS:
				drinkPotions();
				timeout = tickDelay();
				break;
			case EAT_FOOD:
				eatFood();
				break;
			case EQUP_GEAR:
				equipGear();
				break;
			case USE_SPECIAL:
				useSpec();
				break;
			case DRINK_POOL:
				interactWithPool();
				break;
			case TELE_EDGE:
				teleportEdge();
				break;
			case TELE_POH:
				teleportHome();
				break;
			case TELE_LITH:
				teleportLith();
				break;
			case ATTACK_DRAGON:
				attackDragon();
				break;
			case LOOT:
				lootItems();
				break;
			case MOVE_DOWNSTAIRS:
				walkDownstairs();
				break;
			case OPEN_DOOR:
				enterDownstairs();
				break;
			case WALK_DOOR:
				walkDoor();
				break;
			case ENTER_LAIR:
				enterLair();
				break;
			case FIND_BANK:
				interactWithBank();
				break;
			case WITHDRAW:
				withdrawItems();
				break;
			case DEPOSIT:
				depositItems();
				break;
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged e)
	{
		if (e.getGroup().equals("RuneDragons") && e.getKey().equals("startButton"))
		{
			toggle();
		}
	}

	@Subscribe
	private void onActorDeath(ActorDeath event)
	{
		if (!started)
		{
			return;
		}
		if (event.getActor() == currentNPC)
		{
			deathLocation = event.getActor().getWorldLocation();
			log.debug("Our npc died, updating deathLocation: {}", deathLocation.toString());
			currentNPC = null;
			killCount++;
		}
	}

	@Subscribe
	private void onItemSpawned(ItemSpawned event)
	{
		if (!started || !inDragons())
		{
			return;
		}
		if (lootableItem(event))
		{
			log.debug("Adding loot item: {}", client.getItemDefinition(event.getItem().getId()).getName());
			itemsToLoot.add(event);
		}
	}

	@Subscribe
	private void onItemDespawned(ItemDespawned itemDespawned)
	{
		if (!started || !inDragons())
		{
			return;
		}
		itemsToLoot.removeIf(itemSpawned -> itemSpawned.getItem().getId() == itemDespawned.getItem().getId());
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		itemsToLoot.clear();
		if (Objects.requireNonNull(gameStateChanged.getGameState()) == GameState.CONNECTION_LOST)
		{
			resetPlugin();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getHitsplat().getHitsplatType() == HitsplatID.HEAL && event.getActor() == currentNPC && config.useVengeance() && canVengeance())
		{
			MousePackets.queueClickPacket();
			WidgetPackets.queueWidgetAction(client.getWidget(WidgetInfoExtended.SPELL_VENGEANCE.getPackedId()), "Cast");
			timeout = 5;
		}
	}

	// Utils
	private State getCurrentState()
	{
		if (timeout > 0)
		{
			return State.TIMEOUT;
		}

		if (EthanApiPlugin.isMoving())
		{
			timeout = tickDelay();
			return State.ANIMATING;
		}

		if (shouldRestock() && InventoryUtil.getItemAmount(ItemID.TELEPORT_TO_HOUSE, true) <= 1 && !inDragons() && !inEdgeville() && !inPOH() && !inLithkren())
		{
			log.info("Teleport house tab " + InventoryUtil.getItemAmount(ItemID.TELEPORT_TO_HOUSE, true));
			return State.LOGOUT;
		}

		if (inDragons() && !shouldRestock())
		{
			if (!client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC) || !client.isPrayerActive(Prayer.PIETY))
			{
				return State.CONSUME;
			}
			if (client.getBoostedSkillLevel(Skill.HITPOINTS) <= CalculationUtils.getRandomIntBetweenRange(config.eatMin(), config.eatMax()))
			{
				return State.CONSUME;
			}
			if (client.getBoostedSkillLevel(Skill.PRAYER) <= CalculationUtils.getRandomIntBetweenRange(config.prayerMin(), config.prayerMax()))
			{
				return State.CONSUME;
			}
			if (client.getBoostedSkillLevel(Skill.STRENGTH) <= config.combatMin())
			{
				return State.CONSUME;
			}
			if (config.superantifire() && client.getVarbitValue(6101) == 0)
			{
				return State.CONSUME;
			}
			if (!config.superantifire() && client.getVarbitValue(3981) == 0)
			{
				return State.CONSUME;
			}
			return State.COMBAT;
		}
		if (!inLithkren() && (client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC) || client.isPrayerActive(Prayer.PIETY)))
		{
			return State.CONSUME;
		}

		if (inEdgeville())
		{
			if (shouldRestock())
			{
				return State.BANKING;
			}
			else
			{
				return State.TRAVEL;
			}
		}

		if (inPOH())
		{
			return State.TRAVEL;
		}

		if (inLithkren())
		{
			if (player.getWorldArea().intersectsWith(RUNE_DRAGONS_DOOR_ENTER))
			{
				if (!client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC) || !client.isPrayerActive(Prayer.PIETY))
				{
					return State.CONSUME;
				}
				if (client.getBoostedSkillLevel(Skill.PRAYER) <= config.prayerMin())
				{
					return State.CONSUME;
				}
				if (client.getBoostedSkillLevel(Skill.STRENGTH) <= config.combatMin())
				{
					return State.CONSUME;
				}
				if (config.superantifire() && client.getVarbitValue(6101) == 0)
				{
					return State.CONSUME;
				}
				if (!config.superantifire() && client.getVarbitValue(3981) == 0)
				{
					return State.CONSUME;
				}
			}
			return State.TRAVEL;
		}

		if (!inDragons() && !inEdgeville() && !inLithkren() && !inPOH())
		{
			return State.TRAVEL;
		}

		return State.ANIMATING;
	}

	private SubState getCurrentSubState()
	{
		if (state == State.CONSUME)
		{
			if (inLithkren())
			{
				if (!client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC) || !client.isPrayerActive(Prayer.PIETY))
				{
					return SubState.ACTIVATE_PRAYER;
				}
			}
			else
			{
				if (client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC) || client.isPrayerActive(Prayer.PIETY))
				{
					return SubState.DEACTIVATE_PRAYER;
				}
			}
			if (client.getBoostedSkillLevel(Skill.HITPOINTS) <= CalculationUtils.getRandomIntBetweenRange(config.eatMin(), config.eatMax()))
			{
				return SubState.EAT_FOOD;
			}
			if (client.getBoostedSkillLevel(Skill.PRAYER) <= CalculationUtils.getRandomIntBetweenRange(config.prayerMin(), config.prayerMax()))
			{
				return SubState.DRINK_POTIONS;
			}
			if (client.getBoostedSkillLevel(Skill.STRENGTH) <= config.combatMin())
			{
				return SubState.DRINK_POTIONS;
			}
			if (config.superantifire() && client.getVarbitValue(6101) == 0)
			{
				return SubState.DRINK_POTIONS;
			}
			if (!config.superantifire() && client.getVarbitValue(3981) == 0)
			{
				return SubState.DRINK_POTIONS;
			}
		}
		else if (state == State.COMBAT)
		{
			if (player.getInteracting() != null)
			{
				if (currentNPC != player.getInteracting())
				{
					currentNPC = (NPC) player.getInteracting();
					if (currentNPC != null)
					{
						return SubState.ATTACK_DRAGON;
					}
				}
			}
			if (currentNPC == null)
			{
				currentNPC = NpcUtil.findNearestAttackableNpc("Rune dragon").orElse(null);
				if (currentNPC != null)
				{
					return SubState.ATTACK_DRAGON;
				}
			}
			if (!itemsToLoot.isEmpty() && !InventoryUtil.isFull())
			{
				return SubState.LOOT;
			}
			Item mainWeapon = getWeapon(EquipmentInventorySlot.WEAPON.getSlotIdx());
			if (mainWeapon != null)
			{
				if (config.debugMode())
				{
					koffeeUtils.sendDebugMessage("Main weapon is not null!");
				}
				if (currentNPC == player.getInteracting() &&
					(client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) >= config.specTreshhold() * 10) &&
					(getNpcHealth(currentNPC, 330) >= config.specHp()) &&
					(mainWeapon.getId() == config.specId()) &&
					(InventoryUtil.emptySlots() >= 1))
				{
					return SubState.EQUP_GEAR;
				}
				if (client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) < config.specTreshhold() * 10 && mainWeapon.getId() == config.specId())
				{
					return SubState.EQUP_GEAR;
				}
				if (mainWeapon.getId() == config.specId() && client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) >= config.specTreshhold() * 10)
				{
					if (currentNPC == player.getInteracting() && getNpcHealth(currentNPC, 330) >= config.specHp() && client.getVarpValue(VarPlayer.SPECIAL_ATTACK_ENABLED) == 0)
					{
						return SubState.USE_SPECIAL;
					}
				}
			}
			else
			{
				if (config.debugMode())
				{
					koffeeUtils.sendDebugMessage("Main weapon is null");
				}
			}
		}
		else if (state == State.TRAVEL)
		{
			if (inDragons())
			{
				return SubState.TELE_POH;
			}
			if (!inPOH())
			{
				log.info("Not in POH");
				// Outside POH Handling
				if (inDragons() && (shouldRestock() || client.getBoostedSkillLevel(Skill.HITPOINTS) <= config.eatMin() && !InventoryUtil.hasItem(config.foodID())))
				{
					return SubState.TELE_POH;
				}
				if (inEdgeville() && !shouldRestock())
				{
					return SubState.TELE_POH;
				}
				if (!inEdgeville() && shouldRestock())
				{
					return SubState.TELE_POH;
				}
				if (!inEdgeville() && !inDragons() && !inLithkren())
				{
					return SubState.TELE_POH;
				}
			}
			if (inPOH())
			{
				log.info("In POH");
				// Inside POH handling
				if (shouldRestock())
				{
					return SubState.TELE_EDGE;
				}
				if (client.getBoostedSkillLevel(Skill.PRAYER) < client.getRealSkillLevel(Skill.PRAYER) && config.usePOHpool())
				{
					return SubState.DRINK_POOL;
				}
				if (client.getBoostedSkillLevel(Skill.HITPOINTS) < client.getRealSkillLevel(Skill.HITPOINTS) && config.usePOHpool())
				{
					return SubState.DRINK_POOL;
				}
				return SubState.TELE_LITH;
			}
			if (inLithkren())
			{
				log.info("Travel state in Lith");
				log.info(player.getWorldArea().getX() + "-" + player.getWorldArea().getY());
				// In Lith handling
				if (player.getWorldArea().intersectsWith(LITH_TELE))
				{
					return SubState.MOVE_DOWNSTAIRS;
				}
				else if (config.debugMode())
				{
					koffeeUtils.sendDebugMessage("We are not at the LITH tele");
				}
				if (player.getWorldArea().intersectsWith(LITH_TELE_DOWNSTAIRS))
				{
					return SubState.OPEN_DOOR;
				}
				else if (config.debugMode())
				{
					koffeeUtils.sendDebugMessage("We are not at the LITH downstairs");
				}
				if (player.getWorldArea().intersectsWith(RUNE_DRAGONS_DOOR_ENTER))
				{
					return SubState.ENTER_LAIR;
				}
				else if (config.debugMode())
				{
					koffeeUtils.sendDebugMessage("We are not at the dragon's lair");
				}
				if (player.getWorldArea().intersectsWith(RUNE_DRAGONS_DOOR))
				{
					return SubState.WALK_DOOR;
				}
				else if (config.debugMode())
				{
					koffeeUtils.sendDebugMessage("We are not at the LITH walk to door");
				}
			}
		}
		else if (state == State.BANKING)
		{
			if (!Bank.isOpen())
			{
				return SubState.FIND_BANK;
			}
			else
			{
				if (!deposited)
				{
					return SubState.DEPOSIT;
				}
				else
				{
					return SubState.WITHDRAW;
				}
			}
		}
		return SubState.IDLE;
	}

	public void toggle()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (config.startButton() && !started)
		{
			overlayManager.add(runeDragonsOverlay);
			initInventory();
			timer = Instant.now();
			if (!shouldRestock())
			{
				deposited = true;
			}
			started = true;
		}
		else if (!config.startButton() && started)
		{
			resetPlugin();
			started = false;
		}
	}

	public String getElapsedTime()
	{
		Duration duration = Duration.between(timer, Instant.now());
		long durationInMillis = duration.toMillis();
		long second = (durationInMillis / 1000) % 60;
		long minute = (durationInMillis / (1000 * 60)) % 60;
		long hour = (durationInMillis / (1000 * 60 * 60)) % 24;

		return String.format("%02d:%02d:%02d", hour, minute, second);
	}

	private void resetPlugin()
	{
		started = false;
		overlayManager.remove(runeDragonsOverlay);
		inventorySetup.clear();
		itemsToLoot.clear();
		timer = null;
		currentNPC = null;
		timeout = 0;
		deposited = false;
	}

	protected int tickDelay()
	{
		return (int) CalculationUtils.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
	}

	public void updateStats()
	{
		killsPerHour = (int) getPerHour(killCount);
		lootPerHour = (int) getPerHour(totalLoot);
	}

	public void updateLoot(int amount)
	{
		totalLoot += amount;
	}

	public long getPerHour(int quantity)
	{
		Duration timeSinceStart = Duration.between(timer, Instant.now());
		if (!timeSinceStart.isZero())
		{
			return (int) ((double) quantity * (double) Duration.ofHours(1).toMillis() / (double) timeSinceStart.toMillis());
		}
		return 0;
	}

	private boolean lootableItem(ItemSpawned event)
	{
		TileItem item = event.getItem();
		Tile tile = event.getTile();
		int haValue = client.getItemDefinition(item.getId()).getHaPrice();
		int itemPrice = itemManager.getItemPrice(item.getId()) * item.getQuantity();
		return (tile.getWorldLocation().equals(deathLocation) || tile.getWorldLocation().distanceTo(deathLocation) <= 2) && (itemPrice >= config.lootValue() || haValue >= config.lootValue());
	}

	public boolean canVengeance()
	{
		assert client.isClientThread();
		return client.getBoostedSkillLevel(Skill.MAGIC) >= 94 &&
			client.getVarbitValue(4070) == 2 &&
			client.getVarbitValue(Varbits.VENGEANCE_COOLDOWN) == 0 &&
			(InventoryUtil.runePouchQuanitity(ItemID.EARTH_RUNE) >= 10 &&
				InventoryUtil.runePouchQuanitity(ItemID.ASTRAL_RUNE) >= 4 &&
				InventoryUtil.runePouchQuanitity(ItemID.DEATH_RUNE) >= 2);
	}

	protected int getNpcHealth(NPC npc, Integer max)
	{
		if (npc == null || npc.getName() == null)
		{
			return -1;
		}
		int scale = npc.getHealthScale();
		int ratio = npc.getHealthRatio();
		if (ratio < 0 || scale <= 0 || max == null)
		{
			return -1;
		}
		return (int) ((float) (max * ratio / scale) + 0.5f);
	}

	protected Item getWeapon(int slot)
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);

		if (equipment == null)
		{
			return null;
		}

		return equipment.getItem(slot);
	}

	protected boolean inPOH()
	{
		boolean status = Arrays.stream(client.getMapRegions()).anyMatch(HOME_REGIONS::contains);
		if (config.debugMode())
		{
			koffeeUtils.sendDebugMessage("We are in POH - " + status);
		}
		return status;
	}

	protected boolean inDragons()
	{
		boolean status = client.getLocalPlayer().getWorldArea().intersectsWith(RUNE_DRAGONS);
		if (config.debugMode())
		{
			koffeeUtils.sendDebugMessage("We are in dragons - " + status);
		}
		return status;
	}

	protected boolean inLithkren()
	{
		boolean status = Arrays.stream(client.getMapRegions()).anyMatch(LITH_REGIONS::contains);
		if (config.debugMode())
		{
			koffeeUtils.sendDebugMessage("We are in lithkren - " + status);
		}
		return status;
	}

	protected boolean inEdgeville()
	{
		boolean status = client.getLocalPlayer().getWorldArea().intersectsWith(EDGEVILLE_TELE);
		if (config.debugMode())
		{
			koffeeUtils.sendDebugMessage("We are in edgevile - " + status);
		}
		return status;
	}

	protected boolean shouldRestock()
	{
		if (!InventoryUtil.hasItem(config.foodID()))
		{
			if (config.debugMode())
			{
				koffeeUtils.sendDebugMessage("We are missing food");
			}
			if (inDragons())
			{
				return client.getBoostedSkillLevel(Skill.HITPOINTS) <= config.eatMin();
			}
			else
			{
				return true;
			}
		}
		if (config.useSpec() && !InventoryUtil.hasItem(config.specId()))
		{
			if (config.debugMode())
			{
				koffeeUtils.sendDebugMessage("We are missing spec weapon");
			}
			return true;
		}
		if (config.useVengeance() && (!config.useDivinePouch() && !InventoryUtil.hasItem(ItemID.RUNE_POUCH) || (config.useDivinePouch() && !InventoryUtil.hasItem(ItemID.DIVINE_RUNE_POUCH))))
		{
			if (config.debugMode())
			{
				koffeeUtils.sendDebugMessage("We are missing rune pouch");
			}
			return true;
		}
		if (config.superantifire() && !InventoryUtil.hasAnyItems(SUPER_EXTENDED_ANTIFIRE_POTS))
		{
			if (config.debugMode())
			{
				koffeeUtils.sendDebugMessage("We are missing super extended antifire");
			}
			if (inDragons() || inLithkren())
			{
				return client.getVarbitValue(Varbits.SUPER_ANTIFIRE) == 0;
			}
			else
			{
				return true;
			}
		}
		if (!config.superantifire() && !InventoryUtil.hasAnyItems(EXTENDED_ANTIFIRE_POTS))
		{
			if (config.debugMode())
			{
				koffeeUtils.sendDebugMessage("We are missing extended antifire");
			}
			if (inDragons() || inLithkren())
			{
				return client.getVarbitValue(3981) == 0;
			}
			else
			{
				return true;
			}
		}
		if (config.supercombats() && !InventoryUtil.hasAnyItems(DIVINE_SUPER_COMBAT_POTS))
		{
			if (config.debugMode())
			{
				koffeeUtils.sendDebugMessage("We are missing divine super combat");
			}
			if (inDragons() || inLithkren())
			{
				return client.getBoostedSkillLevel(Skill.STRENGTH) <= config.combatMin();
			}
			else
			{
				return true;
			}
		}
		if (!config.supercombats() && !InventoryUtil.hasAnyItems(SUPER_COMBAT_POTS))
		{
			if (config.debugMode())
			{
				koffeeUtils.sendDebugMessage("We are missing super combat");
			}
			if (inDragons() || inLithkren())
			{
				return client.getBoostedSkillLevel(Skill.STRENGTH) <= config.combatMin();
			}
			else
			{
				return true;
			}
		}
		if (inDragons())
		{
			return !InventoryUtil.hasAnyItems(PRAYER_POTS) && client.getBoostedSkillLevel(Skill.PRAYER) <= config.prayerMin();
		}
		else
		{
			if (!InventoryUtil.hasItem(ItemID.TELEPORT_TO_HOUSE))
			{
				return true;
			}
			return !InventoryUtil.hasAnyItems(PRAYER_POTS);
		}
	}

	private void initInventory()
	{
		inventorySetup.clear();
		if (config.useVengeance())
		{
			if (config.useDivinePouch())
			{
				inventorySetup.add(ItemID.DIVINE_RUNE_POUCH);
			}
			else
			{
				inventorySetup.add(ItemID.RUNE_POUCH);
			}
		}
		if (config.superantifire())
		{
			inventorySetup.add(ItemID.EXTENDED_SUPER_ANTIFIRE4);
		}
		if (!config.superantifire())
		{
			inventorySetup.add(ItemID.EXTENDED_ANTIFIRE4);
		}
		if (config.supercombats())
		{
			inventorySetup.add(ItemID.DIVINE_SUPER_COMBAT_POTION4);
		}
		if (!config.supercombats())
		{
			inventorySetup.add(ItemID.SUPER_COMBAT_POTION4);
		}
		if (config.useSpec())
		{
			inventorySetup.add(config.specId());
		}
		inventorySetup.add(ItemID.PRAYER_POTION4);
		inventorySetup.add(ItemID.TELEPORT_TO_HOUSE);
		inventorySetup.add(config.foodID());
		log.info("required inventory items: {}", inventorySetup.toString());
	}

	// Banking
	protected void interactWithBank()
	{
		Optional<NPC> banker = NPCs.search().withAction("Bank").nearestToPlayer();
		Optional<TileObject> bank = TileObjects.search().withAction("Bank").nearestToPlayer();
		if (banker.isPresent())
		{
			NPCInteraction.interact(banker.get(), "Bank");
		}
		else if (bank.isPresent())
		{
			TileObjectInteraction.interact(bank.get(), "Bank");
		}
		else
		{
			koffeeUtils.sendGameMessage("Couldn't find bank or banker. Stopping.");
			resetPlugin();
		}
	}

	protected void depositItems()
	{
		if (config.debugMode())
		{
			koffeeUtils.sendDebugMessage("We are depositing our items");
		}
		BankUtil.depositAllExcept(inventorySetup);
		if (!BankUtil.containsExcept(inventorySetup) || InventoryUtil.isEmpty())
		{
			deposited = true;
		}
		timeout = tickDelay();
	}

	protected void withdrawItems()
	{
		if (config.debugMode())
		{
			koffeeUtils.sendDebugMessage("We are withdrawing our items");
		}
		Optional<Widget> house = Bank.search().withId(ItemID.TELEPORT_TO_HOUSE).first();
		Optional<Widget> superCombat = Bank.search().withId(ItemID.SUPER_COMBAT_POTION4).first();
		Optional<Widget> divineSuperCombat = Bank.search().withId(ItemID.DIVINE_SUPER_COMBAT_POTION4).first();
		Optional<Widget> extended = Bank.search().withId(ItemID.EXTENDED_ANTIFIRE4).first();
		Optional<Widget> superExtended = Bank.search().withId(ItemID.EXTENDED_SUPER_ANTIFIRE4).first();
		Optional<Widget> prayerPot = Bank.search().withId(ItemID.PRAYER_POTION4).first();
		Optional<Widget> food = Bank.search().withId(config.foodID()).first();
		Optional<Widget> pouch = Bank.search().withId(ItemID.RUNE_POUCH).first();
		Optional<Widget> divinePouch = Bank.search().withId(ItemID.DIVINE_RUNE_POUCH).first();
		Optional<Widget> specWeapon = Bank.search().withId(config.specId()).first();

		if (house.isEmpty() || food.isEmpty() || prayerPot.isEmpty() || superCombat.isEmpty() && !config.supercombats() || divineSuperCombat.isEmpty() && config.supercombats() || extended.isEmpty() && !config.superantifire() || superExtended.isEmpty() && config.superantifire())
		{
			if (config.debugMode())
			{
				if (house.isEmpty())
				{
					koffeeUtils.sendDebugMessage("Missing house tab");
				}
				if (food.isEmpty())
				{
					koffeeUtils.sendDebugMessage("Missing food");
				}
				if (prayerPot.isEmpty())
				{
					koffeeUtils.sendDebugMessage("Missing prayer pot");
				}
				if (superCombat.isEmpty() && !config.supercombats())
				{
					koffeeUtils.sendDebugMessage("Missing super combat");
				}
				if (divineSuperCombat.isEmpty() && config.supercombats())
				{
					koffeeUtils.sendDebugMessage("Missing divine super combat");
				}
				if (extended.isEmpty() && !config.superantifire())
				{
					koffeeUtils.sendDebugMessage("Missing extended antifire");
				}
				if (superExtended.isEmpty() && config.superantifire())
				{
					koffeeUtils.sendDebugMessage("Missing super extended antifire");
				}
			}
			koffeeUtils.sendGameMessage("Missing required items. Stopping.");
			resetPlugin();
			return;
		}

		if (!InventoryUtil.hasItem(ItemID.TELEPORT_TO_HOUSE))
		{
			BankInteraction.withdrawX(house.get(), 10);
			return;
		}
		if (superCombat.isPresent() && !InventoryUtil.hasItem(ItemID.SUPER_COMBAT_POTION4) && !config.supercombats())
		{
			BankInteraction.withdraw1(superCombat.get());
			return;
		}
		if (divineSuperCombat.isPresent() && !InventoryUtil.hasItem(ItemID.DIVINE_SUPER_COMBAT_POTION4) && config.supercombats())
		{
			BankInteraction.withdraw1(divineSuperCombat.get());
			return;
		}
		if (pouch.isPresent() && !InventoryUtil.hasItem(ItemID.RUNE_POUCH) && !config.useDivinePouch())
		{
			BankInteraction.withdraw1(pouch.get());
			return;
		}
		if (divinePouch.isPresent() && !InventoryUtil.hasItem(ItemID.DIVINE_RUNE_POUCH) && config.useDivinePouch())
		{
			BankInteraction.withdraw1(divinePouch.get());
			return;
		}
		if (divineSuperCombat.isPresent() && !InventoryUtil.hasItem(ItemID.DIVINE_SUPER_COMBAT_POTION4) && config.supercombats())
		{
			BankInteraction.withdraw1(divineSuperCombat.get());
			return;
		}
		if (extended.isPresent() && !InventoryUtil.hasItem(ItemID.EXTENDED_ANTIFIRE4) && !config.superantifire())
		{
			BankInteraction.withdraw1(extended.get());
			return;
		}
		if (superExtended.isPresent() && !InventoryUtil.hasItem(ItemID.EXTENDED_SUPER_ANTIFIRE4) && config.superantifire())
		{
			BankInteraction.withdraw1(superExtended.get());
			return;
		}
		if (InventoryUtil.getItemAmount(ItemID.PRAYER_POTION4) < config.praypotAmount())
		{
			BankInteraction.withdrawX(prayerPot.get(), config.praypotAmount() - InventoryUtil.getItemAmount(ItemID.PRAYER_POTION4));
			return;
		}
		if (config.useSpec() && specWeapon.isPresent() && !InventoryUtil.hasItem(config.specId()))
		{
			BankInteraction.withdraw1(specWeapon.get());
			return;
		}
		else if (config.useSpec() && specWeapon.isEmpty() && !InventoryUtil.hasItem(config.specId()))
		{
			koffeeUtils.sendGameMessage("Missing spec weapon. Stopping.");
			resetPlugin();

			return;
		}
		if (InventoryUtil.getItemAmount(config.foodID()) < config.foodAmount())
		{
			BankInteraction.withdrawX(food.get(), config.foodAmount() - InventoryUtil.getItemAmount(config.foodID()));
		}
	}

	// Travel
	private void teleportHome()
	{
		client.runScript(138);
		InventoryInteraction.useItem(ItemID.TELEPORT_TO_HOUSE, "Break");
		timeout = 2 + tickDelay();
	}

	private void teleportEdge()
	{
		Optional<TileObject> glory = ObjectUtil.getNearest(13523);
		glory.ifPresent(tileObject -> {
			TileObjectInteraction.interact(tileObject, "Edgeville");
		});
		timeout = 2 + tickDelay();
	}

	private void teleportLith()
	{
		Optional<TileObject> digsite = ObjectUtil.getNearest(33418);
		digsite.ifPresent(tileObject -> {
			TileObjectInteraction.interact(tileObject, "Lithkren");
		});
		timeout = 2 + tickDelay();
	}

	private void walkDownstairs()
	{
		Optional<TileObject> stairs = ObjectUtil.getNearest(32113);
		stairs.ifPresent(tileObject -> {
			TileObjectInteraction.interact(tileObject, "Climb");
		});
		timeout = tickDelay();
	}

	private void enterDownstairs()
	{
		Optional<TileObject> stairs = ObjectUtil.getNearest(32117);
		stairs.ifPresent(tileObject -> {
			TileObjectInteraction.interact(tileObject, "Enter");
		});
		timeout = tickDelay();
	}

	private void walkDoor()
	{
		MousePackets.queueClickPacket();
		MovementPackets.queueMovement(RUNE_DRAGONS_DOOR_TILE);
		timeout = tickDelay();
	}

	private void enterLair()
	{
		Optional<TileObject> stairs = ObjectUtil.getNearest(32153);
		stairs.ifPresent(tileObject -> {
			TileObjectInteraction.interact(tileObject, "Pass");
		});
		timeout = tickDelay();
	}

	private void togglePrayers(boolean active)
	{
		if (client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC) != active)
		{
			PrayerUtil.togglePrayer(Prayer.PROTECT_FROM_MAGIC);
		}
		if (client.isPrayerActive(Prayer.PIETY) != active)
		{
			PrayerUtil.togglePrayer(Prayer.PIETY);
		}
		timeout = tickDelay();
	}

	private void drinkPotions()
	{
		if (client.getBoostedSkillLevel(Skill.PRAYER) <= config.prayerMax())
		{
			InventoryInteraction.useItem(PRAYER_POTS, "Drink");
			return;
		}
		if (client.getBoostedSkillLevel(Skill.STRENGTH) <= config.combatMin())
		{
			if (config.supercombats())
			{
				InventoryInteraction.useItem(DIVINE_SUPER_COMBAT_POTS, "Drink");
			}
			else
			{
				InventoryInteraction.useItem(SUPER_COMBAT_POTS, "Drink");
			}
			return;
		}
		if (config.superantifire() && client.getVarbitValue(6101) == 0)
		{
			InventoryInteraction.useItem(SUPER_EXTENDED_ANTIFIRE_POTS, "Drink");
			return;
		}
		if (!config.superantifire() && client.getVarbitValue(3981) == 0)
		{
			InventoryInteraction.useItem(EXTENDED_ANTIFIRE_POTS, "Drink");
		}
	}

	private void eatFood()
	{
		if (client.getBoostedSkillLevel(Skill.HITPOINTS) <= config.eatMax())
		{
			InventoryInteraction.useItem(config.foodID(), "Eat");
			return;
		}
		timeout = tickDelay();
	}

	private void interactWithPool()
	{
		Optional<TileObject> pool = TileObjects.search().withAction("Drink").nearestToPlayer();
		pool.ifPresent(tileObject -> TileObjectInteraction.interact(tileObject, "Drink"));
	}

	private void attackDragon()
	{
		NPCInteraction.interact(currentNPC, "Attack");
		timeout = tickDelay();
	}

	private void lootItems()
	{
		ItemSpawned item = itemsToLoot.get(0);
		if (item != null)
		{
			if (config.debugMode())
			{
				koffeeUtils.sendDebugMessage("Looting item: " + client.getItemDefinition(item.getItem().getId()).getName());
			}
			Tile lootTile = item.getTile();
			TileItem lootItem = item.getItem();
			MousePackets.queueClickPacket();
			TileItemPackets.queueTileItemAction(new ETileItem(lootTile.getWorldLocation(), lootItem), false);
			int itemPrice = itemManager.getItemPrice(lootItem.getId()) * lootItem.getQuantity();
			if (itemPrice > 0)
			{
				updateLoot(itemPrice);
			}
		}
		timeout = tickDelay();
	}

	private void equipGear()
	{
		Item mainWeapon = getWeapon(EquipmentInventorySlot.WEAPON.getSlotIdx());
		if (mainWeapon != null)
		{
			if (currentNPC == player.getInteracting())
			{
				if (client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) >= config.specTreshhold() * 10)
				{
					if (getNpcHealth(currentNPC, 330) >= config.specHp())
					{
						if (mainWeapon.getId() != config.specId())
						{
							InventoryInteraction.useItem(config.specId(), "Wield");
						}
					}
				}
			}
			if (client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) < config.specTreshhold() * 10)
			{
				if (mainWeapon.getId() == config.specId())
				{
					InventoryInteraction.useItem(config.mainId(), "Wield");
					if (InventoryUtil.hasItem(config.shieldId()))
					{
						sleep(100, 400);
						InventoryInteraction.useItem(config.shieldId(), "Wield");
					}
				}
			}
		}
		timeout = tickDelay();
	}

	private void useSpec()
	{
		Item mainWeapon = getWeapon(EquipmentInventorySlot.WEAPON.getSlotIdx());
		if (mainWeapon != null)
		{
			if (currentNPC == player.getInteracting())
			{
				if (client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) >= config.specTreshhold() * 10)
				{
					if (getNpcHealth(currentNPC, 330) >= config.specHp())
					{
						if (mainWeapon.getId() != config.specId())
						{
							InventoryInteraction.useItem(config.specId(), "Wield");
						}
					}
				}
			}
			if (client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) < config.specTreshhold() * 10)
			{
				if (mainWeapon.getId() == config.specId())
				{
					InventoryInteraction.useItem(config.mainId(), "Wield");
				}
				if (InventoryUtil.hasItem(config.shieldId()))
				{
					sleep(100, 400);
					InventoryInteraction.useItem(config.shieldId(), "Wield");
				}
			}
		}
		timeout = tickDelay();
	}
}
