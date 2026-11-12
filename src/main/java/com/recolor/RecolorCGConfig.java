package com.recolor;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

import java.awt.*;

@ConfigGroup(RecolorCGConfig.GROUP)
public interface RecolorCGConfig extends Config
{
	String GROUP = "recolorCGHD";

	@ConfigSection(
			name = "Colors",
			description = "General color settings",
			position = 0
	)
	String colorsSection = "colors";

	@ConfigSection(
			name = "Toggles",
			description = "Toggle different recolors on or off",
			position = 1
	)
	String togglesSection = "toggles";

	@ConfigSection(
			name = "117 HD",
			description = "Settings for using this plugin alongside 117 HD",
			position = 2
	)
	String hdSection = "hd";

	@ConfigSection(
			name = "Experimental",
			description = "New options that aren't in the original plugin. May look odd with some colours.",
			position = 3,
			closedByDefault = true
	)
	String experimentalSection = "experimental";

	//
	// Begin of color section
	//
	@ConfigItem(
			keyName = "color",
			name = "Color",
			description = "Recolor the Corrupted Gauntlet with this color.",
			position = 0,
			section = colorsSection
	)
	default Color color()
	{
		return new Color(0, 145, 255);
	}

	@ConfigItem(
			keyName = "secondcolor",
			name = "Secondary color",
			description = "Recolor certain parts of the gauntlet differently.",
			position = 1,
			section = colorsSection
	)
	default Color secondcolor()
	{
		return new Color(210, 175, 15);
	}

	@ConfigItem(
			keyName = "secondcolor_active",
			name = "Use secondary color",
			description = "If activated, the secondary color will be applied.",
			position = 2,
			section = colorsSection
	)
	default boolean secondcolor_active()
	{
		return true;
	}

	@ConfigItem(
			keyName = "random",
			name = "Random color each run",
			description = "Every time you enter the corrupted gauntlet, it will be a random color. To randomise both first and secondary color, activate the 'Use secondary color' feature.",
			position = 3,
			section = colorsSection
	)
	default boolean random()
	{
		return false;
	}

	//
	// Begin of toggle section
	//
	@ConfigItem(
			keyName = "npcRecolor",
			name = "Recolor NPCs",
			description = "Recolor all the NPCs in the corrupted gauntlet.",
			position = 0,
			section = togglesSection
	)
	default boolean npcRecolor()
	{
		return true;
	}

	@ConfigItem(
			keyName = "projectileRecolor",
			name = "Recolor projectiles",
			description = "Recolor all the projectiles from NPCs in the corrupted gauntlet.",
			position = 1,
			section = togglesSection
	)
	default boolean projectileRecolor()
	{
		return true;
	}

	@ConfigItem(
			keyName = "groundRecolor",
			name = "Recolor damaging floor",
			description = "Recolor the floor that damages you during the Hunllef fight.",
			position = 2,
			section = togglesSection
	)
	default boolean groundRecolor()
	{
		return true;
	}

	@ConfigItem(
			keyName = "tornado",
			name = "Recolor Hunleff's tornadoes",
			description = "Recolor the tornadoes that spawn during the hunllef fight.",
			position = 3,
			section = togglesSection
	)
	default boolean tornado()
	{
		return true;
	}

	@ConfigItem(
			keyName = "harmonize",
			name = "Harmonize colors",
			description = "Adjusts colors for a unified look.",
			position = 4,
			section = togglesSection
	)
	default boolean harmonize()
	{
		return true;
	}

	//
	// Begin of 117 HD section
	//
	@ConfigItem(
			keyName = "hdPauseLights",
			name = "Pause 117 HD lights in CG",
			description = "While you're inside the Corrupted Gauntlet, turns off 117 HD's NPC lights and Object lights<br>" +
					"(they are hardcoded red and ignore the recolor). Your previous 117 HD settings are restored when you leave.",
			position = 0,
			section = hdSection
	)
	default boolean hdPauseLights()
	{
		return true;
	}

	//
	// Begin of experimental section
	//
	@ConfigItem(
			keyName = "expBetterRandom",
			name = "Better random colours",
			description = "Random mode picks a hue directly (spread evenly over all 64 the game supports) with strong saturation,<br>" +
					"instead of a random RGB value that is often greyish. Also keeps the secondary colour clearly different.",
			position = 0,
			section = experimentalSection
	)
	default boolean expBetterRandom()
	{
		return false;
	}

	@Range(min = 4, max = 32)
	@ConfigItem(
			keyName = "expHueGap",
			name = "Min. secondary hue gap",
			description = "With better random colours, how far apart (out of 64 hues) the primary and secondary colours must be.",
			position = 1,
			section = experimentalSection
	)
	default int expHueGap()
	{
		return 16;
	}

	@Range(min = 0, max = 10)
	@ConfigItem(
			keyName = "expNoRepeat",
			name = "Avoid recent colours",
			description = "With better random colours, don't pick a hue close to any of this many previous runs. 0 = off.",
			position = 2,
			section = experimentalSection
	)
	default int expNoRepeat()
	{
		return 4;
	}

	@ConfigItem(
			keyName = "expTone",
			name = "Vary saturation & brightness",
			description = "Normally only the hue of your colour is used on most surfaces. This also carries over some of its<br>" +
					"saturation and brightness, so dark, pale and vivid colours look different. The damaging floor is left alone.",
			position = 3,
			section = experimentalSection
	)
	default boolean expTone()
	{
		return false;
	}

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
			keyName = "expToneStrength",
			name = "Tone strength",
			description = "How strongly saturation and brightness are carried over.",
			position = 4,
			section = experimentalSection
	)
	default int expToneStrength()
	{
		return 50;
	}

	@ConfigItem(
			keyName = "expFloorPattern",
			name = "Recolour floor pattern",
			description = "Also recolours the red swirl pattern painted on the Corrupted Gauntlet floor tiles.<br>" +
					"It's part of the floor itself rather than an object, so it normally stays red,<br>" +
					"and it can show through the singing bowl's see-through beam.",
			position = 5,
			section = experimentalSection
	)
	default boolean expFloorPattern()
	{
		return false;
	}
}
