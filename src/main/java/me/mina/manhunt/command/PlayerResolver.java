/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.command;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface PlayerResolver {
    public Player resolve(String var1);
}
