package com.sepker.units;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.units.UnitController;
import mindustry.game.Team;
import mindustry.gen.Bullet;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import arc.func.Func;
import arc.func.Prov;
import arc.graphics.Color;
import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Time;

public class SepkerFireSupport {

    public static UnitType supportJet;
    public static BasicBulletType shell;

    public static class ShellData {
        public float damage;
        public float radius;
    }

    public static void load() {
        // ================= 火力支援机 =================
        supportJet = new UnitType("sepker-fire-support-jet") {{
            
            flying = true;
            omniMovement = false;
            rotateSpeed = 0f;
            speed = 50f / 7.5f;
            

            // ⭐ 5000 血 / 24 护甲 —— 铀弹 2 发击落
            health = 5000f;
            armor = 24f;

            hitSize = 6f * 6f;
            itemCapacity = 0;
            useUnitCap = false;
            isEnemy = false;

            // 不被普通防空索敌，但可被绕过索敌的防空击中
            targetable = false;
            hittable = true;
            killable = true;

            createWreck = false;
            createScorch = false;
            hidden = true;
            hideDatabase = true;
            drawCell = false;
            outlines = false;
            bounded = false;
            alwaysShootWhenMoving = true;

            aiController = new Prov<UnitController>() {
                @Override
                public UnitController get() {
                    return new SepkerFireSupportAI();
                }
            };
            controller = new Func<Unit, UnitController>() {
                @Override
                public UnitController get(Unit u) {
                    return new SepkerFireSupportAI();
                }
            };

            // ================= 武器 1：机炮 =================
weapons.add(new Weapon("sepker-fire-support-jet-w1") {{
    x = 8f;
    y = 0f;
    reload = 12f;
    mirror = true;
    shootCone = 15f;
    rotate = false;
    range = 40f * 8f;
    targetAir = true;
    targetGround = true;
    bullet = new BasicBulletType(8f, 100f) {
        {
            sprite = "bullet";
            width = 6f;
            height = 10f;
            lifetime = 60f;
            collidesAir = true;
            collidesGround = true;
            hitEffect = Fx.hitBulletColor;
            despawnEffect = Fx.hitBulletColor;

            // ⭐ 用原版机制：命中时从命中点生成 2 个分裂子弹
            fragBullets = 2;
            fragBullet = new BasicBulletType(6f, 50f) {{
                sprite = "bullet";
                width = 4f;
                height = 6f;
                lifetime = 30f;
                collidesAir = true;
                collidesGround = true;
                hitEffect = Fx.hitBulletColor;
                despawnEffect = Fx.hitBulletColor;
            }};
        }

        @Override
        public void hit(Bullet b, float x, float y) {
            // ⭐ 从 b.data 读 baseDamage，动态设置分裂子弹伤害
            if (b.data instanceof Float && fragBullet != null) {
                float base = (Float) b.data;
                fragBullet.damage = base * 0.5f;
            }
            super.hit(b, x, y);
        }
    };
}});

            // ================= 武器 2：航弹（自定义爆炸）=================
            weapons.add(new Weapon("sepker-fire-support-jet-w2") {{
                x = 0f;
                y = -6f;
                reload = 30f;
                mirror = false;
                rotate = false;
                shootCone = 180f;
                range = 40f * 8f;
                targetAir = true;
                targetGround = true;
                bullet = new BasicBulletType(6f, 0f) {
                    {
                        width = 12f;
                        height = 18f;
                        drag = 0.01f;
                        lifetime = 60f * 5f;
                        collidesAir = false;
                        collidesGround = true;
                        collidesTiles = true;
                        keepVelocity = false;
                        hitEffect = Fx.none;
                        despawnEffect = Fx.none;
                        splashDamage = 0f;
                        splashDamageRadius = 25f * 8f;
                    }

                    @Override
                    public void update(Bullet b) {
                        super.update(b);
                        if (b.time > 60f * 3f) {
                            hit(b, b.x, b.y);
                        }
                    }

                    @Override
                    public void hit(Bullet b, float x, float y) {
                        float dmg = 200f;
                        if (b.data instanceof Float) {
                            dmg = (Float) b.data;
                        }
                        Damage.damage(b.team, x, y, splashDamageRadius, dmg);
                        Fx.titanExplosion.at(x, y);
                        b.remove();
                    }
                };
            }});

            constructor = new Prov<Unit>() {
                @Override
                public Unit get() { return new SepkerFireSupportUnit(); }
            };
        }};

        // ================= 地面支援炮弹 =================
        shell = new BasicBulletType(8f, 0f) {
            {
                sprite = "shell";
                width = 8f;
                height = 14f;
                collidesAir = false;
                collidesGround = false;
                collidesTiles = false;
                collides = false;
                keepVelocity = false;
                lifetime = 60f * 30f;
                hitEffect = Fx.none;
                despawnEffect = Fx.none;

                trailEffect = Fx.smokeCloud;
                trailChance = 0.5f;
                trailColor = Color.valueOf("FF4500");
                trailWidth = 3f;
                trailLength = 12;

                frontColor = Color.valueOf("FF4500");
                backColor  = Color.valueOf("FF4500");
            }

            @Override
            public void update(Bullet b) {
                super.update(b);

                float dx = b.x - b.aimX;
                float dy = b.y - b.aimY;
                if (dx * dx + dy * dy < 8f * 8f) {
                    if (b.data instanceof ShellData) {
                        ShellData d = (ShellData) b.data;
                        Damage.damage(b.team, b.x, b.y, d.radius, d.damage);
                    }
                    Fx.titanExplosion.at(b.x, b.y);
                    b.remove();
                }
            }
        };

        shell.load();
    }

