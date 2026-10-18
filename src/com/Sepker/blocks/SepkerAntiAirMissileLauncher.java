package com.sepker.blocks;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.content.*;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.Effect;
import mindustry.gen.Bullet;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.*;
import mindustry.type.Category;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.meta.BuildVisibility;
import arc.struct.ObjectSet;
import arc.func.Prov;
import arc.func.Cons;
import arc.math.Mathf;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;

public class SepkerAntiAirMissileLauncher extends ItemTurret {

    public SepkerAntiAirMissileLauncher(String name) {
        super(name);

        

        category = Category.turret;

        if (databaseTabs == null) databaseTabs = new ObjectSet<>();
        databaseTabs.add(SepkerPlanet.sepker);

        health = 1500;
        size = 5;
        buildTime = 60f * 8f;
        liquidCapacity = 60f;
        range = 100f * 8f;
        reload = 15f;
        inaccuracy = 0f;
        recoil = 0f;
        rotate = false;
        buildVisibility = BuildVisibility.shown;
        targetAir = true;
        targetGround = false;

        maxAmmo = 16;
        ammoPerShot = 1;

        requirements = ItemStack.with(
            Items.copper, 1000,
            SepkerUraniumItem.uranium, 2000,
            Items.silicon, 1000,
            Items.lead, 1000,
            Items.titanium, 1000,
            Items.graphite, 1000,
            Items.thorium, 1000
        );

        consumePower(10000f / 60f);
        

        // ⭐ 火花爆炸的染色版本
        final Effect pinkSpark = coloredSparkExplosion(Color.valueOf("FFAAFF"));  // 硅弹：原版粉紫
        final Effect greenSpark = coloredSparkExplosion(Color.valueOf("006400")); // 铀弹：深墨绿

        // ================= 弹药 1：硅 =================
        SepkerAntiAirMissile siliconMissile = new SepkerAntiAirMissile();
        siliconMissile.damage = 1000f;
        siliconMissile.splashDamage = 0f;
        siliconMissile.hitEffect = pinkSpark;
        siliconMissile.despawnEffect = pinkSpark;

        ammoTypes.put(Items.silicon, siliconMissile);

        // ================= 弹药 2：铀 =================
        SepkerAntiAirMissile uraniumMissile = new SepkerAntiAirMissile();
        uraniumMissile.damage = 2500f;
        uraniumMissile.splashDamage = 0f;
        uraniumMissile.status = SepkerRadiationEffect.radiation;
        uraniumMissile.statusDuration = 300f;
        uraniumMissile.reloadMultiplier = 1.5f;
        uraniumMissile.pierceArmor = true;
        uraniumMissile.armorMultiplier = 1f;
        // ⭐ 深墨绿火花爆炸
        uraniumMissile.hitEffect = greenSpark;
        uraniumMissile.despawnEffect = greenSpark;

        ammoTypes.put(SepkerUraniumItem.uranium, uraniumMissile);

        consume(new SepkerCoolant(Liquids.cryofluid, 6.67f / 60f));

        buildType = new Prov<Building>() {
            @Override
            public Building get() {
                return new SepkerAntiAirBuild();
            }
        };
    }

    /**
     * ⭐ 自定义染色版 sparkExplosion
     * 复制原版 sparkExplosion 的绘制逻辑，把颜色参数化
     */
    private static Effect coloredSparkExplosion(final Color color) {
        return new Effect(30f, 80f, new Cons<Effect.EffectContainer>() {
            @Override
            public void get(Effect.EffectContainer e) {
                Draw.color(color);
                Draw.alpha(1f - e.fin());

                // 外圈冲击波
                Lines.stroke((1f - e.fin()) * 2f);
                Lines.circle(e.x, e.y, e.fin() * 30f);

                // 向外散落的短火花
                Lines.stroke((1f - e.fin()) * 1.5f);
                for (int i = 0; i < 8; i++) {
                    float angle = i * 45f + e.id * 17f;
                    float len = 15f * e.fout();
                    Lines.lineAngle(e.x, e.y, angle, len);
                }

                Draw.reset();
            }
        });
    }

    public class SepkerAntiAirBuild extends ItemTurretBuild {
        public int shotCounter = 0;

        public static final float[] X_OFFSETS = {-16f, -5f, 5f, 16f};
        public static final float[] Y_OFFSETS = {-16f, -5f, 5f, 16f};

        @Override
        public void draw() {
            float oldRot = rotation;
            rotation = 0f;
            super.draw();
            rotation = oldRot;
        }

        @Override
        protected void findTarget() {
            float airRange = 50f * 8f;
            float airRangeSq = airRange * airRange;
            Unit closestUnit = null;
            float closestDstSq = airRangeSq;

            for (int i = 0; i < Groups.unit.size(); i++) {
                Unit u = Groups.unit.index(i);
                if (u == null || !u.isValid() || u.dead) continue;
                if (u.team == team) continue;
                if (!u.isFlying() && !u.type.flying) continue;

                float dstSq = dst2(u.x, u.y);
                if (dstSq < closestDstSq) {
                    closestDstSq = dstSq;
                    closestUnit = u;
                }
            }

            if (closestUnit != null) {
                target = closestUnit;
                return;
            }

            float bulletRange = 100f * 8f;
            float bulletRangeSq = bulletRange * bulletRange;
            Bullet closestBullet = null;
            closestDstSq = bulletRangeSq;

            for (int i = 0; i < Groups.bullet.size(); i++) {
                Bullet b = Groups.bullet.index(i);
                if (b == null || b.type == null) continue;
                if (b.team == team) continue;

                if (b.type instanceof SepkerBallisticMissile
                    && b.data instanceof SepkerBallisticMissile.MissileData) {
                    SepkerBallisticMissile.MissileData md =
                        (SepkerBallisticMissile.MissileData) b.data;
                    if (md.phase == 2) continue;
                }

                float dstSq = dst2(b.x, b.y);
                if (dstSq < closestDstSq) {
                    closestDstSq = dstSq;
                    closestBullet = b;
                }
            }

            if (closestBullet != null) {
                target = closestBullet;
            } else {
                target = null;
            }
        }

        @Override
        protected void shoot(BulletType type) {
            if (this.ammo.isEmpty()) return;

            int idx = shotCounter % 16;
            int col = idx % 4;
            int row = idx / 4;

            float ox = X_OFFSETS[col];
            float oy = Y_OFFSETS[row];

            float theta = (rotation - 90f) * Mathf.degRad;
            float cos = Mathf.cos(theta);
            float sin = Mathf.sin(theta);

            float origX = SepkerAntiAirMissileLauncher.this.shootX;
            float origY = SepkerAntiAirMissileLauncher.this.shootY;

            SepkerAntiAirMissileLauncher.this.shootX = ox * cos + oy * sin;
            SepkerAntiAirMissileLauncher.this.shootY = -ox * sin + oy * cos;

            super.shoot(type);

            SepkerAntiAirMissileLauncher.this.shootX = origX;
            SepkerAntiAirMissileLauncher.this.shootY = origY;

            shotCounter++;
        }
    }
}