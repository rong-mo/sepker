package com.sepker.logic;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.logic.LAssembler;
import mindustry.logic.LCategory;
import mindustry.logic.LExecutor.LInstruction;
import mindustry.logic.LStatement;
import mindustry.ui.Styles;
import arc.Core;
import arc.func.Cons;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;

public class FireSupportStatement extends LStatement {

    public String mode = "air";
    public String team = "@sharded";
    public String x = "0";
    public String y = "0";
    public String angle = "90";
    public String damage = "100";
    public String radius = "5";

    /**
     * 本地化辅助：
     *   - 逻辑本地化开启 → 走 bundle
     *   - 关闭 → 显示英文 fallback
     */
    private String loc(String key, String fallback) {
        if (Core.settings.getBool("logiclocalization", true)) {
            return Core.bundle.get(key, fallback);
        }
        return fallback;
    }

    @Override
    public void build(Table table) {
        rebuild(table);
    }

    void rebuild(Table table) {
        table.clearChildren();
        table.left();

        table.button(new Cons<Button>() {
            @Override
            public void get(Button b) {
                b.add("air".equals(mode)
                    ? loc("firesupport.air", "Air Support")
                    : loc("firesupport.ground", "Ground Support"));
            }
        }, Styles.logict, new Runnable() {
            @Override
            public void run() {
                mode = "air".equals(mode) ? "ground" : "air";
                rebuild(table);
            }
        }).size(140f, 40f).pad(4f);

        fields(table, loc("firesupport.team", "team"), team, new Cons<String>() {
            @Override public void get(String v) { team = v; }
        });
        fields(table, loc("firesupport.x", "x"), x, new Cons<String>() {
            @Override public void get(String v) { x = v; }
        });
        fields(table, loc("firesupport.y", "y"), y, new Cons<String>() {
            @Override public void get(String v) { y = v; }
        });
        fields(table, loc("firesupport.angle", "angle"), angle, new Cons<String>() {
            @Override public void get(String v) { angle = v; }
        });
        fields(table, loc("firesupport.damage", "damage"), damage, new Cons<String>() {
            @Override public void get(String v) { damage = v; }
        });

        if ("ground".equals(mode)) {
            fields(table, loc("firesupport.radius", "radius"), radius, new Cons<String>() {
                @Override public void get(String v) { radius = v; }
            });
        }
    }

    @Override
    public LInstruction build(LAssembler builder) {
        StringBuilder sb = new StringBuilder();
        sb.append('"').append(mode).append('"');

        return new FireSupportI(
            builder.var(sb.toString()),
            builder.var(team),
            builder.var(x),
            builder.var(y),
            builder.var(angle),
            builder.var(damage),
            builder.var(radius)
        );
    }

    @Override
    public void write(StringBuilder out) {
        out.append("firesupport ")
           .append('"').append(mode).append('"').append(' ')
           .append(team).append(' ')
           .append(x).append(' ')
           .append(y).append(' ')
           .append(angle).append(' ')
           .append(damage).append(' ')
           .append(radius);
    }

    @Override
    public LCategory category() {
        return LCategory.world;
    }

    @Override
    public boolean privileged() {
        return true;
    }

    @Override
    public String name() {
        return "Fire Support";
    }
}