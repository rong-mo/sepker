package com.sepker.blocks;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.content.Items;
import mindustry.world.meta.BuildVisibility;
import arc.struct.ObjectSet;

public class SepkerXuAndMo {
    public static CoreBlock xuAndMo;
    
    public static void load() {
        xuAndMo = new CoreBlock("sepker-xu-and-mo") {{
            
            health = 75000; 
            size = 8;
            itemCapacity = 80000;
            buildTime = 300f * 60f;
            
            requirements = ItemStack.with(
                Items.copper, 13000,
                Items.lead, 13000,
                Items.silicon, 13000,
                Items.thorium, 13000,
                Items.titanium, 13000,
                Items.plastanium, 4000,
                Items.beryllium, 4000,
                Items.tungsten, 4000,
                Items.surgeAlloy, 4000
            );
            
            category = Category.effect;
            solid = true;

            // 1. 强制设置可见性
            buildVisibility = BuildVisibility.shown;

            // 2. 强制加入核心数据库的“赛普克尔”标签
            if (databaseTabs == null) databaseTabs = new ObjectSet<>();
            databaseTabs.add(SepkerPlanet.sepker);

            unitType = SepkerXuJun.xuJun;
            unitCapModifier = 50;
        }};
    }
}
