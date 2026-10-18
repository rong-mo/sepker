package com.sepker.units;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.gen.UnitEntity;
import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureAtlas.AtlasRegion;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.util.Time;

public class SepkerFireSupportUnit extends UnitEntity {

    public float weaponDamage = 100f;
    public float lifeFrames = 0f;

    @Override
    public void update() {
        super.update();

        // 强制直线飞行
        vel.set(
            Angles.trnsx(rotation, type.speed),
            Angles.trnsy(rotation, type.speed)
        );

        // 飞出地图边界 → 销毁
        float w = mindustry.Vars.world.unitWidth();
        float h = mindustry.Vars.world.unitHeight();
        if (x < 0 || y < 0 || x > w || y > h) {
            remove();
            return;
        }

        // 兜底：60 秒自动消失
        lifeFrames += Time.delta;
        if (lifeFrames > 60f * 60f) {
            remove();
        }
    }

    @Override
    public void draw() {
        super.draw();

        // -team 队伍色叠加层
        if (type.region instanceof AtlasRegion) {
            String regionName = ((AtlasRegion) type.region).name;

            StringBuilder sb = new StringBuilder(regionName);
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