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
import arc.Core;
import arc.graphics.Color;
import arc.struct.ObjectSet;
import arc.func.Prov;

public class SepkerLargeUraniumWall extends ShieldWall {

    public SepkerLargeUraniumWall(String name) {
        super(name);

        
        category = Category.defense;

        if (databaseTabs == null) databaseTabs = new ObjectSet<>();
        databaseTabs.add(SepkerPlanet.sepker);
        buildVisibility = BuildVisibility.shown;

        conductivePower = true;

        health = 8192;
        armor = 32;
        size = 2;
        buildTime = 60f * 8f;
        chanceDeflect = 48;

        shieldHealth = 4096f;
        breakCooldown = 60f * 10f;
        glowColor = Color.valueOf("90EE90");

        consumePower(64f / 60f);

        requirements = ItemStack.with(
            SepkerUraniumItem.uraniumStick, 320,
            Items.phaseFabric, 80,
            Items.beryllium, 64,
            Items.thorium, 64,
            Items.copper, 64,
            Items.lead, 16,
            SepkerUraniumItem.uranium, 320
        );

        buildType = new Prov<Building>() {
            @Override
            public Building get() {
                return new LargeUraniumWallBuild();
            }
        };
    }

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

    public class LargeUraniumWallBuild extends ShieldWallBuild {
    }
}