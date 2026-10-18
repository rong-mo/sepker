package com.sepker.blocks;

import com.sepker.content.*;
import com.sepker.*;
import mindustry.content.*;
import mindustry.entities.Effect;
import mindustry.game.EventType.UnitCreateEvent;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.world.consumers.Consume;
import mindustry.ui.Styles;
import arc.Events;
import arc.math.Mathf;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.func.Prov;
import arc.util.io.Reads;
import arc.util.io.Writes;
import java.util.Arrays;

import static mindustry.Vars.*;

public class SepkerCombinedReconstructor extends Reconstructor {

    public static class Recipe {
        public UnitType from;
        public UnitType to;
        public float timeSeconds;
        public ItemStack[] items;
        public LiquidStack[] liquids;

        public Recipe(UnitType from, UnitType to, float timeSeconds, ItemStack[] items) {
            this.from = from;
            this.to = to;
            this.timeSeconds = timeSeconds;
            this.items = items;
        }

        public Recipe liquids(LiquidStack... stacks) {
            this.liquids = stacks;
            return this;
        }
    }

    @SuppressWarnings("unchecked")
    public Seq<Recipe>[] recipePools = new Seq[9];

    public SepkerCombinedReconstructor(String name) {
        super(name);

        category = Category.units;
        buildVisibility = BuildVisibility.shown;

        if (databaseTabs == null) databaseTabs = new ObjectSet<>();
        databaseTabs.add(SepkerPlanet.sepker);

        health = 1000;
        size = 4;
        buildTime = 60f * 5.5f;
        
        hasItems = true;
        hasLiquids = true;
        hasPower = true; 
        liquidCapacity = 60f; 

        consumePower(200f / 60f);
        consume(new ConsumeDynamic());

        requirements = ItemStack.with(
            SepkerUraniumItem.uranium, 100,
            Items.silicon, 140,
            Items.graphite, 40,
            Items.beryllium, 200,
            Items.tungsten, 20
        );

        for (int i = 0; i < 9; i++) {
            recipePools[i] = new Seq<>();
        }

        // 默认给个极低值防止初始化时出错，具体时间在 updateTile 里动态覆盖
        this.constructTime = 1f;

        buildType = new Prov<Building>() {
            @Override
            public Building get() {
                return new SepkerCombinedReconstructorBuild();
            }
        };
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.productionTime); 
        stats.add(Stat.productionTime, "[lightgray]随模式和子模式动态变化[]", StatUnit.none); 
    }

    @Override
    public void init() {
        if (recipePools[0].isEmpty()) {
            // 模式0 (S星)
            addRecipe(0, UnitTypes.dagger, UnitTypes.mace, 10f, ItemStack.with(Items.silicon, 40, Items.graphite, 40));
            addRecipe(0, UnitTypes.nova, UnitTypes.pulsar, 10f, ItemStack.with(Items.silicon, 40, Items.graphite, 40));
            addRecipe(0, UnitTypes.crawler, UnitTypes.atrax, 10f, ItemStack.with(Items.silicon, 40, Items.graphite, 40));
            addRecipe(0, UnitTypes.flare, UnitTypes.horizon, 10f, ItemStack.with(Items.silicon, 40, Items.graphite, 40));
            addRecipe(0, UnitTypes.mono, UnitTypes.poly, 10f, ItemStack.with(Items.silicon, 40, Items.graphite, 40));
            addRecipe(0, UnitTypes.risso, UnitTypes.minke, 10f, ItemStack.with(Items.silicon, 40, Items.graphite, 40));
            addRecipe(0, UnitTypes.retusa, UnitTypes.oxynoe, 10f, ItemStack.with(Items.silicon, 40, Items.graphite, 40));

            // 模式1 (E星)
            addRecipe(3, UnitTypes.stell, UnitTypes.locus, 30f, ItemStack.with(Items.silicon, 40, Items.tungsten, 30)).liquids(new LiquidStack(Liquids.hydrogen, 3f));
            addRecipe(4, UnitTypes.elude, UnitTypes.avert, 50f, ItemStack.with(Items.silicon, 60, Items.tungsten, 40)).liquids(new LiquidStack(Liquids.hydrogen, 3f));
            addRecipe(5, UnitTypes.merui, UnitTypes.cleroi, 45f, ItemStack.with(Items.silicon, 50, Items.tungsten, 40)).liquids(new LiquidStack(Liquids.hydrogen, 3f));
        }
        
        initCapacities();
        super.init();
    }

    public Recipe addRecipe(int poolIndex, UnitType from, UnitType to, float time, ItemStack[] items) {
        Recipe r = new Recipe(from, to, time, items);
        recipePools[poolIndex].add(r);
        return r;
    }

    @Override
    public void initCapacities(){
        capacities = new int[content.items().size];
        Arrays.fill(capacities, 120); 
        itemCapacity = 120; 
        
        consumeBuilder.each(new arc.func.Cons<Consume>() {
            @Override
            public void get(Consume c) {
                c.multiplier = new arc.func.Floatf<Building>() {
                    @Override
                    public float get(Building b) {
                        return state.rules.unitCost(b.team);
                    }
                };
            }
        });
    }

    public class ConsumeDynamic extends Consume {
        @Override
        public float efficiency(Building build) {
            if (!(build instanceof SepkerCombinedReconstructorBuild)) return 1f;
            SepkerCombinedReconstructorBuild b = (SepkerCombinedReconstructorBuild) build;
            Recipe r = b.getCurrentRecipe();
            
            if (r == null) return 0f; 
            if (!b.hasRecipeItems(r)) return 0f; 
            
            if (r.liquids != null) {
                for (LiquidStack ls : r.liquids) {
                    if (b.liquids.get(ls.liquid) < ls.amount * b.delta()) return 0f; 
                }
            }
            return 1f; 
        }

        @Override
        public void update(Building build) {
            if (!(build instanceof SepkerCombinedReconstructorBuild)) return;
            SepkerCombinedReconstructorBuild b = (SepkerCombinedReconstructorBuild) build;
            if (efficiency(build) <= 0f) return;
            
            Recipe r = b.getCurrentRecipe();
            if (r == null) return;

            // 物品在进度为0时一次性扣除
            if (b.progress == 0f && !b.paid) {
                b.consumeRecipeItems(r);
                b.paid = true;
            }
            
            // 液体每帧持续扣除
            if (r.liquids != null) {
                for (LiquidStack ls : r.liquids) {
                    float need = ls.amount * b.delta();
                    b.liquids.remove(ls.liquid, Math.min(b.liquids.get(ls.liquid), need));
                }
            }
        }
    }

    public class SepkerCombinedReconstructorBuild extends ReconstructorBuild {

        public int mode = 0; 
        public int subMode = 0; 
        public boolean paid = false;

        @Override
        public boolean constructing(){
            return payload != null && getCurrentRecipe() != null;
        }

        @Override
        public float fraction(){
            // 原版的进度条逻辑，因为 constructTime 动态更新，此处也能完美工作
            return progress / constructTime;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(mode);
            write.i(subMode);
            write.bool(paid);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            mode = read.i();
            subMode = read.i();
            paid = read.bool();
        }

        @Override
        public int getMaximumAccepted(Item item){
            return 120; 
        }

        public Seq<Recipe> getRecipePool() {
            if (mode == 0) return recipePools[0];
            if (mode == 1) return recipePools[3 + subMode];
            return recipePools[6 + subMode];
        }

        public Recipe getCurrentRecipe() {
            if (payload == null || !(payload instanceof UnitPayload)) return null;
            UnitType from = ((UnitPayload)payload).unit.type;
            for (Recipe r : getRecipePool()) {
                if (r.from == from) return r;
            }
            return null;
        }

        @Override
        public boolean hasUpgrade(UnitType type){
            for (Recipe r : getRecipePool()) {
                if (r.from == type) {
                    return (r.to.unlockedNowHost() || team.isAI()) && !r.to.isBanned();
                }
            }
            return false;
        }

        @Override
        public UnitType upgrade(UnitType type){
            for (Recipe r : getRecipePool()) {
                if (r.from == type) {
                    return r.to;
                }
            }
            return null;
        }

        @Override
        public boolean acceptUnitPayload(Unit unit){
            return hasUpgrade(unit.type);
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            if(this.payload != null) return false;
            if(!(this.enabled || source == this)) return false;
            if(relativeTo(source) == rotation) return false;
            if(!(payload instanceof UnitPayload)) return false;
            UnitPayload up = (UnitPayload) payload;
            return acceptUnitPayload(up.unit);
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
            if (items == null || items.get(item) >= 120) return false;
            if (payload == null) {
                for (Seq<Recipe> pool : recipePools) {
                    for (Recipe r : pool) {
                        for (ItemStack stack : r.items) {
                            if (stack.item == item) return true;
                        }
                    }
                }
                return false;
            }
            Recipe recipe = getCurrentRecipe();
            if (recipe == null) return false;
            for (ItemStack stack : recipe.items) {
                if (stack.item == item) return true;
            }
            return false;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            if (liquids == null || liquids.get(liquid) >= liquidCapacity) return false;
            if (payload == null) {
                for (Seq<Recipe> pool : recipePools) {
                    for (Recipe r : pool) {
                        if (r.liquids != null) {
                            for (LiquidStack stack : r.liquids) {
                                if (stack.liquid == liquid) return true;
                            }
                        }
                    }
                }
                return false;
            }
            Recipe recipe = getCurrentRecipe();
            if (recipe == null) return false;
            if (recipe.liquids != null) {
                for (LiquidStack stack : recipe.liquids) {
                    if (stack.liquid == liquid) return true;
                }
            }
            return false;
        }

        public boolean hasRecipeItems(Recipe recipe) {
            if (recipe == null || items == null) return false; 
            for (ItemStack s : recipe.items) {
                if (!items.has(s.item, s.amount)) return false;
            }
            return true;
        }

        public void consumeRecipeItems(Recipe recipe) {
            if (items != null) {
                for (ItemStack s : recipe.items) {
                    if (items.has(s.item, s.amount)) {
                        items.remove(s.item, s.amount);
                    }
                }
            }
        }

        // ⭐ 终极修复：原版逻辑 + 动态覆盖 constructTime
@Override
public void updateTile() {
    Recipe recipe = getCurrentRecipe();
    
    if (recipe != null) {
        // ⭐ 核心修复：使用 外部类名.this 来访问外层字段
        SepkerCombinedReconstructor.this.constructTime = recipe.timeSeconds * 60f; 
    } else {
        SepkerCombinedReconstructor.this.constructTime = 1f;
        if (payload != null) {
            moveOutPayload();
            progress = 0f;
            paid = false;
        }
    }
    
    // ⭐ 完全交给原版进行进度推进、动画更新、单位生成！
    super.updateTile();
    
    // 如果原版生成了单位，原版会把 progress 重置为 0，我们同步重置 paid 状态
    if (progress == 0f) {
        paid = false;
    }
}

        public void rebuildUI(Table table) {
            buildConfiguration(table);
            StringBuilder sb = new StringBuilder();
            sb.append(mode);
            sb.append(",");
            sb.append(subMode);
            configure(sb.toString());
        }

        @Override
        public void buildConfiguration(Table table) {
            table.clear();
            table.background(Styles.black6);

            Table modeTable = new Table();
            modeTable.defaults().size(64f).pad(4f);

            TextButton btn0 = new TextButton("S星");
            btn0.clicked(new Runnable() {
                @Override
                public void run() {
                    mode = 0; subMode = 0;
                    rebuildUI(table); 
                }
            });
            btn0.update(new Runnable() {
                @Override
                public void run() { btn0.setChecked(mode == 0); }
            });
            modeTable.add(btn0);

            TextButton btn1 = new TextButton("E星");
            btn1.clicked(new Runnable() {
                @Override
                public void run() {
                    mode = 1; subMode = 0;
                    rebuildUI(table);
                }
            });
            btn1.update(new Runnable() {
                @Override
                public void run() { btn1.setChecked(mode == 1); }
            });
            modeTable.add(btn1);

            TextButton btn2 = new TextButton("赛普克尔");
            btn2.clicked(new Runnable() {
                @Override
                public void run() {
                    mode = 2; subMode = 0;
                    rebuildUI(table);
                }
            });
            btn2.update(new Runnable() {
                @Override
                public void run() { btn2.setChecked(mode == 2); }
            });
            modeTable.add(btn2);

            table.add(modeTable).row();

            if (mode == 1 || mode == 2) {
                Table subTable = new Table();
                subTable.defaults().size(72f, 48f).pad(2f);
                
                final String[] names = mode == 1 ? new String[]{"坦克", "飞机", "机甲"} : new String[]{"陆军", "空军", "海军"};
                
                for (int i = 0; i < 3; i++) {
                    final int idx = i;
                    TextButton sub = new TextButton(names[i]);
                    sub.clicked(new Runnable() {
                        @Override
                        public void run() {
                            subMode = idx;
                            rebuildUI(table);
                        }
                    });
                    sub.update(new Runnable() {
                        @Override
                        public void run() { sub.setChecked(subMode == idx); }
                    });
                    subTable.add(sub);
                }
                table.add(subTable).row();
            }

            Recipe currentR = getCurrentRecipe();
            if (currentR != null) {
                StringBuilder timeSb = new StringBuilder();
                timeSb.append("[accent]当前配方时间: ");
                timeSb.append(currentR.timeSeconds);
                timeSb.append(" 秒");
                table.add(timeSb.toString()).padTop(10f).row();
            } else {
                table.add("[lightgray]请放入当前模式对应的单位").padTop(10f).row();
            }

            table.button("关闭", new Runnable() {
                @Override
                public void run() { control.input.config.hideConfig(); }
            }).size(100f, 40f).padTop(10f);
        }

        @Override
        public Object config() {
            StringBuilder sb = new StringBuilder();
            sb.append(mode);
            sb.append(",");
            sb.append(subMode);
            return sb.toString();
        }
    }
}