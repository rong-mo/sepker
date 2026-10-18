package com.sepker.units;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.gen.Player;
import mindustry.gen.UnitEntity;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;

/**
 * 飞机专用单位实体。
 *
 * 只处理玩家直接操控时的 W/S 油门/刹车。
 * AI / RTS 的 rotation 和 vel 由 SepkerJetAI 完全接管。
 */
public class SepkerJetUnit extends UnitEntity {

    public static final float MIN_SPEED = SepkerJetAI.MIN_SPEED;
    public static final float ACCEL     = SepkerJetAI.ACCEL;

    public float currentSpeed = MIN_SPEED;

    @Override
    public void movePref(Vec2 movement) {
        // AI 控制 → 交给原版（SepkerJetAI 会覆盖结果）
        if (!(controller() instanceof Player)) {
            super.movePref(movement);
            return;
        }

        // ---------- 玩家直接操控 ----------
        // 无输入：保持机身方向滑行，不转向
        if (movement == null || movement.len2() < 0.0001f) {
            vel.set(
                Angles.trnsx(rotation, currentSpeed),
                Angles.trnsy(rotation, currentSpeed)
            );
            return;
        }

        // ⭐ 自动转向：朝玩家指定方向偏航。
        //    原版不会替玩家转向 —— UnitComp.prefRotation() 对 omniMovement=false
        //    的单位直接返回当前 rotation（见原版 UnitComp.java:196-206），
        //    而 lookAt() 在 movePref() 之前调用，所以转向只能在这里自己做。
        //    转向率与 SepkerJetAI 的 RTS 转向共用同一来源，保证手感一致。
        float diff = movement.angle() - rotation;
        diff = ((diff % 360f) + 540f) % 360f - 180f;
        float maxTurn = SepkerJetAI.maxTurnPerFrame(type);
        rotation += Mathf.clamp(diff, -maxTurn, maxTurn);

        // 油门/刹车：把玩家意图投影到机身方向
        float fx = Angles.trnsx(rotation, 1f);
        float fy = Angles.trnsy(rotation, 1f);
        float proj = movement.x * fx + movement.y * fy;

        if (proj > 0.01f) {
            currentSpeed = Math.min(type.speed, currentSpeed + ACCEL);
        } else if (proj < -0.01f) {
            currentSpeed = Math.max(MIN_SPEED, currentSpeed - ACCEL);
        }

        vel.set(
            Angles.trnsx(rotation, currentSpeed),
            Angles.trnsy(rotation, currentSpeed)
        );
    }
}