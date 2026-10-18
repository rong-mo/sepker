package com.sepker.ui;

import mindustry.type.Planet;
import mindustry.ui.dialogs.CampaignRulesDialog;

/**
 * 代理：替换 Vars.ui.campaignRules 的实例。
 *
 * 原版 PlanetDialog 的"战役难度"按钮是写死调用 ui.campaignRules.show(planet) 的，
 * 所以把实例换成这个，点击就导到我们自己的面板（本类本身永远不显示）。
 */
public class SepkerCampaignRules extends CampaignRulesDialog {

    @Override
    public void show(Planet planet) {
        new SepkerDifficultyDialog().show(planet);
    }
}
