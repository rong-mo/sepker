package com.sepker.blocks;

import com.sepker.content.*;
import com.sepker.bullets.SepkerRailBullet;
import com.sepker.*;
import mindustry.content.*;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.meta.BuildVisibility;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Time;
import arc.func.Prov;

public class SepkerEMRailgun2 extends ItemTurret {

    /** 最后圆环初始半径（格） */
    public float ringStartTiles = 10f;
    /** 实心圆移动距离（格）——从中心到发射口 */
    public float chargeTravelTiles = 4f;
    /** 脉冲圆环生成间隔（tick，60fps 下 30 = 0.5 秒） */
    public float pulseInterval = 30f;
    /** 第一个脉冲圆环的存活时间（tick）——0.25 秒 = 15f */
    public float firstPulseLife = 45f;

    /** 淡蓝 */
    public static final Color LOADING_COLOR = Color.valueOf("87CEFA");

    public SepkerEMRailgun2(String name) {
        super(name);

        category = Category.turret;
        buildVisibility = BuildVisibility.shown;

        if (databaseTabs == null) databaseTabs = new ObjectSet<>();
        databaseTabs.add(SepkerPlanet.sepker);

        health = 12310;
        size = 8;
        buildTime = 60f * 80f;
        liquidCapacity = 120f;

        range = 105f * 8f;
        inaccuracy = 0f;

        reload = 60f * 11f;

        recoil = 0f;
        rotate = true;

        targetAir = true;
        targetGround = true;

        maxAmmo = 15;
        ammoPerShot = 5;

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

        consumePower(25600f / 60f);
        consumeLiquid(Liquids.cryofluid, 60f / 60f);

        // ============ 钛 ============
        ammoTypes.put(Items.titanium, new SepkerRailBullet() {{
            speed = 40f;
            damage = 0f;
            width = 12f;
            height = 24f;
            lifetime = 105f * 8f / 40f;
            hitSize = 10f;
            buildingDamageMultiplier = 0.01f;
            splashDamage = 19680f;
            splashDamageRadius = 3f * 8f;
            ammoMultiplier = 5f;
            reloadMultiplier = 1.1f;
            knockback = 1f;

            pierce = true;
            pierceBuilding = true;
            pierceCap = -1;

            frontColor = Color.valueOf("1E3A8A");
            backColor  = Color.valueOf("1E3A8A");
            lightColor = Color.valueOf("1E3A8A");
        }});

        // ============ 钍 ============
        ammoTypes.put(Items.thorium, new SepkerRailBullet() {{
            speed = 40f;
            damage = 0f;
            width = 14f;
            height = 28f;
            lifetime = 105f * 8f / 40f;
            hitSize = 12f;
            buildingDamageMultiplier = 0.01f;
            splashDamage = 70000f;
            splashDamageRadius = 4f * 8f;
            ammoMultiplier = 5f;
            knockback = 1f;
            status = StatusEffects.sapped;
            statusDuration = 120f;

            pierce = true;
            pierceBuilding = true;
            pierceCap = -1;

            frontColor = Color.valueOf("FFB6C1");
            backColor  = Color.valueOf("FFB6C1");
            lightColor = Color.valueOf("FFB6C1");

            fragBullets = 2;
            fragRandomSpread = 360f;
            fragOffsetMin = 0f;
            fragOffsetMax = 0f;
            fragBullet = new BasicBulletType(1f, 0f) {{
                width = 8f;
                height = 14f;
                lifetime = 8f;
                splashDamage = 200f;
                splashDamageRadius = 1f * 8f;
                collidesAir = true;
                collidesGround = true;
                buildingDamageMultiplier = 0.01f;
                knockback = 1f;
                status = StatusEffects.sapped;
                statusDuration = 120f;
                frontColor = Color.valueOf("FF1493");
                backColor  = Color.valueOf("FF1493");
                lightColor = Color.valueOf("FF1493");
                hitEffect = Fx.hitBulletColor;
                despawnEffect = Fx.hitBulletColor;
            }};
        }});

        // ============ 铀棒 ============
        ammoTypes.put(SepkerUraniumItem.uraniumStick, new SepkerRailBullet() {{
            speed = 40f;
            damage = 0f;
            width = 16f;
            height = 32f;
            lifetime = 105f * 8f / 40f;
            hitSize = 14f;
            buildingDamageMultiplier = 0.01f;
            splashDamage = 120360f;
            splashDamageRadius = 5f * 8f;
            ammoMultiplier = 5f;
            status = SepkerRadiationEffect.radiation;
            statusDuration = 300f;

            pierce = true;
            pierceBuilding = true;
            pierceCap = -1;

            frontColor = Color.valueOf("006400");
            backColor  = Color.valueOf("006400");
            lightColor = Color.valueOf("006400");
        }});

        buildType = new Prov<Building>() {
            @Override
            public Building get() {
                return new SepkerEMRailgun2Build();
            }
        };
    }

    public class SepkerEMRailgun2Build extends ItemTurretBuild {

        /** 脉冲圆环列表 */
        public Seq<PulseRing> pulseRings = new Seq<>();

        /** 脉冲生成计时 */
        public float pulseTimer = 0f;

        /** 第一个脉冲圆环的初始半径（格）——用于确定线速度 */
        public float firstPulseRadius = -1f;

        /** 上一个 tick 的 reloadCounter——用于检测"刚开火" */
        public float lastReloadCounter = 0f;

        public class PulseRing {
            public float radiusTiles;
            public float initialRadius;
            public float age;
            public float shrinkPerTick;   // 每 tick 缩小的格数
        }

