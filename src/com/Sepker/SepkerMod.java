package com.sepker;

import com.sepker.blocks.*;
import com.sepker.logic.*;
import com.sepker.content.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import mindustry.mod.Mod;
import mindustry.Vars;
import mindustry.content.Planets;
import mindustry.content.SerpuloTechTree;
import mindustry.content.ErekirTechTree;
import mindustry.content.TechTree;
import mindustry.type.Sector;
import mindustry.game.EventType.ContentInitEvent;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Difficulty;
import mindustry.game.Rules;
import mindustry.type.Planet;
import com.sepker.ui.SepkerCampaignRules;
import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.util.Log;
import arc.util.Timer;
import com.sepker.ui.SepkerStartupUI; // ⭐ 导入启动UI

public class SepkerMod extends Mod {

    @Override
    public void loadContent() {
        try {
            // ⭐ 清理上次运行留下的坏 key（防止每次启动都报错）
            try {
                arc.Core.settings.remove("serpulo-s-0-info");
                arc.Core.settings.remove("erekir-s-0-info");
                arc.Core.settings.remove("sepker-s-0-info");
            } catch (Throwable ignored) { }

            SepkerPlanet.load();
            SepkerSectors.load();
            SepkerXuJun.load();
            SepkerXuAndMo.load();
            SepkerFx.load();

            if (Planets.serpulo != null && Planets.serpulo.techTree == null) {
                SerpuloTechTree.load();
            }
            if (Planets.erekir != null && Planets.erekir.techTree == null) {
                ErekirTechTree.load();
            }

            if (Planets.serpulo != null && Planets.serpulo.techTree != null) {
                Planets.serpulo.techTree.addPlanet(SepkerPlanet.sepker);
            }
            if (Planets.erekir != null && Planets.erekir.techTree != null) {
                Planets.erekir.techTree.addPlanet(SepkerPlanet.sepker);
            }

            SepkerTechTree.load();
            SepkerFireSupport.load();
            SepkerLogicStatements.register();

            // ⭐ 战役里原版会在 WorldLoadEvent 中把 worldProcessorPlayerLink 强制设为 false
            //    （Logic.java:110），导致世界处理器无法链接"玩家队伍"的建筑，
            //    我们扇区里的世界按钮/链接会全部失效。这里为 sepker 星球改回来。
            //    直接改一次 + 下一帧再改一次，避免被原版同事件中的赋值覆盖。
            Events.on(WorldLoadEvent.class, new Cons<WorldLoadEvent>() {
                @Override
                public void get(WorldLoadEvent e) {
                    restoreWorldProcessorLink();
                    Core.app.post(new Runnable() {
                        @Override
                        public void run() {
                            restoreWorldProcessorLink();
                        }
                    });
                }
            });

            Events.on(ContentInitEvent.class, new Cons<ContentInitEvent>() {
                @Override
                public void get(ContentInitEvent e) {
                    SepkerPlanet.bindAllContentToSepker();
                    linkTechTreePlanet();
                }
            });

            Events.on(ClientLoadEvent.class, new Cons<ClientLoadEvent>() {
                @Override
                public void get(ClientLoadEvent e) {
                    if (Vars.ui == null) return;

                    // ⭐ 用自定义的战役难度面板替换原版的（原版按钮写死调用 ui.campaignRules.show(planet)）
                    if (!(Vars.ui.campaignRules instanceof SepkerCampaignRules)) {
                        Vars.ui.campaignRules = new SepkerCampaignRules();
                    }

                    // ⭐ 核心：版本检查与重置“不再提醒”
                    String currentVersion = Vars.mods.getMod(SepkerMod.class).meta.version;
                    String lastVersion = Core.settings.getString("sepker-last-version", "none");

                    // 如果当前版本与上次记录的版本不同，强制重置提醒
                    if (!currentVersion.equals(lastVersion)) {
                        Core.settings.put("sepker-no-startup-ui", false);
                        Core.settings.put("sepker-last-version", currentVersion);
                    }

                    // 启动 UI 弹出逻辑
                    if (!Core.settings.getBool("sepker-no-startup-ui", false)) {
                        Timer.schedule(new Timer.Task() {
                            @Override
                            public void run() {
                                new SepkerStartupUI().show();
                            }
                        }, 0.5f); // 延迟1.5秒弹出，防止与原版启动UI冲突
                    }

                    // 原版行星 UI 代理逻辑
                    if (Vars.ui.planet != null) {
                        Vars.ui.planet.shown(new Runnable() {
                            @Override
                            public void run() { installErekirProxy(); }
                        });
                        Vars.ui.planet.hidden(new Runnable() {
                            @Override
                            public void run() { uninstallErekirProxy(); }
                        });
                    }

                    // 原版科技树 UI 代理逻辑
                    if (Vars.ui.research != null) {
                        Vars.ui.research.shown(new Runnable() {
                            @Override
                            public void run() { installErekirProxy(); }
                        });
                        Vars.ui.research.hidden(new Runnable() {
                            @Override
                            public void run() { uninstallErekirProxy(); }
                        });
                    }
                }
            });

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /** 难度 → 敌方倍率：正常 1.0 / 困难 1.4 / 毁灭 2.0 */
    public static float difficultyMultiplier(Difficulty d) {
        if (d == Difficulty.hard) return 1.4f;
        if (d == Difficulty.eradication) return 2f;
        return 1f;
    }

    /**
     * 把难度倍率写进敌方（waveTeam）的 TeamRules。
     *
     * 原版 CampaignRules.apply() 只设了血量（CampaignRules.java:39-42），
     * 不设伤害；这里把血量与伤害一起按难度覆盖。
     */
    public static void applySepkerDifficulty(Planet planet, Rules rules) {
        if (planet == null || rules == null || rules.waveTeam == null) return;

        float m = difficultyMultiplier(planet.campaignRules.difficulty);

        rules.teams.get(rules.waveTeam).unitHealthMultiplier = m;
        rules.teams.get(rules.waveTeam).unitDamageMultiplier = m;
        rules.teams.get(rules.waveTeam).blockHealthMultiplier = m;
        rules.teams.get(rules.waveTeam).blockDamageMultiplier = m;
    }

    /** 让本星球的战役扇区恢复"世界处理器可链接玩家建筑"（原版战役默认关闭） */
    public static void restoreWorldProcessorLink() {
        try {
            if (Vars.state == null || Vars.state.rules == null || Vars.state.rules.sector == null) return;
            if (Vars.state.rules.sector.planet == SepkerPlanet.sepker) {
                Vars.state.rules.worldProcessorPlayerLink = true;
                applySepkerDifficulty(SepkerPlanet.sepker, Vars.state.rules);
            }
        } catch (Throwable t) {
            Log.err("[SepkerMod] restoreWorldProcessorLink failed", t);
        }
    }

    public static void linkTechTreePlanet() {
        try {
            if (SepkerPlanet.sepker == null || SepkerPlanet.sepker.techTree == null) return;
            java.lang.reflect.Field f = TechTree.TechNode.class.getDeclaredField("planet");
            f.setAccessible(true);
            f.set(SepkerPlanet.sepker.techTree, Planets.serpulo);
        } catch (Throwable t) {
            Log.err("[SepkerMod] linkTechTreePlanet failed", t);
        }
    }

    public static void installErekirProxy() {
        try {
            if (Planets.serpulo == null || Planets.erekir == null) return;

            Sector host = null;
            for (Sector s : Planets.serpulo.sectors) {
                if (s.hasBase() && s.info != null) {
                    host = s;
                    break;
                }
            }
            if (host == null) return;
            if (host.info.items instanceof com.sepker.content.MergedItemSeq) return;

            host.info.items = new com.sepker.content.MergedItemSeq(
                host.info.items, Planets.erekir);
        } catch (Throwable t) {
            Log.err("[SepkerMod] installErekirProxy failed", t);
        }
    }

    public static void uninstallErekirProxy() {
        try {
            if (Planets.serpulo == null) return;
            for (Sector s : Planets.serpulo.sectors) {
                if (s.info != null && s.info.items instanceof com.sepker.content.MergedItemSeq) {
                    com.sepker.content.MergedItemSeq proxy =
                        (com.sepker.content.MergedItemSeq) s.info.items;
                    s.info.items = proxy.real;
                }
            }
        } catch (Throwable t) {
            Log.err("[SepkerMod] uninstallErekirProxy failed", t);
        }
    }
}