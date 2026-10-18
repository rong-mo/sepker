package com.sepker.content;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.content.TechTree;
import mindustry.content.Items;
import mindustry.content.Blocks;
import mindustry.type.ItemStack;
import mindustry.game.Objectives;
import arc.struct.Seq;
import arc.struct.ObjectSet;

public class SepkerTechTree {
    public static SepkerBlueGlow blueGlow;
    public static SepkerStorm storm;

    public static void load() {
        // 1. 加载独立铀体系
        SepkerUranium.load();

        // 2. 初始化
        blueGlow = new SepkerBlueGlow("sepker-blue-glow");
        storm = new SepkerStorm("sepker-storm");

        // ⭐ root content = 星球本身（SepkerPlanet.sepker 已 alwaysUnlocked = true）
        TechTree.TechNode root = TechTree.nodeRoot("sepker-combined", SepkerPlanet.sepker, new Runnable() {
            @Override
            public void run() {

                // ==================== 子节点 1：许与墨核心 ====================
                TechTree.node(SepkerXuAndMo.xuAndMo,
                    ItemStack.with(
                        Items.copper, 26000,
                        Items.lead, 26000,
                        Items.silicon, 26000,
                        Items.thorium, 26000,
                        Items.titanium, 26000,
                        Items.plastanium, 8000,
                        Items.beryllium, 8000,
                        Items.tungsten, 8000,
                        Items.surgeAlloy, 8000
                    ),
                    Seq.with(),
                    new Runnable() {
                        @Override
                        public void run() {

                            // ===== 许与墨的子节点：防辐震爆钻头 =====
                            TechTree.node(SepkerShockDrill.shockDrill,
                                ItemStack.with(
                                    Items.copper, 5000,
                                    Items.lead, 5000,
                                    Items.silicon, 5000,
                                    Items.carbide, 3000,
                                    Items.thorium, 3000,
                                    Items.titanium, 3000,
                                    Items.surgeAlloy, 3000,
                                    Items.phaseFabric, 3000
                                ),
                                Seq.with(new Objectives.SectorComplete(SepkerSectors.main2)),
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        // 铀提纯工厂
                                        TechTree.node(SepkerUraniumFactory.uraniumFactory,
                                            ItemStack.with(
                                                SepkerUraniumItem.uranium, 9000,
                                                Items.thorium, 3150,
                                                Items.copper, 1350,
                                                Items.lead, 900
                                            ),
                                            Seq.with(),
                                            new Runnable() { @Override public void run() { } }
                                        );

                                        // ⭐ 相织脱贫器（0.6.10）
                                        //    挂载在铀提纯工厂后；研究前置：占领 main4（遥远的桥）
                                        //    研究花费 = 建造花费 × 5
                                        TechTree.node(SepkerPhaseDepleter.phaseDepleter,
                                            ItemStack.with(
                                                SepkerUraniumItem.uranium, 5000,
                                                Items.thorium, 2500,
                                                Items.phaseFabric, 1250,
                                                Items.copper, 2000,
                                                Items.lead, 750
                                            ),
                                            Seq.with(new Objectives.SectorComplete(SepkerSectors.main4)),
                                            new Runnable() { @Override public void run() { } }
                                        );

                                        // 核反应堆
                                        TechTree.node(SepkerUranium.nuclearReactor,
                                            ItemStack.with(
                                                SepkerUraniumItem.uranium, 2000,
                                                Items.copper, 10000,
                                                Items.titanium, 10000,
                                                Items.thorium, 10000,
                                                Items.silicon, 10000,
                                                Items.carbide, 7000,
                                                Items.oxide, 7000,
                                                Items.metaglass, 10000,
                                                Items.beryllium, 10000,
                                                Items.surgeAlloy, 7000
                                            ),
                                            Seq.with(),
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    TechTree.node(SepkerUranium.industrialTurbine,
                                                        ItemStack.with(
                                                            Items.metaglass, 4000,
                                                            SepkerUraniumItem.uranium, 4000,
                                                            Items.copper, 4000
                                                        ),
                                                        Seq.with(),
                                                        new Runnable() { @Override public void run() { } }
                                                    );
                                                }
                                            }
                                        );

                                        // 亚空间运输传送带
                                        TechTree.node(SepkerUranium.warpConveyor,
                                            ItemStack.with(
                                                SepkerUraniumItem.uranium, 12000,
                                                SepkerUraniumItem.uraniumStick, 9000,
                                                Items.lead, 6000,
                                                Items.graphite, 6000,
                                                Items.silicon, 9000,
                                                Items.phaseFabric, 9000,
                                                Items.surgeAlloy, 6000,
                                                Items.tungsten, 6000
                                            ),
                                            Seq.with(),
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    // ⭐ 大型冷冻液混合器（0.6.10）
                                                    //    挂在工业涡轮之后；前置：着陆 main4
                                                    //    研究花费 = 建造花费 × 4
                                                    TechTree.node(SepkerCryofluidMixer.cryofluidMixer,
                                                        ItemStack.with(
                                                            SepkerUraniumItem.uranium, 720,
                                                            Items.silicon, 1000,
                                                            Items.titanium, 1000,
                                                            Items.lead, 1000
                                                        ),
                                                        Seq.with(new Objectives.OnSector(SepkerSectors.main4)),
                                                        new Runnable() { @Override public void run() { } }
                                                    );
                                                }
                                            }
                                        );
                                    }
                                }
                            );

                            // ===== 许与墨的子节点：铀（发现即解锁）=====
TechTree.nodeProduce(SepkerUraniumItem.uranium, new Runnable() {
    @Override
    public void run() {
        // 铀棒（铀的子节点，发现铀棒即解锁）
        TechTree.nodeProduce(SepkerUraniumItem.uraniumStick, new Runnable() {
            @Override
            public void run() { }
        });
    }
});
                                

                            // ===== 许与墨的子节点：综合陆军工厂 =====
                            TechTree.node(SepkerUranium.combatFactory,
                                ItemStack.with(
                                    Items.copper, 250,
                                    Items.lead, 600,
                                    Items.silicon, 1400,
                                    Items.beryllium, 750,
                                    SepkerUraniumItem.uranium, 1250
                                ),
                                Seq.with(),
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        TechTree.node(SepkerUnits.qingSe,
                                            ItemStack.with(
                                                Items.beryllium, 200,
                                                Items.lead, 100,
                                                Items.titanium, 100,
                                                Items.silicon, 450
                                            ),
                                            Seq.with(),
                                            new Runnable() { @Override public void run() { } }
                                        );

                                        TechTree.node(SepkerUnits.rongGuang,
                                            ItemStack.with(
                                                Items.silicon, 750,
                                                Items.beryllium, 500,
                                                Items.lead, 250,
                                                Items.titanium, 200
                                            ),
                                            Seq.with(),
                                            new Runnable() { @Override public void run() { } }
                                        );

                                        // 综合海空工厂
                                        TechTree.node(SepkerUranium.seaAirFactory,
                                            ItemStack.with(
                                                Items.lead, 250,
                                                Items.metaglass, 600,
                                                Items.silicon, 1400,
                                                Items.beryllium, 750,
                                                SepkerUraniumItem.uranium, 1250
                                            ),
                                            Seq.with(),
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    // 小型机场
                                                    TechTree.node(SepkerUranium.smallAirport,
                                                        ItemStack.with(
                                                            Items.copper, 600,
                                                            Items.lead, 600,
                                                            Items.silicon, 1200,
                                                            Items.metaglass, 600
                                                        ),
                                                        Seq.with(),
                                                        new Runnable() { @Override public void run() { } }
                                                    );
                                                    // ⭐ 联合单位重构厂
TechTree.node(SepkerUranium.combinedReconstructor,
    ItemStack.with(
        SepkerUraniumItem.uranium, 2000,
        Items.silicon, 2800,
        Items.graphite, 800,
        Items.beryllium, 4000,
        Items.tungsten, 400
    ),
    Seq.with(new Objectives.SectorComplete(SepkerSectors.main4)),
    new Runnable() { @Override public void run() { } }
);

                                                    // 局域
                                                    TechTree.node(SepkerSeaAirUnits.juYu,
                                                        ItemStack.with(
                                                            Items.silicon, 4000,
                                                            SepkerUraniumItem.uranium, 1600
                                                        ),
                                                        Seq.with(),
                                                        new Runnable() { @Override public void run() { } }
                                                    );

                                                    // 虹伟
                                                    TechTree.node(SepkerSeaAirUnits.hongWei,
                                                        ItemStack.with(
                                                            Items.silicon, 2000,
                                                            SepkerUraniumItem.uranium, 800,
                                                            Items.beryllium, 1600,
                                                            Items.graphite, 400
                                                        ),
                                                        Seq.with(),
                                                        new Runnable() { @Override public void run() { } }
                                                    );

                                                    // 长枪
                                                    TechTree.node(SepkerSeaAirUnits.changQiang,
                                                        ItemStack.with(
                                                            Items.beryllium, 2000,
                                                            SepkerUraniumItem.uranium, 2000,
                                                            Items.metaglass, 1000,
                                                            Items.silicon, 2000
                                                        ),
                                                        Seq.with(),
                                                        new Runnable() { @Override public void run() { } }
                                                    );

                                                    // 支护
                                                    TechTree.node(SepkerSeaAirUnits.zhiHu,
                                                        ItemStack.with(
                                                            Items.silicon, 3000,
                                                            Items.titanium, 3000,
                                                            SepkerUraniumItem.uranium, 3000,
                                                            Items.beryllium, 2000,
                                                            Items.graphite, 1000
                                                        ),
                                                        Seq.with(),
                                                        new Runnable() { @Override public void run() { } }
                                                    );
                                                }
                                            }
                                        );
                                    }
                                }
                            );

                            // ===== 许与墨的子节点：蓝辉 =====
                            TechTree.node(blueGlow,
                                ItemStack.with(
                                    Items.titanium, 2000,
                                    Items.lead, 1500,
                                    Items.silicon, 1000,
                                    Items.graphite, 1500,
                                    Items.beryllium, 500
                                ),
                                Seq.with(),
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        // 铀墙
                                        TechTree.node(SepkerUranium.uraniumWall,
                                            ItemStack.with(
                                                SepkerUraniumItem.uranium, 4000,
                                                SepkerUraniumItem.uraniumStick, 4000,
                                                Items.phaseFabric, 1000
                                            ),
                                            Seq.with(
                                                new Objectives.Research(Blocks.shieldedWall),
                                                new Objectives.Research(SepkerUraniumItem.uranium)
                                            ),
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    TechTree.node(SepkerUranium.largeUraniumWall,
                                                        ItemStack.with(
                                                            SepkerUraniumItem.uranium, 8000,
                                                            SepkerUraniumItem.uraniumStick, 8000,
                                                            Items.phaseFabric, 2000
                                                        ),
                                                        Seq.with(),
                                                        new Runnable() { @Override public void run() { } }
                                                    );
                                                }
                                            }
                                        );

                                        // 风暴
                                        TechTree.node(storm,
                                            ItemStack.with(
                                                Items.copper, 3000,
                                                Items.thorium, 6000,
                                                Items.silicon, 6000,
                                                Items.titanium, 6000,
                                                Items.surgeAlloy, 9000,
                                                Items.tungsten, 3000
                                            ),
                                            Seq.with(new Objectives.SectorComplete(SepkerSectors.main2)),
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    TechTree.node(SepkerUranium.missileLauncher,
                                                        ItemStack.with(
                                                            Items.copper, 10000,
                                                            Items.graphite, 6000,
                                                            Items.silicon, 14000,
                                                            SepkerUraniumItem.uranium, 7200,
                                                            Items.tungsten, 12000,
                                                            Items.oxide, 4000,
                                                            Items.carbide, 2000,
                                                            SepkerUraniumItem.uraniumStick, 800
                                                        ),
                                                        Seq.with(new Objectives.SectorComplete(SepkerSectors.main4)),
                                                        new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                TechTree.node(SepkerUranium.antiAirLauncher,
    ItemStack.with(
        Items.copper, 5000,
        SepkerUraniumItem.uranium, 10000,
        Items.silicon, 5000,
        Items.lead, 5000,
        Items.titanium, 5000,
        Items.graphite, 5000,
        Items.thorium, 5000
    ),
    Seq.with(),
    new Runnable() {
        @Override
        public void run() {
            // ⭐ Ⅰ型磁轨炮
            // Ⅰ型磁轨炮
TechTree.node(SepkerUranium.emRailgun,       // ⭐ 实例，不是类
    ItemStack.with(
        Items.graphite, 45000,
        Items.silicon, 30000,
        Items.copper, 30000,
        SepkerUraniumItem.uranium, 30000,
        SepkerUraniumItem.uraniumStick, 30000,
        Items.carbide, 30000,
        Items.surgeAlloy, 30000,
        Items.oxide, 15000
    ),
    Seq.with(new Objectives.SectorComplete(SepkerSectors.main4)),
    new Runnable() { @Override public void run() {
    new Runnable() {
    @Override
    public void run() {
        // ⭐ Ⅱ型磁轨炮（研究花费 = 建造花费 × 20）
        TechTree.node(SepkerUranium.emRailgun2,
            ItemStack.with(
                Items.graphite, 60000,
                Items.silicon, 40000,
                Items.copper, 40000,
                SepkerUraniumItem.uranium, 40000,
                SepkerUraniumItem.uraniumStick, 40000,
                Items.carbide, 40000,
                Items.surgeAlloy, 40000,
                Items.oxide, 20000
            ),
            Seq.with(new Objectives.SectorComplete(SepkerSectors.main4)),
            new Runnable() { @Override public void run() { } }
                    );
                    }
                    };
                }
            }
        );
        }
    }
);
                                                            }
                                                        }
                                                    );
                                                }
                                            }
                                        );
                                    }
                                }
                            );
                        }
                    }
                );

                // ==================== 子节点 2：扇区（与许与墨并列） ====================
                TechTree.node(SepkerSectors.main1,
                    Seq.with(
                        new Objectives.SectorComplete(mindustry.content.SectorPresets.origin),
                        new Objectives.SectorComplete(mindustry.content.SectorPresets.planetaryTerminal)
                    ),
                    new Runnable() {
                        @Override
                        public void run() {
                            TechTree.node(SepkerSectors.main2,
                                Seq.with(new Objectives.SectorComplete(SepkerSectors.main1)),
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        TechTree.node(SepkerSectors.main3,
                                            Seq.with(new Objectives.SectorComplete(SepkerSectors.main2)),
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    TechTree.node(SepkerSectors.main4,
                                                        Seq.with(new Objectives.SectorComplete(SepkerSectors.main3)),
                                                        new Runnable() {
                                                            @Override public void run() { }
                                                        }
                                                    );
                                                }
                                            }
                                        );
                                    }
                                }
                            );
                        }
                    }
                );
            }   // ← 闭合 root 的 run() 方法
        });     // ← 闭合 root 的 Runnable 和 TechTree.nodeRoot() 调用

        // ================= 科技树全局配置 =================
        SepkerPlanet.sepker.techTree = root;

        // 蓝辉 / 风暴 databaseTabs
        if (blueGlow.databaseTabs == null) blueGlow.databaseTabs = new ObjectSet<>();
        blueGlow.databaseTabs.add(SepkerPlanet.sepker);
        if (blueGlow.shownPlanets == null) blueGlow.shownPlanets = new ObjectSet<>();
        blueGlow.shownPlanets.add(SepkerPlanet.sepker);

        if (storm.databaseTabs == null) storm.databaseTabs = new ObjectSet<>();
        storm.databaseTabs.add(SepkerPlanet.sepker);
        if (storm.shownPlanets == null) storm.shownPlanets = new ObjectSet<>();
        storm.shownPlanets.add(SepkerPlanet.sepker);

        // 扇区 databaseTabs
        forceAddToDatabase(SepkerSectors.main1);
        forceAddToDatabase(SepkerSectors.main2);
        forceAddToDatabase(SepkerSectors.main3);
        forceAddToDatabase(SepkerSectors.main4);
    }

    private static void forceAddToDatabase(mindustry.type.SectorPreset sector) {
        if (sector == null) return;
        if (sector.databaseTabs == null) sector.databaseTabs = new ObjectSet<>();
        sector.databaseTabs.add(SepkerPlanet.sepker);
        if (sector.shownPlanets == null) sector.shownPlanets = new ObjectSet<>();
        sector.shownPlanets.add(SepkerPlanet.sepker);
    }
}