/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
		conflicts = "117 HD"	// might be adressed in a future update of this plugin, but currently just not compatible
)
public class RecolorCG extends Plugin
{

	private static final List<Integer> OBJECT_IDS = Arrays.asList(35977, 35966, 35965, 35980, 37337, 35969, 35967, 35973, 35994, 35992, 36001, 36000, 35999, 35998, 35997, 35971, 35968, 35972, 35970, 35994, 35995, 35996, 36002, 35974, 35975, 35976, 35968, 35972, 36003, 36004, 36005, 36006, 36007, 36008);
	private static final List<Integer> GROUND_IDS = Arrays.asList(36046, 36052, 36053, 36054, 36055, 36056, 36057, 36058, 36059, 36048, 36047);
	private static final List<Integer> NPC_IDS = Arrays.asList(9035, 9041, 9040, 9044, 9043, 9045, 9048, 9042, 9046, 9047, 9037, 9036, 9039);
	private static final List<Integer> PROJECTILE_IDS = Arrays.asList(1712, 1708, 1702, 1714, 1723);

	public static final int REGION_ID_GAUNTLET_LOBBY = 12127;
	public static final int REGION_ID_GAUNTLET_CORRUPTED = 7768;
	//public static final int REGION_ID_GAUNTLET_NORMAL = 7512;


	//Will likely use the ITEM_IDS in a future update of this plugin
	//private static final List<Integer> ITEM_IDS = Arrays.asList(23824, 23834, 23821, 23822, 23823, 23820, 23835, 23837, 23838, 23585, 23582, 23583, 23584, 23849, 23850, 23851, 23855, 23856, 23857);

