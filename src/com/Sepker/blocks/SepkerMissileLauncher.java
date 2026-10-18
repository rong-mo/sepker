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
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import mindustry.gen.Building;

public class SepkerMissileLauncher extends ItemTurret {

    public SepkerMissileLauncher(String name) {
        super(name);

        

        category = Category.turret;
        buildVisibility = BuildVisibility.shown;

        if (databaseTabs == null) databaseTabs = new ObjectSet<>();
        databaseTabs.add(SepkerPlanet.sepker);

        health = 6500;
        size = 6;
        buildTime = 60f * 40f;
        liquidCapacity = 150f;
        range = 80f * 8f;
        // ⭐ 发射效率：6 秒一发
        reload = 60f * 6f;
        inaccuracy = 15f;
        recoil = 0f;
        rotate = false;
        

        maxAmmo = 325;
        ammoPerShot = 25;

        requirements = ItemStack.with(
            Items.copper, 2500,
            Items.graphite, 1500,
            Items.silicon, 3500,
            SepkerUraniumItem.uranium, 1800,
            Items.tungsten, 3000,
            Items.oxide, 1000,
            Items.carbide, 500,
            SepkerUraniumItem.uraniumStick, 200
        );

        final Color redColor       = Color.valueOf("FF4500");
        final Color deepGreenColor = Color.valueOf("006400");

        final float blastRadius   = 15f * 8f;
        final float uraniumRadius = 20f * 8f;

        // ⭐ 主子弹爆炸：带冲击波
        final Effect redExplosion   = makeExplosion(redColor, blastRadius, true);
        final Effect greenExplosion = makeExplosion(deepGreenColor, uraniumRadius, true);

        // ⭐ 分裂子弹爆炸：不带冲击波（性能优化）
        final Effect redFragExplosion   = makeExplosion(redColor, 5f * 8f, false);
        final Effect greenFragExplosion = makeExplosion(deepGreenColor, 5f * 8f, false);

        // ================= 弹药 1：爆炸混合物（红色爆炸） =================
        SepkerBallisticMissile blastMissile = new SepkerBallisticMissile();
        blastMissile.splashDamage = 6400f;
        blastMissile.splashDamageRadius = blastRadius;
        blastMissile.buildingDamageMultiplier = 0.01f;
        blastMissile.knockback = 1f;
        blastMissile.fragBullets = 7;
        blastMissile.hitEffect = redExplosion;
        blastMissile.despawnEffect = redExplosion;

        blastMissile.fragBullet = new BasicBulletType() {{
            splashDamage = 100f;
            splashDamageRadius = 5f * 8f;
            buildingDamageMultiplier = 0.02f;
            knockback = 1f;
            collidesAir = true;
            collidesGround = true;
            frontColor = redColor;
            backColor  = redColor;
            lightColor = redColor;
            lightOpacity = 0.6f;
            lightRadius = 30f;
            hitEffect = redFragExplosion;
            despawnEffect = redFragExplosion;
        }};

        // ================= 弹药 2：铀（深墨绿爆炸） =================
        SepkerBallisticMissile uraniumMissile = new SepkerBallisticMissile();
        uraniumMissile.splashDamage = 25600f;
        uraniumMissile.splashDamageRadius = uraniumRadius;
        uraniumMissile.buildingDamageMultiplier = 0.02f;
        uraniumMissile.knockback = 3f;
        uraniumMissile.status = SepkerRadiationEffect.radiation;
        uraniumMissile.statusDuration = 1800f;
        uraniumMissile.fragBullets = 12;
        uraniumMissile.hitEffect = greenExplosion;
        uraniumMissile.despawnEffect = greenExplosion;
        uraniumMissile.reloadMultiplier = 2f;

        uraniumMissile.fragBullet = new BasicBulletType() {{
            splashDamage = 250f;
            splashDamageRadius = 5f * 8f;
            buildingDamageMultiplier = 0.01f;
            knockback = 3f;
            status = SepkerRadiationEffect.radiation;
            statusDuration = 1800f;
            collidesAir = true;
            collidesGround = true;
            frontColor = deepGreenColor;
            backColor  = deepGreenColor;
            lightColor = deepGreenColor;
            lightOpacity = 0.6f;
            lightRadius = 30f;
            hitEffect = greenFragExplosion;
            despawnEffect = greenFragExplosion;
        }};

        ammoTypes.put(Items.blastCompound, blastMissile);
        ammoTypes.put(SepkerUraniumItem.uranium, uraniumMissile);

        consumeLiquid(Liquids.water, 25f / 60f);                    // 水保留普通消耗
consume(new SepkerCoolant(Liquids.cryofluid, 53.34f / 60f));   // 冷冻液单一冷却液

        buildType = new Prov<Building>() {
            @Override
            public Building get() {
                return new SepkerMissileLauncherBuild();
            }
        };
    }

    /**
     * 自定义爆炸效果
     * @param color     颜色
     * @param radius    视觉半径
     * @param withWave  是否绘制外圈冲击波
     */
    private static Effect makeExplosion(final Color color, final float radius, final boolean withWave) {
        return new Effect(45f, new Cons<Effect.EffectContainer>() {
            @Override
            public void get(Effect.EffectContainer e) {
                float fin  = e.fin();
                float fout = 1f - fin;

                if (withWave) {
                    Draw.color(color);
                    Draw.alpha(fout * 0.9f);
                    Lines.stroke(5f);
                    Lines.circle(e.x, e.y, radius * fin);
                }

                Draw.color(color);
                Draw.alpha(fout);
                Lines.stroke(3f);
                for (int i = 0; i < 16; i++) {
                    float angle = i * 22.5f + e.id * 7f;
                    Lines.lineAngle(e.x, e.y, angle, radius * fin);
                }

                Draw.color(Color.white);
                Draw.alpha(fout);
                Fill.circle(e.x, e.y, radius * 0.3f * fin);

                Draw.reset();
            }
        });
    }

    public class SepkerMissileLauncherBuild extends ItemTurretBuild {
        public int shotCounter = 0;

        public static final float[] X_OFFSETS = {-18f, -6f, 6f, 18f};
        public static final float[] Y_OFFSETS = {-18f, -6f, 6f, 18f};

        @Override
        public void draw() {
            float oldRot = rotation;
            rotation = 0f;
            super.draw();
            rotation = oldRot;
        }

        @Override
        protected void shoot(BulletType type) {
            for (int i = 0; i < 13; i++) {
                if (this.ammo.isEmpty()) break;

                int idx = shotCounter % 16;
                int col = idx % 4;
                int row = idx / 4;

                float ox = X_OFFSETS[col];
                float oy = Y_OFFSETS[row];

                float theta = (rotation - 90f) * Mathf.degRad;
                float cos = Mathf.cos(theta);
                float sin = Mathf.sin(theta);

                float origX = SepkerMissileLauncher.this.shootX;
                float origY = SepkerMissileLauncher.this.shootY;

                SepkerMissileLauncher.this.shootX = ox * cos + oy * sin;
                SepkerMissileLauncher.this.shootY = -ox * sin + oy * cos;

                super.shoot(type);

                SepkerMissileLauncher.this.shootX = origX;
                SepkerMissileLauncher.this.shootY = origY;

                shotCounter++;
            }
        }
    }
}