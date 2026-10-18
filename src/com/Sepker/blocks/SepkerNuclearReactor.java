package com.sepker.blocks;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.type.Category;
import mindustry.world.blocks.power.ConsumeGenerator;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.entities.Units;
import mindustry.entities.Damage;
import mindustry.entities.effect.ExplosionEffect;
import arc.graphics.Color;
import arc.util.Time;
import arc.struct.ObjectSet;
import arc.func.Prov;
import arc.func.Cons;

public class SepkerNuclearReactor extends ConsumeGenerator {

    public static ExplosionEffect nuclearExplosion;

    public SepkerNuclearReactor(String name) {
        super(name);

        
        
        category = Category.power;
        buildVisibility = BuildVisibility.shown;

        if (databaseTabs == null) databaseTabs = new ObjectSet<>();
        databaseTabs.add(SepkerPlanet.sepker);

        health = 1500;
        size = 5;
        buildTime = 60f * 50f;
        liquidCapacity = 380f;
        itemCapacity = 20;

        powerProduction = 1980000f / 60f;

        consumePower(2500f / 60f);
        consumeItem(SepkerUraniumItem.uraniumStick, 2);
        consumeLiquid(Liquids.water, 140f / 60f);
        consumeLiquid(Liquids.cryofluid, 60f / 60f);
        outputLiquid = new LiquidStack(SepkerSteam.steam, 140f / 60f);

        requirements = ItemStack.with(
            SepkerUraniumItem.uranium, 1000,
            Items.copper, 5000,
            Items.titanium, 5000,
            Items.thorium, 5000,
            Items.silicon, 5000,
            Items.carbide, 3500,
            Items.oxide, 3500,
            Items.metaglass, 5000,
            Items.beryllium, 5000,
            Items.surgeAlloy, 3500
        );

        explosionRadius = 20;
        explosionDamage = 10000;

        nuclearExplosion = new ExplosionEffect(){{
            lifetime = 50f; waveStroke = 8f; waveLife = 10f; waveColor = Color.valueOf("2E8B57"); 
            sparkColor = Color.valueOf("006400"); smokeColor = Color.valueOf("228B22"); 
            sparkStroke = 2f; sparkLen = 20f; sparks = 30; smokeSize = 7f; smokes = 50; smokeRad = 40f;
        }};

        buildType = new Prov<Building>() {
            @Override
            public Building get() {
                return new NuclearReactorBuild();
            }
        };
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.warmupTime, 150f, StatUnit.seconds);
    }

    public class NuclearReactorBuild extends ConsumeGeneratorBuild {
        public boolean exploded = false;
        public float counterFast = 0f, counterNormal = 0f, counterSlow = 0f;
        public int mode = 1;
        public float itemTimer = 0f;

        public float getCurrentCounter() {
            if (mode == 0) return counterFast;
            if (mode == 1) return counterNormal;
            return counterSlow;
        }

        public void setCurrentCounter(float value) {
            if (mode == 0) counterFast = value;
            else if (mode == 1) counterNormal = value;
            else counterSlow = value;
        }

        public float getCurrentLimit() {
            if (mode == 0) return 75f;
            if (mode == 1) return 150f;
            return 300f;
        }

        public float getCurrentMaxOutput() {
            if (mode == 0) return 4000000f / 60f;
            if (mode == 1) return 1980000f / 60f;
            return 940000f / 60f;
        }

        @Override
        public float getPowerProduction() {
            return getCurrentMaxOutput() * (getCurrentCounter() / getCurrentLimit());
        }

        @Override
        public void update() {
            // 1. 手动从电网扣电
            float powerNeeded = 2500f / 60f;
            float powerUsed = 0f;
            if (power.graph != null) {
                powerUsed = power.graph.useBatteries(powerNeeded);
            }
            power.status = powerNeeded > 0 ? powerUsed / powerNeeded : 1f;

            // 2. 读取资源状态
            boolean hasUranium = items.has(SepkerUraniumItem.uraniumStick, 1);
            boolean hasCryo = liquids.get(Liquids.cryofluid) > 0.1f;
            float waterRatio = liquids.get(Liquids.water) / liquidCapacity;

            // 3. 根据水量确定模式
            float runSpeed;
            if (waterRatio >= 0.5f) { mode = 0; runSpeed = 2f; }
            else if (waterRatio > 0.01f) { mode = 1; runSpeed = 1f; }
            else { mode = 2; runSpeed = 0.5f; }

            float limit = getCurrentLimit();
            float counter = getCurrentCounter();

            // 4. 危险检测
            if (counter >= limit * 0.5f && !hasCryo && !exploded) {
                explodeAndDestroy();
                return;
            }

            // 5. 手动消耗铀棒
            if (hasUranium && hasCryo) {
                itemTimer += Time.delta * runSpeed;
                while (itemTimer >= 30f && items.has(SepkerUraniumItem.uraniumStick, 1)) {
                    items.remove(SepkerUraniumItem.uraniumStick, 1);
                    itemTimer -= 30f;
                }
            } else {
                itemTimer = 0f;
            }

            // 6. 手动消耗液体并产出蒸汽
            if (hasUranium && hasCryo) {
                liquids.remove(Liquids.water, (140f / 60f) * runSpeed * Time.delta);
                liquids.remove(Liquids.cryofluid, (60f / 60f) * runSpeed * Time.delta);
                liquids.add(SepkerSteam.steam, (140f / 60f) * runSpeed * Time.delta);
                dumpLiquid(SepkerSteam.steam);
            }

            // 7. 计数器更新
            float newCounter;
            if (hasUranium && hasCryo) {
                newCounter = Math.min(counter + Time.delta / 60f * runSpeed, limit);
            } else {
                newCounter = Math.max(counter - Time.delta / 60f * runSpeed * 2f, 0f);
            }
            setCurrentCounter(newCounter);

            // 8. efficiency 用于 UI 显示
            efficiency = 1f;
        }

        @Override
        public void onDestroyed() {
            super.onDestroyed();
            if (getCurrentCounter() >= getCurrentLimit() * 0.5f) {
                explodeAndDestroy();
            }
        }

        public void explodeAndDestroy() {
            if (exploded) return;
            exploded = true;

            float blastRadius = 20f * 8f;
            float blastDamage = 10000f;

            Damage.damage(team, x, y, blastRadius, blastDamage);

            Units.nearby(team, x, y, blastRadius, new Cons<Unit>() {
                @Override
                public void get(Unit unit) {
                    unit.apply(SepkerRadiationEffect.radiation, 600f);
                }
            });

            nuclearExplosion.at(x, y);
            kill();
        }
    }
}