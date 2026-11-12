package com.recolor;

import javax.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Deque;
import java.util.ArrayDeque;
import java.awt.Color;

@Slf4j
@PluginDescriptor(
		name = "CG recolor",
		description = "Escape the red prison today!"
)
public class RecolorCG extends Plugin
{

	// Declarations
	private static final List<Integer> OBJECT_IDS = Arrays.asList(35965, 35966, 35967, 35968, 35968, 35969, 35970, 35971, 35972, 35972, 35973, 35974, 35975, 35976, 35977, 35978, 35979, 35980, 35992, 35994, 35994, 35995, 35996, 35997, 35998, 35999, 36000, 36001, 36002, 36003, 36004, 36005, 36006, 36007, 36008, 37337);
	private static final List<Integer> GROUND_IDS = Arrays.asList(36046, 36047, 36048, 36052, 36053, 36054, 36055, 36056, 36057, 36058, 36059);
	private static final List<Integer> NPC_IDS = Arrays.asList(9035, 9036, 9037, 9038, 9039, 9040, 9041, 9042, 9043, 9044, 9045, 9046, 9047, 9048);
	private static final List<Integer> PROJECTILE_IDS = Arrays.asList(1702, 1708, 1712, 1714, 1723);
	private static final int REGION_ID_GAUNTLET_LOBBY = 12127;
	private static final int REGION_ID_GAUNTLET_CORRUPTED = 7768;
	//private static final int REGION_ID_GAUNTLET_NORMAL = 7512;

	// Will likely use the ITEM_IDS in a future update of this plugin
	//private static final List<Integer> ITEM_IDS = Arrays.asList(23824, 23834, 23821, 23822, 23823, 23820, 23835, 23837, 23838, 23585, 23582, 23583, 23584, 23849, 23850, 23851, 23855, 23856, 23857);


	ModelDataProcessor dataProcessor;
	Random rand;
	int regionId;
	Color randomColor1;
	Color randomColor2;


	@Inject
	private ClientThread clientThread;

	@Inject
	private Client client;

	@Inject
	private RecolorCGConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private PluginManager pluginManager;

	// 117 HD config keys (group "hd" in HdPluginConfig)
	private static final String HD_PLUGIN_NAME = "117 HD";
	private static final String HD_GROUP = "hd";
	private static final String[] HD_LIGHT_KEYS = {"npcLights", "objectLights"};

	// Our own record of what we changed, kept in a separate group so writing to it
	// never triggers this plugin's own config handler (which reloads the scene).
	// Persisted, so settings still get restored if the client closed while inside CG.
	private static final String STATE_GROUP = "recolorCGHDstate";
	private static final String STATE_ACTIVE = "active";
	private static final String UNSET = "__unset__";

	@Provides
	RecolorCGConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RecolorCGConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		log.debug("Recolor started!");
		rand = new Random();
		rollRandomColors();

		// Vanilla model facecolors are stored in a .txt -> the new model colors can be calculated before the models even appear making the spawnEvents less expensive
		this.dataProcessor = new ModelDataProcessor("/model_facecolors.txt", config.color(), config.color(), config.harmonize());
		recolorFromConfig();

