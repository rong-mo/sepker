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
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

/**
 * 常驻对话框（fastmessage 的 dialog 类型）。
 *
 * 行为：
 *   - 黑框从第一次显示起一直留在底部，只有「暂停」或回主菜单才关闭；
 *   - 普通对话不阻塞处理器，每句压进缓存队列；点击屏幕/「继续」按钮补全当前行，再点一下切下一句；
 *   - 选择也会排进同一个缓存队列，等它前面的句子都切完才显示；
 *     在它出现并被点选之前，处理器一直阻塞；
 *   - 选完后黑框继续显示被选中的那一项，后面仍可继续切缓存；
 *   - 名字牌右侧有一个「继续」按钮，比点屏幕更可靠。
 */
public class FastDialog {

    public static DialogRoot current;
    public static int tokenSeq;
    /** 选择模式最近一次点中的选项（1 起，0 = 没选） */
    public static int lastChoice;
    /** 上一次选择对应的 token；指令用它判断这个结果是不是自己的 */
    public static int lastChoiceToken;

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
        return current != null && current.parent != null && current.hasToken(token);
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

    /** 一条缓存：普通句子（options == null）或一次选择 */
    public static class Item {
        public String name;
        public String text;
        public String[] options;
        public int token;

        public Item(String name, String text, String[] options, int token) {
            this.name = name == null ? "" : name;
            this.text = text == null ? "" : text;
            this.options = options;
            this.token = token;
        }

        public boolean choice() {
            return options != null;
        }
    }

    public static class DialogRoot extends Table {

        public boolean waiting;
        public boolean choiceMode;
        public boolean closing;
        public boolean typing;
        public boolean justPicked;
        public boolean firstShow = true;

        /** 当前正在显示、等待点选的选择 token；0 = 没有 */
        public int activeToken;

        public String speaker = "";
        public String fullText = "";
        public int revealed;
        public float acc;
        public String[] opts;

        public Seq<Item> queue = new Seq<>();

        public Table box;
        public Table plate;
        public Label plateLabel;
        public TextButton continueButton;
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
            // childrenOnly：空处点不到黑框（落到根节点的点击监听上），但里面的选项按钮可以点
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

            // 名字右侧的「继续」按钮
            continueButton = new TextButton(Core.bundle.get("fastmsg.continue", "继续"), Styles.logict);
            continueButton.pack();
            continueButton.visible = false;
            continueButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    event.stop();
                    onScreenClick();
                }
            });
            addChild(continueButton);

            // 还有下一句缓存时的小三角
            arrow = new Label("▼", Styles.outlineLabel);
            arrow.setFontScale(0.8f);
            arrow.pack();
            arrow.visible = false;
            addChild(arrow);
        }

        public boolean isFresh() {
            return fullText.length() == 0 && queue.size == 0 && !typing && !choiceMode;
        }

        public void push(String name, String text) {
            if (text == null || text.length() == 0) {
                return;
            }
            String n = name == null ? "" : name;

            if (!choiceMode && fullText.length() > 0 && n.equals(speaker) && text.equals(fullText)) {
                return;
            }
            if (queue.size > 0 && !queue.peek().choice()
                && queue.peek().name.equals(n) && queue.peek().text.equals(text)) {
                return;
            }

            Item item = new Item(n, text, null, 0);
            if (isFresh()) {
                showItem(item);
            } else if (queue.size < 64) {
                queue.add(item);
            }
        }

        public int beginChoice(String name, String[] options) {
            int token = ++FastDialog.tokenSeq;
            Item item = new Item(name, "", options, token);
            // 选择不抢当前显示的句子：排到缓存队尾，等前面都切完再出现
            if (isFresh()) {
                showItem(item);
            } else if (queue.size < 64) {
                queue.add(item);
            }
            return token;
        }

        public void showItem(Item item) {
            if (item.choice()) {
                showChoiceItem(item);
            } else {
                showLine(item.name, item.text);
            }
        }

        public void showLine(String name, String text) {
            speaker = name == null ? "" : name;
            fullText = text == null ? "" : text;
            revealed = 0;
            acc = 0;
            typing = fullText.length() > 0;
            choiceMode = false;
            waiting = false;
            activeToken = 0;

            updatePlate();
            arrow.visible = false;
            rebuildBox();
        }

        public void showChoiceItem(Item item) {
            speaker = item.name;
            fullText = "";
            revealed = 0;
            acc = 0;
            typing = false;
            opts = item.options;
            choiceMode = true;
            waiting = true;
            activeToken = item.token;
            FastDialog.lastChoice = 0;

            updatePlate();
            arrow.visible = false;
            rebuildBox();
        }

        public void updatePlate() {
            plateLabel.setText(speaker);
            plate.pack();
            plate.visible = speaker.length() > 0;
        }

        /** 这个 token 对应的选择是否还在等（在队列里或在显示中） */
        public boolean hasToken(int token) {
            if (token <= 0) {
                return false;
            }
            if (activeToken == token && waiting) {
                return true;
            }
            for (int i = 0; i < queue.size; i++) {
                if (queue.get(i).token == token) {
                    return true;
                }
            }
            return false;
        }

        /** 当前是否有需要玩家点击推进的东西 */
        public boolean wantsInput() {
            return (choiceMode && waiting) || typing || queue.size > 0;
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

        public void tick() {
            if (closing || box == null) {
                return;
            }
            if (getWidth() <= 0f) {
                return;
            }

            // 只有回主菜单才自动关；HUD 隐藏（过场）时对话框按用户要求继续常驻
            if (mindustry.Vars.state.isMenu()) {
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
                // 45 字/秒：Time.delta 是 tick，先换算成秒
                acc += Time.delta * 45f / 60f;
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

            if (continueButton != null) {
                continueButton.visible = !choiceMode && (typing || queue.size > 0);
                if (continueButton.visible) {
                    float px;
                    float py;
                    if (plate != null && plate.visible) {
                        px = plate.x + plate.getWidth() + Scl.scl(6f);
                        py = plate.y + (plate.getHeight() - continueButton.getHeight()) * 0.5f;
                    } else {
                        px = box.x + Scl.scl(8f);
                        py = box.y + box.getHeight() - continueButton.getHeight() * 0.5f;
                    }
                    continueButton.setPosition(px, py);
                    continueButton.toFront();
                }
            }

            if (arrow != null) {
                arrow.visible = !choiceMode && !typing && queue.size > 0;
                if (arrow.visible) {
                    arrow.setPosition(box.x + box.getWidth() - arrow.getWidth() - Scl.scl(10f), box.y + Scl.scl(4f));
                    arrow.color.a = 0.35f + 0.65f * Mathf.absin(Time.time, 0.1f, 1f);
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
                Item item = queue.remove(0);
                showItem(item);
            }
        }

        public void pick(int index) {
            if (closing || !choiceMode || !waiting) {
                return;
            }
            waiting = false;
            choiceMode = false;
            FastDialog.lastChoice = index + 1;
            FastDialog.lastChoiceToken = activeToken;
            activeToken = 0;
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
            activeToken = 0;
            queue.clear();
            if (FastDialog.current == this) {
                FastDialog.current = null;
            }
            actions(Actions.fadeOut(0.15f, Interp.fade), Actions.remove());
        }
    }
}
