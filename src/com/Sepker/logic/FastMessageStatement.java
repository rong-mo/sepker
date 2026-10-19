package com.sepker.logic;

import arc.Core;
import arc.func.Cons;
import arc.func.Cons2;
import arc.func.Prov;
import arc.graphics.Color;
import arc.scene.ui.Button;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.logic.LAssembler;
import mindustry.logic.LCategory;
import mindustry.logic.LExecutor.LInstruction;
import mindustry.logic.LStatement;
import mindustry.ui.Styles;

/**
 * fastmessage 指令 —— 可即时替换文本的"快速消息"。
 *
 * 用法：先用 print 指令往文本缓冲区写内容，再执行本指令。
 *   fastmessage "<类型>" <显示秒数> <cover|wait> ...
 *
 * 类型：
 *   notify / announce / toast / mission —— 原版四种消息位置；
 *   comm   —— 通信特效：屏幕上下 45° 条纹带 + <> 滑动 + 「通讯开始 / 通讯终止」，
 *             追加两个 token：start|stop 颜色；
 *   dialog —— 真正的对话框：底部黑框、名字牌、打字机、点击翻页，
 *             追加：talk|choose|pause 名字 [选项...]。
 */
public class FastMessageStatement extends LStatement {

    public static final int K_NOTIFY = 0;
    public static final int K_ANNOUNCE = 1;
    public static final int K_TOAST = 2;
    public static final int K_MISSION = 3;
    public static final int K_COMM = 4;
    public static final int K_DIALOG = 5;

    public static final int C_START = 0;
    public static final int C_STOP = 1;

    public static final int D_TALK = 0;
    public static final int D_CHOOSE = 1;
    public static final int D_PAUSE = 2;

    /** 类型按钮里的顺序 */
    public static final int[] KIND_ORDER = {K_ANNOUNCE, K_NOTIFY, K_TOAST, K_MISSION, K_COMM, K_DIALOG};

    public int kind = K_ANNOUNCE;
    public String duration = "3";
    public boolean cover = true;

    // 通信
    public boolean commStop = false;
    public String commColor = "4db2ffff";

    // 对话框
    public int dialogMode = D_TALK;
    public String name = "";
    public Seq<String> options = new Seq<>();

    /** 必须是 public —— 匿名内部类会调用它（旧 ART 不支持 nestmate） */
    public String loc(String key, String fallback) {
        if (Core.settings.getBool("logiclocalization", true)) {
            return Core.bundle.get(key, fallback);
        }
        return fallback;
    }

    public static String kindName(int kind) {
        switch (kind) {
            case K_NOTIFY: return "notify";
            case K_TOAST: return "toast";
            case K_MISSION: return "mission";
            case K_COMM: return "comm";
            case K_DIALOG: return "dialog";
            default: return "announce";
        }
    }

    public static int kindFromToken(String token) {
        if ("notify".equals(token)) return K_NOTIFY;
        if ("toast".equals(token)) return K_TOAST;
        if ("mission".equals(token)) return K_MISSION;
        if ("comm".equals(token)) return K_COMM;
        if ("dialog".equals(token)) return K_DIALOG;
        return K_ANNOUNCE;
    }

    public static String dialogModeName(int mode) {
        if (mode == D_CHOOSE) return "choose";
        if (mode == D_PAUSE) return "pause";
        return "talk";
    }

    public static int dialogModeFromToken(String token) {
        if ("choose".equals(token)) return D_CHOOSE;
        if ("pause".equals(token)) return D_PAUSE;
        return D_TALK;
    }

    public String kindLabel(int k) {
        if (k == K_COMM) return loc("fastmsg.kind.comm", "comm");
        if (k == K_DIALOG) return loc("fastmsg.kind.dialog", "dialog");
        String key = "messagetype.label." + kindName(k);
        if (Core.settings.getBool("logiclocalization", true) && Core.bundle.has(key)) {
            return Core.bundle.get(key);
        }
        return kindName(k);
    }

