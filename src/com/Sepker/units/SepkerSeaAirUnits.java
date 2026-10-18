package com.sepker.units;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.content.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.UnitController;
import mindustry.gen.*;
import mindustry.type.*;
import arc.graphics.Color;
import arc.func.Func;
import arc.func.Prov;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;

public class SepkerSeaAirUnits {
    public static UnitType juYu, hongWei, changQiang, zhiHu;

    public static ObjectMap<UnitType, Float> turnRadius = new ObjectMap<>();

    public static float turnRadiusOf(UnitType type) {
        return turnRadius.get(type, 15f);
    }

    public static void load() {

        // ================= 局域 =================
        juYu = new UnitType("sepker-ju-yu") {{
            flying = true;                     // ⭐ 空军
            omniMovement = false;
            health = 450;
            armor = 0;
            hitSize = 12f;
            speed = 25f / 7.5f;
            rotateSpeed = 5f;
            itemCapacity = 0;
            useUnitCap = true;
            isEnemy = false;
            hideDatabase = false;

            aiController = new Prov<SepkerJetAI>() {
                @Override
                public SepkerJetAI get() { return new SepkerJetAI(); }
            };

            controller = new Func<Unit, UnitController>() {
                @Override
                public UnitController get(Unit u) { return new SepkerJetAI(); }
            };

            weapons.add(new Weapon("sepker-ju-yu-weapon") {{
                x = 0f; y = 0f;
                reload = 24f;
                mirror = false;
                range = 20f * 8f;
                targetAir = true;
                targetGround = false;
                bullet = new BasicBulletType(15f, 250f) {{
                    width = 6f; height = 10f; lifetime = 60f;
                    collidesAir = true; collidesGround = false;
                    hitEffect = Fx.hitBulletColor;
                    despawnEffect = Fx.hitBulletColor;
                }};
            }});

            constructor = new Prov<Unit>() {
                @Override
                public Unit get() { return new SepkerJetUnit(); }
            };
        }};

        // ================= 虹伟 =================
        hongWei = new UnitType("sepker-hong-wei") {{
            flying = true;                     // ⭐ 空军
            omniMovement = false;
            health = 250;
            armor = 0;
            hitSize = 20f;
            speed = 15f / 7.5f;
            rotateSpeed = 3f;
            itemCapacity = 40;
            useUnitCap = true;
            isEnemy = false;
            hideDatabase = false;

            aiController = new Prov<SepkerJetAI>() {
                @Override
                public SepkerJetAI get() { return new SepkerJetAI(); }
            };

            controller = new Func<Unit, UnitController>() {
                @Override
                public UnitController get(Unit u) { return new SepkerJetAI(); }
            };

            // ⭐ 武器改成炸弹（天垠风格）
            weapons.add(new Weapon("sepker-hong-wei-weapon") {{
                x = 0f;
                y = 0f;
                reload = 60f;
                mirror = false;
                range = 5f * 8f;
                targetAir = false;
                targetGround = true;

                bullet = new BombBulletType(4f, 0f) {{   // ⭐ 炸弹类型
                    width = 8f;
                    height = 12f;
                    lifetime = 30f;
                    splashDamage = 200f;
                    splashDamageRadius = 5f * 8f;
                    healPercent = 1f;                   // ⭐ 5% → 1%
                    collidesAir = false;
                    collidesGround = true;
                    collidesTeam = true;                // ⭐ 可修复友方
                    hitEffect = Fx.hitBulletColor;
                    despawnEffect = Fx.hitBulletColor;
                }};
            }});

            constructor = new Prov<Unit>() {
                @Override
                public Unit get() { return new SepkerJetUnit(); }
            };
        }};

        // ================= 长枪 =================
        changQiang = new UnitType("sepker-chang-qiang") {{
            naval = true;                      // ⭐ 海军
            health = 500;
            armor = 2;
            hitSize = 12f;
            speed = 10f / 7.5f;
            rotateSpeed = 3f;
            itemCapacity = 0;
            useUnitCap = true;
            isEnemy = false;
            hideDatabase = false;

            immunities.add(StatusEffects.wet);

            weapons.add(new Weapon("sepker-chang-qiang-side-cannon") {{
                x = -5f; y = 0f;
                reload = 20f;
                mirror = true;
                alternate = false;
                shootCone = 360f;
                rotate = true;
                range = 25f * 8f;
                targetAir = true;
                targetGround = true;
                bullet = new BasicBulletType(10f, 15f) {{
                    width = 4f; height = 8f; lifetime = 50f;
                    collidesAir = true; collidesGround = true;
                    hitEffect = Fx.hitBulletColor;
                    despawnEffect = Fx.hitBulletColor;
                }};
            }});

            weapons.add(new Weapon("sepker-chang-qiang-main-cannon") {{
                x = 0f; y = -3f;
                reload = 60f;
                mirror = false;
                shootCone = 360f;
                rotate = true;
                range = 25f * 8f;
                targetAir = true;
                targetGround = true;
                bullet = new BasicBulletType(8f, 0f) {{
                    width = 6f; height = 10f; lifetime = 50f;
                    collidesAir = true; collidesGround = true;
                    splashDamage = 115f;
                    splashDamageRadius = 5f * 8f;
                    hitEffect = Fx.hitBulletColor;
                    despawnEffect = Fx.hitBulletColor;
                }};
            }});

            constructor = new Prov<Unit>() {
                @Override
                public Unit get() { return UnitWaterMove.create(); }
            };
        }};

        // ================= 支护 =================
        zhiHu = new UnitType("sepker-zhi-hu") {{
            naval = true;                      // ⭐ 海军
            health = 300;
            armor = 4;
            hitSize = 16f;
            speed = 5f / 7.5f;
            rotateSpeed = 2f;
            itemCapacity = 100;
            buildSpeed = 2f;
            useUnitCap = true;
            isEnemy = false;
            hideDatabase = false;

            immunities.add(StatusEffects.wet);

            weapons.add(new Weapon("sepker-zhi-hu-heal") {{
                x = 0f;
                y = 0f;
                reload = 60f;
                mirror = false;
                shootCone = 360f;
                rotate = true;
                range = 15f * 8f;
                targetAir = true;
                targetGround = true;
                bullet = new BasicBulletType(6f, 1f) {{  // ⭐ 伤害 0 → 1
                    width = 4f;
                    height = 6f;
                    lifetime = 40f;
                    healPercent = 5f;
                    collidesAir = true;
                    collidesGround = true;
                    collidesTeam = true;                // ⭐ 可修复友方单位
                    hitEffect = Fx.hitBulletColor;
                    despawnEffect = Fx.hitBulletColor;
                }};
            }});

            abilities.add(new RepairFieldAbility(12f, 90f, 10f * 8f));   // ⭐ 原版修复场

            constructor = new Prov<Unit>() {
                @Override
                public Unit get() { return UnitWaterMove.create(); }
            };
        }};

        // ================= 转弯半径 =================
        turnRadius.put(juYu,   5f);
        turnRadius.put(hongWei, 15f);

        // ================= 数据库绑定 =================
        if (juYu.databaseTabs == null) juYu.databaseTabs = new ObjectSet<>();
        juYu.databaseTabs.add(SepkerPlanet.sepker);
        if (juYu.shownPlanets == null) juYu.shownPlanets = new ObjectSet<>();
        juYu.shownPlanets.add(SepkerPlanet.sepker);

        if (hongWei.databaseTabs == null) hongWei.databaseTabs = new ObjectSet<>();
        hongWei.databaseTabs.add(SepkerPlanet.sepker);
        if (hongWei.shownPlanets == null) hongWei.shownPlanets = new ObjectSet<>();
        hongWei.shownPlanets.add(SepkerPlanet.sepker);

        if (changQiang.databaseTabs == null) changQiang.databaseTabs = new ObjectSet<>();
        changQiang.databaseTabs.add(SepkerPlanet.sepker);
        if (changQiang.shownPlanets == null) changQiang.shownPlanets = new ObjectSet<>();
        changQiang.shownPlanets.add(SepkerPlanet.sepker);

        if (zhiHu.databaseTabs == null) zhiHu.databaseTabs = new ObjectSet<>();
        zhiHu.databaseTabs.add(SepkerPlanet.sepker);
        if (zhiHu.shownPlanets == null) zhiHu.shownPlanets = new ObjectSet<>();
        zhiHu.shownPlanets.add(SepkerPlanet.sepker);
    }
}