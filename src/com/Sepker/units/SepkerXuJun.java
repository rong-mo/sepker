package com.sepker.units;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.content.StatusEffects;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.Core;
import arc.func.Prov;
import arc.math.Mathf;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;

public class SepkerXuJun {
    public static UnitType xuJun;

    public static void load() {
        xuJun = new UnitType("sepker-xu-jun") {{
            
            flying = true;
            health = 1000f;
            armor = 5f;
            hitSize = 20f;
            speed = 8f;
            buildSpeed = 3f;
            mineSpeed = 15f;
            mineTier = 150;
            itemCapacity = 350;
            payloadCapacity = 9f;
            useUnitCap = false;
            isEnemy = false;
            hideDatabase = false;

            // 武器 1：左侧
            weapons.add(new Weapon("sepker-xu-jun-weapon-left") {{
                x = -6f; 
                y = -2f;
                reload = 30f;
                range = 35f * 8f;
                bullet = new LaserBulletType() {{
                    damage = 300f;
                    healAmount = 1000f;
                    buildingDamageMultiplier = 0.01f;
                    homingPower = 0.1f;
                    homingRange = 200f;
                    status = StatusEffects.slow;
                    statusDuration = 60f;
                    colors = new Color[]{Color.valueOf("87CEFA"), Color.valueOf("E0F4FF"), Color.valueOf("4A9BD9")};
                    width = 3f;
                    length = 35f * 8f;
                    pierce = true;
                    pierceCap = -1;
                }};
            }});

            // 武器 2：右侧
            weapons.add(new Weapon("sepker-xu-jun-weapon-right") {{
                x = 6f; 
                y = -2f;
                reload = 30f;
                range = 35f * 8f;
                bullet = new LaserBulletType() {{
                    damage = 300f;
                    healAmount = 1000f;
                    buildingDamageMultiplier = 0.01f;
                    homingPower = 0.1f;
                    homingRange = 200f;
                    status = StatusEffects.slow;
                    statusDuration = 60f;
                    colors = new Color[]{Color.valueOf("87CEFA"), Color.valueOf("E0F4FF"), Color.valueOf("4A9BD9")};
                    width = 3f;
                    length = 35f * 8f;
                    pierce = true;
                    pierceCap = -1;
                }};
            }});

            constructor = new Prov<Unit>() {
                @Override
                public Unit get() {
                    return new XuJunEntity();
                }
            };
        }};
    }

    public static class XuJunEntity extends UnitEntity {
        @Override
        public void draw() {
            // 1. 在后方绘制 5 个尾部光圈
            TextureRegion glow = Core.atlas.find("evoke-team"); 
            
            if (glow != null && glow.found()) {
                Draw.color(team.color); 
                
                for (int i = 1; i <= 5; i++) {
                    float scl = 1f + (i * 0.35f); 
                    float offset = i * 4f;        
                    
                    // 修复：导入了 arc.math.Mathf，并使用 (float) 强转避免精度丢失报错
                    float rad = (float) (rotation * Mathf.PI / 180f);
                    float dx = -(float)Math.sin(rad) * offset;
                    float dy = -(float)Math.cos(rad) * offset;
                    
                    Draw.alpha(0.8f - (i * 0.12f)); 
                    Draw.rect(glow, x + dx, y + dy, glow.width * scl, glow.height * scl, rotation - 90f);
                }
                Draw.reset(); 
            }

            // 2. 绘制原版本体、武器等
            super.draw();
            
            // 3. 绘制自定义队伍色贴图
            StringBuilder sb = new StringBuilder(type().name);
            sb.append("-team");
            TextureRegion teamRegion = Core.atlas.find(sb.toString());
            if (teamRegion != null && teamRegion.found()) {
                Draw.color(team.color);
                Draw.rect(teamRegion, x, y, rotation - 90f);
                Draw.reset();
            }
        }
    }
}
