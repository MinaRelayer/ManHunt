/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.support;

import me.mina.manhunt.scoreboard.ScoreboardManager;

public class FakeScoreboardManager
implements ScoreboardManager {
    public int startCount = 0;
    public int stopCount = 0;
    public boolean started = false;

    public void start() {
        ++this.startCount;
        this.started = true;
    }

    public void stop() {
        ++this.stopCount;
        this.started = false;
    }

    public void applyToAll() {
    }
}
