package com.sepker.content;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.type.Liquid;
import arc.graphics.Color;

public class SepkerSteam {
    public static Liquid steam;

    public static void load() {
        steam = new Liquid("steam", Color.valueOf("D3D3D3")) {{
            
            heatCapacity = 0.5f;
            explosiveness = 0f;
            flammability = 0f;
            temperature = 1.0f;
            viscosity = 0.2f;
            boilPoint = -1f; // 不再是液体，作为气体处理
            gas = true;
        }};
    }
}