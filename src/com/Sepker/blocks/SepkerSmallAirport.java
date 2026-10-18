package com.sepker.blocks;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;
import mindustry.entities.Units;
import mindustry.ui.Bar;
import mindustry.graphics.Pal;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.func.Cons;
import arc.func.Floatp;
import arc.func.Prov;

import static mindustry.Vars.content;

public class SepkerSmallAirport extends Block {

    public int capacity = 10;
    public float captureRadius = 5f * 8f;   // 5 格

    /** 机库网格列数（右半区域；行数随存量自适应） */
    public static final int HANGAR_COLS = 2;

    /** 降落 / 起飞动画时长（tick，60 = 1 秒） */
    public float animTime = 30f;

    public SepkerSmallAirport(String name) {
        super(name);

        
        category = Category.units;
        buildVisibility = BuildVisibility.shown;

        if (databaseTabs == null) databaseTabs = new ObjectSet<>();
        databaseTabs.add(SepkerPlanet.sepker);

        health = 800;
        size = 4;
        buildTime = 60f * 8f;
        solid = true;
        update = true;
        configurable = true;

        requirements = ItemStack.with(
            Items.copper, 100,
            Items.lead, 100,
            Items.silicon, 200,
            Items.metaglass, 100
        );

        buildType = new Prov<Building>() {
            @Override
            public Building get() {
                return new SepkerSmallAirportBuild();
            }
        };
    }

    /** ⭐ 机库槽位：记录飞机类型 + 当前血量 */
    public static class PlaneSlot {
        public UnitType type;
        public float health;

        public PlaneSlot(UnitType type, float health) {
            this.type = type;
            this.health = health;
        }
    }

    public class SepkerSmallAirportBuild extends Building {

        /** 机库库存 */
        public Seq<PlaneSlot> storedPlanes = new Seq<>();

        /** 动画状态：0=空闲 1=降落中 2=起飞中 */
        public int animState = 0;
        public float animTimer = 0f;

        /** 即将起飞飞机的槽位（保存血量） */
        public PlaneSlot pendingTakeoff = null;

        /** ⭐ 配置面板表格（用于「立即刷新」，不再等下一次点击） */
        public Table configTable;
        /** ⭐ 刷新是否已排队，避免同一帧重复提交 */
        public boolean panelQueued = false;

        /** ⭐ 正在降落的飞机：动画期间不重复画进机库 */
        public PlaneSlot landingSlot = null;
        /** 降落动画起点（捕获点）与终点（机库槽位），均为方块本地坐标 */
        public float landingFromX = 0f, landingFromY = 0f;
        public float landingToX = 0f, landingToY = 0f;
        /** 降落终点处的缩放（机库格大小）；动画里从 1（原始大小）动态缩到它 */
        public float landingToScale = 1f;
        /** 起飞动画起点（机库槽位，本地坐标） */
        public float takeoffFromX = 0f, takeoffFromY = 0f;
        /** 起飞起点的缩放（机库格大小）；动画里从它动态放大到 1（原始大小） */
        public float takeoffFromScale = 1f;
        /** ⭐ 降落起始朝向：保留捕获时的机头方向，动画里再逐渐转到正上方 */
        public float landingFromRotation = 90f;
        /** ⭐ 单位上限不足的提示计时（tick） */
        public float capWarnTimer = 0f;

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

