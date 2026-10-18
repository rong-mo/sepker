package com.sepker.blocks;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.type.*;
import mindustry.type.Category;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.meta.BuildVisibility;
import mindustry.entities.effect.ExplosionEffect;
import mindustry.world.draw.DrawTurret;
import mindustry.entities.part.DrawPart;
import mindustry.entities.part.RegionPart;
import mindustry.gen.Building;
import arc.graphics.Color;
import arc.struct.ObjectSet;
import arc.func.Prov;

public class SepkerStorm extends ItemTurret {

    public static ExplosionEffect thoriumExplosion;
    public static ExplosionEffect carbideExplosion;
    public static ExplosionEffect fragExplosion;
    public static ExplosionEffect uraniumExplosion;
    public static ExplosionEffect uraniumFragExplosion;

    public SepkerStorm(String name) {
        super(name);

        
        category = Category.turret;
        buildVisibility = BuildVisibility.shown;

        if (databaseTabs == null) databaseTabs = new ObjectSet<>();
        databaseTabs.add(SepkerPlanet.sepker);

        health = 4500;
        size = 4;
        buildTime = 60f * 10f;
        liquidCapacity = 120f;
        range = 55f * 8f;
        reload = 72f;

        recoil = 4f;
        recoilTime = 8f;

        shootY = 32f;
        shootX = 0f;
        
        

        requirements = ItemStack.with(
            Items.copper, 1000,
            Items.thorium, 2000,
            Items.silicon, 2000,
            Items.titanium, 2000,
            Items.surgeAlloy, 3000,
            Items.tungsten, 1000
        );
        

        // ================= 初始化专属配色爆炸特效 =================
        thoriumExplosion = new ExplosionEffect(){{
            lifetime = 50f; waveStroke = 5f; waveLife = 8f; waveColor = Color.valueOf("FFB6C1");
            sparkColor = Color.valueOf("FFC0CB"); smokeColor = Color.valueOf("FFB6C1");
            sparkStroke = 2f; sparkLen = 20f; sparks = 30; smokeSize = 7f; smokes = 50; smokeRad = 40f;
        }};

        carbideExplosion = new ExplosionEffect(){{
            lifetime = 50f; waveStroke = 5f; waveLife = 8f; waveColor = Color.valueOf("DDA0DD");
            sparkColor = Color.valueOf("D8BFD8"); smokeColor = Color.valueOf("DDA0DD");
            sparkStroke = 2f; sparkLen = 20f; sparks = 30; smokeSize = 7f; smokes = 50; smokeRad = 40f;
        }};

        fragExplosion = new ExplosionEffect(){{
            lifetime = 30f; waveStroke = 3f; waveLife = 5f; waveColor = Color.valueOf("DDA0DD");
            sparkColor = Color.valueOf("D8BFD8"); smokeColor = Color.valueOf("DDA0DD");
            sparkStroke = 1.5f; sparkLen = 10f; sparks = 10; smokeSize = 4f; smokes = 15; smokeRad = 15f;
        }};

        uraniumExplosion = new ExplosionEffect(){{
            lifetime = 50f; waveStroke = 5f; waveLife = 8f; waveColor = Color.valueOf("2E8B57");
            sparkColor = Color.valueOf("006400"); smokeColor = Color.valueOf("228B22");
            sparkStroke = 2f; sparkLen = 20f; sparks = 30; smokeSize = 7f; smokes = 50; smokeRad = 40f;
        }};

        uraniumFragExplosion = new ExplosionEffect(){{
            lifetime = 30f; waveStroke = 3f; waveLife = 5f; waveColor = Color.valueOf("2E8B57");
            sparkColor = Color.valueOf("006400"); smokeColor = Color.valueOf("228B22");
            sparkStroke = 1.5f; sparkLen = 10f; sparks = 10; smokeSize = 4f; smokes = 15; smokeRad = 15f;
        }};

        // ================= 弹药定义 =================
        ammoTypes.put(Items.thorium, new BasicBulletType(12f, 1800f) {{
            width = 12f; height = 20f; lifetime = 55f * 8f / 12f; hitSize = 6f;
            pierce = true; pierceCap = 4; pierceBuilding = true; buildingDamageMultiplier = 0.01f;
            splashDamage = 1800f * 0.3f; splashDamageRadius = 4f * 8f; knockback = 1f;
            status = StatusEffects.none; hitEffect = thoriumExplosion; despawnEffect = thoriumExplosion;
            shootEffect = Fx.shootBig; smokeEffect = Fx.shootSmokeTitan;
            frontColor = Color.valueOf("FFC0CB"); backColor = Color.valueOf("FFB6C1"); lightColor = Color.valueOf("FFB6C1");
            collidesAir = true; collidesGround = true; scaleLife = true;
        }});

        ammoTypes.put(Items.carbide, new BasicBulletType(20f, 3500f) {{
            width = 16f; height = 24f; lifetime = 55f * 8f / 20f; hitSize = 8f;
            buildingDamageMultiplier = 0.01f; splashDamage = 3500f * 0.5f; splashDamageRadius = 6f * 8f; knockback = 3f;
            hitEffect = carbideExplosion; despawnEffect = carbideExplosion; shootEffect = Fx.shootBig; smokeEffect = Fx.shootSmokeTitan;
            frontColor = Color.valueOf("D8BFD8"); backColor = Color.valueOf("DDA0DD"); lightColor = Color.valueOf("D8BFD8");

            fragBullets = 24;
            fragBullet = new BasicBulletType(8f, 400f) {{
                width = 6f; height = 10f; lifetime = 15f; hitSize = 3f;
                buildingDamageMultiplier = 0f; splashDamage = 50f; splashDamageRadius = 3f * 8f; knockback = 0f;
                pierce = true; pierceCap = -1; hitEffect = fragExplosion; despawnEffect = fragExplosion;
                collidesAir = true; collidesGround = true;
                frontColor = Color.valueOf("D8BFD8"); backColor = Color.valueOf("DDA0DD"); lightColor = Color.valueOf("D8BFD8");
            }};
            collidesAir = true; collidesGround = true; scaleLife = true;
        }});

        ammoTypes.put(SepkerUraniumItem.uranium, new BasicBulletType(20f, 5500f) {{
            width = 18f; height = 28f; lifetime = 75f * 8f / 20f; hitSize = 10f;
            pierce = true; pierceCap = -1; pierceBuilding = true; buildingDamageMultiplier = 0.5f;
            splashDamage = 2750f; splashDamageRadius = 10f * 8f; knockback = 10f;
            status = SepkerRadiationEffect.radiation; statusDuration = 300f;
            hitEffect = uraniumExplosion; despawnEffect = uraniumExplosion; shootEffect = Fx.shootBig; smokeEffect = Fx.shootSmokeTitan;
            frontColor = Color.valueOf("006400"); backColor = Color.valueOf("228B22"); lightColor = Color.valueOf("006400");

            fragBullets = 24;
            fragBullet = new BasicBulletType(10f, 325f) {{
                width = 6f; height = 10f; lifetime = 15f; hitSize = 4f;
                buildingDamageMultiplier = 0.5f; splashDamage = 325f; splashDamageRadius = 3f * 8f; knockback = 10f;
                pierce = true; pierceCap = -1; pierceBuilding = true;
                status = SepkerRadiationEffect.radiation; statusDuration = 300f;
                frontColor = Color.valueOf("006400"); backColor = Color.valueOf("228B22"); lightColor = Color.valueOf("006400");
                hitEffect = uraniumFragExplosion; despawnEffect = uraniumFragExplosion;
                collidesAir = true; collidesGround = true;
            }};
            collidesAir = true; collidesGround = true; scaleLife = true;
        }});

        consumeCoolant(20f / 60f);

        // ⭐ 修正：使用正确的包路径，并简化 parts 的添加方式
        drawer = new DrawTurret() {{
            parts.add(new RegionPart("-weapon") {{
                progress = DrawPart.PartProgress.recoil;
                moveY = -1f;
            }});
        }};

        buildType = new Prov<Building>() {
            @Override
            public Building get() {
                return new SepkerStormBuild();
            }
        };
    }

    public class SepkerStormBuild extends ItemTurretBuild {
        // 绘制全部交给 DrawTurret
    }
}