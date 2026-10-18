package com.sepker.content;

import mindustry.Vars;
import mindustry.type.Sector;
import mindustry.type.*;
import mindustry.world.modules.ItemModule.ItemConsumer;   // ⭐ 加这个
import arc.struct.Seq;

public class MergedItemSeq extends ItemSeq {

    public final ItemSeq real;
    public final Planet target;

    public MergedItemSeq(ItemSeq real, Planet target) {
        this.real = real;
        this.target = target;
    }

    @Override
    public int get(Item item) {
        int sum = real.get(item);
        if (target != null) {
            for (Sector s : target.sectors) {
                if (s.hasBase() && s.info != null) {
                    sum += s.info.items.get(item);
                }
            }
        }
        return sum;
    }

    @Override
    public boolean has(Item item, int amount) {
        return get(item) >= amount;
    }

    @Override
    public boolean has(ItemSeq seq) {
        for (Item item : Vars.content.items()) {
            if (seq.get(item) > get(item)) return false;
        }
        return true;
    }

    @Override
    public void each(ItemConsumer cons) {   // ⭐ 简化
        for (Item item : Vars.content.items()) {
            int v = get(item);
            if (v != 0) cons.accept(item, v);
        }
    }

    @Override
    public Seq<ItemStack> toSeq() {
        Seq<ItemStack> out = new Seq<>();
        for (Item item : Vars.content.items()) {
            int v = get(item);
            if (v != 0) out.add(new ItemStack(item, v));
        }
        return out;
    }

    @Override
    public ItemStack[] toArray() {
        return toSeq().toArray(ItemStack.class);
    }

    @Override public void clear() { real.clear(); }
    @Override public void set(Item item, int amount) { real.set(item, amount); }
    @Override public void add(Item item, int amount) { real.add(item, amount); }
    @Override public void remove(Item item, int amount) { real.remove(item, amount); }
    @Override public void min(int number) { real.min(number); }
}