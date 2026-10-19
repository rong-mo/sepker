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

    // ⭐ 扇区在星球网格上的位置（PlanetGrid.create(2) 共 92 格）。
    //    选的是同一片经线（约 -20°）上互相相邻的四格，保证四关在星球视图里连成一片：
    //      91 (23.5°N,-21.6°) - 12 (0.0°,-20.9°) - 87 (47.4°N,-17.9°) - 62 (58.5°N,-49.7°)
    //    相邻关系：12↔91、91↔87、87↔62。main2 压在赤道，main1/3/4 都在北半球；
    //    将来往南推进时，从 main2 往 90 / 77 / 81 / 33 这几格方向加新扇区即可继续连片。
    public static final int MAIN1_SECTOR = 91, MAIN2_SECTOR = 12, MAIN3_SECTOR = 87, MAIN4_SECTOR = 62;

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

        main1 = new SectorPreset("sepkerMain1", SepkerPlanet.sepker, MAIN1_SECTOR) {{
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

        main2 = new SectorPreset("sepkerMain2", SepkerPlanet.sepker, MAIN2_SECTOR) {{
            addStartingItems = true;
            captureWave = 25;
            difficulty = 3;
            generator = new FileMapGenerator(k2, this);
        }};

        main3 = new SectorPreset("sepkerMain3", SepkerPlanet.sepker, MAIN3_SECTOR) {{
            captureWave = 40;
            difficulty = 5;
            generator = new FileMapGenerator(k3, this);
        }};

        main4 = new SectorPreset("sepkerMain4", SepkerPlanet.sepker, MAIN4_SECTOR) {{
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