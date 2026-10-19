package com.sepker.content;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import arc.graphics.Color;
import arc.func.Prov;
import mindustry.Vars;
import mindustry.content.Planets;
import mindustry.graphics.g3d.GenericMesh;
import mindustry.graphics.g3d.HexMesh;
import mindustry.type.Planet;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.type.SectorPreset;
import arc.struct.ObjectSet;

public class SepkerPlanet {
    public static Planet sepker;

    public static void load() {
        // 第 4 个参数是网格细分等级：0=12 格、1=32、2=92、3=272、4=812。
        // 用 3 是为了让扇区排得密一些（相邻格间距比 2 小约 1.7 倍），便于做连贯的战线。
        sepker = new Planet("sepker", Planets.sun, 1f, 3) {{

            visible = true;
            accessible = true;
            alwaysUnlocked = true;
            orbitRadius = 80f;
            orbitTime = 600f;
            rotateTime = 180f;
            hasAtmosphere = true;
            atmosphereColor = Color.valueOf("14E8B2");
            atmosphereRadIn = 0.02f;
            atmosphereRadOut = 0.3f;
            bloom = true;
            iconColor = Color.valueOf("14E8B2");

            meshLoader = new Prov<GenericMesh>() {
                @Override
                public GenericMesh get() {
                    return new HexMesh(sepker, 8);
                }
            };

            // ⭐ 起始扇区 = 主线第一关所在的格子（必须与 SepkerSectors.main1 的索引一致）
            startSector = SepkerSectors.MAIN1_SECTOR;
            generator = new SepkerPlanetGenerator();
            allowLaunchToNumbered = false;

            // ⭐ 战役规则面板（原版默认为 isVanilla()，mod 星球是 false）
            allowCampaignRules = true;
            // 让"敌方入侵区块"开关有意义
            allowSectorInvasion = true;
            // 让"RTS 进攻 AI"开关有意义
            showRtsAIRule = true;
            // 默认开启"重置丢失区块"（planet.clearSectorOnLose 保持 false 才能显示该开关）
            campaignRules.clearSectorOnLose = true;

            // ⭐ 和原版 Serpulo 一样（Planets.java:138/140）：发射时可选择核心蓝图、
            //    可携带并编辑资源。两者默认都是 false（Planet.java:115/117），
            //    不开的话发射界面只会显示一个只有「返回/发射」的简版。
            allowLaunchSchematics = true;
            allowLaunchLoadout = true;
        }};
    }

    // 强制将所有内容绑定到赛普克尔星球的数据库、显示星球、物品池
    public static void bindAllContentToSepker() {
        if (sepker == null) return;


        for (Item item : Vars.content.items()) {
            if (item.shownPlanets == null) item.shownPlanets = new ObjectSet<>();
            item.shownPlanets.add(sepker);
            if (item.databaseTabs == null) item.databaseTabs = new ObjectSet<>();
            item.databaseTabs.add(sepker);
        }

        // 3. 液体
        for (Liquid liquid : Vars.content.liquids()) {
            if (liquid.shownPlanets == null) liquid.shownPlanets = new ObjectSet<>();
            liquid.shownPlanets.add(sepker);
            if (liquid.databaseTabs == null) liquid.databaseTabs = new ObjectSet<>();
            liquid.databaseTabs.add(sepker);
        }

        // 4. 方块
        for (Block block : Vars.content.blocks()) {
            if (block.shownPlanets == null) block.shownPlanets = new ObjectSet<>();
            block.shownPlanets.add(sepker);
            if (block.databaseTabs == null) block.databaseTabs = new ObjectSet<>();
            block.databaseTabs.add(sepker);
        }

        // 5. 单位
        for (mindustry.type.UnitType unit : Vars.content.units()) {
            if (unit.shownPlanets == null) unit.shownPlanets = new ObjectSet<>();
            unit.shownPlanets.add(sepker);
            if (unit.databaseTabs == null) unit.databaseTabs = new ObjectSet<>();
            unit.databaseTabs.add(sepker);
        }

        // 6. 扇区
        for (SectorPreset sector : Vars.content.sectors()) {
            if (sector.shownPlanets == null) sector.shownPlanets = new ObjectSet<>();
            sector.shownPlanets.add(sepker);
            if (sector.databaseTabs == null) sector.databaseTabs = new ObjectSet<>();
            sector.databaseTabs.add(sepker);
        }
    }
}