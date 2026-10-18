package com.sepker.bullets;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.Vars;
import arc.util.Time;
import arc.Core;
import arc.math.Mathf;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;

public class SepkerAntiAirMissile extends BasicBulletType {

    public float climbDistance = 3f * 8f;
    public float climbSpeed = 12f;

    public float cruiseSpeed = 60f;
    public float yOffset = 1.5f * 8f;
    public float maxChaseDistance = 100f * 8f;

    public float hitRange = 16f;
    public float leadTime = 0.15f;

    // ⭐ 以炮塔为中心的重锁半径
    public float unitLockRange = 60f * 8f;    // 空中单位：50 格
    public float bulletLockRange = 100f * 8f; // 敌方子弹：100 格

    public static final String SPRITE_BASE = "ballistic-missile";
    public static final float BASE_HIT_SIZE = 1.5f * 8f;

    public float startScale = 0.25f;
    public float climbEndScale = 0.5f;
    public float trackEndScale = 1f;

    public SepkerAntiAirMissile() {
        super(0f, 0f);
        this.speed = 0f;
        this.lifetime = 60 * 15;
        this.keepVelocity = false;
        this.hitSize = BASE_HIT_SIZE;
        this.sprite = SPRITE_BASE;

        this.damage = 500f;
        this.pierceArmor = true;
        this.armorMultiplier = 1f;

        this.collidesAir = true;
        this.collidesGround = false;
        this.collidesTiles = false;
        this.collides = true;

        this.lightColor = null;
        this.lightOpacity = 0f;
        this.lightRadius = 0f;

        this.frontColor = Color.white;
        this.backColor = Color.white;
    }

    private String getAtlasName(String baseName) {
        String modName = Vars.mods.getMod(SepkerMod.class).meta.name;
        StringBuilder sb = new StringBuilder(modName);
        sb.append('-').append(baseName);
        return sb.toString();
    }

    @Override
    public void load() {
        super.load();
        frontRegion = Core.atlas.find(getAtlasName(SPRITE_BASE));
    }

    @Override
    public void drawLight(Bullet b) { /* 关闭光晕 */ }

    public static class AntiAirData {
        public int phase = 0;
        public float climbStartY;
        public float turretX, turretY;   // ⭐ 发射炮塔的位置
        public float trackProgress = 0f;
    }

    @Override
    public void init(Bullet b) {
        super.init(b);
        AntiAirData data = new AntiAirData();
        data.climbStartY = b.y;
        data.phase = 0;
        data.trackProgress = 0f;

        // ⭐ 记录发射炮塔位置，重锁时以它为中心
        boolean found = false;
        if (b.owner instanceof mindustry.world.blocks.defense.turrets.Turret.TurretBuild) {
            mindustry.world.blocks.defense.turrets.Turret.TurretBuild tb =
                (mindustry.world.blocks.defense.turrets.Turret.TurretBuild) b.owner;
            data.turretX = tb.x;
            data.turretY = tb.y;
            found = true;
        }
        if (!found) {
            // 没有炮塔（手动发射等），用导弹初始位置
            data.turretX = b.x;
            data.turretY = b.y;
        }

        b.data = data;
        b.vel.setZero();
    }

    @Override
    public void update(Bullet b) {
        if (!(b.data instanceof AntiAirData)) return;
        AntiAirData data = (AntiAirData) b.data;
        float dt = Time.delta / 60f;
        b.vel.setZero();

        switch (data.phase) {
            case 0: {
                b.y += climbSpeed * 8f * dt;
                b.rotation(90f);
                if (b.y - data.climbStartY >= climbDistance) {
                    b.y = data.climbStartY + climbDistance;
                    data.phase = 1;
                }
                break;
            }
            case 1: {
                data.trackProgress = Math.min(data.trackProgress + dt / 2f, 1f);

                // ⭐ 超距自爆：以炮塔为中心
                float odx = b.x - data.turretX;
                float ody = b.y - data.turretY;
                if (Math.sqrt(odx * odx + ody * ody) > maxChaseDistance) {
                    b.type.hitEffect.at(b.x, b.y);
                    b.remove();
                    return;
                }

                // ⭐ 重锁：以炮塔为中心搜索
                LockedTarget lock = findClosestTarget(b, data);

                float tx, ty, tvx = 0f, tvy = 0f;
                boolean hasTarget = lock != null;

                if (hasTarget) {
                    tx = lock.x;
                    ty = lock.y;
                    tvx = lock.vx;
                    tvy = lock.vy;
                } else {
                    tx = b.aimX;
                    ty = b.aimY;
                }

                float dx = tx - b.x;
                float dy = ty - b.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                // 命中判定
                if (dist <= hitRange) {
                    if (hasTarget) lock.applyDamage(damage);
                    b.x = tx;
                    b.y = ty;
                    b.type.hitEffect.at(b.x, b.y);
                    b.remove();
                    return;
                }

                // 预判未来位置
                float leadFrames = leadTime * 60f;
                float leadX = tx + tvx * leadFrames;
                float leadY = ty + tvy * leadFrames;

                float ldx = leadX - b.x;
                float ldy = leadY - b.y;
                float ldist = (float) Math.sqrt(ldx * ldx + ldy * ldy);
                float move = cruiseSpeed * 8f * dt;

                if (ldist > 1f) {
                    b.x += ldx / ldist * move;
                    b.y += ldy / ldist * move;
                    b.rotation((float) Math.toDegrees(Math.atan2(ldy, ldx)));
                }
                break;
            }
        }
    }

