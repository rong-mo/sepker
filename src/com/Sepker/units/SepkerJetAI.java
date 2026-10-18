package com.sepker.units;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.ai.types.CommandAI;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.util.Time;

/**
 * 完全自定义的飞机 AI。
 *
 * RTS 指令通过 CommandAI 的公共方法写入字段：
 *   - commandPosition(Vec2)   → targetPos
 *   - commandTarget(Teamc)    → attackTarget
 *   - commandQueue(Position)  → commandQueue
 *   - command(UnitCommand)    → command
 *
 * ⭐ 到达路径点后不丢弃，重新放回队列末尾 → 循环巡逻
 */
public class SepkerJetAI extends CommandAI {

    public static final float MIN_SPEED = 3f * 8f / 60f;
    public static final float ACCEL = 0.05f;
    public static final float SLOW_RADIUS = 5f * 8f;
    public static final float ARRIVE_RADIUS = 8f;

    /** 转向角速度（度/秒）：由转弯半径决定。
     *  玩家直接操控（SepkerJetUnit）与 RTS 指令共用这一个来源，手感才一致。 */
    public static float turnDegPerSec(UnitType type) {
        float speedPerSec = type.speed * 60f;
        float radiusWorld = SepkerSeaAirUnits.turnRadiusOf(type) * 8f;
        return (float) Math.toDegrees(speedPerSec / radiusWorld);
    }

    /** 本帧最多能转多少度 */
    public static float maxTurnPerFrame(UnitType type) {
        return turnDegPerSec(type) * Time.delta / 60f;
    }

    @Override
    public void updateUnit() {
        Unit unit = unit();
        if (unit == null) return;

        // ---------- 0. 保证 command 非空 ----------
        if (command == null && unit.type.commands.size > 0) {
            command = unit.type.defaultCommand == null
                ? unit.type.commands.first()
                : unit.type.defaultCommand;
        }

        // ---------- 1. 原版非移动逻辑 ----------
        updateVisuals();
        updateTargeting();

        // ---------- 2. 目标有效性 ----------
        if (attackTarget != null && invalid(attackTarget)) {
            attackTarget = null;
            targetPos = null;
        }

        // ---------- 3. 队列推进 ----------
        if (attackTarget == null && targetPos == null) {
            advanceQueue();
        }

        // ---------- 4. 快照目标 ----------
        float snapX = 0f, snapY = 0f;
        boolean hasRtsOrder = false;
        boolean isAttackOrder = false;

        if (attackTarget != null) {
            snapX = attackTarget.getX();
            snapY = attackTarget.getY();
            hasRtsOrder = true;
            isAttackOrder = true;
        } else if (targetPos != null) {
            snapX = targetPos.x;
            snapY = targetPos.y;
            hasRtsOrder = true;
        }

        // ---------- 5. 移动 ----------
        if (hasRtsOrder) {
            float targetAngle = Angles.angle(unit.x, unit.y, snapX, snapY);

            float diff = targetAngle - unit.rotation;
            diff = ((diff % 360f) + 540f) % 360f - 180f;

            float maxTurn = maxTurnPerFrame(unit.type);
            unit.rotation += Mathf.clamp(diff, -maxTurn, maxTurn);

            float dx = snapX - unit.x;
            float dy = snapY - unit.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            float speed = unit.type.speed;
            if (dist < SLOW_RADIUS) {
                speed = Math.max(MIN_SPEED, speed * dist / SLOW_RADIUS);
            }

            if (isAttackOrder) {
                float weaponRange = unit.range();
                if (dist < weaponRange) {
                    speed = MIN_SPEED;
                }
            }

            unit.vel.set(
                Angles.trnsx(unit.rotation, speed),
                Angles.trnsy(unit.rotation, speed)
            );

            // ⭐ 到达路径点：不放回原处丢弃，而是循环——
            //   把当前点重新塞回队列末尾，让飞机在所有点之间循环飞行
            if (!isAttackOrder && dist < ARRIVE_RADIUS) {
                Vec2 reached = targetPos;
                targetPos = null;

                if (reached != null) {
                    // 队列里还有其它路径点 → 循环
                    if (commandQueue.size > 0) {
                        commandQueue.add(reached.cpy());
                    }
                    // 队列空 → 纯单点命令，正常结束（不动）
                }

                advanceQueue();
            }

        } else {
            // 无 RTS 指令：自动向前
            unit.vel.add(
                Angles.trnsx(unit.rotation, ACCEL),
                Angles.trnsy(unit.rotation, ACCEL)
            );

            float maxSpeed = unit.type.speed;
            float curSpeed = unit.vel.len();

            if (curSpeed > maxSpeed) {
                unit.vel.setLength(maxSpeed);
            } else if (curSpeed < MIN_SPEED) {
                if (curSpeed > 0.0001f) {
                    unit.vel.setLength(MIN_SPEED);
                } else {
                    unit.vel.set(
                        Angles.trnsx(unit.rotation, MIN_SPEED),
                        Angles.trnsy(unit.rotation, MIN_SPEED)
                    );
                }
            }
        }
    }

    /** 从命令队列里取下一个目标 */
    private void advanceQueue() {
        if (commandQueue.size == 0) return;

        Position next = commandQueue.remove(0);

        if (next instanceof Teamc) {
            attackTarget = (Teamc) next;
            targetPos = null;
        } else if (next instanceof Vec2) {
            targetPos = ((Vec2) next).cpy();
            attackTarget = null;
        }
    }
}