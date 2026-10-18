package com.sepker.logic;

import arc.Core;
import arc.func.Cons;
import arc.math.Interp;
import arc.scene.event.Touchable;
import arc.scene.actions.Actions;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;

/**
 * 可即时替换的消息显示层（fastmessage 的 @cover 模式用）。
 *
 * 原版 UI.announce / showInfoToast 每次都会新建一张表，且把引用存在 private 的
 * UI.lastAnnouncement 里，mod 无法撤销；HudFragment 的 toast 更是强制 3.5 秒排队。
 * 所以这里自己建表并保留引用：显示新消息前先把上一张移除，实现"直接替换文本"。
 */
public class FastMessages {

    public static Table announceTable;
    public static Table toastTable;
    public static Table notifyTable;

    public static boolean alive(Table t) {
        return t != null && t.parent != null;
    }

    public static void clear(Table t) {
        if (alive(t)) t.remove();
    }

    /** 0=notify 1=announce 2=toast 3=mission */
    public static boolean busy(int type) {
        switch (type) {
            case 0: return Vars.ui.hudfrag.hasToast() || alive(notifyTable);
            case 1: return Vars.ui.hasAnnouncement() || alive(announceTable);
            case 2: return Vars.ui.hasAnnouncement() || alive(toastTable);
            default: return false;
        }
    }

    // ============ 中间（黑框）= announce ============
    public static void announce(String text, float duration) {
        clear(announceTable);

        final Table t = new Table(Styles.black3);
        t.touchable = Touchable.disabled;
        t.margin(8f).add(text).style(Styles.outlineLabel).labelAlign(Align.center);
        t.update(new Runnable() {
            @Override
            public void run() {
                t.setPosition(
                    Core.graphics.getWidth() / 2f,
                    Core.graphics.getHeight() / 2f,
                    Align.center
                );
                t.toFront();
                if (Vars.state.isMenu() || !Vars.ui.hudfrag.shown) {
                    t.remove();
                }
            }
        });
        t.actions(Actions.fadeOut(duration, Interp.pow4In), Actions.remove());
        t.pack();
        t.act(0.1f);
        Core.scene.add(t);
        announceTable = t;
    }

    // ============ 顶部 = toast ============
    public static void toast(String text, float duration) {
        clear(toastTable);

        final Table table = new Table();
        table.touchable = Touchable.disabled;
        table.setFillParent(true);
        table.top().table(Styles.black3, new Cons<Table>() {
            @Override
            public void get(Table c) {
                c.margin(4).add(text).style(Styles.outlineLabel);
            }
        }).padTop(10);
        table.update(new Runnable() {
            @Override
            public void run() {
                if (Vars.state.isMenu()) table.remove();
            }
        });
        table.actions(
            Actions.delay(duration * 0.9f),
            Actions.fadeOut(duration * 0.1f, Interp.fade),
            Actions.remove()
        );
        Core.scene.add(table);
        toastTable = table;
    }

    // ============ 黑框（下方浮动）= notify ============
    public static void notify(String text) {
        clear(notifyTable);

        final Table inner = new Table(Tex.button);
        inner.margin(8);
        inner.image(Icon.info).pad(3);
        inner.add(text).wrap().width(280f).get().setAlignment(Align.center, Align.center);
        inner.pack();

        final Table container = Core.scene.table();
        container.top().add(inner);
        container.setTranslation(0, inner.getPrefHeight());
        container.actions(
            Actions.translateBy(0, -inner.getPrefHeight(), 1f, Interp.fade),
            Actions.delay(2.5f),
            Actions.fadeOut(0.5f, Interp.fade),
            Actions.remove()
        );
        notifyTable = container;
    }
}
