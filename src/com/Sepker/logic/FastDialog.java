package com.sepker.logic;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.GlyphLayout;
import arc.math.Interp;
import arc.math.Mathf;
import arc.scene.actions.Actions;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

/**
 * 常驻对话框（fastmessage 的 dialog 类型）。
 *
 * 行为（按用户 2026-10-19 的修正）：
 *   - 黑框从第一次对话/选择开始一直显示，只有「暂停」或离开地图才关闭；
 *   - 普通对话：不阻塞处理器，每执行一次就把这一句追加进队列（缓存表）；
 *     玩家点屏幕：正在打字则立刻补全，否则切到下一句缓存；
 *   - 选择：阻塞到玩家点某个选项，结果写到指令里填的逻辑变量（覆盖旧值）；
 *     选完后黑框继续显示，内容变成被选中的那一项，之后仍可点击切下一句；
 *   - 名字牌每次换行都会更新。
 */
public class FastDialog {

    public static DialogRoot current;
    public static int tokenSeq;
    /** 选择模式最近一次点中的选项（1 起，0 = 没选） */
    public static int lastChoice;

    public static void pushTalk(String name, String text) {
        if (text == null || text.length() == 0) {
            return;
        }
        ensure().push(name, text);
    }

    public static int showChoice(String name, String[] options) {
        return ensure().beginChoice(name, options);
    }

    public static boolean isWaiting(int token) {
        return current != null && current.parent != null
            && current.token == token && current.waiting;
    }

    public static void close() {
        if (current != null) {
            DialogRoot r = current;
            current = null;
            r.closeNow();
        }
    }

    public static DialogRoot ensure() {
        if (current != null && current.parent == null) {
            current = null;
        }
        if (current == null || current.closing) {
            current = new DialogRoot();
            Core.scene.add(current);
        }
        return current;
    }

    /** 一句缓存 */
    public static class Line {
        public String name;
        public String text;

        public Line(String name, String text) {
            this.name = name == null ? "" : name;
            this.text = text == null ? "" : text;
        }

        public boolean same(String n, String t) {
            return name.equals(n == null ? "" : n) && text.equals(t == null ? "" : t);
        }
    }

    public static class DialogRoot extends Table {

        public int token;
        public boolean waiting;
        public boolean choiceMode;
        public boolean closing;
        public boolean typing;
        public boolean justPicked;
        public boolean firstShow = true;

        public String speaker = "";
        public String fullText = "";
        public int revealed;
        public float acc;
        public String[] opts;

        public Seq<Line> queue = new Seq<>();

        public Table box;
        public Table plate;
        public Label plateLabel;
        public Label arrow;
        public Label label;
        public Table optionsTable;

        public float pad;
        public float textWidth;

