package com.sepker.content;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.Vars;
import mindustry.maps.generators.FileMapGenerator;
import mindustry.type.SectorPreset;

public class SepkerSectors {
    public static SectorPreset main1, main2, main3, main4;

    public static void load() {
        // ⭐ 拼 mod 前缀（FileMapGenerator 会剥离它）
        String modName = Vars.mods.getMod(SepkerMod.class).meta.name;

        StringBuilder s1 = new StringBuilder(modName);
        s1.append("-main1");
        final String k1 = s1.toString();

        StringBuilder s2 = new StringBuilder(modName);
        s2.append("-main2");
        final String k2 = s2.toString();

        StringBuilder s3 = new StringBuilder(modName);
        s3.append("-main3");
        final String k3 = s3.toString();

        StringBuilder s4 = new StringBuilder(modName);
        s4.append("-main4");
        final String k4 = s4.toString();

        main1 = new SectorPreset("sepkerMain1", SepkerPlanet.sepker, 0) {{
            addStartingItems = true;
            captureWave = 10;
            difficulty = 3;
            overrideLaunchDefaults = true;
            // ⭐ overrideLaunchDefaults = true 会让本区块忽略星球上的设置，
            //    所以这两个开关必须在这里再设一遍（否则边陲废墟的发射界面没有核心选择）
            allowLaunchSchematics = true;
            allowLaunchLoadout = true;
            noLighting = true;
            isLastSector = false;
            generator = new FileMapGenerator(k1, this);
        }};

        main2 = new SectorPreset("sepkerMain2", SepkerPlanet.sepker, 1) {{
            addStartingItems = true;
            captureWave = 25;
            difficulty = 3;
            generator = new FileMapGenerator(k2, this);
        }};

        main3 = new SectorPreset("sepkerMain3", SepkerPlanet.sepker, 2) {{
            captureWave = 40;
            difficulty = 5;
            generator = new FileMapGenerator(k3, this);
        }};

        main4 = new SectorPreset("sepkerMain4", SepkerPlanet.sepker, 3) {{
            captureWave = 60;
            difficulty = 7;
            isLastSector = true;
            generator = new FileMapGenerator(k4, this);
        }};

        main1.databaseTabs = arc.struct.ObjectSet.with(SepkerPlanet.sepker);
        main2.databaseTabs = arc.struct.ObjectSet.with(SepkerPlanet.sepker);
        main3.databaseTabs = arc.struct.ObjectSet.with(SepkerPlanet.sepker);
        main4.databaseTabs = arc.struct.ObjectSet.with(SepkerPlanet.sepker);
    }
}