        // ============================================================
        // 更新
        // ============================================================
        @Override
        public void updateTile() {
            // ⭐ 机库缓慢回血：每秒 50 点（60 tick = 1 秒）
            //    放在最前面，降落/起飞动画期间也照常回血
            if (storedPlanes.size > 0) {
                float heal = 50f * Time.delta / 60f;
                for (PlaneSlot s : storedPlanes) {
                    if (s.health < s.type.health) {
                        s.health = Math.min(s.type.health, s.health + heal);
                    }
                }
            }

            // ⭐ 单位上限提示倒计时
            if (capWarnTimer > 0f) capWarnTimer -= Time.delta;

            // 动画进行中
            if (animState != 0) {
                animTimer += Time.delta;
                if (animTimer >= animTime) {
                    if (animState == 2 && pendingTakeoff != null) {
                        // ⭐ 出生前再查一次单位上限：这半秒里上限可能被别人占满。
                        //    超上限的单位会被原版 unitCapDeath 秒杀（"刚起飞就坠机"）。
                        if (Units.canCreate(team, pendingTakeoff.type)) {
                            // 起飞完成：在「左半中心顶端」（生成点）生成飞机
                            Unit u = pendingTakeoff.type.spawn(
                                team, x + spawnLocalX(), y + spawnLocalY()
                            );
                            if (u != null) {
                                u.rotation = 90f;
                                // ⭐ 恢复血量
                                u.health = pendingTakeoff.health;
                                // ⭐ 刚起飞给 5 秒 fast 效果（原版 fast：速度 ×1.6）
                                u.apply(StatusEffects.fast, 5f * 60f);
                            }
                        } else {
                            // ⭐ 上限被占满 → 放回机库，不能让飞机凭空消失
                            storedPlanes.add(pendingTakeoff);
                            capWarnTimer = 120f;
                            refreshPanel();
                        }
                        pendingTakeoff = null;
                    }
                    if (animState == 1) {
                        landingSlot = null;
                    }
                    animState = 0;
                    animTimer = 0f;
                }
                return;
            }

            // 空闲时检测捕获
            if (storedPlanes.size >= capacity) return;

            Units.nearby(team, x, y - 5f * 8f, captureRadius, new Cons<Unit>() {
                @Override
                public void get(Unit u) {
                    if (u == null || !u.isValid() || u.dead) return;
                    if (u.type != SepkerSeaAirUnits.juYu && u.type != SepkerSeaAirUnits.hongWei) return;
                    if (storedPlanes.size >= capacity) return;

                    // 检查飞机是否正对机场（夹角 < 30°）
                    float toAirport = Mathf.angle(x - u.x, y - u.y);
                    if (Math.abs(Angles.angleDist(u.rotation, toAirport)) > 30f) return;

                    // ⭐ 捕获时记录当前血量
                    PlaneSlot slot = new PlaneSlot(u.type, u.health);
                    landingSlot = slot;
                    // 降落起点 = 捕获点（本地坐标）
                    landingFromX = u.x - x;
                    landingFromY = u.y - y;
                    // ⭐ 保留捕获时的机头朝向，动画里再逐渐转到正上方
                    landingFromRotation = u.rotation;
                    storedPlanes.add(slot);
                    // 降落终点 = 它在机库里的槽位（右半自适应）
                    int idx = storedPlanes.size - 1;
                    landingToX = hangarSlotLocalX(idx, storedPlanes.size);
                    landingToY = hangarSlotLocalY(idx, storedPlanes.size);
                    landingToScale = hangarSlotScale(u.type, storedPlanes.size);
                    u.remove();
                    animState = 1;
                    animTimer = 0f;
                    refreshPanel();   // ⭐ 库存数字立即更新
                }
            });
        }

        // ============================================================
        // 右侧进度条
        // ============================================================
        @Override
        public void displayBars(Table table) {
            super.displayBars(table);

            // ⭐ 飞机容量：已存 / 上限
            table.add(new Bar("飞机容量", Pal.ammo, new Floatp() {
                @Override
                public float get() {
                    return storedPlanes.size / (float) capacity;
                }
            })).row();

            // ⭐ 飞机血量：库存总血量 / 满血总量
            table.add(new Bar("飞机血量", Pal.health, new Floatp() {
                @Override
                public float get() {
                    if (storedPlanes.size == 0) return 0f;
                    float cur = 0f;
                    float max = 0f;
                    for (PlaneSlot s : storedPlanes) {
                        cur += s.health;
                        max += s.type.health;
                    }
                    return max > 0f ? cur / max : 0f;
                }
            }));
        }

        // ============================================================
        // 配置面板：起飞按钮（二级菜单）
        // ============================================================
        @Override
        public void buildConfiguration(Table table) {
            this.configTable = table;

            // ⭐ 第一行：库存（StringBuilder，不能用 +）
            StringBuilder sbHead = new StringBuilder();
            sbHead.append("库存：").append(storedPlanes.size).append("/").append(capacity);
            table.add(sbHead.toString()).pad(4f).colspan(2).row();

            // ⭐ 上限不足提示
            if (capWarnTimer > 0f) {
                table.add("⚠ 已达单位上限，无法起飞").color(Color.scarlet).pad(4f).colspan(2).row();
            }

            if (storedPlanes.size == 0) {
                table.add("无机库").pad(4f).colspan(2);
                return;
            }

            // 统计每种飞机数量
            ObjectMap<UnitType, Integer> counts = new ObjectMap<>();
            for (PlaneSlot s : storedPlanes) {
                counts.put(s.type, counts.get(s.type, 0) + 1);
            }

            // 遍历每种类型，各生成一个起飞按钮
            for (UnitType t : counts.keys()) {
                final UnitType type = t;
                int count = counts.get(t);

                // ⭐ 按钮文字（StringBuilder，不能用 +）
                StringBuilder sbBtn = new StringBuilder();
                sbBtn.append("起飞 ").append(type.localizedName).append(" ×").append(count);

                TextButton btn = new TextButton(sbBtn.toString());
                btn.clicked(new Runnable() {
                    @Override
                    public void run() {
                        takeoff(type);
                    }
                });
                table.add(btn).size(240f, 50f).pad(4f).colspan(2).row();
            }
        }

