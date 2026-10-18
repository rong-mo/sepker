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
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.production.GenericCrafter.GenericCrafterBuild;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawGlowRegion;
import mindustry.world.draw.DrawMulti;
import arc.struct.ObjectSet;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.world.Block;

public class SepkerUraniumFactory {
    public static GenericCrafter uraniumFactory;

    public static void load() {
        uraniumFactory = new GenericCrafter("uranium-purification-factory") {{
            
            health = 400;
            size = 3;
            buildTime = 60f * 4f;

            category = Category.crafting;
            buildVisibility = BuildVisibility.shown;

            requirements = ItemStack.with(
                SepkerUraniumItem.uranium, 1000,
                Items.thorium, 350,
                Items.copper, 150,
                Items.lead, 100
            );

            consumeItems(ItemStack.with(
                SepkerUraniumItem.uranium, 5,
                Items.graphite, 5,
                Items.tungsten, 1
            ));

            consumePower(1000f / 60f);

            outputItem = new ItemStack(SepkerUraniumItem.uraniumStick, 1);

            craftTime = 90f;

            drawer = new DrawMulti(
                new DrawDefault(),
                new DrawGlowRegion() {{
                    color = Color.valueOf("90EE90");
                    suffix = "-glow";
                    alpha = 0.8f;
                }},
                new UraniumAnimDrawer()
            );
        }};

        if (uraniumFactory.databaseTabs == null) uraniumFactory.databaseTabs = new ObjectSet<>();
        uraniumFactory.databaseTabs.add(SepkerPlanet.sepker);
        if (uraniumFactory.shownPlanets == null) uraniumFactory.shownPlanets = new ObjectSet<>();
        uraniumFactory.shownPlanets.add(SepkerPlanet.sepker);
    }

    public static class UraniumAnimDrawer extends DrawBlock {
        public TextureRegion[] regions;

        @Override
        public void load(Block block) {
            regions = new TextureRegion[5];
            regions[0] = Core.atlas.find("uranium-purification-factory-frame1");
            regions[1] = Core.atlas.find("uranium-purification-factory-frame2");
            regions[2] = Core.atlas.find("uranium-purification-factory-frame3");
            regions[3] = Core.atlas.find("uranium-purification-factory-frame4");
            regions[4] = Core.atlas.find("uranium-purification-factory-frame5");
        }

        @Override
        public void draw(Building build) {
            if (regions == null) return;

            // ⭐ 用 efficiency 判断是否真正在运转，而不是 warmup
            float eff = 0f;
            if (build instanceof GenericCrafterBuild) {
                eff = ((GenericCrafterBuild) build).efficiency;
            }
            boolean running = eff > 0.001f;

            int idx;
            if (running) {
                // 运转时循环播放第 2~5 帧（索引 1~4），每帧 0.1 秒
                idx = (int)(Time.time / 6f) % 4 + 1;
            } else {
                // 停用时只显示第 1 帧（索引 0）
                idx = 0;
            }

            TextureRegion r = regions[idx];
            if (r != null && r.found()) {
                Draw.rect(r, build.x, build.y);
            }
        }
    }
}