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
import arc.util.Align;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

/**
 * 真正的对话框（fastmessage 的 dialog 类型）。
 *
 * - 对话：屏幕底部升起一个黑色对话框，从文本缓冲区打字机式打印；
 *   左上角挂一个小黑框显示名字；点击屏幕补全当前行，再点一下翻到下一条（可阻塞）。
 * - 选择：在对话的基础上，把正文换成 2 个以上的文字按钮（选项由指令里的输入框决定），
 *   点击某个选项后关闭对话框，并把序号写进逻辑变量 sepkerchoice（1 起）。
 * - 暂停：直接关掉对话框。
 */
public class FastDialog {

    public static final int TALK = 0;
    public static final int CHOOSE = 1;

    /** dialog 指令当前是否在等待玩家点击 */
    public static DialogRoot current;
    public static int tokenSeq;
    /** 选择模式最近一次点中的选项（1 起，0 = 没选） */
    public static int lastChoice;

    public static int showTalk(String name, String text) {
        return open(name, text, null);
    }

    public static int showChoice(String name, String[] options) {
        return open(name, null, options);
    }

    public static int open(String name, String text, String[] options) {
        close();
        tokenSeq++;
        lastChoice = 0;

        DialogRoot root = new DialogRoot(tokenSeq, name, text, options);
        current = root;
        Core.scene.add(root);
        return tokenSeq;
    }

    public static boolean isWaiting(int token) {
        return current != null && current.parent != null
            && current.token == token && current.waiting;
    }

    /** 同一个名字 + 同一段文字是否已经在显示（覆盖模式避免每帧重建、反复播放上升动画） */
    public static boolean sameTalk(String name, String text) {
        return current != null && current.parent != null && !current.choice
            && current.speaker.equals(name == null ? "" : name)
            && current.fullText.equals(text == null ? "" : text);
    }

    public static void close() {
        if (current != null) {
            DialogRoot r = current;
            current = null;
            r.closeNow();
        }
    }

    public static class DialogRoot extends Table {

        public int token;
        public boolean waiting = true;
        public boolean choice;
        public boolean typing;
        public boolean closing;
        public String fullText = "";
        public int revealed;
        public float acc;
        public String speaker = "";
        public String[] opts;

        public Table box;
        public Table plate;
        public Label label;
        public Label arrow;
        public Table optionsTable;

        public float pad;
        public float textWidth;

        public DialogRoot(int token, String speaker, String text, String[] options) {
            this.token = token;
            this.speaker = speaker == null ? "" : speaker;
            this.fullText = text == null ? "" : text;
            this.opts = options;
            this.choice = options != null;

            setFillParent(true);
            touchable = Touchable.enabled;

            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onScreenClick();
                }
            });

            build();
            update(new Runnable() {
                @Override
                public void run() {
                    tick();
                }
            });
        }

        public void build() {
            pad = Math.max(8f, Scl.scl(10f));

            float sceneW = Core.scene.getWidth();
            float sceneH = Core.scene.getHeight();
            if (sceneW <= 0f) {
                sceneW = Core.graphics.getWidth();
            }
            if (sceneH <= 0f) {
                sceneH = Core.graphics.getHeight();
            }

            float maxTextW = sceneW * 0.74f;
            float minBoxW = sceneW * 0.52f;
            float lineH = Fonts.outline.getLineHeight();

            float textW = minBoxW - pad * 2f;
            float textH = lineH * 3f;

            if (!choice && fullText.length() > 0) {
                GlyphLayout gl = new GlyphLayout();
                gl.setText(Fonts.outline, fullText, Color.white, maxTextW, Align.left, true);
                textW = Mathf.clamp(gl.width, minBoxW - pad * 2f, maxTextW);
                textH = Math.max(gl.height, lineH * 3f);
            } else if (choice) {
                textW = Math.max(minBoxW - pad * 2f, maxTextW * 0.8f);
            }
            this.textWidth = textW;

            box = new Table(Styles.black8);
            box.margin(pad);
            // childrenOnly：空处点不到黑框（落到根节点的点击监听上），但里面的选项按钮可以点
            box.touchable = Touchable.childrenOnly;

            if (choice) {
                optionsTable = new Table();
                optionsTable.touchable = Touchable.childrenOnly;
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
                box.add(optionsTable).width(textW).left();
            } else {
                label = new Label("", Styles.outlineLabel);
                label.setWrap(true);
                label.setAlignment(Align.topLeft);
                box.add(label).width(textW).height(textH).left();

                typing = fullText.length() > 0;
                if (!typing) {
                    label.setText("");
                }
            }

            box.pack();
            box.setPosition((sceneW - box.getWidth()) / 2f, Math.max(Scl.scl(50f), sceneH * 0.10f));
            box.moveBy(0f, -box.getHeight());
            box.actions(Actions.moveBy(0f, box.getHeight(), 0.35f, Interp.pow2Out));

            addChild(box);

            // 左上角的小黑框（名字）
            plate = new Table(Styles.black);
            plate.margin(Math.max(3f, Scl.scl(4f)));
            plate.touchable = Touchable.disabled;
            plate.add(new Label(speaker, Styles.outlineLabel)).pad(2f);
            plate.pack();
            plate.visible = speaker.length() > 0;
            addChild(plate);

            // 等待点击的小三角
            arrow = new Label("▼", Styles.outlineLabel);
            arrow.setFontScale(0.8f);
            arrow.pack();
            arrow.visible = false;
            addChild(arrow);

            if (typing) {
                label.setText("");
            }
        }

        public void tick() {
            if (closing || box == null) {
                return;
            }
            if (getWidth() <= 0f) {
                return;
            }

            if (Vars.state.isMenu() || !Vars.ui.hudfrag.shown) {
                closeNow();
                return;
            }

            // 打字机
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

            float bx = box.x;
            float by = box.y;

            if (plate != null && plate.visible) {
                plate.setPosition(bx + Scl.scl(8f), by + box.getHeight() - plate.getHeight() * 0.5f);
            }

            if (arrow != null) {
                arrow.visible = waiting && !typing && !choice;
                if (arrow.visible) {
                    arrow.setPosition(bx + box.getWidth() - arrow.getWidth() - Scl.scl(10f), by + Scl.scl(4f));
                    arrow.color.a = 0.35f + 0.65f * Mathf.absin(Time.time, 0.8f, 1f);
                }
            }
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
            if (closing || choice) {
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
            if (waiting) {
                closeNow();
            }
        }

        public void pick(int index) {
            if (closing) {
                return;
            }
            FastDialog.lastChoice = index + 1;
            waiting = false;
            closeNow();
        }

        public void closeNow() {
            if (closing) {
                return;
            }
            closing = true;
            waiting = false;
            if (FastDialog.current == this) {
                FastDialog.current = null;
            }
            actions(Actions.fadeOut(0.15f, Interp.fade), Actions.remove());
        }
    }
}
