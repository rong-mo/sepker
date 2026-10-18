package com.sepker.logic;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.game.Team;
import mindustry.logic.LVar;
import mindustry.logic.LExecutor;
import mindustry.logic.LExecutor.LInstruction;
import arc.util.Log;

public class FireSupportI implements LInstruction {

    LVar mode, team, x, y, angle, damage, radius;

    public FireSupportI(LVar mode, LVar team, LVar x, LVar y,
                        LVar angle, LVar damage, LVar radius) {
        this.mode = mode;
        this.team = team;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.damage = damage;
        this.radius = radius;
    }

    @Override
    public void run(LExecutor exec) {
        String modeStr = asString(mode);
        Team teamObj = asTeam(team);

        if (teamObj == null) {
            Log.info("[FireSupport] team is null, skip");
            return;
        }

        // ⭐ 逻辑里的 x/y 是格数 → 世界单位 = 格数 × tilesize(8)
        float xf = (float) x.num() * 8f;
        float yf = (float) y.num() * 8f;
        float angleF = (float) angle.num();
        float dmgF = (float) damage.num();
        float radF = (float) radius.num();


        if ("air".equals(modeStr)) {
            SepkerFireSupport.airSupport(teamObj, xf, yf, angleF, dmgF);
        } else {
            SepkerFireSupport.groundSupport(teamObj, xf, yf, angleF, dmgF, radF);
        }
    }

    private String asString(LVar v) {
        if (v == null) return "null";
        if (v.isobj && v.objval != null) return v.objval.toString();
        if (v.isobj) return "null";
        return v.numval == (long) v.numval
            ? String.valueOf((long) v.numval)
            : String.valueOf(v.numval);
    }

    private Team asTeam(LVar v) {
        if (v == null) return null;
        if (v.isobj && v.objval instanceof Team) return (Team) v.objval;
        return null;
    }
}