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
import mindustry.type.LiquidStack;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.meta.BuildVisibility;
import arc.struct.ObjectSet;

/**
 * 大型冷冻液混合器（0.6.10 新增）
 *
 * 利用铀把钛强行混合进水里，产出冷冻液。
 *
 * 速率口径（原版 GenericCrafter 的真实语义）：
 *   - 物品输入 = 每「次」生产（ConsumeItems 在 trigger 时扣）；
 *   - 液体 / 电力 = 每 tick 连续（ConsumeLiquid / ConsumePower / outputLiquid，
 *                  面板显示时 ×60，即每秒）。
 */
public class SepkerCryofluidMixer {
    public static GenericCrafter cryofluidMixer;

    public static void load() {
        cryofluidMixer = new GenericCrafter("sepker-large-cryofluid-mixer") {{

            health = 1600;
            size = 4;
            buildTime = 60f * 18f;                // 18 秒

            category = Category.crafting;         // 分类：制造
            buildVisibility = BuildVisibility.shown;

            itemCapacity = 20;
            liquidCapacity = 96f;
            outputsLiquid = true;

            // ===== 建造花费 =====
            requirements = ItemStack.with(
                SepkerUraniumItem.uranium, 180,
                Items.silicon, 250,
                Items.titanium, 250,
                Items.lead, 250
            );

            // ===== 输入：每次生产扣 =====
            consumeItems(ItemStack.with(
                SepkerUraniumItem.uranium, 1,
                Items.titanium, 4
            ));

            // ===== 输入：每秒连续 =====
            consumeLiquid(Liquids.water, 48f / 60f);   // 48 水/秒
            consumePower(600f / 60f);                  // 600 电力/秒

            // ===== 输出：每秒连续 =====
            outputLiquid = new LiquidStack(Liquids.cryofluid, 48f / 60f);   // 48 冷冻液/秒

            // ===== 生产时间：1 秒 =====
            craftTime = 60f;
        }};

        // 绑定到赛普克尔星球数据库
        if (cryofluidMixer.databaseTabs == null) cryofluidMixer.databaseTabs = new ObjectSet<>();
        cryofluidMixer.databaseTabs.add(SepkerPlanet.sepker);
        if (cryofluidMixer.shownPlanets == null) cryofluidMixer.shownPlanets = new ObjectSet<>();
        cryofluidMixer.shownPlanets.add(SepkerPlanet.sepker);
    }
}