        /** ⭐ 从库存里取出一架指定类型的飞机，开始起飞动画
         *  必须是 public —— 匿名内部类需要从外部访问，
         *  Android 旧 ART 不支持 nestmate 机制（private 会报 IllegalAccessError）
         */
        public void takeoff(UnitType type) {
            if (animState != 0) return;
            if (storedPlanes.size == 0) return;

            // ⭐ 单位上限检查：超上限的飞机出生即被原版 unitCapDeath 秒杀
            //    （原版 UnitComp.add()：count() > cap() → unitCapDeath），
            //    所以上限已满时直接拒绝起飞，并把提示显示在面板上。
            if (!Units.canCreate(team, type)) {
                capWarnTimer = 120f;
                refreshPanel();
                return;
            }

            for (int i = 0; i < storedPlanes.size; i++) {
                PlaneSlot s = storedPlanes.get(i);
                if (s.type == type) {
                    // ⭐ 起飞起点 = 它在机库里的槽位
                    takeoffFromX = hangarSlotLocalX(i, storedPlanes.size);
                    takeoffFromY = hangarSlotLocalY(i, storedPlanes.size);
                    takeoffFromScale = hangarSlotScale(s.type, storedPlanes.size);
                    pendingTakeoff = s;
                    storedPlanes.remove(i);
                    animState = 2;
                    animTimer = 0f;
                    refreshPanel();   // ⭐ 库存和起飞按钮立即更新
                    return;
                }
            }
        }

        // ============================================================
        // 绘制
        // ============================================================

        /** 方块半宽 / 半高（世界单位）：4x4 方块 → 16 */
        public float halfSize() { return size * 4f; }

        /** 左半中心（跑道）的 x：起飞点（中点）与生成点（顶端）都在这条竖线上 */
        public float runwayX() { return -size * 2f; }

        /** 生成点 = 左半中心顶端 */
        public float spawnLocalX() { return -size * 2f; }
        public float spawnLocalY() { return size * 4f; }

        /** 机库网格：右半区域，HANGAR_COLS 列，行数随存量自适应 */
        public float hangarCellW() { return size * 4f / HANGAR_COLS; }

        public float hangarCellH(int count) {
            int rows = Math.max(1, (count + HANGAR_COLS - 1) / HANGAR_COLS);
            return size * 8f / rows;
        }

        public float hangarSlotLocalX(int index, int count) {
            int col = index % HANGAR_COLS;
            return (col + 0.5f) * hangarCellW();
        }

        public float hangarSlotLocalY(int index, int count) {
            int row = index / HANGAR_COLS;
            return size * 4f - (row + 0.5f) * hangarCellH(count);
        }

        /** 角度插值：沿最短方向从 from 转到 to（t = 0..1） */
        public static float lerpAngle(float from, float to, float t) {
            float diff = ((to - from) % 360f + 540f) % 360f - 180f;
            return from + diff * t;
        }

        /** 贴图的「原始大小」：原版贴图按 4 像素/世界单位制作（scl() = 0.25），
         *  直接用 region.width 会大 4 倍，必须乘 scl()。 */
        public float texW(TextureRegion r) { return r.width * r.scl(); }
        public float texH(TextureRegion r) { return r.height * r.scl(); }

        /** 该机型在机库格子里应缩到多少（相对单位原始大小） */
        public float hangarSlotScale(UnitType unitType, int count) {
            if (unitType == null || unitType.region == null || !unitType.region.found()) return 1f;
            float nw = texW(unitType.region);
            float nh = texH(unitType.region);
            return Math.min(hangarCellW() * 0.85f / nw, hangarCellH(count) * 0.85f / nh);
        }

        /** 画一层贴图（本体或武器）。scale = 1 为原始大小。 */
        public void drawLayer(TextureRegion r, float wx, float wy, float rot, float scale) {
            if (r == null || !r.found()) return;
            Draw.rect(r, wx, wy, texW(r) * scale, texH(r) * scale, rot);
        }