		// If the user is already logged in AND inside the gauntlet, Hun still needs to be recolored
		// Hun gets recolored on GamestateChanges
		if(client.getGameState() == GameState.LOGGED_IN)
		{
			regionId = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
			updateHdLights(regionId == REGION_ID_GAUNTLET_CORRUPTED);
			if(regionId == REGION_ID_GAUNTLET_CORRUPTED)
			{
				clientThread.invoke(()->
				{
					client.setGameState(GameState.LOADING);
				});
			}
		}
	}

	@Override
	protected void shutDown()
	{
		restoreHdLights();

		clientThread.invoke(() ->
		{
			rand = null;

			if(client.getGameState() == GameState.LOGGED_IN)
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Some game models may still be affected by the plugin. Please re-log to ensure that everything is properly reset.", null);
				clearAll();
				client.setGameState(GameState.LOADING);
			}

			synchronized (dataProcessor)
			{
				dataProcessor.cleanUp();
				dataProcessor = null;
			}
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if(event.getGroup().equals(RecolorCGConfig.GROUP))
		{
			String key = event.getKey();
			if (key.equals("random") || key.equals("expBetterRandom") || key.equals("expHueGap"))
			{
				rollRandomColors();
			}
			recolorFromConfig();
			clientThread.invoke(() ->
			{
				recolorNPCs(true);
				client.setGameState(GameState.LOADING);
			});
		}

	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (OBJECT_IDS.contains(event.getGameObject().getId()))
		{
			recolorGameObject(event.getGameObject(), false, true);
		}
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		if (GROUND_IDS.contains(event.getGroundObject().getId()))
		{
			recolorGroundObject(event.getGroundObject(), false);
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (NPC_IDS.contains(event.getNpc().getId()))
		{
			recolorNPC(event.getNpc(), true);
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		if (NPC_IDS.contains(event.getNpc().getId()))
		{
			recolorNPC(event.getNpc(), true);
		}
	}


	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		if(PROJECTILE_IDS.contains(event.getProjectile().getId()))
		{
			recolorProjectile(event.getProjectile());
		}
	}


	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if(event.getGameState() == GameState.LOGGED_IN)
		{
			regionId = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
			updateHdLights(regionId == REGION_ID_GAUNTLET_CORRUPTED);
			if(regionId == REGION_ID_GAUNTLET_LOBBY)
			{
				if (config.random())
				{
					rollRandomColors();
					recolorFromConfig();
				}
			}
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if(event.getVarbitId() == 9177 && event.getValue() == 1)	// varbit 9177 is 1 if player is in the boss room
		{
			clientThread.invoke(() ->
			{
				client.setGameState(GameState.LOADING);
			});
		}
	}


	@Subscribe
	public void onPreMapLoad(PreMapLoad event)
	{
		regionId = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
		if(regionId == REGION_ID_GAUNTLET_CORRUPTED || regionId == REGION_ID_GAUNTLET_LOBBY) {
			Scene scene = event.getScene();
			Tile[][][] sceneTiles = scene.getTiles();
			for (Tile[][] tiles : sceneTiles) {
				for (Tile[] tiles1 : tiles) {
					for (Tile tile : tiles1) {
						if (tile == null)
							continue;
						GameObject[] gameObjects = tile.getGameObjects();
						GroundObject groundObject = tile.getGroundObject();
						for (GameObject object : gameObjects) {
							if (object == null) {
								continue;
							}
							int ID = object.getId();
							if (OBJECT_IDS.contains(ID)) {
								recolorGameObject(object, true, true);
							}
						}
						if (groundObject != null) {
							int ID = groundObject.getId();
							if (GROUND_IDS.contains(ID)) {
								recolorGroundObject(groundObject, true);
							}
						}
						if (config.expFloorPattern() && regionId == REGION_ID_GAUNTLET_CORRUPTED) {
							recolorTile(tile);
						}
					}
				}
			}
		}
	}

	public void recolorGameObject(GameObject gameObject, Boolean preMapLoad, Boolean useRecolored)
	{

		Renderable renderable = gameObject.getRenderable();
		Model model;

		if(preMapLoad)
		{
			model = verifyPreMapLoadModel(renderable);
		}
		else
		{
			model = verifyModel(renderable);
		}

		if (model == null)
		{
			if(!preMapLoad)
			{
				log.debug("recolorGameObject returned null!" + " , PreMapLoad: " + preMapLoad);
			}
			return;
		}

		synchronized (dataProcessor)
		{
			dataProcessor.applyColors(gameObject.getId(), "GameObject", model, useRecolored);
		}
	}

	public void recolorGroundObject(GroundObject groundObject, Boolean preMapLoad)
	{

		Renderable renderable = groundObject.getRenderable();
		Model model;

		if(preMapLoad)
		{
			model = verifyPreMapLoadModel(renderable);
		}
		else
		{
			model = verifyModel(renderable);
		}

		if (model == null)
		{
			log.debug("recolorGroundObject returned null!");
			return;
		}
		if(groundObject.getId() == 36047 || groundObject.getId() == 36048)
		{	//Damaging ground recolor depends on config
			if(!config.groundRecolor())
			{
				dataProcessor.applyColors(groundObject.getId(), "GroundObject", model, false);
				return;
			}
			else
			{
				dataProcessor.applyColors(groundObject.getId(), "GroundObject", model, true);
				return;
			}
		}

		synchronized (dataProcessor)
		{
			dataProcessor.applyColors(groundObject.getId(), "GroundObject", model, true);
		}

	}

	public void recolorNPC(NPC npc, Boolean useRecolored)
	{
		if(!useRecolored)
		{
			if (npc.getModel() == null)
			{
				log.debug("recolorAll returned null! - NPC");
				return;
			}
			synchronized (dataProcessor)
			{
				dataProcessor.applyColors(npc.getId(), "NPC", npc.getModel(), false);
			}
			return;
		}
		// Spotanim needs to be set if we mage the npc that has to be recolored
		if (client.getLocalPlayer().getInteracting() != null)
		{
			if(client.getLocalPlayer().getInteracting().hasSpotAnim(1724) || client.getLocalPlayer().getInteracting().hasSpotAnim(85))
			{
				client.getLocalPlayer().getInteracting().createSpotAnim(0,-1,0,0);
			}
		}

		if (config.tornado())
		{
			if (config.npcRecolor())    // tornados AND npcs are to be recolored - we can use the same call for both
			{
				if (npc.getModel() == null)
				{
					log.debug("recolorAll returned null! - NPC");
					return;
				}
				synchronized (dataProcessor)
				{
					dataProcessor.applyColors(npc.getId(), "NPC", npc.getModel(), true);
				}
			}
			else	// tornados are to be recolored, npcs not - we need to differentiate
			{
				if(npc.getModel() == null)
				{
					log.debug("recolorAll returned null! - NPC");
					return;
				}
				synchronized (dataProcessor)
				{
					if(npc.getId() == 9039)
					{
						dataProcessor.applyColors(npc.getId(), "NPC", npc.getModel(), true);
					}
					else
					{
						dataProcessor.applyColors(npc.getId(), "NPC", npc.getModel(), false);
					}
				}
			}
		}
		else	// if-case inverted
		{
			if(config.npcRecolor())	// tornados are NOT to be recolored, npcs are - we need to differentiate
			{
				if(npc.getModel() == null)
				{
					log.debug("recolorAll returned null! - NPC");
					return;
				}
				synchronized (dataProcessor)
				{
					if(npc.getId() == 9039)
					{
						dataProcessor.applyColors(npc.getId(), "NPC", npc.getModel(), false);
					}
					else
					{
						dataProcessor.applyColors(npc.getId(), "NPC", npc.getModel(), true);
					}
				}
			}
			else	//tornados AND npcs are NOT to be recolored - we can use the same call for both
			{
				if(npc.getModel() == null)
				{
					log.debug("recolorAll returned null! - NPC");
					return;
				}
				synchronized (dataProcessor)
				{
					dataProcessor.applyColors(npc.getId(), "NPC", npc.getModel(), false);
				}
			}
		}
	}

	public void recolorProjectile(Projectile projectile)
	{
		Model model = projectile.getModel();
		if (model == null)
		{
			log.debug("recolorProjectile returned null!");
			return;
		}

		if(config.projectileRecolor())
		{
			synchronized (dataProcessor)
			{
				dataProcessor.applyColors(projectile.getId(), "Projectile", model, true);
			}
		}
		else
		{
			synchronized (dataProcessor)
			{
				dataProcessor.applyColors(projectile.getId(), "Projectile", model, false);
			}
		}
	}

	public void recolorNPCs(Boolean useRecolor)
	{
		IndexedObjectSet<? extends NPC> npcs = client.getWorldView(-1).npcs();
		for (NPC npc:npcs) {
			recolorNPC(npc, useRecolor);
		}
	}

	// Model.getModel() returns null, therefore we need to do an instanceof check
	private Model verifyModel(Renderable renderable)
	{
		if (renderable instanceof Model)
		{
			return (Model) renderable;
		}
		else
		{
			try
			{
				Model model = renderable.getModel();
				if (model == null)
				{
					log.debug("verifyModel returned null!");
					return null;
				}
				return model;
			}
			catch (NullPointerException e)
			{
				return null;
			}
		}
	}

	private Model verifyPreMapLoadModel(Renderable renderable)
	{
		if (renderable instanceof Model)
		{
			return (Model) renderable;
		}
		return null;
	}


	public void clearAll()
	{
		Scene scene = client.getTopLevelWorldView().getScene();
		Tile[][][] sceneTiles = scene.getTiles();
		recolorNPCs(false);
		for (Tile[][] tiles : sceneTiles)
		{
			for (Tile[] tiles1 : tiles)
			{
				for (Tile tile : tiles1)
				{
					if (tile == null)
						continue;
					GameObject[] gameObjects = tile.getGameObjects();
					for (GameObject object : gameObjects) {
						if(object == null)
						{
							continue;
						}
						int ID = object.getId();
						if (OBJECT_IDS.contains(ID)) {
							recolorGameObject(object, false, false);
						}
					}
				}
			}
		}
	}

	// NOTE: 117 HD Support and experimental features are the work of Marts.
	// Link to Marts GitHub: https://github.com/Martsssssss
	//
	// 117 HD support
	//
	// 117 HD gives every corrupted NPC and many corrupted objects a hardcoded red light.
	// There is no API to recolor those, so while inside CG we switch those two 117 HD
	// light toggles off, remembering the user's own values, and put them back on leaving.

	private boolean is117HdEnabled()
	{
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (HD_PLUGIN_NAME.equals(plugin.getName()))
			{
				return pluginManager.isPluginEnabled(plugin);
			}
		}
		return false;
	}

	private boolean hdLightsPaused()
	{
		return "true".equals(configManager.getConfiguration(STATE_GROUP, STATE_ACTIVE));
	}

	private void updateHdLights(boolean inCorruptedGauntlet)
	{
		boolean shouldPause = inCorruptedGauntlet && config.hdPauseLights() && is117HdEnabled();
		if (shouldPause && !hdLightsPaused())
		{
			pauseHdLights();
		}
		else if (!shouldPause && hdLightsPaused())
		{
			restoreHdLights();
		}
	}

	private void pauseHdLights()
	{
		for (String key : HD_LIGHT_KEYS)
		{
			String current = configManager.getConfiguration(HD_GROUP, key);
			configManager.setConfiguration(STATE_GROUP, key, current == null ? UNSET : current);
			configManager.setConfiguration(HD_GROUP, key, false);
		}
		configManager.setConfiguration(STATE_GROUP, STATE_ACTIVE, true);
		log.debug("Paused 117 HD NPC/object lights for the Corrupted Gauntlet");
	}

	private void restoreHdLights()
	{
		if (!hdLightsPaused())
		{
			return;
		}
		for (String key : HD_LIGHT_KEYS)
		{
			String saved = configManager.getConfiguration(STATE_GROUP, key);
			if (saved == null || UNSET.equals(saved))
			{
				// The user never changed it, so go back to 117 HD's default
				configManager.unsetConfiguration(HD_GROUP, key);
			}
			else
			{
				configManager.setConfiguration(HD_GROUP, key, saved);
			}
			configManager.unsetConfiguration(STATE_GROUP, key);
		}
		configManager.unsetConfiguration(STATE_GROUP, STATE_ACTIVE);
		log.debug("Restored 117 HD NPC/object lights");
	}

	//
	// Colour selection
	//
	private static final int HUES = 63;             // hue 63 wraps round to the same red as 0, so 0-62 are the distinct ones
	private static final int REPEAT_DISTANCE = 4;   // hues closer than this to a recent run count as a repeat
	private final Deque<Integer> recentHues = new ArrayDeque<>();

	private void rollRandomColors()
	{
		if (!config.expBetterRandom())
		{
			// Original behaviour: any RGB value
			randomColor1 = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
			randomColor2 = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
			return;
		}

		int hue1 = pickFreshHue();
		int gap = Math.min(config.expHueGap(), HUES / 2);
		int span = HUES - 2 * gap + 1;  // offsets that keep at least 'gap' hues away in both directions
		int hue2 = (hue1 + gap + rand.nextInt(Math.max(1, span))) % HUES;

		randomColor1 = hueToColor(hue1);
		randomColor2 = hueToColor(hue2);

		recentHues.addFirst(hue1);
		trimHistory();
		log.debug("Random hues: primary {}, secondary {}", hue1, hue2);
	}

	private int pickFreshHue()
	{
		trimHistory();
		for (int tries = 0; tries < 50; tries++)
		{
			int hue = rand.nextInt(HUES);
			boolean repeat = false;
			for (int recent : recentHues)
			{
				int d = Math.abs(hue - recent);
				if (Math.min(d, HUES - d) < REPEAT_DISTANCE)
				{
					repeat = true;
					break;
				}
			}
			if (!repeat)
			{
				return hue;
			}
		}
		return rand.nextInt(HUES);
	}

	private void trimHistory()
	{
		while (recentHues.size() > config.expNoRepeat())
		{
			recentHues.removeLast();
		}
	}

	private Color hueToColor(int hue)
	{
		float saturation = 0.65f + rand.nextFloat() * 0.35f;
		float brightness = 0.7f + rand.nextFloat() * 0.3f;
		return Color.getHSBColor(hue / (float) HUES, saturation, brightness);
	}

	// Works out the colours to use from the config and rebuilds the recolor data
	private void recolorFromConfig()
	{
		Color primary;
		Color secondary;
		if (config.random())
		{
			primary = randomColor1;
			secondary = config.secondcolor_active() ? randomColor2 : randomColor1;
		}
		else
		{
			primary = config.color();
			secondary = config.secondcolor_active() ? config.secondcolor() : config.color();
		}

		int satShift = 0;
		int brightShift = 0;
		if (config.expTone())
		{
			float strength = config.expToneStrength() / 100f;
			float[] hsb = Color.RGBtoHSB(primary.getRed(), primary.getGreen(), primary.getBlue(), null);
			// Middle-of-the-road colours change little; vivid/pale and bright/dark colours push the surfaces that way
			satShift = Math.round((hsb[1] * 7f - 4.5f) * strength);
			brightShift = Math.max(-24, Math.min(16, Math.round((hsb[2] - 0.7f) * 60f * strength)));
		}

		currentPrimary = primary;
		synchronized (dataProcessor)
		{
			dataProcessor.setTone(satShift, brightShift);
			dataProcessor.recolorData(primary, secondary, config.harmonize());
		}
	}

	//
	// Experimental: the red pattern painted on the floor itself
	//
	// The floor's red swirl pattern is part of the tiles (tile paint and tile models), not an object,
	// so the normal recolor never touches it. It also shows through the see-through singing bowl beam.
	// Tile colours are rebuilt from the map every time the area loads, so nothing needs undoing.
	private Color currentPrimary = new Color(25, 45, 135);
	private static final int RED_REFERENCE = 65452;

	private void recolorTile(Tile tile)
	{
		SceneTilePaint paint = tile.getSceneTilePaint();
		if (paint != null)
		{
			paint.setSwColor(recolorTileColor(paint.getSwColor()));
			paint.setSeColor(recolorTileColor(paint.getSeColor()));
			paint.setNwColor(recolorTileColor(paint.getNwColor()));
			paint.setNeColor(recolorTileColor(paint.getNeColor()));
		}

		SceneTileModel tileModel = tile.getSceneTileModel();
		if (tileModel != null)
		{
			recolorTileColors(tileModel.getTriangleColorA());
			recolorTileColors(tileModel.getTriangleColorB());
			recolorTileColors(tileModel.getTriangleColorC());
		}
	}

	private void recolorTileColors(int[] colors)
	{
		if (colors == null)
		{
			return;
		}
		for (int i = 0; i < colors.length; i++)
		{
			colors[i] = recolorTileColor(colors[i]);
		}
	}

	private int recolorTileColor(int hsl)
	{
		// skip hidden/invalid values (e.g. 12345678) and anything that isn't clearly red
		if (hsl < 0 || hsl > 0xFFFF)
		{
			return hsl;
		}
		int hue = (hsl >> 10) & 63;
		int sat = (hsl >> 7) & 7;
		if (sat < 2 || (hue > 4 && hue < 59))
		{
			return hsl;
		}
		synchronized (dataProcessor)
		{
			return dataProcessor.hueShift(hsl, currentPrimary, config.harmonize() ? hsl : RED_REFERENCE);
		}
	}
}
