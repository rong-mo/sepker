package com.sepker;

import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import mindustry.entities.Effect;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;

public class SepkerFx {

    public static Effect ELMtrajectory;
    public static Effect ELMshoot;
    public static Effect ELMhit;

    public static final float TRAIL_FORWARD_OFFSET = 4f;
    public static final float TRAIL_HALF_LEN_BASE = 2f;
    public static final float TRAIL_SPEED_FACTOR = 0.8f;
    public static final float RAMP_FRACTION = 0.2f;

    public static float rampFactor(float fin) {
        if (fin < RAMP_FRACTION) {
            return fin / RAMP_FRACTION;
        } else if (fin > 1f - RAMP_FRACTION) {
            return (1f - fin) / RAMP_FRACTION;
        } else {
            return 1f;
        }
    }

    public static void load() {
        ELMtrajectory = new Effect(14f, new Cons<Effect.EffectContainer>() {
            @Override
            public void get(Effect.EffectContainer e) {
                float cx = e.x + Angles.trnsx(e.rotation, TRAIL_FORWARD_OFFSET);
                float cy = e.y + Angles.trnsy(e.rotation, TRAIL_FORWARD_OFFSET);

                // ⭐ factor 是函数峰值：0 → 1 → 0
                float factor = rampFactor(e.fin());
                float spd = (e.data instanceof Float) ? (Float) e.data : 0f;
                float dynamicHalfLen = TRAIL_HALF_LEN_BASE + spd * TRAIL_SPEED_FACTOR;

                // ⭐ 所有绘制的 alpha 都乘上 factor——首尾淡化，峰值最亮
                float alpha = e.fout() * factor;
                float stroke = 1.5f * e.fout() * factor;

                Draw.color(Color.valueOf("87CEFA"));
                Draw.alpha(alpha);

                // 左右两条竖线
                Lines.stroke(stroke);
                for (int s : Mathf.signs) {
                    float ox = Angles.trnsx(e.rotation + 90f * s, 8f);
                    float oy = Angles.trnsy(e.rotation + 90f * s, 8f);

                    float halfLen = dynamicHalfLen * factor;
                    float dx = Angles.trnsx(e.rotation, halfLen);
                    float dy = Angles.trnsy(e.rotation, halfLen);

                    Lines.line(
                        cx + ox - dx, cy + oy - dy,
                        cx + ox + dx, cy + oy + dy
                    );
                }

                // 中间空心三角形
                Lines.stroke(stroke);
                Lines.poly(cx, cy, 3, 6f * factor, e.rotation);

                // 光晕
                Drawf.light(cx, cy, 45f * e.fout() * factor,
                    Color.valueOf("87CEFA"), 0.6f);

                Draw.reset();
            }
        }).layer(Layer.bullet - 1f);

        ELMshoot = new Effect(10f, new Cons<Effect.EffectContainer>() {
            @Override
            public void get(Effect.EffectContainer e) {
                Draw.color(Color.valueOf("87CEFA"));
                for (int i : Mathf.signs) {
                    Drawf.tri(e.x, e.y, 4f * e.fout(), 25f, e.rotation + 90f * i);
                }
                Drawf.light(e.x, e.y, 50f * e.fout(),
                    Color.valueOf("87CEFA"), 0.7f);
                Draw.reset();
            }
        }).layer(Layer.bullet - 0.5f);

        ELMhit = new Effect(12f, new Cons<Effect.EffectContainer>() {
            @Override
            public void get(Effect.EffectContainer e) {
                Draw.color(Color.valueOf("87CEFA"));
                Draw.alpha(1f - e.fin());
                Lines.stroke((1f - e.fin()) * 2f);
                Lines.circle(e.x, e.y, e.fin() * 20f);
                Drawf.light(e.x, e.y, 40f * (1f - e.fin()),
                    Color.valueOf("87CEFA"), 0.7f);
                Draw.reset();
            }
        });
    }
}