/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.mina.manhunt.lang.LangManager;
import net.kyori.adventure.text.Component;

public class FakeLangManager
implements LangManager {
    public final Map<String, String> messages = new HashMap<String, String>();
    public final List<String> sentKeys = new ArrayList<String>();
    public String currentLanguage = "en_us";

    public void load() {
    }

    public void reload() {
    }

    public String getCurrentLanguage() {
        return this.currentLanguage;
    }

    public String getRaw(String key) {
        return this.messages.getOrDefault(key, key);
    }

    public String get(String key, Object ... placeholders) {
        String value = this.getRaw(key);
        int i = 0;
        while (i + 1 < placeholders.length) {
            value = value.replace("{" + String.valueOf(placeholders[i]) + "}", String.valueOf(placeholders[i + 1]));
            i += 2;
        }
        return value;
    }

    public Component getPrefix() {
        return Component.text((String)"[ManHunt] ");
    }

    public Component getComponent(String key, Object ... placeholders) {
        this.sentKeys.add(key);
        return Component.text((String)this.get(key, placeholders));
    }

    public Component getComponentWithoutPrefix(String key, Object ... placeholders) {
        this.sentKeys.add(key);
        return Component.text((String)this.get(key, placeholders));
    }

    public String toPlain(String miniMessageInput) {
        return miniMessageInput == null ? "" : miniMessageInput;
    }
}
