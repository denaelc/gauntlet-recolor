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

import net.runelite.api.Model;

import java.awt.*;
import java.io.*;
import java.util.*;


public class ModelDataProcessor
{
    private Map<String, Map<Integer, int[][]>> originalColorData = new HashMap<>();
    private Map<String, Map<Integer, int[][]>> recoloredColorData = new HashMap<>();

    public ModelDataProcessor(String filePath, Color newColor, Color secondaryColor) throws IOException
    {
        cacheData(filePath);
        recolorData(newColor, secondaryColor);
    }

    // creates a hashmap with all the facecolors, IDs and types (gameObject, Groundobject etc.)
    // could be simplified if the .txt gets simplified
    private void cacheData(String filePath) throws IOException
    {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath)))
        {
            String line;
            String currentType = null;
            int currentId = -1;
            int[][] colors = new int[3][];

            while ((line = reader.readLine()) != null)
            {
                if (line.trim().isEmpty()) continue;
                if (line.contains(" ID: "))
                {
                    if (currentType != null && currentId != -1)
                    {
                        originalColorData.computeIfAbsent(currentType, k -> new HashMap<>()).put(currentId, colors);
                    }
                    currentType = line.split(" ")[0];
                    currentId = Integer.parseInt(line.split(": ")[1].split(" ")[0]);
                    colors = new int[3][];
                } else if (line.startsWith("FaceColors"))
                {
                    int index = Integer.parseInt(line.substring(10, 11)) - 1;
                    colors[index] = Arrays.stream(line.split(": ")[1].replace("[", "").replace("]", "").split(", "))
                            .mapToInt(Integer::parseInt).toArray();
                }
            }
            if (currentType != null && currentId != -1)
            {
                originalColorData.computeIfAbsent(currentType, k -> new HashMap<>()).put(currentId, colors);
            }
        }
    }

    // creates a second hashmap with the recolored values, based of the vanilla hashmap
    public void recolorData(Color newColor, Color secondaryColor)
    {
        recoloredColorData.clear();
        originalColorData.forEach((type, models) ->
        {
            Map<Integer, int[][]> recoloredMap = new HashMap<>();
            models.forEach((id, colors) -> {
                int[][] recoloredColors = new int[colors.length][];
                for (int i = 0; i < colors.length; i++)
                {
                    recoloredColors[i] = recolor(colors[i], newColor, secondaryColor);
                }
                recoloredMap.put(id, recoloredColors);
            });
            recoloredColorData.put(type, recoloredMap);
        });
    }

    // recolors a single array of colors (e.g. facecolors1 of a single model)
    private int[] recolor(int[] originalColors, Color newColor, Color secondaryColor)
    {
        int[] newColors = new int[originalColors.length];
        for (int i = 0; i < originalColors.length; i++) {
            // Color needs to be in the relevant range and > 50, else there will be visual bugs
            if (inRange(originalColors[i]) &&Math.abs(originalColors[i]) > 50)
            {
                newColors[i] = newColorHsb(originalColors[i], newColor, secondaryColor);
            }
            else
            {
                newColors[i] = originalColors[i];
            }
        }
        return newColors;
    }

    // Making sure that the colors in the relevant range of colors
    public boolean inRange(int facecolor)
    {
        return Math.abs(59327 - facecolor) < 10000 || Math.abs(959 - facecolor) < 10000;
    }

    // applies the colors to a model
    public void applyColor(Model model, int[] f1, int[] f2, int[] f3)
    {
        int[] faceColors = model.getFaceColors1();
        int[] faceColors2 = model.getFaceColors2();
        int[] faceColors3 = model.getFaceColors3();

        //int length1 = Math.min(f1.length, faceColors.length);
        //int length2 = Math.min(f2.length, faceColors2.length);
        //int length3 = Math.min(f3.length, faceColors3.length);
        if (f1.length <= faceColors.length && f2.length <= faceColors2.length && f3.length <= faceColors3.length)
        {
            System.arraycopy(f1, 0, faceColors, 0, f1.length);
            System.arraycopy(f2, 0, faceColors2, 0, f2.length);
            System.arraycopy(f3, 0, faceColors3, 0, f3.length);
        }
        else{
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"))) {
                for (int i = 0; i < faceColors.length; i++) {
                    writer.write(faceColors[i] + ", ");
                }
                writer.newLine();
                for (int i = 0; i < faceColors2.length; i++) {
                    writer.write(faceColors2[i] + ", ");
                }
                writer.newLine();
                for (int i = 0; i < faceColors3.length; i++) {
                    writer.write(faceColors3[i] + ", ");
                }
                writer.newLine();
                for (int i = 0; i < f1.length; i++) {
                    writer.write(f1[i] + ", ");
                }
                writer.newLine();
                System.out.println("Data written to file successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // returns the new color in the rs2hsb format
    public int newColorHsb(int faceColor, Color newColor, Color secondaryColor)
    {
        // > 60k are mostly the very bright colors.
        if(faceColor > 60000)
        {
            if(secondaryColor != newColor)
            {
                return brightColors(faceColor, secondaryColor);
            }
            return brightColors(faceColor, newColor);
        }
        // all other colors should only be Hue shifted. This prevents normally unlit models from becoming too bright or too dark
        return shiftHue(faceColor, shiftHueAmmount(faceColor, colorToRs2hsb(newColor)));
    }

    // Method is functional, but has a lot of expensive variables. Will likely be adressed in a future iteration.
    // Could be implemented similar to shiftHue
    //
    // General Idea: calculate the distance of the vanilla facecolor to a reference color (65452) and then apply that distance
    // to the new (reference) color, to get a similar shifted color.
    public int brightColors(int faceColor, Color newColor)
    {
        int newColorHsb = colorToRs2hsb(newColor);

        // values of the facecolor
        int hueFace = extractHsbValues(faceColor, 6, 11);
        int saturationFace = extractHsbValues(faceColor, 3, 8);
        int brightnessFace = extractHsbValues(faceColor, 7, 1);
        // values of the new reference color
        int hueRef = extractHsbValues(newColorHsb, 6, 11);
        int saturationRef = extractHsbValues(newColorHsb, 3, 8);
        int brightnessRef = extractHsbValues(newColorHsb, 7, 1);
        // values for the current reference color (65452)
        int referenceHue = 63;
        int referenceSat = 7;
        int referenceBright = 44;

        int hueDiff = referenceHue - hueFace;
        int satDiff = referenceSat - saturationFace;
        int brightDiff = referenceBright - brightnessFace;

        int newHue = hueRef - hueDiff;
        newHue = (newHue % 64 + 64) % 64;

        int newSat = saturationRef - satDiff;
        newSat = (newSat % 8 + 8) % 8;

        int newBright = brightnessRef - brightDiff / 4;     // reducing the brightness difference before applying it, to prevent complete white/black results
        newBright -= Math.min(newSat, newBright / 2);
        // making sure that the new brightness is never below 0 or above 127
        if(newBright < 0)
        {
            newBright = 0;
        }
        if(newBright > 127)
        {
            newBright = 127;
        }

        return (newHue << 10) + (newSat << 7) + newBright;
    }

    // Returns the hsb values
    static int extractHsbValues(int hsbColor, int k, int p)
    {
        return (((1 << k) - 1) & (hsbColor >> (p - 1)));
    }

    // Shifts the Hue by a given shiftAmmount
    public static int shiftHue(int hsbValue, int hueShiftAmmount)
    {
        // Extracting the Hue
        int hue = (hsbValue >> 10) & 0x3F;

        // calculating the Hue-difference
        hue = (hue + hueShiftAmmount) % 64; // 64, since Hue is only in the first 6 bits
        if (hue < 0)
        {
            hue += 64; // handling negative Hue values
        }

        return (hsbValue & ~0xFC00) | (hue << 10);
    }

    // Calculates the hue-shift-ammount between two given values
    public static int shiftHueAmmount(int hsbValue1, int hsbValue2)
    {

        int hue1 = (hsbValue1 >> 10) & 0x3F;
        int hue2 = (hsbValue2 >> 10) & 0x3F;

        int hueShiftAmount = hue2 - hue1;
        if (hueShiftAmount < -32)
        {
            hueShiftAmount += 64; // handling negative values
        }
        else if (hueShiftAmount > 32)
        {
            hueShiftAmount -= 64; // handling positive values
        }
        return hueShiftAmount;
    }

    // not my method, I dont know who to give credit for it, but I took it from AnkouOSRS, https://github.com/AnkouOSRS/cox-light-colors/blob/master/src/main/java/com/coxlightcolors/CoxLightColorsPlugin.java
    private int colorToRs2hsb(Color color)
    {
        float[] hsbVals = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        // "Correct" the brightness level to avoid going to white at full saturation, or having a low brightness at
        // low saturation
        hsbVals[2] -= Math.min(hsbVals[1], hsbVals[2] / 2);
        int encode_hue = (int)(hsbVals[0] * 63);
        int encode_saturation = (int)(hsbVals[1] * 7);
        int encode_brightness = (int)(hsbVals[2] * 127);
        return (encode_hue << 10) + (encode_saturation << 7) + (encode_brightness);
    }

    // applies either the vanilla or the recolored hashmap data to a given model
    public void applyColors(int objectId, String type, Model model, boolean useRecolored)
    {
        Map<Integer, int[][]> data = useRecolored ? recoloredColorData.getOrDefault(type, Collections.emptyMap()) : originalColorData.getOrDefault(type, Collections.emptyMap());
        int[][] colors = data.get(objectId);
        if (colors != null && colors[0] != null && colors[1] != null && colors[2] != null)
        {
            applyColor(model, colors[0], colors[1], colors[2]);
        }
    }

    // deletes the hashmaps
    public void cleanUp()
    {
        originalColorData.clear();
        recoloredColorData.clear();
    }
}