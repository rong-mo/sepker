package com.sepker.logic;

import arc.Core;
import arc.graphics.Color;
import mindustry.Vars;
import mindustry.core.UI;
import mindustry.logic.LExecutor;
import mindustry.logic.LExecutor.LInstruction;
import mindustry.logic.LVar;
import mindustry.logic.MessageType;

/**
 * fastmessage 指令实现。
 *
 * 原版四种消息（notify/announce/toast/mission）：
 *   - @cover：先移除上一条，再显示新的一条 → 可以每帧替换，不阻塞；
 *   - @wait：与原版一致，已有消息在显示时阻塞处理器。
 *
 * comm：播放通信特效，默认阻塞满 3 秒动画（开始 / 终止都一样）；
 *       开始用指令里的颜色（默认淡黄），终止固定浅红。
 *
 * dialog：
 *   - talk：不阻塞，把这一句压进常驻对话框的缓存队列，点击屏幕切换下一句；
 *   - choose：阻塞到玩家点选，序号写进指令里填的逻辑变量；
 *   - pause：关闭对话框（清空缓存）。
 */
public class FastMessageI implements LInstruction {

    public int kind = FastMessageStatement.K_ANNOUNCE;
    public LVar duration;
    public boolean cover = true;

    public boolean commStop = false;
    public String commColor = "fff59dff";

    public int dialogMode = FastMessageStatement.D_TALK;
    public String name = "";
    public String[] options;
    /** 选择结果写进这个变量（由指令里的「变量」输入框决定，可为 null） */
    public LVar outVar;

    /** 通信动画的令牌（阻塞用） */
    public transient int pendingCommToken;
    /** 选择模式的令牌（阻塞用） */
    public transient int pendingToken;

    public FastMessageI() {
    }

    public FastMessageI(int kind, LVar duration, boolean cover, boolean commStop, String commColor,
                        int dialogMode, String name, String[] options, LVar outVar) {
        this.kind = kind;
        this.duration = duration;
        this.cover = cover;
        this.commStop = commStop;
        this.commColor = commColor;
        this.dialogMode = dialogMode;
        this.name = name;
        this.options = options;
        this.outVar = outVar;
    }

    public MessageType messageType() {
        switch (kind) {
            case FastMessageStatement.K_NOTIFY: return MessageType.notify;
            case FastMessageStatement.K_TOAST: return MessageType.toast;
            case FastMessageStatement.K_MISSION: return MessageType.mission;
            default: return MessageType.announce;
        }
    }

    @Override
    public void run(LExecutor exec) {
        if (Vars.headless && kind != FastMessageStatement.K_MISSION) {
            exec.textBuffer.setLength(0);
            return;
        }

        if (kind == FastMessageStatement.K_COMM) {
            runComm(exec);
        } else if (kind == FastMessageStatement.K_DIALOG) {
            runDialog(exec);
        } else {
            runVanilla(exec);
        }
    }

    public void block(LExecutor exec) {
        exec.counter.numval--;
        exec.yield = true;
    }

    public String formatText(String raw) {
        String text = UI.formatIcons(raw);
        if (text == null) {
            text = "";
        }
        if (text.startsWith("@")) {
            String substr = text.substring(1);
            if (Core.bundle.has(substr)) {
                text = Core.bundle.get(substr);
            }
        }
        return text;
    }

    public void runVanilla(LExecutor exec) {
        MessageType type = messageType();

        if (!cover && FastMessages.busy(type.ordinal())) {
            block(exec);
            return;
        }

        String raw = exec.textBuffer.toString();
        if (raw.isEmpty()) {
            return;
        }

        String text = formatText(raw);

        float dur = duration == null ? 3f : (float) duration.numf();
        if (dur <= 0f) {
            dur = 3f;
        }

        if (type == MessageType.notify) {
            FastMessages.notify(text);
        } else if (type == MessageType.announce) {
            FastMessages.announce(text, dur);
        } else if (type == MessageType.toast) {
            FastMessages.toast(text, dur);
        } else if (type == MessageType.mission) {
            Vars.state.rules.mission = text;
        }

        exec.textBuffer.setLength(0);
    }

    public void runComm(LExecutor exec) {
        // 动画还在播：阻塞到它结束，保证处理器真的等满 3 秒
        if (pendingCommToken != 0) {
            if (FastComm.isPlaying(pendingCommToken)) {
                block(exec);
                return;
            }
            pendingCommToken = 0;
            exec.textBuffer.setLength(0);
            return;
        }

        Color color = Color.valueOf("fff59dff");
        String raw = commColor == null ? "" : commColor.trim();
        try {
            if (raw.startsWith("%") || raw.startsWith("#")) {
                raw = raw.substring(1);
            }
            if (raw.length() > 0) {
                color = Color.valueOf(raw);
            }
        } catch (Throwable ignored) {
        }

        int token = FastComm.show(commStop, color);
        exec.textBuffer.setLength(0);

        if (token != 0) {
            pendingCommToken = token;
            block(exec);
        }
    }

    public void runDialog(LExecutor exec) {
        if (dialogMode == FastMessageStatement.D_PAUSE) {
            FastDialog.close();
            pendingToken = 0;
            exec.textBuffer.setLength(0);
            return;
        }

        boolean choosing = dialogMode == FastMessageStatement.D_CHOOSE;

        if (choosing) {
            if (pendingToken != 0) {
                if (FastDialog.isWaiting(pendingToken)) {
                    block(exec);
                    return;
                }

                // 只有「自己这一次选择」的结果才算数（别的选择可能已经覆盖了 lastChoice）
                int finishedToken = pendingToken;
                int choice = FastDialog.lastChoiceToken == finishedToken ? FastDialog.lastChoice : 0;
                pendingToken = 0;
                FastDialog.lastChoice = 0;
                FastDialog.lastChoiceToken = 0;

                if (choice > 0 && outVar != null) {
                    outVar.setnum(choice);
                }

                exec.textBuffer.setLength(0);
                return;
            }

            if (options == null || options.length == 0) {
                exec.textBuffer.setLength(0);
                return;
            }

            pendingToken = FastDialog.showChoice(name, options);
            if (FastDialog.isWaiting(pendingToken)) {
                block(exec);
            }
            exec.textBuffer.setLength(0);
            return;
        }

        // 普通对话：不阻塞，压进队列，由点击推进
        String raw = exec.textBuffer.toString();
        if (raw.isEmpty()) {
            return;
        }

        String text = formatText(raw);
        exec.textBuffer.setLength(0);
        FastDialog.pushTalk(name, text);
    }
}