    /** 空中支援 */
    public static void airSupport(Team team, float x, float y, float angle, float damage) {
        if (supportJet == null) return;

        float spawnDist = 5f * 8f;
        float sx = x - Angles.trnsx(angle, spawnDist);
        float sy = y - Angles.trnsy(angle, spawnDist);

        Unit unit = supportJet.create(team);
        unit.set(sx, sy);
        unit.rotation = angle;
        unit.add();

        if (unit instanceof SepkerFireSupportUnit) {
            ((SepkerFireSupportUnit) unit).weaponDamage = damage;
        }
    }

    /** 地面支援 */
    public static void groundSupport(Team team, float x, float y, float angle,
                                     float damage, float radiusTiles) {
        final float dmg = damage;
        final float rad = radiusTiles * 8f;

        if (shell == null) return;

        float dist = distanceToEdge(x, y, angle);

        for (int i = 0; i < 15; i++) {
            final int idx = i;
            Time.run(i * 12f, new Runnable() {
                @Override
                public void run() {
                    float tx = x + Mathf.range(5f * 8f);
                    float ty = y + Mathf.range(5f * 8f);

                    float sx = x + Angles.trnsx(angle, dist + 16f) + Mathf.range(5f * 8f);
                    float sy = y + Angles.trnsy(angle, dist + 16f) + Mathf.range(5f * 8f);

                    float flyAngle = Angles.angle(sx, sy, tx, ty);

                    Bullet b = shell.create(null, team, sx, sy, flyAngle);
                    if (b != null) {
                        b.aimX = tx;
                        b.aimY = ty;

                        ShellData d = new ShellData();
                        d.damage = dmg;
                        d.radius = rad;
                        b.data = d;
                    }
                }
            });
        }
    }

    private static float distanceToEdge(float x, float y, float angle) {
        float dx = Mathf.cosDeg(angle);
        float dy = Mathf.sinDeg(angle);

        float worldW = mindustry.Vars.world.unitWidth();
        float worldH = mindustry.Vars.world.unitHeight();

        float tX = Float.MAX_VALUE;
        float tY = Float.MAX_VALUE;

        if (dx > 0.0001f) tX = (worldW - x) / dx;
        else if (dx < -0.0001f) tX = (0f - x) / dx;

        if (dy > 0.0001f) tY = (worldH - y) / dy;
        else if (dy < -0.0001f) tY = (0f - y) / dy;

        return Math.min(tX, tY);
    }
}