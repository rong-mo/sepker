package com.sepker.bullets;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Bullet;
import mindustry.Vars;
import arc.util.Time;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;

public class SepkerBallisticMissile extends BasicBulletType {

    // 三阶段飞行参数
    public float climbDistance = 4f * 8f;
    public float climbSpeed = 3f;
    public float cruiseSpeed = 50f;
    public float descentDistance = 3f * 8f;
    public float descentSpeed = 3f;

    // 向上平移 2 格
    public float yOffset = 2f * 8f;

    public static final String SPRITE_BASE = "ballistic-missile";
    public static final float BASE_HIT_SIZE = 1.5f * 8f;
    public float drawScale = 1f;

    public SepkerBallisticMissile() {
        super(0f, 0f);
        this.speed = 0f;
        this.lifetime = 60 * 15;
        this.keepVelocity = false;
        this.hitSize = BASE_HIT_SIZE;
        this.sprite = SPRITE_BASE;

        this.collidesTiles = false;
        this.collidesGround = false;
        this.collidesAir = false;
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

    public static class MissileData {
        public int phase = 0;
        public float targetX, targetY;
        public float climbStartY;
        public float descentStartY;
    }

    @Override
    public void init(Bullet b) {
        super.init(b);
        MissileData data = new MissileData();
        data.targetX = b.aimX;
        data.targetY = b.aimY;
        data.climbStartY = b.y;
        data.phase = 0;
        b.data = data;
        b.vel.setZero();
    }

    @Override
    public void update(Bullet b) {
        if (!(b.data instanceof MissileData)) return;
        MissileData data = (MissileData) b.data;
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
                float targetY = data.targetY + climbDistance;
                float dx = data.targetX - b.x;
                float dy = targetY - b.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float move = cruiseSpeed * 8f * dt;

                if (dist <= move || dist < 1f) {
                    b.x = data.targetX;
                    b.y = targetY;
                    data.phase = 2;
                    data.descentStartY = b.y;
                } else {
                    b.x += dx / dist * move;
                    b.y += dy / dist * move;

                    // ⭐ 瞬时朝向目标
                    b.rotation((float) Math.toDegrees(Math.atan2(dy, dx)));
                }
                break;
            }
            case 2: {
                b.y -= descentSpeed * 8f * dt;
                b.rotation(270f);   // ⭐ 瞬时朝下

                if (data.descentStartY - b.y >= descentDistance) {
                    b.type.hit(b, b.x, b.y);
                    b.remove();
                }
                break;
            }
        }
    }

    @Override
    public void draw(Bullet b) {
        if (frontRegion == null || !frontRegion.found()) { super.draw(b); return; }
        if (!(b.data instanceof MissileData)) return;
        MissileData data = (MissileData) b.data;

        float baseScale = drawScale * (b.hitSize / BASE_HIT_SIZE) * 0.4f;

        float finalScale = baseScale;
        switch (data.phase) {
            case 0: {
                float progress = Math.min((b.y - data.climbStartY) / climbDistance, 1f);
                finalScale = baseScale * (1f + progress);
                break;
            }
            case 1: {
                finalScale = baseScale * 2f;
                break;
            }
            case 2: {
                float progress = Math.min((data.descentStartY - b.y) / descentDistance, 1f);
                finalScale = baseScale * (2f - progress * 1.75f);
                break;
            }
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