        /** 画一架飞机：本体贴图 + 武器贴图（如果有）。不画 -cell 层。
         *  angle 用单位自己的 rotation；原版单位贴图朝上，所以绘制角度要 -90。 */
        public void drawPlane(UnitType type, float wx, float wy, float angle, float scale) {
            if (type == null || type.region == null || !type.region.found()) return;

            float rot = angle - 90f;

            // 本体
            drawLayer(type.region, wx, wy, rot, scale);

            // 武器：原版已把 mirror 的副本按 -x 展开进 type.weapons，逐个画即可。
            //       静置展示用 baseRotation（不跟瞄准角）。
            for (int i = 0; i < type.weapons.size; i++) {
                Weapon w = type.weapons.get(i);
                if (w == null || w.region == null || !w.region.found()) continue;
                float ox = Angles.trnsx(rot, w.x, w.y);
                float oy = Angles.trnsy(rot, w.x, w.y);
                drawLayer(w.region, wx + ox, wy + oy, rot + w.baseRotation, scale);
            }
        }

        @Override
        public void draw() {
            // 底板（机场自己的贴图，目前还没画）
            Draw.rect(region, x, y);

            float half = halfSize();
            float rw = runwayX();

            // ---------- 机库内停放的飞机（右半，自适应大小） ----------
            int count = storedPlanes.size;
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    PlaneSlot s = storedPlanes.get(i);
                    // 正在降落的那架交给动画画，别在机库里重复画一遍
                    if (s == landingSlot) continue;
                    drawPlane(
                        s.type,
                        x + hangarSlotLocalX(i, count),
                        y + hangarSlotLocalY(i, count),
                        90f, hangarSlotScale(s.type, count)
                    );
                }
            }

            // ---------- 降落动画 ----------
            // 路径：降落点 → 跑道末端（左半中心末端）→ 左半中心中点 → 机库槽位
            // 朝向：保留捕获时的机头方向，逐渐转到正上方（90°）
            if (animState == 1 && landingSlot != null) {
                float t = Mathf.clamp(animTimer / animTime, 0f, 1f);
                float lx, ly;

                if (t < 0.45f) {
                    float k = t / 0.45f;
                    lx = Mathf.lerp(landingFromX, rw, k);
                    ly = Mathf.lerp(landingFromY, -half, k);
                } else if (t < 0.75f) {
                    float k = (t - 0.45f) / 0.30f;
                    lx = rw;
                    ly = Mathf.lerp(-half, 0f, k);
                } else {
                    float k = (t - 0.75f) / 0.25f;
                    lx = Mathf.lerp(rw, landingToX, k);
                    ly = Mathf.lerp(0f, landingToY, k);
                }

                float angle = lerpAngle(landingFromRotation, 90f, t);
                // ⭐ 动态缩减：从单位原始大小一路缩到机库格大小，不跳变
                float sc = Mathf.lerp(1f, landingToScale, t);
                drawPlane(landingSlot.type, x + lx, y + ly, angle, sc);
            }

            // ---------- 起飞动画 ----------
            // 路径：机库槽位 → 起飞点（左半中心）→ 直线平移至生成点（左半中心顶端）
            if (animState == 2 && pendingTakeoff != null) {
                float t = Mathf.clamp(animTimer / animTime, 0f, 1f);
                float lx, ly;

                if (t < 0.5f) {
                    float k = t / 0.5f;
                    lx = Mathf.lerp(takeoffFromX, rw, k);
                    ly = Mathf.lerp(takeoffFromY, 0f, k);
                } else {
                    float k = (t - 0.5f) / 0.5f;
                    lx = rw;
                    ly = Mathf.lerp(0f, half, k);
                }

                // ⭐ 动态放大：从机库格大小一路放大到单位原始大小，不跳变
                float sc = Mathf.lerp(takeoffFromScale, 1f, t);
                drawPlane(pendingTakeoff.type, x + lx, y + ly, 90f, sc);
            }
        }

        // ============================================================
        // 存档
        // ============================================================
        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(storedPlanes.size);
            for (PlaneSlot s : storedPlanes) {
                write.s(s.type.id);
                write.f(s.health);   // ⭐ 同时存血量
            }
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            int n = read.i();
            if (storedPlanes == null) storedPlanes = new Seq<>();
            storedPlanes.clear();
            for (int i = 0; i < n; i++) {
                short id = read.s();
                float hp = read.f();
                UnitType t = content.getByID(mindustry.ctype.ContentType.unit, id);
                if (t != null) {
                    storedPlanes.add(new PlaneSlot(t, hp));
                }
            }
        }
    }
}