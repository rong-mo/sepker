package com.sepker.blocks;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.type.Category;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.meta.BuildVisibility;
import arc.struct.ObjectSet;
import arc.func.Prov;
import mindustry.gen.Building;

public class SepkerIndustrialTurbine extends GenericCrafter {

    public SepkerIndustrialTurbine(String name) {
        super(name);

        
        
        category = Category.power;
        buildVisibility = BuildVisibility.shown;

        if (databaseTabs == null) databaseTabs = new ObjectSet<>();
        databaseTabs.add(SepkerPlanet.sepker);

        health = 800;
        size = 3;
        buildTime = 60f * 6f;
        liquidCapacity = 120f;

        // 电力输入：1000/秒
        consumePower(1000f / 60f);

        // 输入：60蒸汽/秒
        consumeLiquid(SepkerSteam.steam, 60f / 60f);

        // 输出：50水/秒
        outputLiquid = new LiquidStack(Liquids.water, 50f / 60f);

        // 生产时间：1秒
        craftTime = 60f;

        // 建造花费
        requirements = ItemStack.with(
            Items.metaglass, 1000,
            SepkerUraniumItem.uranium, 1000,
            Items.copper, 1000
        );

        buildType = new Prov<Building>() {
            @Override
            public Building get() {
                return new IndustrialTurbineBuild();
            }
        };
    }

    public class IndustrialTurbineBuild extends GenericCrafterBuild {
    }
}