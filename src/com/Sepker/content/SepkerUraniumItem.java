package com.sepker.content;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.type.Item;
import arc.graphics.Color;
import arc.struct.ObjectSet;

public class SepkerUraniumItem {
    public static Item uranium;
    public static Item uraniumStick; // ⭐ 新增：铀棒
    public static Item depletedUranium; // ⭐ 0.6.10 新增：贫铀

    public static void load() {
        // 铀（原矿）
        uranium = new Item("uranium", Color.valueOf("004700")) {{
            
            explosiveness = 0.4f;   
            flammability = 0.2f;    
            radioactivity = 3.0f;   
            charge = 0.01f;         
            hardness = 6;           
            cost = 1.5f;            
            alwaysUnlocked = false;
            hidden = false;         
        }};

        // ⭐ 铀棒（提纯产物）
        uraniumStick = new Item("uranium-stick", Color.valueOf("004700")) {{
            
            explosiveness = 0.1f;
            flammability = 0.1f;
            radioactivity = 5f;
            charge = 0.05f;
            cost = 2.0f;
            alwaysUnlocked = false;
            hidden = false;
        }};

        // ⭐ 贫铀（相织脱贫器的产物）
        //    面板按 value × 100% 显示（Item.java:108 用的是 stats.addPercent）
        depletedUranium = new Item("depleted-uranium", Color.valueOf("3f4a3d")) {{
            explosiveness = 0f;      // 0%  爆炸性
            flammability = 0f;       // 0%  燃烧性
            radioactivity = 10f;     // 1000% 放射性
            charge = 0f;             // 0% 放电性
            cost = 2.5f;
            alwaysUnlocked = false;
            hidden = false;
        }};

        // 绑定到赛普克尔星球数据库（铀）
        if (uranium.databaseTabs == null) uranium.databaseTabs = new ObjectSet<>();
        uranium.databaseTabs.add(SepkerPlanet.sepker);
        if (uranium.shownPlanets == null) uranium.shownPlanets = new ObjectSet<>();
        uranium.shownPlanets.add(SepkerPlanet.sepker);

        // 绑定到赛普克尔星球数据库（铀棒）
        if (uraniumStick.databaseTabs == null) uraniumStick.databaseTabs = new ObjectSet<>();
        uraniumStick.databaseTabs.add(SepkerPlanet.sepker);
        if (uraniumStick.shownPlanets == null) uraniumStick.shownPlanets = new ObjectSet<>();
        uraniumStick.shownPlanets.add(SepkerPlanet.sepker);

        // 绑定到赛普克尔星球数据库（贫铀）
        if (depletedUranium.databaseTabs == null) depletedUranium.databaseTabs = new ObjectSet<>();
        depletedUranium.databaseTabs.add(SepkerPlanet.sepker);
        if (depletedUranium.shownPlanets == null) depletedUranium.shownPlanets = new ObjectSet<>();
        depletedUranium.shownPlanets.add(SepkerPlanet.sepker);
    }
}