        public DialogRoot() {
            setFillParent(true);
            touchable = Touchable.enabled;

            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onScreenClick();
                }
            });

            buildShell();
            update(new Runnable() {
                @Override
                public void run() {
                    tick();
                }
            });
        }

        public void buildShell() {
            pad = Math.max(8f, Scl.scl(10f));

            box = new Table(Styles.black8);
            box.margin(pad);
            // childrenOnly：空白处点不到黑框（落到根节点的点击监听上），但里面的选项按钮可以点
            box.touchable = Touchable.childrenOnly;
            addChild(box);

            // 左上角的小黑框（名字）
            plate = new Table(Styles.black);
            plate.margin(Math.max(3f, Scl.scl(4f)));
            plate.touchable = Touchable.disabled;
            plateLabel = new Label("", Styles.outlineLabel);
            plate.add(plateLabel).pad(2f);
            plate.pack();
            plate.visible = false;
            addChild(plate);

            // 还有下一句缓存时的小三角
            arrow = new Label("▼", Styles.outlineLabel);
            arrow.setFontScale(0.8f);
            arrow.pack();
            arrow.visible = false;
            addChild(arrow);
        }

        public void push(String name, String text) {
            if (text == null || text.length() == 0) {
                return;
            }
            String n = name == null ? "" : name;

            // 同一句在当前行重复（处理器每帧执行）时不要刷屏
            if (!choiceMode && fullText.length() > 0 && n.equals(speaker) && text.equals(fullText)) {
                return;
            }
            if (queue.size > 0 && queue.peek().same(n, text)) {
                return;
            }

            if (fullText.length() == 0 && queue.size == 0 && !choiceMode && !typing) {
                showLine(n, text);
                return;
            }

            if (queue.size < 64) {
                queue.add(new Line(n, text));
            }
        }

        public int beginChoice(String name, String[] options) {
            speaker = name == null ? "" : name;
            fullText = "";
            revealed = 0;
            acc = 0;
            typing = false;
            opts = options;
            choiceMode = true;
            waiting = true;
            FastDialog.lastChoice = 0;
            token = ++FastDialog.tokenSeq;

            plateLabel.setText(speaker);
            plate.pack();
            plate.visible = speaker.length() > 0;

            rebuildBox();
            return token;
        }

        public void showLine(String name, String text) {
            speaker = name == null ? "" : name;
            fullText = text == null ? "" : text;
            revealed = 0;
            acc = 0;
            typing = fullText.length() > 0;
            choiceMode = false;
            waiting = false;

            plateLabel.setText(speaker);
            plate.pack();
            plate.visible = speaker.length() > 0;

            arrow.visible = false;
            rebuildBox();
        }

        public void rebuildBox() {
            if (box == null) {
                return;
            }
            box.clearChildren();

            float sceneW = Core.scene.getWidth();
            if (sceneW <= 0f) {
                sceneW = Core.graphics.getWidth();
            }
            float sceneH = Core.scene.getHeight();
            if (sceneH <= 0f) {
                sceneH = Core.graphics.getHeight();
            }

            float maxTextW = sceneW * 0.74f;
            float minBoxW = sceneW * 0.52f;
            float lineH = Fonts.outline.getLineHeight();

            float textW = minBoxW - pad * 2f;
            float textH = lineH * 3f;

            if (choiceMode) {
                textW = Math.max(textW, maxTextW * 0.8f);
                optionsTable = new Table();
                optionsTable.touchable = Touchable.childrenOnly;
                if (opts != null) {
                    for (int i = 0; i < opts.length; i++) {
                        final int index = i;
                        String text = opts[i] == null ? "" : opts[i];
                        TextButton button = new TextButton(text, Styles.logict);
                        button.clicked(new Runnable() {
                            @Override
                            public void run() {
                                pick(index);
                            }
                        });
                        optionsTable.add(button).growX().height(Scl.scl(40f)).pad(3f).row();
                    }
                }
                box.add(optionsTable).width(textW).left();
                label = null;
            } else {
                if (fullText.length() > 0) {
                    GlyphLayout gl = new GlyphLayout();
                    gl.setText(Fonts.outline, fullText, Color.white, maxTextW, Align.left, true);
                    textW = Mathf.clamp(gl.width, minBoxW - pad * 2f, maxTextW);
                    textH = Math.max(gl.height, lineH * 3f);
                }
                label = new Label("", Styles.outlineLabel);
                label.setWrap(true);
                label.setAlignment(Align.topLeft);
                box.add(label).width(textW).height(textH).left();
            }

            this.textWidth = textW;
            box.pack();
            positionBox();

            if (firstShow) {
                firstShow = false;
                box.moveBy(0f, -box.getHeight());
                box.actions(Actions.moveBy(0f, box.getHeight(), 0.35f, Interp.pow2Out));
            }
        }

        public void positionBox() {
            float sceneW = Core.scene.getWidth();
            if (sceneW <= 0f) {
                sceneW = Core.graphics.getWidth();
            }
            float sceneH = Core.scene.getHeight();
            if (sceneH <= 0f) {
                sceneH = Core.graphics.getHeight();
            }
            box.setPosition((sceneW - box.getWidth()) / 2f, Math.max(Scl.scl(50f), sceneH * 0.10f));
        }

        /** 当前是否有需要玩家点击推进的东西（有空缓存 / 正在打字 / 等着选项） */
        public boolean wantsInput() {
            return choiceMode ? waiting : (typing || queue.size > 0);
        }

        public void tick() {
            if (closing || box == null) {
                return;
            }
            if (getWidth() <= 0f) {
                return;
            }

            // 只有回主菜单才自动关；HUD 隐藏（过场）时对话框按用户要求继续常驻
            if (Vars.state.isMenu()) {
                closeNow();
                return;
            }

            // 有下一步可推进时才吃点击；空闲时让点击穿透，玩家可以继续操作
            boolean want = wantsInput();
            touchable = want ? Touchable.enabled : Touchable.disabled;
            if (box != null) {
                box.touchable = want ? Touchable.childrenOnly : Touchable.disabled;
            }

            if (typing && label != null) {
                acc += Time.delta * 45f;
                int guard = 0;
                while (acc >= 1f && revealed < fullText.length() && guard < 300) {
                    revealed = nextSafe(fullText, revealed);
                    acc -= 1f;
                    guard++;
                }
                if (revealed >= fullText.length()) {
                    typing = false;
                    label.setText(fullText);
                } else {
                    label.setText(fullText.substring(0, revealed));
                }
            }

            if (plate != null && plate.visible) {
                plate.setPosition(box.x + Scl.scl(8f), box.y + box.getHeight() - plate.getHeight() * 0.5f);
            }

            if (arrow != null) {
                arrow.visible = !choiceMode && !typing && queue.size > 0;
                if (arrow.visible) {
                    arrow.setPosition(box.x + box.getWidth() - arrow.getWidth() - Scl.scl(10f), box.y + Scl.scl(4f));
                    arrow.color.a = 0.35f + 0.65f * Mathf.absin(Time.time, 0.8f, 1f);
                }
            }

            // 选项按钮点击后冒泡到根节点的那一次点击要吞掉，下一帧自动解除
            justPicked = false;
        }

        /** 从 from 开始的下一个安全下标：颜色标签 [xxx] 一次整段显示，避免打出半个标签 */
        public static int nextSafe(String s, int from) {
            if (s == null || from >= s.length()) {
                return s == null ? 0 : s.length();
            }
            if (s.charAt(from) == '[') {
                int close = s.indexOf(']', from + 1);
                if (close > from) {
                    return close + 1;
                }
            }
            return from + 1;
        }

        public void onScreenClick() {
            if (closing) {
                return;
            }
            if (justPicked) {
                justPicked = false;
                return;
            }
            if (choiceMode && waiting) {
                return;
            }
            if (typing) {
                typing = false;
                revealed = fullText.length();
                if (label != null) {
                    label.setText(fullText);
                }
                return;
            }
            if (queue.size > 0) {
                Line line = queue.remove(0);
                showLine(line.name, line.text);
            }
        }

        public void pick(int index) {
            if (closing || !choiceMode || !waiting) {
                return;
            }
            waiting = false;
            choiceMode = false;
            FastDialog.lastChoice = index + 1;
            justPicked = true;

            String chosen = opts != null && index >= 0 && index < opts.length && opts[index] != null ? opts[index] : "";
            showLine(speaker, chosen);
        }

        public void closeNow() {
            if (closing) {
                return;
            }
            closing = true;
            waiting = false;
            queue.clear();
            if (FastDialog.current == this) {
                FastDialog.current = null;
            }
            actions(Actions.fadeOut(0.15f, Interp.fade), Actions.remove());
        }
    }
}
