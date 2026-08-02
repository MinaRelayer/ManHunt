/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.team;

import net.kyori.adventure.text.format.NamedTextColor;

public enum TeamType {
    HUNTER(NamedTextColor.RED),
    RUNNER(NamedTextColor.GREEN),
    SPECTATOR(NamedTextColor.GRAY),
    NONE(null);

    private final NamedTextColor color;

    private TeamType(NamedTextColor color) {
        this.color = color;
    }

    public NamedTextColor getColor() {
        return this.color;
    }

    public String getConfigKey() {
        return this.name().toLowerCase();
    }
}
