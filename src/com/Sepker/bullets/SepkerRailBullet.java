package com.sepker.bullets;

import com.sepker.SepkerFx;
import com.sepker.SepkerMod;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Bullet;
import mindustry.Vars;
import arc.Core;
import arc.graphics.Color;
import arc.math.Angles;
import arc.math.Mathf;

public class SepkerRailBullet extends BasicBulletType {

    public SepkerRailBullet() {
        super(15f, 300f);

        sprite = "sepker-rail-bullet";
        width = 8f;
        height = 16f;
        lifetime = 60f;
        hitSize = 6f;

        pierce = true;
        pierceCap = -1;
        pierceBuilding = true;
        buildingDamageMultiplier = 0.5f;
        pierceDamageFactor = 0.1f;

        collidesAir = true;
        collidesGround = true;
        collidesTiles = true;

        shootEffect   = SepkerFx.ELMshoot;
        hitEffect     = SepkerFx.ELMhit;
        despawnEffect = mindustry.content.Fx.none;

        trailEffect   = SepkerFx.ELMtrajectory;
        trailChance   = 0.6f;
        trailRotation = true;
        trailInterval = 1f;

        frontColor = Color.valueOf("87CEFA");
        backColor  = Color.valueOf("4A9BD9");
        lightColor = Color.valueOf("87CEFA");
        lightOpacity = 0.8f;
        lightRadius = 30f;
    }

    @Override
    public void load() {
        super.load();

        String modName = Vars.mods.getMod(SepkerMod.class).meta.name;

        StringBuilder sb = new StringBuilder(modName);
        sb.append('-').append(sprite);
        frontRegion = Core.atlas.find(sb.toString());

        StringBuilder sb2 = new StringBuilder(modName);
        sb2.append('-').append(sprite).append("-back");
        backRegion = Core.atlas.find(sb2.toString());
    }

    /** ⭐ 重写：把速度通过 data 传给 Effect */
    @Override
    public void updateTrailEffects(Bullet b) {
        boolean canSpawn = trailMinVelocity <= 0f
            || b.vel.len2() >= trailMinVelocity * trailMinVelocity;
        float spd = b.vel.len();

        if (trailChance > 0 && canSpawn) {
            if (Mathf.chanceDelta(trailChance)) {
                spawnTrail(b, spd);
            }
        }

        if (trailInterval > 0f && canSpawn) {
            if (b.timer(0, trailInterval)) {
                spawnTrail(b, spd);
            }
        }
    }

    private void spawnTrail(Bullet b, float spd) {
        float ox = 0f, oy = 0f;
        if (trailSpread > 0) {
            float ang = Mathf.random(360f);
            float r = Mathf.random(trailSpread);
            ox = Angles.trnsx(ang, r);
            oy = Angles.trnsy(ang, r);
        }

        float rot = trailRotation ? b.rotation() : trailParam;
        // ⭐ 5 参数版本：传速度到 data
        trailEffect.at(b.x + ox, b.y + oy, rot, trailColor, Float.valueOf(spd));
    }
}