    public String dialogModeLabel() {
        switch (dialogMode) {
            case D_CHOOSE: return loc("fastmsg.dialog.choose", "choose");
            case D_PAUSE: return loc("fastmsg.dialog.pause", "pause");
            default: return loc("fastmsg.dialog.talk", "talk");
        }
    }

    public Color commColor() {
        String v = commColor == null ? "" : commColor.trim();
        try {
            if (v.startsWith("%") || v.startsWith("#")) v = v.substring(1);
            if (v.length() > 0) return Color.valueOf(v);
        } catch (Throwable ignored) {
        }
        return Color.valueOf("4db2ffff");
    }

    public void ensureOptions() {
        if (options == null) options = new Seq<>();
        while (options.size < 2) {
            options.add("");
        }
    }

    @Override
    public void build(Table table) {
        rebuild(table);
    }

    /** 必须是 public —— 匿名内部类会递归调用 */
    public void rebuild(final Table table) {
        table.clearChildren();

        // 类型选择
        table.button(new Cons<Button>() {
            @Override
            public void get(final Button b) {
                b.label(new Prov<CharSequence>() {
                    @Override
                    public CharSequence get() {
                        return kindLabel(kind);
                    }
                }).growX().wrap().labelAlign(Align.center);

                b.clicked(new Runnable() {
                    @Override
                    public void run() {
                        openKindSelect(b, table);
                    }
                });
            }
        }, Styles.logict, new Runnable() {
            @Override
            public void run() { }
        }).size(160f, 40f).padLeft(2).color(table.color);

        if (kind == K_COMM) {
            buildComm(table);
            return;
        }

        if (kind == K_DIALOG) {
            buildDialog(table);
            return;
        }

        // 原版类型：cover / wait
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

        if (kind == K_ANNOUNCE || kind == K_TOAST) {
            fields(table, loc("fastmsg.duration", "duration"), duration, new Cons<String>() {
                @Override
                public void get(String v) {
                    duration = v;
                }
            });
        }
    }

    public void openKindSelect(final Button b, final Table table) {
        showSelectTable(b, new Cons2<Table, Runnable>() {
            @Override
            public void get(Table t, final Runnable hide) {
                t.defaults().size(150f, 40f);
                for (int i = 0; i < KIND_ORDER.length; i++) {
                    final int k = KIND_ORDER[i];
                    t.button(kindLabel(k), Styles.logicTogglet, new Runnable() {
                        @Override
                        public void run() {
                            kind = k;
                            if (k == K_DIALOG && cover) {
                                cover = false;
                            }
                            if (kind == K_DIALOG && dialogMode == D_CHOOSE) {
                                ensureOptions();
                            }
                            hide.run();
                            rebuild(table);
                        }
                    }).checked(kind == k).row();
                }
            }
        });
    }

    public void buildComm(final Table table) {
        table.button(new Cons<Button>() {
            @Override
            public void get(Button b) {
                b.add(commStop ? loc("fastmsg.comm.stop", "stop") : loc("fastmsg.comm.start", "start"));
            }
        }, Styles.logict, new Runnable() {
            @Override
            public void run() {
                commStop = !commStop;
                rebuild(table);
            }
        }).size(110f, 40f).pad(4f);

        table.add(new Image(Tex.whiteui)).size(20f).color(commColor()).padLeft(4f).padRight(4f);

        table.button(new Cons<Button>() {
            @Override
            public void get(Button b) {
                b.image(Icon.pencilSmall);
            }
        }, Styles.logict, new Runnable() {
            @Override
            public void run() {
                Vars.ui.picker.show(commColor(), new Cons<Color>() {
                    @Override
                    public void get(Color c) {
                        commColor = c.toString();
                        rebuild(table);
                    }
                });
            }
        }).size(40f, 40f).pad(2f);
    }