    /**
     * ⭐ 以炮塔位置为中心搜索最近目标：
     *   - 空中单位：50 格
     *   - 敌方子弹：100 格
     */
    private LockedTarget findClosestTarget(Bullet self, AntiAirData data) {
        LockedTarget best = null;
        float bestDstSq = Float.MAX_VALUE;

        // 1. 空中单位（50 格，以炮塔为中心）
        float unitRangeSq = unitLockRange * unitLockRange;
        for (int i = 0; i < Groups.unit.size(); i++) {
            Unit u = Groups.unit.index(i);
            if (u == null || !u.isValid() || u.dead) continue;
            if (u.team == self.team) continue;
            if (!u.isFlying() && !u.type.flying) continue;

            float dx = u.x - data.turretX;
            float dy = u.y - data.turretY;
            float dstSq = dx * dx + dy * dy;
            if (dstSq > unitRangeSq) continue;

            if (dstSq < bestDstSq) {
                bestDstSq = dstSq;
                best = new LockedTarget(u.x, u.y, u.vel.x, u.vel.y, u);
            }
        }

        // 2. 敌方子弹（100 格，以炮塔为中心）
        float bulletRangeSq = bulletLockRange * bulletLockRange;
        for (int i = 0; i < Groups.bullet.size(); i++) {
            Bullet other = Groups.bullet.index(i);
            if (other == null || other == self) continue;
            if (other.team == self.team) continue;
            if (other.type == null) continue;
            if (other.owner instanceof Bullet) continue;

            // 跳过阶段 2 的垂发导弹
            if (other.type instanceof SepkerBallisticMissile
                && other.data instanceof SepkerBallisticMissile.MissileData) {
                SepkerBallisticMissile.MissileData md =
                    (SepkerBallisticMissile.MissileData) other.data;
                if (md.phase == 2) continue;
            }

            float dx = other.x - data.turretX;
            float dy = other.y - data.turretY;
            float dstSq = dx * dx + dy * dy;
            if (dstSq > bulletRangeSq) continue;

            if (dstSq < bestDstSq) {
                bestDstSq = dstSq;
                float vx = Mathf.cosDeg(other.rotation());
                float vy = Mathf.sinDeg(other.rotation());
                best = new LockedTarget(other.x, other.y, vx, vy, other);
            }
        }

        return best;
    }

    private static class LockedTarget {
        public float x, y, vx, vy;
        public Unit unit;
        public Bullet bullet;

        LockedTarget(float x, float y, float vx, float vy, Unit u) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.unit = u;
        }

        LockedTarget(float x, float y, float vx, float vy, Bullet b) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.bullet = b;
        }

        public void applyDamage(float damage) {
            if (unit != null && unit.isValid() && !unit.dead) unit.damage(damage);
            if (bullet != null && bullet.isAdded()) bullet.remove();
        }
    }

    @Override
    public void draw(Bullet b) {
        if (frontRegion == null || !frontRegion.found()) { super.draw(b); return; }
        if (!(b.data instanceof AntiAirData)) return;
        AntiAirData data = (AntiAirData) b.data;

        float finalScale;
        if (data.phase == 0) {
            float progress = Math.min((b.y - data.climbStartY) / climbDistance, 1f);
            finalScale = startScale + (climbEndScale - startScale) * progress;
        } else {
            finalScale = climbEndScale + (trackEndScale - climbEndScale) * data.trackProgress;
        }

        Draw.color(Color.white);
        Draw.rect(frontRegion,
            b.x, b.y + yOffset,
            frontRegion.width * finalScale,
            frontRegion.height * finalScale,
            b.rotation() - 90f);
        Draw.reset();
    }
}