package com.sepker.ui;

import com.sepker.content.SepkerPlanet;
import mindustry.type.Planet;
import mindustry.ui.dialogs.CampaignRulesDialog;

/**
 * 代理：替换 Vars.ui.campaignRules 的实例。
 *
 * 原版 PlanetDialog 的“战役难度”按钮是写死调用 ui.campaignRules.show(planet) 的，
 * 所以把实例换成这个；点击时先判断星球归属——只有赛普克尔走自定义面板，
 * 其它星球（原版星球、别的 mod 星球）原样交回原版对话框，不改变它们的表现。
 */
public class SepkerCampaignRules extends CampaignRulesDialog {

    public static boolean isSepker(Planet planet) {
        return planet != null && SepkerPlanet.sepker != null && planet == SepkerPlanet.sepker;
    }

    @Override
    public void show(Planet planet) {
        if (isSepker(planet)) {
            new SepkerDifficultyDialog().show(planet);
        } else {
            super.show(planet);
        }
    }
}