    public void buildDialog(final Table table) {
        table.button(new Cons<Button>() {
            @Override
            public void get(Button b) {
                b.add(dialogModeLabel());
            }
        }, Styles.logict, new Runnable() {
            @Override
            public void run() {
                dialogMode = (dialogMode + 1) % 3;
                if (dialogMode == D_CHOOSE) {
                    ensureOptions();
                }
                rebuild(table);
            }
        }).size(110f, 40f).pad(4f);

        if (dialogMode != D_PAUSE) {
            table.add(loc("fastmsg.name", "name")).padLeft(2f).padRight(4f);
            table.field(name, Styles.nodeField, new Cons<String>() {
                @Override
                public void get(String s) {
                    name = s;
                }
            }).size(140f, 40f).pad(2f).color(table.color);
        }

        if (dialogMode == D_CHOOSE) {
            table.add(buildOptions(table)).padLeft(2f);
        }
    }

    public Table buildOptions(final Table rootTable) {
        Table t = new Table();

        for (int i = 0; i < options.size; i++) {
            final int index = i;
            StringBuilder label = new StringBuilder(loc("fastmsg.option", "option"));
            label.append(i + 1);
            t.add(label.toString()).padLeft(2f);
            t.field(options.get(i), Styles.nodeField, new Cons<String>() {
                @Override
                public void get(String s) {
                    if (index < options.size) {
                        options.set(index, s);
                    }
                }
            }).size(120f, 40f).pad(2f).color(t.color);
            if (i % 2 == 1) {
                t.row();
            }
        }

        // 右下角的加号：点一下多加一个选项（最多 9 个：LParser 单行上限 16 token）
        t.row();
        t.button("+", Styles.logict, new Runnable() {
            @Override
            public void run() {
                if (options.size >= 9) {
                    return;
                }
                options.add("");
                rebuild(rootTable);
            }
        }).size(40f, 40f).pad(2f).right();

        return t;
    }

    @Override
    public LInstruction build(LAssembler builder) {
        String d = duration;
        if (d == null || d.trim().isEmpty()) {
            d = "3";
        }
        String[] opts = options == null ? new String[0] : options.toArray(String.class);
        return new FastMessageI(kind, builder.var(d), cover, commStop, commColor, dialogMode, name == null ? "" : name, opts);
    }

    @Override
    public void write(StringBuilder out) {
        out.append("fastmessage \"");
        out.append(kindName(kind));
        out.append("\" ");
        out.append(duration == null || duration.trim().isEmpty() ? "3" : duration);
        out.append(cover ? " cover" : " wait");

        if (kind == K_COMM) {
            out.append(commStop ? " stop " : " start ");
            out.append(commColor == null ? "4db2ffff" : commColor);
        } else if (kind == K_DIALOG) {
            out.append(' ').append(dialogModeName(dialogMode));
            out.append(' ').append(quote(name));
            if (dialogMode == D_CHOOSE && options != null) {
                // 选项数量必须显式写出：自定义解析器拿不到本行 token 数（LParser 的数组是 16 长且带上一行残留）
                int n = Math.min(options.size, 9);
                out.append(' ').append(n);
                for (int i = 0; i < n; i++) {
                    out.append(' ').append(quote(options.get(i)));
                }
            }
        }
    }

    /**
     * 用双引号包住文本，保留空格 / 中文 / # / ; / 反斜杠。
     *
     * 注意：v159 的 LParser 不支持字符串里的 \" 转义（连原版 print 都会解析失败），
     * 所以这里不转义：把双引号换成单引号，换行换成空格。
     */
    public static String quote(String s) {
        if (s == null) {
            s = "";
        }
        StringBuilder b = new StringBuilder(s.length() + 2);
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                c = '\'';
            } else if (c == '\n' || c == '\r') {
                c = ' ';
            }
            b.append(c);
        }
        b.append('"');
        return b.toString();
    }

    /** 去掉 token 两端的引号（不做反转义，和 LParser 行为一致） */
    public static String unquote(String t) {
        if (t == null) {
            return "";
        }
        if (t.length() >= 2 && t.charAt(0) == '"' && t.charAt(t.length() - 1) == '"') {
            return t.substring(1, t.length() - 1);
        }
        return t;
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
