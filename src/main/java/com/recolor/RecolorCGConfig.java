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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.*;

@ConfigGroup("recolorCG")
public interface RecolorCGConfig extends Config
{
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
		return new Color(25, 45, 135);
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
		return new Color(165, 210, 10);
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
			name = "Recolor Hunllef's projectiles",
			description = "Recolor Hunllef's mage, range and prayer disabling attack.",
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
			warning = "This feature can significantly reduce the visibility of the damaging floor.",
			position = 2,
			section = togglesSection
	)
	default boolean groundRecolor()
	{
		return false;
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

}