	private ArrayList<GameObject> recordedGameObjects = new ArrayList<>();
	private ArrayList<GroundObject> recordedGroundObjects = new ArrayList<>();
	private ArrayList<NPC> recordedNPCs = new ArrayList<>();
	private ArrayList<NPC> recordedChangedNPCs = new ArrayList<>();
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
		log.info("Recolor started!");
		rand = new Random();
		randomColor1 = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
		randomColor2 = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));

		// Vanilla model facecolors are stored in a .txt -> the new model colors can be calculated before the models even appear making the spawnEvents less expensive
		if(config.secondcolor_active())
		{
			dataProcessor = new ModelDataProcessor("src/main/resources/model_facecolors.txt", config.color(), config.secondcolor());
		}
		else
		{
			dataProcessor = new ModelDataProcessor("src/main/resources/model_facecolors.txt", config.color(), config.color());
		}

	}

	@Override
	protected void shutDown() throws Exception
	{
		clientThread.invoke(() ->
		{
			clearAll();

			//freeing the stored data.
			recordedGameObjects.clear();
			recordedGroundObjects.clear();
			recordedNPCs.clear();
			recordedChangedNPCs.clear();

			recordedProjectiles.clear();
			recordedModels.clear();
			sceneIDs.clear();
			dataProcessor.cleanUp();

			dataProcessor = null;
			rand = null;

			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Some game models may still be affected by the plugin. Please re-log to ensure that everything is properly reset.", null);
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
					dataProcessor.recolorData(config.color(), config.secondcolor());
				}
				else
				{
					dataProcessor.recolorData(config.color(), config.color());
				}
			}
			else
			{
				if(config.secondcolor_active())
				{
					dataProcessor.recolorData(randomColor1, randomColor2);
				}
				else
				{
					dataProcessor.recolorData(randomColor1, randomColor1);
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
			resetGameObject(event.getGameObject());
			recolorGameObject(event.getGameObject());
		}
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		if (GROUND_IDS.contains(event.getGroundObject().getId()))
		{
			recordedGroundObjects.add(event.getGroundObject());
			resetGroundObject(event.getGroundObject());
			recolorGroundObject(event.getGroundObject());
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (NPC_IDS.contains(event.getNpc().getId()))
		{
			recordedNPCs.add(event.getNpc());
			resetNPC(event.getNpc());
			recolorNPC(event.getNpc());
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		if (NPC_IDS.contains(event.getNpc().getId()))
		{
			recordedNPCs.add(event.getNpc());
			resetNPC(event.getNpc());
			recolorNPC(event.getNpc());
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		if(PROJECTILE_IDS.contains(event.getProjectile().getId()))
		{
			recordedProjectiles.add(event.getProjectile());
			resetProjectile(event.getProjectile());
			recolorProjectile(event.getProjectile());
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (NPC_IDS.contains(event.getNpc().getId()))
		{
			resetNPC(event.getNpc());
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event) throws Exception
	{
		// Gate changes its model when you pass through it. The new model needs to be recolored immediately or else it will be vanilla red.
		// If you find a less expensive alternative, contact me.
		if(client.getGameState() == GameState.LOGGED_IN)
		{
			regionId = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
			if (regionId == REGION_ID_GAUNTLET_CORRUPTED)
			{
				recolorGate();
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if(event.getGameState() == GameState.LOGGED_IN)
		{
			resetSceneIDs();
			regionId = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
			if(regionId == REGION_ID_GAUNTLET_LOBBY)
			{
				recordedGameObjects.clear();
				recordedGroundObjects.clear();
				recordedNPCs.clear();
				recordedChangedNPCs.clear();
				recordedProjectiles.clear();
				if (config.random())
				{
					randomColor1 = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
					randomColor2 = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
					if(config.secondcolor_active())
					{
						dataProcessor.recolorData(randomColor1, randomColor2);
					}
					else
					{
						dataProcessor.recolorData(randomColor1, randomColor1);
					}
				}
			}
		}
	}



	public void clearAll()
	{
		for(int i = 0; i < recordedGameObjects.size(); i++)
		{
			GameObject g = recordedGameObjects.get(i);

			Renderable renderable = g.getRenderable();
			Model model = verifyModel(renderable);
			if (model == null)
			{
				log.info("clearAll returned null! - GameObject");
				continue;
			}
			dataProcessor.applyColors(g.getId(), "GameObject", model, false);
		}

		for(int i = 0; i < recordedGroundObjects.size(); i++)
		{
			GroundObject g = recordedGroundObjects.get(i);

			Renderable renderable = g.getRenderable();
			Model model = verifyModel(renderable);
			if (model == null)
			{
				log.info("clearAll returned null! - GroundObject");
				continue;
			}

			dataProcessor.applyColors(g.getId(), "GroundObject", model, false);
		}


		for(int i = 0; i < recordedNPCs.size(); i++)
		{
			NPC g = recordedNPCs.get(i);
			if (g.getModel() == null)
			{
				log.info("clearAll returned null! - GroundObject");
				continue;
			}
			dataProcessor.applyColors(g.getId(), "NPC", g.getModel(), false);
		}


		for(int i = 0; i < recordedProjectiles.size(); i++)
		{
			Projectile g = recordedProjectiles.get(i);
			dataProcessor.applyColors(g.getId(), "Projectile", g.getModel(), false);
		}

	}

	public void recolorAll(){
		for(int i = 0; i < recordedGameObjects.size(); i++)
		{
			GameObject g = recordedGameObjects.get(i);

			Renderable renderable = g.getRenderable();
			Model model = verifyModel(renderable);
			if (model == null)
			{
				log.info("recolorAll returned null! - GameObject");
				continue;
			}
			dataProcessor.applyColors(g.getId(), "GameObject", model, true);
			recordedModels.add(model);
			sceneIDs.add(model.getSceneId());
			model.setSceneId(0);
		}

		for(int i = 0; i < recordedGroundObjects.size(); i++)
		{
			GroundObject g = recordedGroundObjects.get(i);
			if(g.getId() == 36047 || g.getId() == 36048)	//Damaging ground recolor depends on config
			{
				if(!config.groundRecolor())
				{
					continue;
				}
			}

			Renderable renderable = g.getRenderable();
			Model model = verifyModel(renderable);
			if (model == null)
			{
				log.info("recolorAll returned null! - GroundObject");
				continue;
			}

			dataProcessor.applyColors(g.getId(), "GroundObject", model, true);
			recordedModels.add(model);
			sceneIDs.add(model.getSceneId());
			model.setSceneId(0);
		}

		if(config.npcRecolor() && config.tornado())
		{
			for (int i = 0; i < recordedNPCs.size(); i++)
			{
				NPC g = recordedNPCs.get(i);
				if(g.getModel() == null){
					log.info("recolorAll returned null! - NPC");
					continue;
				}
				dataProcessor.applyColors(g.getId(), "NPC", g.getModel(), true);
			}
		}
		else if (!config.npcRecolor() && config.tornado())
		{
			for (int i = 0; i < recordedNPCs.size(); i++)
			{
				NPC g = recordedNPCs.get(i);
				if(g.getModel() == null)
				{
					log.info("recolorAll returned null! - NPC2");
					continue;
				}
				if(g.getId() == 9039)
				{
					dataProcessor.applyColors(g.getId(), "NPC", g.getModel(), true);
				}
			}
		}
		else if (config.npcRecolor() && !config.tornado())
		{
			for (int i = 0; i < recordedNPCs.size(); i++)
			{
				NPC g = recordedNPCs.get(i);
				if(g.getModel() == null)
				{
					log.info("recolorAll returned null! - NPC3");
					continue;
				}
				if(g.getId() == 9039)
				{
					continue;
				}
				dataProcessor.applyColors(g.getId(), "NPC", g.getModel(), true);
			}
		}

		if(config.projectileRecolor())
		{
			for (int i = 0; i < recordedProjectiles.size(); i++)
			{
				Projectile g = recordedProjectiles.get(i);
				dataProcessor.applyColors(g.getId(), "Projectile", g.getModel(), true);
			}
		}

	}

	public void recolorGameObject(GameObject gameObject)
	{
		Renderable renderable = gameObject.getRenderable();

		Model model = verifyModel(renderable);
		if (model == null)
		{
			log.info("recolorGameObject returned null!");
			return;
		}

		dataProcessor.applyColors(gameObject.getId(), "GameObject", model, true);
		recordedModels.add(model);
		sceneIDs.add(model.getSceneId());
		model.setSceneId(0);
	}

	public void recolorGroundObject(GroundObject groundObject)
	{

		if(groundObject.getId() == 36047 || groundObject.getId() == 36048){	//Damaging ground recolor depends on config
			if(!config.groundRecolor())
			{
				return;
			}
		}

		Renderable renderable = groundObject.getRenderable();
		Model model = verifyModel(renderable);
		if (model == null)
		{
			log.info("recolorGroundObject returned null!");
			return;
		}

		dataProcessor.applyColors(groundObject.getId(), "GroundObject", model, true);
		recordedModels.add(model);
		sceneIDs.add(model.getSceneId());
		model.setSceneId(0);

	}

	public void recolorNPC(NPC npc)
	{
		if(config.npcRecolor() && config.tornado())
		{
			Model model = npc.getModel();
			if (model == null)
			{
				log.info("recolorNPC returned null! v1");
				return;
			}

			dataProcessor.applyColors(npc.getId(), "NPC", model, true);
		}


		if(!config.npcRecolor() && config.tornado())
		{
			if(npc.getId() != 9039)
			{
				return;
			}
			Model model = npc.getModel();
			if (model == null)
			{
				log.info("recolorNPC returned null! v2");
				return;
			}

			dataProcessor.applyColors(npc.getId(), "NPC", model, true);
		}

		if(config.npcRecolor() && !config.tornado())
		{
			if(npc.getId() == 9039)
			{
				return;
			}
			Model model = npc.getModel();
			if (model == null)
			{
				log.info("recolorNPC returned null! v3");
				return;
			}
			dataProcessor.applyColors(npc.getId(), "NPC", model, true);
		}


	}

	public void recolorProjectile(Projectile projectile)
	{
		if(config.projectileRecolor())
		{
			Model model = projectile.getModel();
			if (model == null)
			{
				log.info("recolorProjectile returned null!");
				return;
			}
			dataProcessor.applyColors(projectile.getId(), "Projectile", model, true);
		}

	}

	public void resetGameObject(GameObject gameObject)
	{
		Renderable renderable = gameObject.getRenderable();
		Model model = verifyModel(renderable);
		dataProcessor.applyColors(gameObject.getId(), "GameObject", model, false);
		recordedModels.add(model);
		sceneIDs.add(model.getSceneId());
		model.setSceneId(0);
	}

	public void resetGroundObject(GroundObject groundObject)
	{
		Renderable renderable = groundObject.getRenderable();
		Model model = verifyModel(renderable);
		if (model == null)
		{
			log.info("resetGroundObject returned null!");
			return;
		}
		dataProcessor.applyColors(groundObject.getId(), "GroundObject", model, false);
		recordedModels.add(model);
		sceneIDs.add(model.getSceneId());
		model.setSceneId(0);
	}

	public void resetNPC(NPC npc)
	{
		Model model = npc.getModel();
		if (model == null)
		{
			log.info("resetNPC returned null!");
			return;
		}
		dataProcessor.applyColors(npc.getId(), "NPC", model, false);
	}

	public void resetProjectile(Projectile projectile)
	{
		Model model = projectile.getModel();
		if (model == null)
		{
			log.info("resetProjectile returned null!");
			return;
		}
		dataProcessor.applyColors(projectile.getId(), "Projectile", model, false);
	}

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
					log.info("recolorGate returned null!");
					continue;
				}
				dataProcessor.applyColors(g.getId(), "GameObject", model, true);
			}
		}
	}

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
				log.info("verifyModel returned null!");
				return null;
			}
			return model;
		}
	}

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