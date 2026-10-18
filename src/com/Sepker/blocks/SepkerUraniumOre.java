package com.sepker.blocks;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.world.blocks.environment.OreBlock;
import mindustry.world.Tile;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.game.EventType.WorldLoadEvent;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Timer;
import arc.Events;
import arc.func.Cons;

public class SepkerUraniumOre {
    public static OreBlock uraniumOre;
    static final Seq<Tile> uraniumTiles = new Seq<>();

    public static void load() {
        SepkerRadiationEffect.load();

        // ⭐ 修正：不再传入 uranium 物品，让它直接使用自己的贴图 sepker-uranium-ore.png
        uraniumOre = new OreBlock("sepker-uranium-ore") {{
            
            itemDrop = SepkerUraniumItem.uranium; // 设置掉落物为铀
        }};

        // 绑定到赛普克尔星球数据库
        if (uraniumOre.databaseTabs == null) uraniumOre.databaseTabs = new ObjectSet<>();
        uraniumOre.databaseTabs.add(SepkerPlanet.sepker);
        if (uraniumOre.shownPlanets == null) uraniumOre.shownPlanets = new ObjectSet<>();
        uraniumOre.shownPlanets.add(SepkerPlanet.sepker);

        // 世界加载时扫描所有铀矿图块
        Events.on(WorldLoadEvent.class, new Cons<WorldLoadEvent>() {
            @Override
            public void get(WorldLoadEvent event) {
                uraniumTiles.clear();
                Vars.world.tiles.eachTile(new Cons<Tile>() {
                    @Override
                    public void get(Tile tile) {
                        if (tile.floor() == uraniumOre) {
                            uraniumTiles.add(tile);
                        }
                    }
                });
            }
        });

        // 每秒对铀矿上的建筑造成 1 点伤害
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                for (Tile tile : uraniumTiles) {
                    Building build = tile.build;
                    if (build != null && build.isValid()) {
                        if (build.block.name.equals("shock-drill")) continue;
                        build.damage(1f);
                    }
                }
            }
        }, 0f, 1f);
    }
}