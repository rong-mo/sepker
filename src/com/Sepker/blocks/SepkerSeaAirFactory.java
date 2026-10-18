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

public class SepkerSeaAirFactory extends UnitFactory {

    public SepkerSeaAirFactory(String name) {
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
            Items.lead, 50,
            Items.metaglass, 120,
            Items.silicon, 280,
            Items.beryllium, 150,
            SepkerUraniumItem.uranium, 250
        );

        // ================= 原版 空军 T1 =================
plans.add(new UnitPlan(UnitTypes.flare,  60f * 15f,
    ItemStack.with(Items.silicon, 15)));
plans.add(new UnitPlan(UnitTypes.mono,   60f * 15f,
    ItemStack.with(Items.silicon, 30, Items.lead, 15)));

// ================= 原版 海军 T1 =================
plans.add(new UnitPlan(UnitTypes.risso,  60f * 45f,
    ItemStack.with(Items.silicon, 20, Items.metaglass, 35)));
plans.add(new UnitPlan(UnitTypes.retusa, 60f * 35f,
    ItemStack.with(Items.silicon, 15, Items.titanium, 20)));

        // ================= 自定义 =================
        // 局域
        plans.add(new UnitPlan(SepkerSeaAirUnits.juYu, 60f * 40f,
            ItemStack.with(Items.silicon, 100, SepkerUraniumItem.uranium, 40)));
        // 虹伟
        plans.add(new UnitPlan(SepkerSeaAirUnits.hongWei, 60f * 80f,
            ItemStack.with(
                Items.silicon, 50,
                SepkerUraniumItem.uranium, 20,
                Items.beryllium, 40,
                Items.graphite, 10
            )));
        // 长枪
        plans.add(new UnitPlan(SepkerSeaAirUnits.changQiang, 60f * 80f,
            ItemStack.with(
                Items.beryllium, 100,
                SepkerUraniumItem.uranium, 100,
                Items.metaglass, 50,
                Items.silicon, 100
            )));
        // 支护
        plans.add(new UnitPlan(SepkerSeaAirUnits.zhiHu, 60f * 40f,
            ItemStack.with(
                Items.silicon, 150,
                Items.titanium, 150,
                SepkerUraniumItem.uranium, 150,
                Items.beryllium, 100,
                Items.graphite, 50
            )));

        buildType = new Prov<Building>() {
            @Override
            public Building get() {
                return new SepkerSeaAirFactoryBuild();
            }
        };
    }

    public class SepkerSeaAirFactoryBuild extends UnitFactoryBuild {
        @Override
        public void draw() {
            // 底座带旋转
            Draw.rect(region, x, y, rotdeg());

            // 出口带旋转
            Draw.rect(outRegion, x, y, rotdeg());

            // 制造动画
            if (currentPlan != -1) {
                final UnitPlan plan = plans.get(currentPlan);
                Draw.draw(Layer.blockOver, new Runnable() {
                    @Override
                    public void run() {
                        Drawf.construct(SepkerSeaAirFactoryBuild.this, plan.unit,
                            rotdeg() - 90f, progress / plan.time, speedScl, time);
                    }
                });
            }

            Draw.z(Layer.blockOver);

            payRotation = rotdeg();
            drawPayload();

            Draw.z(Layer.blockOver + 0.1f);

            // 顶盖带旋转
            Draw.rect(topRegion, x, y, rotdeg());
        }
    }
}