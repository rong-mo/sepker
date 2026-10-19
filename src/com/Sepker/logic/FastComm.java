package com.sepker.logic;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ui.Styles;

/**
 * 通信特效（fastmessage 的 comm 类型）。
 *
 * 画面：
 *   - 屏幕顶部 / 底部各一条约 1cm 的带子，带子里是 45° 斜线，一直朝左右滚动
 *     （滚动速度 = 条纹间距 / 0.5 秒，看起来就是每 0.5 秒复制一条并移动）；
 *   - 顶 / 底各压一条直线封边，3 秒内一直存在；
 *   - 0~1 秒：> 从左往右、< 从右往左滑到各自位置；
 *   - 1~2 秒：<> 变暗，中间淡入「通讯开始」/「通讯终止」；
 *   - 2~3 秒：<> 反向滑回并整体淡出。
 *
 * 终止版本除颜色固定为浅红、文字改为「通讯终止」外，动画完全一样。
 */
public class FastComm {

    /** 当前正在播放的通信特效；null = 没有 */
    public static CommTable current;
    public static int tokenSeq;

    /** @return 本次动画的 token；同一类型正在播放时返回它自己的 token */
    public static int show(boolean stop, Color color) {
        // 场景被清空（退出地图）后不要拿着一张已经脱离场景的旧表做去重
        if (current != null && current.parent == null) {
            current = null;
        }
        if (current != null && current.stop == stop) {
            return current.token;
        }
        clear();
        tokenSeq++;
        current = new CommTable(stop, color, tokenSeq);
        Core.scene.add(current);
        return tokenSeq;
    }

    public static boolean isPlaying(int token) {
        return current != null && current.parent != null && current.token == token;
    }

    public static void clear() {
        if (current != null) {
            CommTable t = current;
            current = null;
            t.remove();
        }
    }

    public static class CommTable extends Table {

        /** true = 通讯终止（浅红），false = 通讯开始（自定义颜色） */
        public boolean stop;
        public int token;
        public final Color color = new Color();
        public float time;
        public float alpha;
        public Label chevR, chevL, title;

        public CommTable(boolean stop, Color color, int token) {
            this.stop = stop;
            this.token = token;
            this.color.set(stop ? Color.valueOf("ff8a8a") : color);

            setFillParent(true);
            touchable = Touchable.disabled;

            chevR = new Label(">", Styles.outlineLabel);
            chevL = new Label("<", Styles.outlineLabel);
            title = new Label("", Styles.outlineLabel);

            chevR.setFontScale(2.4f);
            chevL.setFontScale(2.4f);
            title.setFontScale(1.35f);

            title.setText(stop ? Core.bundle.get("sepker.comm.stop", "通讯终止")
                               : Core.bundle.get("sepker.comm.start", "通讯开始"));

            addChild(chevR);
            addChild(chevL);
            addChild(title);

            update(new Runnable() {
                @Override
                public void run() {
                    tick();
                }
            });
        }

        public void removeSelf() {
            if (FastComm.current == this) {
                FastComm.current = null;
            }
            remove();
        }

        public void tick() {
            time += Time.delta;

            // 0~0.15 秒淡入；2~3 秒一边反向滑回一边淡出（对齐「2~3秒反向操作且淡出」）
            float in = Mathf.clamp(time / 0.15f);
            float out = time <= 2f ? 1f : Mathf.clamp((3f - time) / 1f);
            alpha = in * out;

            float w = getWidth();
            float h = getHeight();
            if (w <= 0f || h <= 0f) {
                return;
            }

            float p = Mathf.clamp(time, 0f, 1f);
            float q = Mathf.clamp(time - 2f, 0f, 1f);

            // > 从左往右；< 从右往左
            float xR = Mathf.lerp(-50f, w * 0.86f, p);
            float xL = Mathf.lerp(w + 30f, w * 0.10f, p);

            // 2~3 秒反向滑回
            if (q > 0f) {
                xR = Mathf.lerp(w * 0.86f, -50f, q);
                xL = Mathf.lerp(w * 0.10f, w + 30f, q);
            }

            chevR.pack();
            chevL.pack();
            title.pack();

            chevR.setPosition(xR, h * 0.5f - chevR.getHeight() * 0.5f);
            chevL.setPosition(xL, h * 0.5f - chevL.getHeight() * 0.5f);
            title.setPosition(w * 0.5f - title.getWidth() * 0.5f, h * 0.5f - title.getHeight() * 0.5f);

            float chevA = alpha * (time <= 1f ? 1f : Mathf.lerp(1f, 0.45f, Mathf.clamp(time - 1f, 0f, 1f)));
            float titleA = alpha * Mathf.clamp(time - 1f, 0f, 1f);

            chevR.setColor(color.r, color.g, color.b, chevA);
            chevL.setColor(color.r, color.g, color.b, chevA);
            title.setColor(color.r, color.g, color.b, titleA);

            if (time >= 3.05f) {
                removeSelf();
                return;
            }

            // 只在地图菜单里回收；HUD 被隐藏（过场）时特效要继续播完，否则阻塞会提前解除
            if (Vars.state.isMenu()) {
                removeSelf();
            }
        }

        @Override
        public void draw() {
            float w = getWidth();
            float h = getHeight();
            float band = Math.max(18f, h * 0.045f);

            drawBand(0f, h - band, w, band, -1f);
            drawBand(0f, 0f, w, band, 1f);

            super.draw();
        }

        /** dir = -1 顶部（线条向左滚），dir = 1 底部（线条向右滚） */
        public void drawBand(float x, float y, float w, float h, float dir) {
            if (alpha <= 0.001f) {
                return;
            }

            float spacing = Math.max(10f, h * 0.9f);
            float offset = (Time.time * spacing / 0.5f) % spacing;

            Draw.color(color.r, color.g, color.b, alpha * 0.9f);
            Lines.stroke(Math.max(1.5f, h * 0.06f));

            float start = -h - spacing + (dir > 0f ? offset : -offset);
            for (float sx = start; sx < w + spacing; sx += spacing) {
                Lines.line(x + sx, y, x + sx + h, y + h);
            }

            // 封顶 / 封底的直线
            Lines.stroke(Math.max(2f, h * 0.05f));
            float ey = dir > 0f ? y : y + h;
            Lines.line(x, ey, x + w, ey);

            Draw.reset();
        }
    }
}