        @Override
        public void updateTile() {
            super.updateTile();
            updatePulseRings();

            lastReloadCounter = reloadCounter;
        }

        /** 更新脉冲圆环：生成 + 收缩 + 清理 */
        public void updatePulseRings() {
            // ⭐ 检测新一轮装填（reloadCounter 从接近 0 开始）
            if (lastReloadCounter < reload * 0.5f && reloadCounter < lastReloadCounter) {
                // 刚开火，清空脉冲
                pulseRings.clear();
                pulseTimer = 0f;
                firstPulseRadius = -1f;
            }

            if (reloadCounter <= 0f || reload <= 0f) {
                pulseRings.clear();
                pulseTimer = 0f;
                firstPulseRadius = -1f;
                return;
            }

            float progress = Mathf.clamp(reloadCounter / reload);

            // ⭐ 处理已有脉冲（不管进度——让它们自然结束）
            for (int i = pulseRings.size - 1; i >= 0; i--) {
                PulseRing r = pulseRings.get(i);
                r.age += Time.delta;
                r.radiusTiles -= r.shrinkPerTick * Time.delta;
                if (r.radiusTiles <= 0f) {
                    pulseRings.remove(i);
                }
            }

            // ⭐ 只在 0.1 ≤ progress < 0.8 时生成新脉冲
            if (progress < 0.1f || progress >= 0.8f) {
                pulseTimer = 0f;
                return;
            }

            // 生成新脉冲
            pulseTimer += Time.delta;
            if (pulseTimer >= pulseInterval) {
                pulseTimer -= pulseInterval;

                float mainRadiusTiles = mainRingRadius(progress);

                PulseRing r = new PulseRing();
                r.initialRadius = mainRadiusTiles;
                r.radiusTiles = mainRadiusTiles;
                r.age = 0f;

                // ⭐ 第一个脉冲决定线速度
                if (firstPulseRadius < 0f) {
                    firstPulseRadius = mainRadiusTiles;
                }
                // 每 tick 缩小的格数 = 半径 / 存活 tick
                r.shrinkPerTick = firstPulseRadius / firstPulseLife;

                pulseRings.add(r);
            }
        }

        /** 最后圆环的半径（格）——0.0-0.1 保持 10 格，0.1-0.8 从 10 → 0 */
        public float mainRingRadius(float progress) {
            if (progress < 0.1f) return ringStartTiles;
            if (progress >= 0.8f) return 0f;
            float t = (progress - 0.1f) / 0.7f;
            return Mathf.lerp(ringStartTiles, 0f, t);
        }

        /** 最后圆环的透明度——0.0-0.1 淡入，0.7-0.8 淡出 */
        public float mainRingAlpha(float progress) {
            if (progress < 0.1f) return progress / 0.1f;
            if (progress < 0.7f) return 1f;
            if (progress < 0.8f) return 1f - (progress - 0.7f) / 0.1f;
            return 0f;
        }

        @Override
        public void draw() {
            super.draw();

            if (reloadCounter <= 0f || reload <= 0f) return;
            float progress = Mathf.clamp(reloadCounter / reload);

            Draw.z(Layer.blockOver + 0.5f);

            drawMainRing(progress);
            drawPulseRings();
            drawChargeCircle(progress);

            Draw.reset();
        }

        /** 最后圆环 */
        public void drawMainRing(float progress) {
            if (progress >= 0.8f) return;

            float radiusTiles = mainRingRadius(progress);
            float alpha = mainRingAlpha(progress);

            // ⭐ 半径接近 0 或透明度接近 0 时不画
            if (radiusTiles <= 0.01f || alpha <= 0.01f) return;

            Draw.color(LOADING_COLOR);
            Draw.alpha(alpha);
            Lines.stroke(2.5f);
            Lines.circle(x, y, radiusTiles * 8f);
        }

        /** 脉冲圆环 */
        public void drawPulseRings() {
            for (PulseRing r : pulseRings) {
                if (r.radiusTiles <= 0.01f || r.initialRadius <= 0f) continue;

                // 透明度 = 当前半径 / 初始半径
                float alpha = r.radiusTiles / r.initialRadius * 0.85f;
                if (alpha <= 0.01f) continue;

                Draw.color(LOADING_COLOR);
                Draw.alpha(alpha);
                Lines.stroke(2f);
                Lines.circle(x, y, r.radiusTiles * 8f);
            }
        }

        /** 实心圆：0.8 → 1.5 格（0.8-0.9），然后移动（0.9-1.0） */
        public void drawChargeCircle(float progress) {
            if (progress < 0.8f) return;

            float radiusTiles;
            float distWorld;

            if (progress < 0.9f) {
                float t = (progress - 0.8f) / 0.1f;
                radiusTiles = Mathf.lerp(0f, 1.5f, t);   // ⭐ 从 0 开始
                distWorld = 0f;
            } else {
                float t = Mathf.clamp((progress - 0.9f) / 0.1f);
                radiusTiles = 1.5f;
                distWorld = chargeTravelTiles * 8f * t;
            }

            // ⭐ 无目标时 progress 停在 1.0 → 实心圆保持在发射口
            float tx = x + Angles.trnsx(rotation, distWorld);
            float ty = y + Angles.trnsy(rotation, distWorld);

            Draw.color(LOADING_COLOR);
            Draw.alpha(1f);
            Fill.circle(tx, ty, radiusTiles * 8f);

            Draw.color(Color.white);
            Draw.alpha(0.8f);
            Fill.circle(tx, ty, radiusTiles * 8f * 0.5f);
        }
    }
}