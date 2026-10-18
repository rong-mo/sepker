package com.sepker.blocks;

import com.sepker.content.*;
import com.sepker.bullets.SepkerRailBullet;
import com.sepker.*;
import mindustry.content.*;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.pattern.ShootPattern;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.meta.BuildVisibility;
import arc.graphics.Color;
import arc.struct.ObjectSet;
import arc.func.Prov;

public class SepkerEMRailgun extends ItemTurret {

    public SepkerEMRailgun(String name) {
        super(name);

        category = Category.turret;
        buildVisibility = BuildVisibility.shown;

        if (databaseTabs == null) databaseTabs = new ObjectSet<>();
        databaseTabs.add(SepkerPlanet.sepker);

        health = 10240;
        size = 6;
        buildTime = 60f;
        liquidCapacity = 120f;

        range = 45f * 8f;
        inaccuracy = 15f;   // ⭐ 30f → 15f

        reload = 120f;
        shoot = new ShootPattern() {{
            shots = 10;
            shotDelay = 6f;
        }};

        recoil = 0f;
        rotate = true;

        targetAir = true;
        targetGround = true;

        maxAmmo = 80;
        ammoPerShot = 1;

        requirements = ItemStack.with(
            Items.graphite, 3000,
            Items.silicon, 2000,
            Items.copper, 2000,
            SepkerUraniumItem.uranium, 2000,
            SepkerUraniumItem.uraniumStick, 2000,
            Items.carbide, 2000,
            Items.surgeAlloy, 2000,
            Items.oxide, 1000
        );

        consumePower(12800f / 60f);
        // ⭐ 常驻 60/秒——不是 boost
        consumeLiquid(Liquids.cryofluid, 60f / 60f);

        // ==================================================
        // 弹药 1：钛 —— 深蓝
        // ==================================================
        ammoTypes.put(Items.titanium, new SepkerRailBullet() {{
            speed = 20f;
            damage = 520f;
            width = 8f;
            height = 14f;
            lifetime = 45f * 8f / 20f;
            hitSize = 6f;
            buildingDamageMultiplier = 0.01f;
            ammoMultiplier = 10f;
            reloadMultiplier = 2f;
            knockback = 1f;

            pierce = true;
            pierceBuilding = true;
            pierceCap = 4;

            frontColor = Color.valueOf("1E3A8A");
            backColor  = Color.valueOf("1E3A8A");
            lightColor = Color.valueOf("1E3A8A");
        }});

        // ==================================================
        // 弹药 2：钍 —— 淡粉主 + 深粉分裂
        // ==================================================
        ammoTypes.put(Items.thorium, new SepkerRailBullet() {{
            speed = 18f;
            damage = 0f;
            width = 10f;
            height = 16f;
            lifetime = 45f * 8f / 18f;
            hitSize = 7f;
            buildingDamageMultiplier = 0.01f;
            splashDamage = 1480f;
            splashDamageRadius = 2f * 8f;
            ammoMultiplier = 2f;
            knockback = 1f;
            status = StatusEffects.sapped;
            statusDuration = 60f;

            pierce = true;
            pierceBuilding = true;
            pierceCap = 4;

            frontColor = Color.valueOf("FFB6C1");
            backColor  = Color.valueOf("FFB6C1");
            lightColor = Color.valueOf("FFB6C1");

            fragBullets = 2;
            fragRandomSpread = 360f;
            fragOffsetMin = 0f;
            fragOffsetMax = 0f;
            fragBullet = new BasicBulletType(1f, 0f) {{
                width = 6f;
                height = 10f;
                lifetime = 8f;
                splashDamage = 200f;
                splashDamageRadius = 1f * 8f;
                collidesAir = true;
                collidesGround = true;
                buildingDamageMultiplier = 0.01f;
                knockback = 1f;
                status = StatusEffects.sapped;
                statusDuration = 60f;
                frontColor = Color.valueOf("FF1493");
                backColor  = Color.valueOf("FF1493");
                lightColor = Color.valueOf("FF1493");
                hitEffect = Fx.hitBulletColor;
                despawnEffect = Fx.hitBulletColor;
            }};
        }});

        // ==================================================
        // 弹药 3：铀棒 —— 墨绿主 + 浅绿一级 + 亮绿二级
        // ==================================================
        ammoTypes.put(SepkerUraniumItem.uraniumStick, new SepkerRailBullet() {{
            speed = 18f;
            damage = 0f;
            width = 12f;
            height = 20f;
            lifetime = 45f * 8f / 18f;
            hitSize = 8f;
            buildingDamageMultiplier = 0.01f;
            splashDamage = 4860f;
            splashDamageRadius = 3f * 8f;
            ammoMultiplier = 4f;
            status = SepkerRadiationEffect.radiation;
            statusDuration = 180f;

            pierce = true;
            pierceBuilding = true;
            pierceCap = 4;

            frontColor = Color.valueOf("006400");
            backColor  = Color.valueOf("006400");
            lightColor = Color.valueOf("006400");

            fragBullets = 23;
            fragRandomSpread = 360f;
            fragOffsetMin = 0f;
            fragOffsetMax = 0f;
            fragBullet = new BasicBulletType(2f, 0f) {{
                width = 8f;
                height = 14f;
                lifetime = 8f;
                splashDamage = 500f;
                splashDamageRadius = 4f * 8f;
                collidesAir = true;
                collidesGround = true;
                buildingDamageMultiplier = 0.01f;
                status = SepkerRadiationEffect.radiation;
                statusDuration = 180f;
                frontColor = Color.valueOf("90EE90");
                backColor  = Color.valueOf("90EE90");
                lightColor = Color.valueOf("90EE90");

                // ⭐ 二级：35 颗，散布半径 3 格
                fragBullets = 35;
                fragRandomSpread = 360f;
                fragOffsetMin = 0f;
                fragOffsetMax = 0f;
                fragBullet = new BasicBulletType(3f, 0f) {{
                    width = 6f;
                    height = 10f;
                    lifetime = 8f;
                    splashDamage = 20f;
                    splashDamageRadius = 8f * 8f;
                    collidesAir = true;
                    collidesGround = true;
                    buildingDamageMultiplier = 0.01f;
                    frontColor = Color.valueOf("00FF00");
                    backColor  = Color.valueOf("00FF00");
                    lightColor = Color.valueOf("00FF00");
                    hitEffect = Fx.hitBulletColor;
                    despawnEffect = Fx.hitBulletColor;
                }};

                hitEffect = Fx.hitBulletColor;
                despawnEffect = Fx.hitBulletColor;
            }};

            hitEffect = Fx.hitBulletColor;
            despawnEffect = Fx.hitBulletColor;
        }});

        buildType = new Prov<Building>() {
            @Override
            public Building get() {
                return new SepkerEMRailgunBuild();
            }
        };
    }

    public class SepkerEMRailgunBuild extends ItemTurretBuild {
    }
}