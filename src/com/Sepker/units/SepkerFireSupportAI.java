package com.sepker.units;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.ai.types.FlyingAI;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Bullet;
import mindustry.gen.Unit;
import mindustry.type.Weapon;
import arc.math.Angles;
import arc.util.Time;

public class SepkerFireSupportAI extends FlyingAI {

    /** 独立冷却 */
    public float gunCooldown = 0f;
    public float bombCooldown = 0f;

    public static final float GUN_INTERVAL = 12f;   // 5/秒
    public static final float BOMB_INTERVAL = 30f;  // 2/秒

    @Override
    public void updateUnit() {
        Unit unit = unit();
        if (unit == null) return;

        // 1. 直线飞行
        unit.vel.set(
            Angles.trnsx(unit.rotation, unit.type.speed),
            Angles.trnsy(unit.rotation, unit.type.speed)
        );

        // 2. 朝前方瞄准
        unit.aim(unit.x + Angles.trnsx(unit.rotation, 1000f),
                 unit.y + Angles.trnsy(unit.rotation, 1000f));

        // 3. 手动开火（只服务端/单机）
        if (mindustry.Vars.net.client() && !mindustry.Vars.net.server()) return;
        if (unit.mounts == null) return;

        // 基础伤害（逻辑里填入的值）
        float baseDamage = 100f;
        if (unit instanceof SepkerFireSupportUnit) {
            baseDamage = ((SepkerFireSupportUnit) unit).weaponDamage;
        }

        gunCooldown += Time.delta;
        bombCooldown += Time.delta;

        float weaponRotation = unit.rotation - 90f;

        for (int i = 0; i < unit.mounts.length; i++) {
            WeaponMount mount = unit.mounts[i];
            if (mount == null || mount.weapon == null) continue;

            Weapon weapon = mount.weapon;
            BulletType bulletType = weapon.bullet;
            if (bulletType == null) continue;

            // 判断是哪种武器
            boolean isBomb = weapon.reload >= 30f;

            // 枪口位置
            float mx = unit.x + Angles.trnsx(weaponRotation, weapon.x, weapon.y);
            float my = unit.y + Angles.trnsy(weaponRotation, weapon.x, weapon.y);

            if (isBomb) {
                if (bombCooldown >= BOMB_INTERVAL) {
                    Bullet b = bulletType.create(unit, unit.team, mx, my, unit.rotation);
                    if (b != null) {
                        // ⭐ 航弹伤害通过 b.data 传给 hit()
                        b.data = Float.valueOf(baseDamage * 2f);
                        b.add();
                    }
                }
            } else {
                if (gunCooldown >= GUN_INTERVAL) {
                    // ⭐ 只创建主子弹，分裂子弹由原版 fragBullets 机制自动生成
                    Bullet main = bulletType.create(unit, unit.team, mx, my, unit.rotation);
                    if (main != null) {
                        main.damage = baseDamage * 1f;
                        // ⭐ b.data 携带 baseDamage，供 hit() 里设置分裂子弹伤害
                        main.data = Float.valueOf(baseDamage);
                        main.add();
                    }
                }
            }
        }

        // 统一清冷却
        if (gunCooldown >= GUN_INTERVAL) gunCooldown -= GUN_INTERVAL;
        if (bombCooldown >= BOMB_INTERVAL) bombCooldown -= BOMB_INTERVAL;
    }
}