package com.sepker.logic;

import arc.Core;
import arc.func.Cons;
import arc.func.Prov;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.logic.LAssembler;
import mindustry.logic.LCategory;
import mindustry.logic.LExecutor.LInstruction;
import mindustry.logic.LStatement;
import mindustry.logic.MessageType;
import mindustry.ui.Styles;

/**
 * fastmessage 指令 —— 可即时替换文本的"快速消息"。
 *
 * 用法：先用 print 指令往文本缓冲区写内容，再执行本指令。
 *   fastmessage "<类型>" <显示秒数> <cover|wait>
 *
 * 类型：notify（黑框）/ announce（中间）/ toast（顶部）/ mission（任务栏）
 */
public class FastMessageStatement extends LStatement {

    public MessageType type = MessageType.announce;
    public String duration = "3";
    public boolean cover = true;

    /** 必须是 public —— 匿名内部类会调用它（旧 ART 不支持 nestmate） */
    public String loc(String key, String fallback) {
        if (Core.settings.getBool("logiclocalization", true)) {
            return Core.bundle.get(key, fallback);
        }
        return fallback;
    }

    /** 类型名本地化（照原版 LStatement.bundle(Enum) 的键规则：messagetype.label.<name>） */
    public String typeLabel() {
        String key = "messagetype.label." + type.name().toLowerCase();
        if (Core.settings.getBool("logiclocalization", true) && Core.bundle.has(key)) {
            return Core.bundle.get(key);
        }
        return type.name();
    }

    @Override
    public void build(Table table) {
        rebuild(table);
    }

    /** 必须是 public —— 匿名内部类会递归调用 */
    public void rebuild(Table table) {
        table.clearChildren();

        // 类型选择（和原版 message 一样）
        table.button(new Cons<Button>() {
            @Override
            public void get(Button b) {
                b.label(new Prov<CharSequence>() {
                    @Override
                    public CharSequence get() { return typeLabel(); }
                }).growX().wrap().labelAlign(Align.center);

                b.clicked(new Runnable() {
                    @Override
                    public void run() {
                        showSelect(b, MessageType.all, type, new Cons<MessageType>() {
                            @Override
                            public void get(MessageType o) {
                                type = o;
                                rebuild(table);
                            }
                        }, 2, new Cons<Cell>() {
                            @Override
                            public void get(Cell c) {
                                c.width(150f);
                            }
                        });
                    }
                });
            }
        }, Styles.logict, new Runnable() {
            @Override
            public void run() { }
        }).size(160f, 40f).padLeft(2).color(table.color);

        // 模式开关：cover / wait
        table.button(new Cons<Button>() {
            @Override
            public void get(Button b) {
                b.add(cover ? loc("fastmsg.cover", "cover") : loc("fastmsg.wait", "wait"));
            }
        }, Styles.logict, new Runnable() {
            @Override
            public void run() {
                cover = !cover;
                rebuild(table);
            }
        }).size(120f, 40f).pad(4f);

        if (type == MessageType.announce || type == MessageType.toast) {
            fields(table, loc("fastmsg.duration", "duration"), duration, new Cons<String>() {
                @Override public void get(String v) { duration = v; }
            });
        }
    }

    @Override
    public LInstruction build(LAssembler builder) {
        String d = duration;
        if (d == null || d.trim().isEmpty()) {
            d = "3";
        }
        return new FastMessageI(type, builder.var(d), cover);
    }

    @Override
    public void write(StringBuilder out) {
        out.append("fastmessage \"")
           .append(type.name())
           .append("\" ")
           .append(duration)
           .append(' ')
           .append(cover ? "cover" : "wait");
    }

    @Override
    public LCategory category() {
        return LCategory.world;
    }

    @Override
    public boolean privileged() {
        return true;
    }
}
