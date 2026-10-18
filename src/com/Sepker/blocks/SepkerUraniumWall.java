package com.sepker.blocks;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.blocks.defense.ShieldWall;
import mindustry.world.meta.BuildVisibility;
import mindustry.gen.Building;
import mindustry.Vars;
import arc.Core;
import arc.graphics.Color;
import arc.struct.ObjectSet;
import arc.func.Prov;

public class SepkerUraniumWall extends ShieldWall {

    public SepkerUraniumWall(String name) {
        super(name);

        
        category = Category.defense;

        if (databaseTabs == null) databaseTabs = new ObjectSet<>();
        databaseTabs.add(SepkerPlanet.sepker);
        buildVisibility = BuildVisibility.shown;

        conductivePower = true;

        health = 2048;
        armor = 16;
        size = 1;
        buildTime = 60f * 2f;
        chanceDeflect = 24;

        shieldHealth = 1024f;
        breakCooldown = 60f * 10f;
        glowColor = Color.valueOf("90EE90");

        consumePower(12f / 60f);

        requirements = ItemStack.with(
            SepkerUraniumItem.uraniumStick, 80,
            Items.phaseFabric, 20,
            Items.beryllium, 16,
            Items.thorium, 16,
            Items.copper, 16,
            Items.lead, 4,
            SepkerUraniumItem.uranium, 80
        );

        buildType = new Prov<Building>() {
            @Override
            public Building get() {
                return new UraniumWallBuild();
            }
        };
    }

    /** ⭐ 反射指向原版盾墙的护盾/辉光贴图 */
    @Override
    public void load() {
        super.load();

        try {
            java.lang.reflect.Field shieldField = ShieldWall.class.getDeclaredField("shieldRegion");
            shieldField.setAccessible(true);
            shieldField.set(this, Core.atlas.find("shielded-wall-shield"));
        } catch (Throwable ignored) { }

        try {
            java.lang.reflect.Field glowField = ShieldWall.class.getDeclaredField("glowRegion");
            glowField.setAccessible(true);
            glowField.set(this, Core.atlas.find("shielded-wall-glow"));
        } catch (Throwable ignored) { }
    }

    public class UraniumWallBuild extends ShieldWallBuild {
    }
}