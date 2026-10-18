package com.sepker.ui;

import arc.Core;
import arc.func.Boolc;
import arc.func.Boolf;
import arc.func.Boolp;
import arc.func.Cons;
import arc.scene.ui.Button;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import com.sepker.SepkerMod;
import mindustry.Vars;
import mindustry.game.CampaignRules;
import mindustry.game.Difficulty;
import mindustry.gen.Call;
import mindustry.gen.Tex;
import mindustry.type.Planet;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

/**
 * 赛普克尔的战役难度 / 规则面板（0.6.10）。
 *
 * 原版 CampaignRulesDialog 的难度来自写死的 Difficulty 枚举（5 档），
 * 而本 mod 只要 3 档且倍率不同，所以这里重画一个。
 * 数据仍然存在 planet.campaignRules 里，难度用 Difficulty.normal / hard / eradication 三档表示。
 */
public class SepkerDifficultyDialog extends BaseDialog {

    public Planet planet;

    public SepkerDifficultyDialog() {
        super("@campaign.difficulty");
        addCloseButton();

        hidden(new Runnable() {
            @Override
            public void run() {
                if (planet == null) return;

                planet.saveRules();

                if (Vars.state.isGame() && Vars.state.isCampaign() && Vars.state.getPlanet() == planet) {
                    planet.campaignRules.apply(planet, Vars.state.rules);
                    SepkerMod.applySepkerDifficulty(planet, Vars.state.rules);
                    Call.setRules(Vars.state.rules);
                }
            }
        });

        onResize(new Runnable() {
            @Override
            public void run() {
                rebuild();
            }
        });
    }

    public void show(Planet p) {
        planet = p;
        rebuild();
        show();
    }

    /** 必须是 public —— 匿名内部类会调用 */
    public void rebuild() {
        if (planet == null) return;

        final CampaignRules rules = planet.campaignRules;
        cont.clear();

        cont.top().pane(new Cons<Table>() {
            @Override
            public void get(Table inner) {
                inner.top().left().defaults().fillX().left().pad(5);

                // ===== 难度：正常 / 困难 / 毁灭 =====
                inner.table(Tex.button, new Cons<Table>() {
                    @Override
                    public void get(Table t) {
                        t.margin(10f);
                        ButtonGroup<Button> group = new ButtonGroup<>();
                        t.defaults().size(140f, 50f);

                        Difficulty[] opts = {
                            Difficulty.normal,
                            Difficulty.hard,
                            Difficulty.eradication
                        };

                        for (final Difficulty diff : opts) {
                            t.button(diff.localized(), Styles.flatTogglet, new Runnable() {
                                @Override
                                public void run() {
                                    rules.difficulty = diff;
                                }
                            })
                            .group(group)
                            .checked(new Boolf<TextButton>() {
                                @Override
                                public boolean get(TextButton b) {
                                    return rules.difficulty == diff;
                                }
                            })
                            .tooltip(diffInfo(diff));
                        }
                    }
                }).left().fill(false).expand(false, false).row();

                // ===== 开关 =====
                if (planet.allowSectorInvasion) {
                    check(inner, "@rules.invasions", new Boolc() {
                        @Override public void get(boolean b) { rules.sectorInvasion = b; }
                    }, new Boolp() {
                        @Override public boolean get() { return rules.sectorInvasion; }
                    });
                }

                check(inner, "@rules.hidespawns", new Boolc() {
                    @Override public void get(boolean b) { rules.hideSpawns = b; }
                }, new Boolp() {
                    @Override public boolean get() { return rules.hideSpawns; }
                });

                if (planet.showRtsAIRule) {
                    check(inner, "@rules.rtsai.campaign", new Boolc() {
                        @Override public void get(boolean b) { rules.rtsAI = b; }
                    }, new Boolp() {
                        @Override public boolean get() { return rules.rtsAI; }
                    });
                }

                if (!planet.clearSectorOnLose) {
                    check(inner, "@rules.clearsectoronloss", new Boolc() {
                        @Override public void get(boolean b) { rules.clearSectorOnLose = b; }
                    }, new Boolp() {
                        @Override public boolean get() { return rules.clearSectorOnLose; }
                    });
                }
            }
        }).growY();
    }

    /** 每档难度的说明：按本 mod 的实际倍率，而不是原版枚举里的 */
    public String diffInfo(Difficulty diff) {
        String key = "sepker.difficulty." + diff.name() + ".info";
        if (Core.bundle.has(key)) return Core.bundle.get(key);

        float m = SepkerMod.difficultyMultiplier(diff);
        if (m == 1f) return Core.bundle.get("difficulty.nomodifiers", "No modifiers");
        return "+" + (int) ((m - 1f) * 100f) + "%";
    }

    /** 必须是 public —— 匿名内部类会调用 */
    public void check(Table parent, String text, Boolc cons, Boolp prov) {
        String infoText = text.substring(1) + ".info";
        var cell = parent.check(text, cons).checked(prov.get());
        if (Core.bundle.has(infoText)) {
            cell.tooltip(text + ".info");
        }
        cell.get().left();
        parent.row();
    }
}
