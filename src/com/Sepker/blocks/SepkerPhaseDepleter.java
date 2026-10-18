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
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.meta.BuildVisibility;
import arc.struct.ObjectSet;

/**
 * 相织脱贫器（0.6.10 新增）
 *
 * 用铀快速生产相织布，同时产出贫铀。
 *
 * 速率口径（照原版消费者的真实语义）：
 *   - 物品输入 / 输出 = 每「次」生产（ConsumeItems 在 trigger 时扣）；
 *   - 液体 / 电力     = 每 tick 连续扣（ConsumeLiquid / ConsumePower，
 *                        面板显示时 ×60，即每秒）。
 */
public class SepkerPhaseDepleter {
    public static GenericCrafter phaseDepleter;

    public static void load() {
        phaseDepleter = new GenericCrafter("sepker-phase-depleter") {{

            health = 500;
            size = 4;
            buildTime = 60f * 6f;                 // 6 秒

            category = Category.crafting;         // 分类：制造
            buildVisibility = BuildVisibility.shown;

            itemCapacity = 150;

            // ===== 建造花费 =====
            requirements = ItemStack.with(
                SepkerUraniumItem.uranium, 1000,   // 1K 铀
                Items.thorium, 500,                // 500 钍
                Items.phaseFabric, 250,            // 250 相织布
                Items.copper, 400,                 // 400 铜
                Items.lead, 150                    // 150 铅
            );

            // ===== 输入：每次生产扣 =====
            consumeItems(ItemStack.with(
                SepkerUraniumItem.uranium, 10,
                Items.sand, 20
            ));

            // ===== 输入：每秒连续 =====
            consumeLiquid(Liquids.water, 6f / 60f);   // 6 水/秒
            consumePower(1250f / 60f);                // 1250 电力/秒

            // ===== 输出：每次生产出 =====
            outputItems = new ItemStack[]{
                new ItemStack(Items.phaseFabric, 20),                    // 相织布 ×20
                new ItemStack(SepkerUraniumItem.depletedUranium, 5),     // 贫铀 ×5
                new ItemStack(Items.scrap, 5)                            // 废料 ×5
            };

            // ===== 生产时间：一次 5 秒 =====
            craftTime = 300f;
        }};

        // 绑定到赛普克尔星球数据库
        if (phaseDepleter.databaseTabs == null) phaseDepleter.databaseTabs = new ObjectSet<>();
        phaseDepleter.databaseTabs.add(SepkerPlanet.sepker);
        if (phaseDepleter.shownPlanets == null) phaseDepleter.shownPlanets = new ObjectSet<>();
        phaseDepleter.shownPlanets.add(SepkerPlanet.sepker);
    }
}
