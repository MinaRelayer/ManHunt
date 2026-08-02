/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.lang;

import net.kyori.adventure.text.Component;

public interface LangManager {
    public void load();

    public void reload();

    public String getCurrentLanguage();

    public String getRaw(String var1);

    public String get(String var1, Object ... var2);

    public Component getPrefix();

    public Component getComponent(String var1, Object ... var2);

    public Component getComponentWithoutPrefix(String var1, Object ... var2);

    public String toPlain(String var1);
}
