package com.sepker.logic;

import arc.Core;
import mindustry.Vars;
import mindustry.core.UI;
import mindustry.logic.LExecutor;
import mindustry.logic.LExecutor.LInstruction;
import mindustry.logic.LVar;
import mindustry.logic.MessageType;

/**
 * fastmessage 指令实现。
 *
 * 与原版 message 指令的区别：
 *   - @cover（默认）：先移除上一条，再显示新的一条 → 可以每帧替换，不阻塞；
 *   - @wait：与原版一致，已有消息在显示时阻塞处理器（exec.yield）。
 * 文本同样取自 exec.textBuffer，执行后清空。
 */
public class FastMessageI implements LInstruction {

    public MessageType type = MessageType.announce;
    public LVar duration;
    public boolean cover = true;

    public FastMessageI() {}

    public FastMessageI(MessageType type, LVar duration, boolean cover) {
        this.type = type;
        this.duration = duration;
        this.cover = cover;
    }

    @Override
    public void run(LExecutor exec) {
        if (Vars.headless && type != MessageType.mission) {
            exec.textBuffer.setLength(0);
            return;
        }

        // @wait：和原版一样阻塞
        if (!cover && FastMessages.busy(type.ordinal())) {
            exec.counter.numval--;
            exec.yield = true;
            return;
        }

        String raw = exec.textBuffer.toString();

        // 缓冲区真的为空时直接跳过，不要弹一个空框
        if (raw.isEmpty()) {
            return;
        }

        String text = UI.formatIcons(raw);
        if (text == null) {
            text = "";
        }

        // 原版行为：以 @ 开头的文本按 bundle 键解析（和 message 指令一致）
        if (text.startsWith("@")) {
            String substr = text.substring(1);
            if (Core.bundle.has(substr)) {
                text = Core.bundle.get(substr);
            }
        }

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
}
