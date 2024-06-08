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
import java.awt.*;
import java.util.*;
import java.util.List;

@Slf4j
@PluginDescriptor(
		name = "CG recolor",
		description = "Escape the red prison today!",
		conflicts = "117 HD"	// might be adressed in a future update of this plugin, but currently not compatible
)
public class RecolorCG extends Plugin
{

	// Declarations
	private static final List<Integer> OBJECT_IDS = Arrays.asList(35965, 35966, 35967, 35968, 35968, 35969, 35970, 35971, 35972, 35972, 35973, 35974, 35975, 35976, 35977, 35980, 35992, 35994, 35994, 35995, 35996, 35997, 35998, 35999, 36000, 36001, 36002, 36003, 36004, 36005, 36006, 36007, 36008, 37337);
	private static final List<Integer> GROUND_IDS = Arrays.asList(36046, 36047, 36048, 36052, 36053, 36054, 36055, 36056, 36057, 36058, 36059);
	private static final List<Integer> NPC_IDS = Arrays.asList(9035, 9036, 9037, 9038, 9039, 9040, 9041, 9042, 9043, 9044, 9045, 9046, 9047, 9048);
	private static final List<Integer> PROJECTILE_IDS = Arrays.asList(1702, 1708, 1712, 1714, 1723);
	private final WorldPoint centerTile = new WorldPoint(1976, 5687, 1);	// one of the 4 center tiles in the boss room
	private static final int REGION_ID_GAUNTLET_LOBBY = 12127;
	private static final int REGION_ID_GAUNTLET_CORRUPTED = 7768;
	//private static final int REGION_ID_GAUNTLET_NORMAL = 7512;

	// Will likely use the ITEM_IDS in a future update of this plugin
	//private static final List<Integer> ITEM_IDS = Arrays.asList(23824, 23834, 23821, 23822, 23823, 23820, 23835, 23837, 23838, 23585, 23582, 23583, 23584, 23849, 23850, 23851, 23855, 23856, 23857);

