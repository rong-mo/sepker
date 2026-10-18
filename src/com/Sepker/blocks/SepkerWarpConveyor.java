package com.sepker.blocks;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.content.*;
import mindustry.core.Renderer;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.type.Category;
import mindustry.ui.Styles;
import mindustry.world.blocks.distribution.StackConveyor;
import mindustry.world.meta.BuildVisibility;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.ImageButton;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.ObjectSet;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.func.Prov;
import arc.func.Cons2;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class SepkerWarpConveyor extends StackConveyor {

    /** ⭐ 采样计时器 ID */
    public final int timerCheckMoved = timers++;

    public float warpRange = 100f * 8f;

    public TextureRegion texBridgeBody;
    public TextureRegion texBridgeLine;
    public TextureRegion texBridgeEnd;
    public TextureRegion texBridgeArrow;

    public boolean texLoaded = false;

    public float bridgeWidth = 6.5f;
    public float arrowSpacing = 4f, arrowOffset = 2f, arrowPeriod = 0.4f, arrowTimeScl = 6.2f;

    public SepkerWarpConveyor(String name) {
        super(name);

        
        category = Category.distribution;
        buildVisibility = BuildVisibility.shown;

        if (databaseTabs == null) databaseTabs = new ObjectSet<>();
        databaseTabs.add(SepkerPlanet.sepker);

        health = 1;
        size = 1;
        buildTime = 60f * 0.05f;
        itemCapacity = 500;
        speed = 15000f / 60f / 500;
        conductivePower = true;
        consumePower(24f / 60f);

        requirements = ItemStack.with(
            SepkerUraniumItem.uranium, 20,
            SepkerUraniumItem.uraniumStick, 15,
            Items.lead, 10,
            Items.graphite, 10,
            Items.silicon, 15,
            Items.phaseFabric, 15,
            Items.surgeAlloy, 10,
            Items.tungsten, 10
        );

        configurable = true;
        saveConfig = false;
        copyConfig = false;

        config(Integer.class, new Cons2<SepkerWarpConveyorBuild, Integer>() {
            @Override
            public void get(SepkerWarpConveyorBuild build, Integer value) {
                build.applyConfig(value);
            }
        });

        buildType = new Prov<Building>() {
            @Override
            public Building get() {
                return new SepkerWarpConveyorBuild();
            }
        };
    }

    @Override
    public void init() {
        super.init();
        updateClipRadius((warpRange / 8f + 0.5f) * tilesize);
    }

    @Override
    public void load() {
        super.load();
    }

    String atlasName(String modName, String base) {
        StringBuilder sb = new StringBuilder(modName);
        sb.append('-').append(base);
        return sb.toString();
    }

    void lazyInitTextures() {
        if (texLoaded) return;

        String modName = mindustry.Vars.mods.getMod(SepkerMod.class).meta.name;

        TextureRegion test = Core.atlas.find(atlasName(modName, "sepker-warp-conveyor-0"));
        if (test == null || !test.found()) return;

        if (regions != null && regions.length >= 3) {
            regions[0] = test;
            regions[1] = Core.atlas.find(atlasName(modName, "sepker-warp-conveyor-1"));
            regions[2] = Core.atlas.find(atlasName(modName, "sepker-warp-conveyor-2"));
        }

        TextureRegion edge = Core.atlas.find(atlasName(modName, "sepker-warp-conveyor-edge"));
        if (edge != null && edge.found()) edgeRegion = edge;

        TextureRegion stack = Core.atlas.find(atlasName(modName, "sepker-warp-conveyor-stack"));
        if (stack != null && stack.found()) stackRegion = stack;

        TextureRegion glow = Core.atlas.find(atlasName(modName, "sepker-warp-conveyor-glow"));
        if (glow != null && glow.found()) glowRegion = glow;

        TextureRegion edgeGlow = Core.atlas.find(atlasName(modName, "sepker-warp-conveyor-edge-glow"));
        if (edgeGlow != null && edgeGlow.found()) edgeGlowRegion = edgeGlow;

        texBridgeBody = Core.atlas.find(atlasName(modName, "sepker-warp-conveyor-bridge"));
        if (texBridgeBody == null || !texBridgeBody.found()) {
            texBridgeBody = regions != null && regions.length > 0 ? regions[0] : null;
        }

        texBridgeLine = Core.atlas.find(atlasName(modName, "sepker-warp-conveyor-bridge-line"));
        if (texBridgeLine == null || !texBridgeLine.found()) {
            texBridgeLine = Core.atlas.find("phase-conveyor-bridge");
        }

        texBridgeEnd = Core.atlas.find(atlasName(modName, "sepker-warp-conveyor-bridge-end"));
        if (texBridgeEnd == null || !texBridgeEnd.found()) {
            texBridgeEnd = Core.atlas.find("phase-conveyor-end");
        }

        texBridgeArrow = Core.atlas.find(atlasName(modName, "sepker-warp-conveyor-bridge-arrow"));
        if (texBridgeArrow == null || !texBridgeArrow.found()) {
            texBridgeArrow = Core.atlas.find("phase-conveyor-arrow");
        }

        texLoaded = true;
    }

    public class SepkerWarpConveyorBuild extends StackConveyorBuild {

        public int mode = 0;
        public int link = -1;
        public int incomingFrom = -1;

        /** ⭐ 箭头动画时间 */
        public float time = 0f;
        /** ⭐ 箭头动画速度（0 静止，1 全速） */
        public float timeSpeed = 0f;
        /** ⭐ 上一采样周期是否有物品移动 */
        public boolean wasMoved = false;
        /** ⭐ 当前周期是否有物品移动 */
        public boolean moved = false;

        public boolean awaitingConnection = false;

        /** ⭐ 配置面板表格（用于「立即刷新」，不再等下一次点击） */
        public Table configTable;
        /** ⭐ 刷新是否已排队，避免同一帧重复提交 */
        public boolean panelQueued = false;
        /** ⭐ 按钮文字缓存，避免每帧重复 setText 触发重排 */
        public String actionText = "";

        public void applyConfig(int value) {
            if (value == -2) {
                mode = 0;
                clearLinks();
                awaitingConnection = false;
            } else if (value == -3) {
                mode = 1;
            } else if (value >= 0) {
                setLink(value);
                awaitingConnection = false;
            }
            refreshPanel();
        }

        // ============================================================
        // ⭐ 立即刷新配置面板
        //    configure() 只发包不重建 UI，所以这里自己重建表格内容。
        //    必须延迟到下一帧：否则会在按钮回调里把正在处理事件的控件拆掉。
        // ============================================================
        public void refreshPanel() {
            if (panelQueued) return;
            panelQueued = true;
            Core.app.post(new Runnable() {
                @Override
                public void run() {
                    panelQueued = false;
                    rebuildPanel();
                }
            });
        }

        public void rebuildPanel() {
            if (configTable == null) return;
            if (mindustry.Vars.headless || mindustry.Vars.control == null) return;
            if (mindustry.Vars.control.input == null
                || mindustry.Vars.control.input.config == null) return;
            // 面板没开着（或不是这块）就不折腾
            if (mindustry.Vars.control.input.config.getSelected() != this) return;

            configTable.clear();
            buildConfiguration(configTable);
            configTable.pack();
        }

        public void setLink(int targetPos) {
            Building other = world.build(targetPos);
            if (!(other instanceof SepkerWarpConveyorBuild)) return;
            SepkerWarpConveyorBuild b = (SepkerWarpConveyorBuild) other;
            if (b.mode != 1) return;
            if (b.team != team) return;
            if (dst2(b) > warpRange * warpRange) return;
            clearOutgoing();
            link = targetPos;
            b.incomingFrom = pos();
        }

        /** ⭐ 只断开自己的「出口链接」，保留「入口链接」
         *  串联时末端继续往后接，不会把上游那一段一起拆掉 */
        public void clearOutgoing() {
            if (link != -1) {
                Building other = world.build(link);
                if (other instanceof SepkerWarpConveyorBuild
                    && ((SepkerWarpConveyorBuild) other).incomingFrom == pos()) {
                    ((SepkerWarpConveyorBuild) other).incomingFrom = -1;
                }
                link = -1;
            }
        }

        public void clearLinks() {
            if (link != -1) {
                Building other = world.build(link);
                if (other instanceof SepkerWarpConveyorBuild
                    && ((SepkerWarpConveyorBuild) other).incomingFrom == pos()) {
                    ((SepkerWarpConveyorBuild) other).incomingFrom = -1;
                }
                link = -1;
            }
            if (incomingFrom != -1) {
                Building other = world.build(incomingFrom);
                if (other instanceof SepkerWarpConveyorBuild
                    && ((SepkerWarpConveyorBuild) other).link == pos()) {
                    ((SepkerWarpConveyorBuild) other).link = -1;
                }
                incomingFrom = -1;
            }
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(mode);
            write.i(link);
            write.i(incomingFrom);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            mode = read.i();
            link = read.i();
            incomingFrom = read.i();
        }

        @Override
        public void buildConfiguration(Table table) {
            this.configTable = table;
            lazyInitTextures();
            String modName = mindustry.Vars.mods.getMod(SepkerMod.class).meta.name;

            final TextureRegionDrawable icon0 = new TextureRegionDrawable(
                Core.atlas.find(atlasName(modName, "sepker-warp-conveyor-config-0")));
            final TextureRegionDrawable icon1 = new TextureRegionDrawable(
                Core.atlas.find(atlasName(modName, "sepker-warp-conveyor-config-1")));

            ImageButton btn0 = new ImageButton(icon0, Styles.clearTogglei);
            btn0.update(new Runnable() {
                @Override
                public void run() { btn0.setChecked(mode == 0); }
            });
            btn0.clicked(new Runnable() {
                @Override
                public void run() { configure(-2); }
            });
            table.add(btn0).size(48f).pad(4f);

            ImageButton btn1 = new ImageButton(icon1, Styles.clearTogglei);
            btn1.update(new Runnable() {
                @Override
                public void run() { btn1.setChecked(mode == 1); }
            });
            btn1.clicked(new Runnable() {
                @Override
                public void run() { configure(-3); }
            });
            table.add(btn1).size(48f).pad(4f);

            if (mode == 1) {
                table.row();

                // ⭐ 一个按钮三种状态，文字每帧更新 —— 点了立刻变，不用关面板重开
                //   初始文字按当前状态给，避免重建后第一帧是空白按钮
                actionText = awaitingConnection ? "取消连接" : (link == -1 ? "连接模式" : "断开连接");
                final TextButton action = new TextButton(actionText);
                action.update(new Runnable() {
                    @Override
                    public void run() {
                        String want;
                        if (awaitingConnection) {
                            want = "取消连接";
                        } else if (link == -1) {
                            // ⭐ 末端（只接收、没有出口）也显示「连接模式」，
                            //    这样可以直接继续往后串联，不用先断开再重连
                            want = "连接模式";
                        } else {
                            want = "断开连接";
                        }
                        if (!want.equals(actionText)) {
                            actionText = want;
                            action.setText(want);
                        }
                    }
                });
                action.clicked(new Runnable() {
                    @Override
                    public void run() {
                        if (awaitingConnection) {
                            awaitingConnection = false;
                        } else if (link == -1) {
                            awaitingConnection = true;
                        } else {
                            // ⭐ 只断自己的出口，上游那段不动
                            clearOutgoing();
                        }
                    }
                });
                table.add(action).size(200f, 50f).pad(4f).colspan(2);
            }
        }

        @Override
        public Object config() {
            return null;
        }

        @Override
        public boolean onConfigureBuildTapped(Building other) {
            if (!(other instanceof SepkerWarpConveyorBuild)) return true;

            SepkerWarpConveyorBuild b = (SepkerWarpConveyorBuild) other;

            if (mode != 1 || b.mode != 1) return false;
            if (!awaitingConnection) return false;
            if (b.team != team) return false;
            if (dst2(b) > warpRange * warpRange) return false;

            configure(b.pos());
            return false;
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
            if (mode == 0) return super.acceptItem(source, item);

            // 末端（只接收、没有出口）：只收自己的上游，避免从输出侧倒灌
            if (link == -1 && incomingFrom != -1) {
                return source != null
                    && source.pos() == incomingFrom
                    && items.total() < block.itemCapacity;
            }

            // 起点 / 中间 / 尚未连接：一律收。
            // ⭐ 之前这里在 link 和 incomingFrom 都是 -1 时直接 return false，
            //    导致「还没连好的桥接块」完全喂不进去 —— 首端就卡在这一步。
            //    现在先当缓冲区收着，连上之后会自然往下流。
            return items.total() < block.itemCapacity;
        }

        @Override
        public void handleItem(Building source, Item item) {
            if (mode == 0) {
                super.handleItem(source, item);
                return;
            }
            items.add(item, 1);
            noSleep();
        }

        @Override
        public void updateTile() {
            if (mode == 0) {
                super.updateTile();
                return;
            }

            // ⭐ 首端修复：持续把「父类 StackConveyorBuild.link」清空。
            //    原版传送带往相邻传送带整栈转移的条件是 front().link == -1
            //    （StackConveyor.java:303），而本类把 link 遮蔽成了"桥接目标"，
            //    父类的 link 一旦被对方设过就再也没人清，导致普通传送带只推得进第一栈。
            //    必须用父类型强转才能访问到被遮蔽的父类字段。
            ((StackConveyorBuild) this).link = -1;

            // ⭐ 每 30 tick 采样一次 moved
            if (timer(timerCheckMoved, 30f)) {
                wasMoved = moved;
                moved = false;
            }

            // ⭐ 箭头动画速度平滑
            timeSpeed = Mathf.approachDelta(timeSpeed, wasMoved ? 1f : 0f, 1f / 60f);
            time += timeSpeed * Time.delta;

            // 维护 link / incomingFrom
            if (link != -1) {
                Building b = world.build(link);
                if (!(b instanceof SepkerWarpConveyorBuild)
                    || b.team != team
                    || ((SepkerWarpConveyorBuild) b).mode != 1) {
                    link = -1;
                }
            }
            if (incomingFrom != -1) {
                Building b = world.build(incomingFrom);
                if (!(b instanceof SepkerWarpConveyorBuild)
                    || b.team != team
                    || ((SepkerWarpConveyorBuild) b).mode != 1
                    || ((SepkerWarpConveyorBuild) b).link != pos()) {
                    incomingFrom = -1;
                }
            }

            // ⭐ 上游推给下游：成功推一个就 moved = true
            if (link != -1) {
                Building other = world.build(link);
                if (other instanceof SepkerWarpConveyorBuild) {
                    SepkerWarpConveyorBuild b = (SepkerWarpConveyorBuild) other;
                    while (items.total() > 0
                        && b.items.total() < b.block.itemCapacity) {
                        Item item = items.first();
                        items.remove(item, 1);
                        b.items.add(item, 1);
                        b.noSleep();
                        noSleep();
                        moved = true;   // ⭐ 有物品传递
                    }
                }
            }

            // ⭐ 末端 dump：link == -1 就是"没有出口"，就该向四周倾倒。
            //    之前写成 incomingFrom != -1，导致"既没出口也没入口"的块变成黑洞，
            //    物品只进不出。
            if (link == -1) {
                dumpToNeighbors();
            }
        }

        /**
         * 把一个物品交给相邻建筑。必须是 public —— 供本类内部调用即可，但
         * 保持 public 以免以后被匿名内部类访问时踩旧 ART 的坑。
         *
         * 对普通 StackConveyor 邻居，不能只问 acceptItem：它只在自己处于
         * stateLoad 时才收，线中间一律拒绝。原版传送带之间走的是"整栈转移"
         * （StackConveyor.java:301-314），这里照同样的协议交付。
         */
        public boolean pushItemTo(Building neighbor, Item item) {
            if (neighbor instanceof StackConveyorBuild
                && !(neighbor instanceof SepkerWarpConveyorBuild)) {

                StackConveyorBuild sc = (StackConveyorBuild) neighbor;

                // ⭐ 它正对着我们 = 它是上游（往我们这里送货），不要把它倒灌回去
                if (sc.front() == this) return false;

                // 对方的 link 必须是 -1（空闲、没被别的传送带占住），否则它不收
                if (sc.team == team
                    && sc.link == -1
                    && sc.items.total() < sc.getMaximumAccepted(item)) {

                    sc.items.add(item, 1);
                    sc.lastItem = item;
                    sc.link = tile.pos();
                    sc.cooldown = 1;
                    return true;
                }
            }

            if (neighbor.acceptItem(this, item)) {
                neighbor.handleItem(this, item);
                return true;
            }

            return false;
        }

        private void dumpToNeighbors() {
            int guard = 0;
            while (items.total() > 0 && guard++ < block.itemCapacity) {
                Item item = items.first();
                boolean dumped = false;
                for (int i = 0; i < 4; i++) {
                    Building neighbor = nearby(i);
                    if (neighbor == null || neighbor == this) continue;
                    if (neighbor.pos() == incomingFrom) continue;
                    if (pushItemTo(neighbor, item)) {
                        items.remove(item, 1);
                        dumped = true;
                        moved = true;   // ⭐ 有物品传递
                        break;
                    }
                }
                if (!dumped) break;
            }
        }

        @Override
        public void drawCached() {
            lazyInitTextures();

            if (mode == 1) {
                // ⭐ 桥接模式没有自动拼接贴图，必须自己把本体画进缓存层。
                //    之前这里模式 1 什么都不画，缓存层就留了一块空白 →
                //    看起来像"贴图丢失 / 没刷新"。
                if (texBridgeBody != null && texBridgeBody.found()) {
                    Draw.rect(texBridgeBody, x, y, rotdeg());
                }
                return;
            }

            super.drawCached();
        }

        @Override
        public void draw() {
            lazyInitTextures();

            if (mode == 1) {
                Draw.z(Layer.block - 0.1f);
                if (texBridgeBody != null && texBridgeBody.found()) {
                    Draw.rect(texBridgeBody, x, y, rotdeg());
                }
                if (link != -1) {
                    Building other = world.build(link);
                    if (other != null) drawBridge(other.x, other.y);
                }
                return;
            }

            super.draw();
        }

        private void drawBridge(float otherX, float otherY) {
            Draw.z(Layer.power);

            if (Mathf.zero(Renderer.bridgeOpacity)) return;
            Draw.alpha(Renderer.bridgeOpacity);

            int i = relativeTo((int)(otherX / tilesize), (int)(otherY / tilesize));

            if (texBridgeEnd != null && texBridgeEnd.found()) {
                Draw.rect(texBridgeEnd, x, y, i * 90 + 90);
                Draw.rect(texBridgeEnd, otherX, otherY, i * 90 + 270);
            }

            float dx = x - otherX;
            float dy = y - otherY;
            float len = (float) Math.sqrt(dx * dx + dy * dy);

            if (len > 0.001f) {
                float nx = dx / len * (tilesize / 2f);
                float ny = dy / len * (tilesize / 2f);

                if (texBridgeLine != null && texBridgeLine.found()) {
                    Lines.stroke(bridgeWidth);
                    Lines.line(texBridgeLine,
                        x - nx, y - ny,
                        otherX + nx, otherY + ny,
                        false);
                }

                if (texBridgeArrow != null && texBridgeArrow.found() && Lod.l1) {
                    float ux = dx / len;
                    float uy = dy / len;
                    int dist = Math.max(Math.abs((int)(dx / tilesize)), Math.abs((int)(dy / tilesize))) - 1;

                    if (dist > 0) {
                        int arrows = (int)(dist * tilesize / arrowSpacing);
                        float angle = (float) Math.toDegrees(Math.atan2(-uy, -ux));

                        for (int a = 0; a < arrows; a++) {
                            float t = (tilesize / 2f + a * arrowSpacing + arrowOffset);
                            float ax = x - ux * t;
                            float ay = y - uy * t;

                            Draw.alpha(Mathf.absin(a - time / 60f / arrowTimeScl, arrowPeriod, 1f) * Renderer.bridgeOpacity * Lod.alpha1);
                            Draw.rect(texBridgeArrow, ax, ay, angle);
                        }
                    }
                }
            }

            Draw.reset();
        }

        @Override
        public void onRemoved() {
            super.onRemoved();
            clearLinks();
        }
    }
}