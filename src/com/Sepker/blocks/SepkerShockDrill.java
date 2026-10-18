package com.sepker.blocks;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.production.Drill.DrillBuild;
import mindustry.world.meta.BuildVisibility;
import mindustry.gen.Building;
import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.Time;
import arc.struct.ObjectSet;
import arc.func.Prov;

public class SepkerShockDrill {
    public static Drill shockDrill;

    public static void load() {
        shockDrill = new Drill("shock-drill") {{
            
            health = 800;
            size = 5;
            buildTime = 60f * 12f;
            itemCapacity = 10000;
            drillTime = 5f;
            tier = 10000;
            category = Category.production;
            buildVisibility = BuildVisibility.shown;

            // ⭐ int 类型，不是 float
            dumpTime = 1;

            liquidBoostIntensity = 1.8708f;
            consumeLiquid(Liquids.water, 0.1f);
            consumeLiquid(Liquids.cryofluid, 0.1f).boost();

            requirements = ItemStack.with(
                Items.copper, 1000,
                Items.lead, 1000,
                Items.carbide, 500,
                Items.thorium, 1000,
                Items.titanium, 1000,
                Items.surgeAlloy, 500,
                Items.phaseFabric, 500
            );

            buildType = new Prov<Building>() {
                @Override
                public Building get() {
                    return new DrillBuild() {
                        public TextureRegion rotatorGlow;
                        public float glowRotation = 0f;

                        @Override
                        public void updateTile() {
                            super.updateTile();

                            // ⭐ 额外强制 dump：每 tick 最多 20 个
                            if (items.total() > 0) {
                                for (int i = 0; i < 20; i++) {
                                    if (items.total() == 0) break;
                                    dump();
                                }
                            }
                        }

                        @Override
                        public void draw() {
                            super.draw();

                            if (rotatorGlow == null) {
                                rotatorGlow = Core.atlas.find("shock-drill-rotator-glow");
                            }

                            if (rotatorGlow != null && rotatorGlow.found() && warmup() > 0.001f) {
                                glowRotation += 2f * warmup() * Time.delta;
                                Draw.alpha(warmup());
                                Draw.rect(rotatorGlow, x, y, glowRotation);
                                Draw.alpha(1f);
                            }
                        }
                    };
                }
            };
        }};

        if (shockDrill.databaseTabs == null) shockDrill.databaseTabs = new ObjectSet<>();
        shockDrill.databaseTabs.add(SepkerPlanet.sepker);
        if (shockDrill.shownPlanets == null) shockDrill.shownPlanets = new ObjectSet<>();
        shockDrill.shownPlanets.add(SepkerPlanet.sepker);
    }
}