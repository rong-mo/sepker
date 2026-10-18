package com.sepker.blocks;

import mindustry.type.Liquid;
import mindustry.world.consumers.ConsumeCoolant;
import arc.func.Boolf;

/**
 * 只接受指定液体的冷却液消费者。
 * 继承 ConsumeCoolant —— BaseTurret.init() 会把它识别为 coolant，
 * 从而在数据库显示"XXX% 开火速率"，但 filter 只匹配指定液体。
 */
public class SepkerCoolant extends ConsumeCoolant {

    public SepkerCoolant(final Liquid liquid, float amount) {
        super(amount);

        // 覆盖默认 filter —— 只接受指定液体
        this.filter = new Boolf<Liquid>() {
            @Override
            public boolean get(Liquid l) {
                return l == liquid;
            }
        };
    }
}