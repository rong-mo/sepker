package com.sepker.logic;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.logic.*;
import mindustry.gen.LogicIO;
import arc.func.*;
import arc.util.Log;

public class SepkerLogicStatements {

    public static void register() {
        try {
            LAssembler.customParsers.put("firesupport",
                new Func<String[], LStatement>() {
                    @Override
                    public LStatement get(String[] tokens) {
                        FireSupportStatement s = new FireSupportStatement();

                        // ⭐ unquote 内联，避免 private 方法被匿名类访问
                        if (tokens.length > 1) {
                            String t = tokens[1];
                            if (t != null && t.length() >= 2
                                && t.charAt(0) == '"'
                                && t.charAt(t.length() - 1) == '"') {
                                t = t.substring(1, t.length() - 1);
                            }
                            s.mode = t;
                        }

                        if (tokens.length > 2) s.team   = tokens[2];
                        if (tokens.length > 3) s.x      = tokens[3];
                        if (tokens.length > 4) s.y      = tokens[4];
                        if (tokens.length > 5) s.angle  = tokens[5];
                        if (tokens.length > 6) s.damage = tokens[6];
                        if (tokens.length > 7) s.radius = tokens[7];
                        return s;
                    }
                });

            LogicIO.allStatements.add(new Prov<LStatement>() {
                @Override
                public LStatement get() {
                    return new FireSupportStatement();
                }
            });

            // ⭐ fastmessage：可即时替换文本的快速消息指令
            LAssembler.customParsers.put("fastmessage",
                new Func<String[], LStatement>() {
                    @Override
                    public LStatement get(String[] tokens) {
                        FastMessageStatement s = new FastMessageStatement();

                        if (tokens.length > 1) {
                            s.kind = FastMessageStatement.kindFromToken(
                                FastMessageStatement.unquote(tokens[1]));
                        }

                        if (tokens.length > 2) s.duration = tokens[2];
                        if (tokens.length > 3) s.cover = !tokens[3].equals("wait");

                        if (s.kind == FastMessageStatement.K_COMM) {
                            if (tokens.length > 4) {
                                s.commStop = FastMessageStatement.unquote(tokens[4]).equals("stop");
                            }
                            if (tokens.length > 5) {
                                s.commColor = FastMessageStatement.unquote(tokens[5]);
                            }
                        } else if (s.kind == FastMessageStatement.K_DIALOG) {
                            if (tokens.length > 4) {
                                s.dialogMode = FastMessageStatement.dialogModeFromToken(
                                    FastMessageStatement.unquote(tokens[4]));
                            }
                            if (tokens.length > 5) {
                                s.name = FastMessageStatement.unquote(tokens[5]);
                            }
                            if (s.dialogMode == FastMessageStatement.D_CHOOSE) {
                                int count = 0;
                                if (tokens.length > 6 && tokens[6] != null) {
                                    try {
                                        count = Integer.parseInt(
                                            FastMessageStatement.unquote(tokens[6]));
                                    } catch (Throwable ignored) {
                                    }
                                }
                                if (count < 0) count = 0;
                                if (count > 9) count = 9;
                                for (int i = 0; i < count; i++) {
                                    int idx = 7 + i;
                                    if (idx < tokens.length && tokens[idx] != null) {
                                        s.options.add(FastMessageStatement.unquote(tokens[idx]));
                                    }
                                }
                                s.ensureOptions();
                            }
                        }

                        return s;
                    }
                });

            LogicIO.allStatements.add(new Prov<LStatement>() {
                @Override
                public LStatement get() {
                    return new FastMessageStatement();
                }
            });

            Log.info("[SepkerMod] Logic statements registered (firesupport, fastmessage).");

        } catch (Throwable t) {
            Log.err("[SepkerMod] Failed to register custom logic statement", t);
        }
    }
}