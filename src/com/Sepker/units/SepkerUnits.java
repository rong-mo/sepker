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
import mindustry.gen.*;
import mindustry.type.*;
import arc.graphics.Color;
import arc.struct.ObjectSet;
import arc.func.Prov;

public class SepkerUnits {
    public static UnitType qingSe, rongGuang;

    public static void load() {

        // ================= 青涩 =================
        qingSe = new UnitType("sepker-qing-se") {{
            health = 150;                      // ⭐ 900 → 150
            armor = 8;
            hitSize = 12f;
            speed = 1f;
            rotateSpeed = 4f;
            itemCapacity = 20;
            useUnitCap = true;
            targetPriority = 2f;
            isEnemy = false;
            hideDatabase = false;

            weapons.add(new Weapon("sepker-qing-se-weapon") {{
                x = 0f;
                y = 0f;
                reload = 60f / 1.8f;
                mirror = false;
                range = 20f * 8f;

                bullet = new BasicBulletType(6f, 0f) {{
                    width = 6f;
                    height = 8f;
                    lifetime = 30f;
                    collidesAir = true;
                    collidesGround = true;
                    splashDamage = 100f;
                    splashDamageRadius = 2f * 8f;
                    buildingDamageMultiplier = 1.2f;
                    hitEffect = Fx.hitBulletColor;
                    despawnEffect = Fx.hitBulletColor;
                    frontColor = Color.white;
                    backColor = Color.white;
                    status = StatusEffects.burning;
                    statusDuration = 60f;
                    fragBullets = 2;
                    fragBullet = new BasicBulletType(6f, 4f) {{
                        width = 4f;
                        height = 6f;
                        lifetime = 15f;
                        collidesAir = true;
                        collidesGround = true;
                        buildingDamageMultiplier = 1.2f;
                        hitEffect = Fx.hitBulletColor;
                        despawnEffect = Fx.hitBulletColor;
                        frontColor = Color.white;
                        backColor = Color.white;
                        status = StatusEffects.burning;
                        statusDuration = 60f;
                    }};
                }};
            }});

            constructor = new Prov<Unit>() {
                @Override
                public Unit get() { return TankUnit.create(); }
            };
        }};

        // ================= 荣光 =================
        rongGuang = new UnitType("sepker-rong-guang") {{
            health = 1000;
            armor = 16;
            hitSize = 16f;
            speed = 0.4f;
            boostMultiplier = 3f;              // ⭐ 助推 9 格/秒（0.4 * 3 * 7.5）
            rotateSpeed = 2f;
            itemCapacity = 40;
            useUnitCap = true;
            targetPriority = 3f;
            canBoost = true;
            isEnemy = false;
            hideDatabase = false;

            weapons.add(new Weapon("sepker-rong-guang-weapon") {{
                x = 0f;
                y = 0f;
                reload = 60f;
                mirror = false;
                range = 25f * 8f;

                bullet = new BasicBulletType(4f, 20f) {{    // ⭐ 伤害 10 → 20
                    sprite = "large-bullet";
                    width = 6f;
                    height = 8f;
                    lifetime = 40f;
                    pierce = true;
                    pierceCap = 2;
                    healPercent = 1f;                    // ⭐ 修复 1%
                    collidesAir = true;
                    collidesGround = true;
                    collidesTeam = true;                 // ⭐ 可修复友方单位
                    frontColor = Color.valueOf("90EE90");
                    backColor = Color.valueOf("90EE90");
                    lightColor = Color.valueOf("90EE90");
                    lightOpacity = 0.6f;
                    lightRadius = 30f;
                    hitEffect = Fx.hitBulletColor;
                    despawnEffect = Fx.hitBulletColor;
                }};
            }});

            abilities.add(new RepairFieldAbility(4f, 60f, 10f * 8f));   // ⭐ 原版修复场
            abilities.add(new StatusFieldAbility(StatusEffects.overclock, 60f, 600f, 15f * 8f));

            constructor = new Prov<Unit>() {
                @Override
                public Unit get() { return UnitEntity.create(); }
            };
        }};

        // ================= 数据库绑定 =================
        if (qingSe.databaseTabs == null) qingSe.databaseTabs = new ObjectSet<>();
        qingSe.databaseTabs.add(SepkerPlanet.sepker);
        if (qingSe.shownPlanets == null) qingSe.shownPlanets = new ObjectSet<>();
        qingSe.shownPlanets.add(SepkerPlanet.sepker);

        if (rongGuang.databaseTabs == null) rongGuang.databaseTabs = new ObjectSet<>();
        rongGuang.databaseTabs.add(SepkerPlanet.sepker);
        if (rongGuang.shownPlanets == null) rongGuang.shownPlanets = new ObjectSet<>();
        rongGuang.shownPlanets.add(SepkerPlanet.sepker);
    }
}