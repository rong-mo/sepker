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
 *   - @cover（默认）：先移除上一条，再显示新的一条 → 可以每帧替换，不阻塞；
 *   - @wait：与原版一致，已有消息在显示时阻塞处理器（exec.yield）。
 *
 * comm：播放通信特效（3 秒），start 用自定义颜色，stop 固定浅红。
 *
 * dialog：
 *   - talk：底部黑框 + 名字牌 + 打字机，wait 模式下阻塞到玩家点击；
 *   - choose：选项按钮，阻塞到玩家点选，序号写入逻辑变量 sepkerchoice（1 起）；
 *   - pause：直接关掉对话框。
 * 文本同样取自 exec.textBuffer，执行后清空。
 */
public class FastMessageI implements LInstruction {

    public int kind = FastMessageStatement.K_ANNOUNCE;
    public LVar duration;
    public boolean cover = true;

    public boolean commStop = false;
    public String commColor = "4db2ffff";

    public int dialogMode = FastMessageStatement.D_TALK;
    public String name = "";
    public String[] options;

    /** 当前 dialog 的 token（只在阻塞模式下使用） */
    public transient int pendingToken;

    public FastMessageI() {
    }

    public FastMessageI(int kind, LVar duration, boolean cover, boolean commStop, String commColor,
                        int dialogMode, String name, String[] options) {
        this.kind = kind;
        this.duration = duration;
        this.cover = cover;
        this.commStop = commStop;
        this.commColor = commColor;
        this.dialogMode = dialogMode;
        this.name = name;
        this.options = options;
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

        // @wait：和原版一样阻塞
        if (!cover && FastMessages.busy(type.ordinal())) {
            block(exec);
            return;
        }

        String raw = exec.textBuffer.toString();

        // 缓冲区真的为空时直接跳过，不要弹一个空框
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
        Color color = Color.valueOf("4db2ffff");
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

        FastComm.show(commStop, color);
        exec.textBuffer.setLength(0);
    }

    public void runDialog(LExecutor exec) {
        if (dialogMode == FastMessageStatement.D_PAUSE) {
            FastDialog.close();
            pendingToken = 0;
            exec.textBuffer.setLength(0);
            return;
        }

        boolean choosing = dialogMode == FastMessageStatement.D_CHOOSE;
        boolean blocking = choosing || !cover;

        if (blocking && pendingToken != 0) {
            if (FastDialog.isWaiting(pendingToken)) {
                block(exec);
                return;
            }

            int choice = FastDialog.lastChoice;
            pendingToken = 0;
            FastDialog.lastChoice = 0;

            if (choice > 0) {
                LVar out = exec.optionalVar("sepkerchoice");
                if (out != null) {
                    out.setnum(choice);
                }
            }

            exec.textBuffer.setLength(0);
            return;
        }

        if (choosing) {
            String[] opts = options == null ? new String[0] : options;
            if (opts.length == 0) {
                exec.textBuffer.setLength(0);
                return;
            }
            pendingToken = FastDialog.showChoice(name, opts);
            if (FastDialog.isWaiting(pendingToken)) {
                block(exec);
            }
            exec.textBuffer.setLength(0);
            return;
        }

        String raw = exec.textBuffer.toString();
        if (raw.isEmpty()) {
            return;
        }

        String text = formatText(raw);
        exec.textBuffer.setLength(0);

        if (blocking) {
            pendingToken = FastDialog.showTalk(name, text);
            if (FastDialog.isWaiting(pendingToken)) {
                block(exec);
            }
        } else {
            if (!FastDialog.sameTalk(name, text)) {
                FastDialog.showTalk(name, text);
            }
        }
    }
}
