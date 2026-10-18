package com.sepker.blocks;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.content.*;
import mindustry.world.meta.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.type.Category;
import mindustry.entities.bullet.*;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import arc.graphics.*;
import arc.struct.ObjectSet; // 确保导入了这个

public class SepkerBlueGlow extends ItemTurret {

    public SepkerBlueGlow(String name) {
        super(name);

        category = Category.turret; 
        

        // 1. 强制设置可见性（确保在建筑菜单里能显示，而不是隐藏）
        buildVisibility = BuildVisibility.shown;

        // 2. 强制加入核心数据库的“赛普克尔”标签
        if (databaseTabs == null) databaseTabs = new ObjectSet<>();
        databaseTabs.add(SepkerPlanet.sepker);

        // 其他属性保持不变
        health = 1200;
        size = 2;
        buildTime = 60f;
        liquidCapacity = 60f;
        range = 20f * 8f;
        reload = 10f;              
        requirements = ItemStack.with(
        Items.titanium, 200,
        Items.lead, 150,
        Items.silicon, 100,
        Items.graphite, 150,
        Items.beryllium, 50);
        

        ammo(Items.titanium, new ContinuousLaserBulletType(){{
            damage = 200;
            buildingDamageMultiplier = 0.02f;
            pierce = true;
            pierceCap = -1;
            length = 20f * 8f;
            status = StatusEffects.shocked;
            statusDuration = 60f;
            ammoMultiplier = 1f;
            
            colors = new Color[]{
                Color.valueOf("87CEFA"),
                Color.valueOf("E0F4FF"),
                Color.valueOf("4A9BD9")
            };
            width = 3f;
        }});

        consumeCoolant(0.1f);

        buildType = new arc.func.Prov<mindustry.gen.Building>() {
            @Override
            public mindustry.gen.Building get() {
                return new SepkerBlueGlowBuild();
            }
        };
    }

    public class SepkerBlueGlowBuild extends ItemTurretBuild {
    }
}