	private ArrayList<GameObject> recordedGameObjects = new ArrayList<>();
	private ArrayList<GroundObject> recordedGroundObjects = new ArrayList<>();
	private ArrayList<NPC> recordedNPCs = new ArrayList<>();
	private ArrayList<Projectile> recordedProjectiles = new ArrayList<>();
	private ArrayList<Model> recordedModels = new ArrayList<>();
	private ArrayList<Integer> sceneIDs = new ArrayList<>();

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
		randomColor1 = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
		randomColor2 = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));

		// Vanilla model facecolors are stored in a .txt -> the new model colors can be calculated before the models even appear making the spawnEvents less expensive
		if(config.secondcolor_active() && config.random())
		{
			this.dataProcessor = new ModelDataProcessor("src/main/resources/model_facecolors.txt", randomColor1, randomColor2);
		}
		else if (config.secondcolor_active() && !config.random())
		{
			this.dataProcessor = new ModelDataProcessor("src/main/resources/model_facecolors.txt", config.color(), config.secondcolor());
		}
		else if (!config.secondcolor_active() && config.random())
		{
			this.dataProcessor = new ModelDataProcessor("src/main/resources/model_facecolors.txt", randomColor1, randomColor1);
		}
		else
		{
			this.dataProcessor = new ModelDataProcessor("src/main/resources/model_facecolors.txt", config.color(), config.color());
		}

		// If the user is already logged in AND inside the gauntlet, Hun still needs to be recolored
		// Hun gets recolored on GamestateChanges
		if(client.getGameState() == GameState.LOGGED_IN)
		{
			regionId = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
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
		clientThread.invoke(() ->
		{
			clearAll();
			resetSceneIDs();

			//freeing the stored data.
			recordedGameObjects.clear();
			recordedGroundObjects.clear();
			recordedNPCs.clear();
			recordedProjectiles.clear();
			recordedModels.clear();
			sceneIDs.clear();
			synchronized (dataProcessor)
			{
				dataProcessor.cleanUp();
				dataProcessor = null;
			}
			rand = null;

			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Some game models may still be affected by the plugin. Please re-log to ensure that everything is properly reset.", null);
			if(client.getGameState() == GameState.LOGGED_IN)
			{
				client.setGameState(GameState.LOADING);
			}
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if(event.getGroup().equals("recolorCG"))
		{
			if(event.getKey().equals("random"))
			{
				randomColor1 = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
				randomColor2 = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
			}
			if (!config.random())
			{
				if (config.secondcolor_active())
				{
					synchronized (dataProcessor)
					{
						dataProcessor.recolorData(config.color(), config.secondcolor());
					}
				}
				else
				{
					synchronized (dataProcessor)
					{
						dataProcessor.recolorData(config.color(), config.color());
					}
				}
			}
			else
			{
				if(config.secondcolor_active())
				{
					synchronized (dataProcessor)
					{
						dataProcessor.recolorData(randomColor1, randomColor2);
					}
				}
				else
				{
					synchronized (dataProcessor)
					{
						dataProcessor.recolorData(randomColor1, randomColor1);
					}
				}
			}
			clientThread.invoke(() ->
			{
				clearAll();
				recolorAll();
			});
		}

	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (OBJECT_IDS.contains(event.getGameObject().getId()))
		{
			recordedGameObjects.add(event.getGameObject());

			// need to recolor the depleted ressources on spawn
			int ID = event.getGameObject().getId();
			if(ID == 35974 || ID == 35968 || ID == 35976 || ID == 35972 || ID == 35970)
			{
				recolorGameObject(event.getGameObject());
			}
			// only needs to happen on spawn when in boss room. For every other situation, recoloring on gamestate-changes is sufficient
			if(client.getVarbitValue(9177) == 1)
			{
				if(ID == 35992||ID == 36000|| ID == 36001)
				{
					recolorGameObject(event.getGameObject());
				}
			}
		}
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		if (GROUND_IDS.contains(event.getGroundObject().getId()))
		{
			recordedGroundObjects.add(event.getGroundObject());

			// only needs to happen on spawn when in boss room. For every other situation, recoloring on gamestate-changes is sufficient
			if(client.getVarbitValue(9117) == 1)
			{
				if(event.getGroundObject().getId() == 36047 || event.getGroundObject().getId() == 36048|| event.getGroundObject().getId() == 36046)
				{
					recolorGroundObject(event.getGroundObject());
				}
			}
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (NPC_IDS.contains(event.getNpc().getId()))
		{
			recordedNPCs.add(event.getNpc());
			recolorNPC(event.getNpc());
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		if (NPC_IDS.contains(event.getNpc().getId()))
		{
			recordedNPCs.add(event.getNpc());
			recolorNPC(event.getNpc());
		}
	}


	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		if(PROJECTILE_IDS.contains(event.getProjectile().getId()))
		{
			recordedProjectiles.add(event.getProjectile());
			recolorProjectile(event.getProjectile());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if(event.getGameState() == GameState.LOADING)
		{
			regionId = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
			if(regionId == REGION_ID_GAUNTLET_CORRUPTED)
			{
				recordedGameObjects.clear();
				recordedGroundObjects.clear();
			}
		}
		if(event.getGameState() == GameState.LOGGED_IN)
		{
			regionId = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
			if(regionId == REGION_ID_GAUNTLET_CORRUPTED)
			{
				clientThread.invokeAtTickEnd(()->
				{
				recolorAll();
				});
			}
			if(regionId == REGION_ID_GAUNTLET_LOBBY)
			{
				// clearing everything after every run
				resetSceneIDs();
				recordedGameObjects.clear();
				recordedGroundObjects.clear();
				recordedNPCs.clear();
				recordedProjectiles.clear();

				if (config.random())
				{
					randomColor1 = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
					randomColor2 = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
					if(config.secondcolor_active())
					{
						synchronized (dataProcessor)
						{
							dataProcessor.recolorData(randomColor1, randomColor2);
						}
					}
					else
					{
						synchronized (dataProcessor)
						{
							dataProcessor.recolorData(randomColor1, randomColor1);
						}
					}
				}
			}
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if(event.getVarbitId() == 9177 && event.getValue() == 1)	// varbit 9177 is 1 if player is in the boss room
		{
			recolorGate();
		}
	}

	// resets all GameObjects, GroundObjects, NPCs (including Hunllef) and Projectiles to their default colors, if they are stored in the corresponding list.
	public void clearAll()
	{
		for(int i = 0; i < recordedGameObjects.size(); i++)
		{
			GameObject g = recordedGameObjects.get(i);

			Renderable renderable = g.getRenderable();
			Model model = verifyModel(renderable);
			if (model == null)
			{
				log.debug("clearAll returned null! - GameObject");
				continue;
			}
			synchronized (dataProcessor)
			{
				dataProcessor.applyColors(g.getId(), "GameObject", model, false);
			}
		}

		for(int i = 0; i < recordedGroundObjects.size(); i++)
		{
			GroundObject g = recordedGroundObjects.get(i);

			Renderable renderable = g.getRenderable();
			Model model = verifyModel(renderable);
			if (model == null)
			{
				log.debug("clearAll returned null! - GroundObject");
				continue;
			}
			synchronized (dataProcessor)
			{
				dataProcessor.applyColors(g.getId(), "GroundObject", model, false);
			}
		}

		for(int i = 0; i < recordedNPCs.size(); i++)
		{
			NPC g = recordedNPCs.get(i);
			if (g.getModel() == null)
			{
				log.debug("clearAll returned null! - GroundObject");
				continue;
			}
			synchronized (dataProcessor)
			{
				dataProcessor.applyColors(g.getId(), "NPC", g.getModel(), false);
			}
		}

		for(int i = 0; i < recordedProjectiles.size(); i++)
		{
			Projectile g = recordedProjectiles.get(i);
			synchronized (dataProcessor)
			{
				dataProcessor.applyColors(g.getId(), "Projectile", g.getModel(), false);
			}
		}

	}

	// recolors all GameObjects, GroundObjects, NPCs (including Hunllef) and Projectiles to their desired colors, if they are stored in the corresponding list.
	// differentiating between NPCs and tornados, even though tornados are technically a NPC
	public void recolorAll()
	{
		for(GameObject gameObject : recordedGameObjects)
		{
			recolorGameObject(gameObject);
		}

		for(GroundObject groundObject : recordedGroundObjects)
		{
			recolorGroundObject(groundObject);
		}

		for(NPC npc : recordedNPCs)
		{
			recolorNPC(npc);
		}

	}

	public void recolorGameObject(GameObject gameObject)
	{
		Renderable renderable = gameObject.getRenderable();
		Model model = verifyModel(renderable);
		if (model == null)
		{
			log.debug("recolorGameObject returned null!");
			return;
		}

		synchronized (dataProcessor)
		{
			dataProcessor.applyColors(gameObject.getId(), "GameObject", model, true);
		}
		recordedModels.add(model);
		sceneIDs.add(model.getSceneId());
		model.setSceneId(0);
	}

	public void recolorGroundObject(GroundObject groundObject)
	{
		if(groundObject.getId() == 36047 || groundObject.getId() == 36048)
		{	//Damaging ground recolor depends on config
			if(!config.groundRecolor())
			{
				return;
			}
		}

		Renderable renderable = groundObject.getRenderable();
		Model model = verifyModel(renderable);
		if (model == null)
		{
			log.debug("recolorGroundObject returned null!");
			return;
		}

		synchronized (dataProcessor)
		{
			dataProcessor.applyColors(groundObject.getId(), "GroundObject", model, true);
		}
		recordedModels.add(model);
		sceneIDs.add(model.getSceneId());
		model.setSceneId(0);

	}

	public void recolorNPC(NPC npc)
	{
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
		if(config.projectileRecolor())
		{
			Model model = projectile.getModel();
			if (model == null)
			{
				log.debug("recolorProjectile returned null!");
				return;
			}
			synchronized (dataProcessor)
			{
				dataProcessor.applyColors(projectile.getId(), "Projectile", model, true);
			}
		}
	}

	// This method is needed because the gate changes upon entering. That means it has to be recolored if a) the timer runs out and you get teleported or b) you pass through it
	public void recolorGate()
	{
		for(int i = 0; i < recordedGameObjects.size(); i++)
		{
			GameObject g = recordedGameObjects.get(i);
			if(g.getId() == 37337)
			{
				Renderable renderable = g.getRenderable();
				Model model = verifyModel(renderable);
				if (model == null)
				{
					log.debug("recolorGate returned null!");
					continue;
				}
				synchronized (dataProcessor)
				{
					dataProcessor.applyColors(g.getId(), "GameObject", model, true);
				}
				recordedModels.add(model);
				sceneIDs.add(model.getSceneId());
				model.setSceneId(0);
			}
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
			Model model = renderable.getModel();
			if (model == null)
			{
				log.debug("verifyModel returned null!");
				return null;
			}
			return model;
		}
	}

	// resets all changed SceneIDs of the models to prevent issues outside of the gauntlet
	private void resetSceneIDs()
	{
		int size = sceneIDs.size();
		for (int i = 0; i < size; i++)
		{
			recordedModels.get(i).setSceneId(sceneIDs.get(i));
		}
		recordedModels.clear();
		sceneIDs.clear();
	}

}