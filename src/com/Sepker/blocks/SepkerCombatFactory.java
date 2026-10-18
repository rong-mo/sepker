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
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.meta.BuildVisibility;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import arc.graphics.g2d.Draw;
import arc.struct.ObjectSet;
import arc.func.Prov;

public class SepkerCombatFactory extends UnitFactory {

    public SepkerCombatFactory(String name) {
        super(name);

        
        category = Category.units;
        buildVisibility = BuildVisibility.shown;

        if (databaseTabs == null) databaseTabs = new ObjectSet<>();
        databaseTabs.add(SepkerPlanet.sepker);

        rotate = true;

        health = 800;
        size = 4;
        buildTime = 60f * 8f;
        itemCapacity = 300;

        consumePower(150f / 60f);

        requirements = ItemStack.with(
            Items.copper, 50,
            Items.lead, 120,
            Items.silicon, 280,
            Items.beryllium, 150,
            SepkerUraniumItem.uranium, 250
        );

        // ================= 原版 T1 =================
        plans.add(new UnitPlan(UnitTypes.dagger, 60f * 15f,
            ItemStack.with(Items.silicon, 10, Items.lead, 10)));
        plans.add(new UnitPlan(UnitTypes.crawler, 60f * 10f,
            ItemStack.with(Items.silicon, 8, Items.coal, 10)));
        plans.add(new UnitPlan(UnitTypes.nova, 60f * 40f,
            ItemStack.with(Items.silicon, 30, Items.lead, 20, Items.titanium, 20)));
        plans.add(new UnitPlan(UnitTypes.stell, 60f * 20f,
            ItemStack.with(Items.beryllium, 50, Items.silicon, 70)));
        plans.add(new UnitPlan(UnitTypes.merui, 60f * 20f,
            ItemStack.with(Items.beryllium, 40, Items.silicon, 50)));
        plans.add(new UnitPlan(UnitTypes.elude, 60f * 30f,
            ItemStack.with(Items.silicon, 60, Items.graphite, 50)));

        // ================= 自定义 T1 =================
        plans.add(new UnitPlan(SepkerUnits.qingSe, 60f * 40f, ItemStack.with(
            Items.beryllium, 40,
            Items.lead, 20,
            Items.titanium, 20,
            Items.silicon, 90
        )));
        plans.add(new UnitPlan(SepkerUnits.rongGuang, 60f * 60f, ItemStack.with(
            Items.silicon, 150,
            Items.beryllium, 100,
            Items.lead, 50,
            Items.titanium, 40
        )));

        buildType = new Prov<Building>() {
            @Override
            public Building get() {
                return new SepkerCombatFactoryBuild();
            }
        };
    }

    public class SepkerCombatFactoryBuild extends UnitFactoryBuild {
        @Override
        public void draw() {
            // ⭐ 底座带旋转（原版不带，我们加上）
            Draw.rect(region, x, y, rotdeg());

            // 出口带旋转（原版自带的）
            Draw.rect(outRegion, x, y, rotdeg());

            // ⭐ 制造动画：用匿名 Runnable 避免 Android lambda 崩溃
            if (currentPlan != -1) {
                final UnitPlan plan = plans.get(currentPlan);
                Draw.draw(Layer.blockOver, new Runnable() {
                    @Override
                    public void run() {
                        Drawf.construct(SepkerCombatFactoryBuild.this, plan.unit,
                            rotdeg() - 90f, progress / plan.time, speedScl, time);
                    }
                });
            }

            Draw.z(Layer.blockOver);

            payRotation = rotdeg();
            drawPayload();

            Draw.z(Layer.blockOver + 0.1f);

            // ⭐ 顶盖带旋转
            Draw.rect(topRegion, x, y, rotdeg());
        }
    }
}