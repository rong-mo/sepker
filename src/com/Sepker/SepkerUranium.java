package com.sepker.content;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;

public class SepkerUranium {
    public static SepkerNuclearReactor nuclearReactor;
    public static SepkerIndustrialTurbine industrialTurbine;
    public static SepkerMissileLauncher missileLauncher;
    public static SepkerAntiAirMissileLauncher antiAirLauncher;
    public static SepkerUraniumWall uraniumWall;
    public static SepkerLargeUraniumWall largeUraniumWall;
    public static SepkerCombatFactory combatFactory;
    public static SepkerWarpConveyor warpConveyor;
    public static SepkerSeaAirFactory seaAirFactory;
    public static SepkerSmallAirport smallAirport;
    public static SepkerEMRailgun emRailgun;
    public static SepkerEMRailgun2 emRailgun2;
    public static SepkerCombinedReconstructor combinedReconstructor;

    public static void load() {
        SepkerSteam.load();
        SepkerUraniumItem.load();
        SepkerUraniumOre.load();
        SepkerShockDrill.load();
        SepkerUraniumFactory.load();
        SepkerPhaseDepleter.load();   // ⭐ 0.6.10 新增：相织脱贫器
        SepkerCryofluidMixer.load();  // ⭐ 0.6.10 新增：大型冷冻液混合器
        SepkerUnits.load();
        SepkerSeaAirUnits.load();

        nuclearReactor = new SepkerNuclearReactor("sepker-nuclear-reactor");
        industrialTurbine = new SepkerIndustrialTurbine("sepker-industrial-turbine");
        missileLauncher = new SepkerMissileLauncher("sepker-missile-launcher");
        antiAirLauncher = new SepkerAntiAirMissileLauncher("sepker-antiair-launcher");
        uraniumWall = new SepkerUraniumWall("sepker-uranium-wall");
        largeUraniumWall = new SepkerLargeUraniumWall("sepker-large-uranium-wall");
        combatFactory = new SepkerCombatFactory("sepker-combat-factory");
        warpConveyor = new SepkerWarpConveyor("sepker-warp-conveyor");
        seaAirFactory = new SepkerSeaAirFactory("sepker-sea-air-factory");
        smallAirport = new SepkerSmallAirport("sepker-small-airport");
        emRailgun = new SepkerEMRailgun("sepker-em-railgun");
        emRailgun2 = new SepkerEMRailgun2("sepker-em-railgun-2");
        combinedReconstructor = new SepkerCombinedReconstructor("sepker-combined-reconstructor"); 
    }
}