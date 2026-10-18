package com.sepker.content;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.type.StatusEffect;
import arc.graphics.Color;

public class SepkerRadiationEffect {
    public static StatusEffect radiation;

    public static void load() {
        radiation = new StatusEffect("sepker-radiation") {{
            
            damage = 8f;                     // 每秒伤害
            color = Color.valueOf("4A9BD9");
            effect = mindustry.content.Fx.none; // 可替换为辐射粒子效果
        }};
    }
}