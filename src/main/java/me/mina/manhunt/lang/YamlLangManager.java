/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.lang;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import me.mina.manhunt.config.PluginConfig;
import me.mina.manhunt.lang.LangManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

public class YamlLangManager
implements LangManager {
    private static final String FALLBACK_LANGUAGE = "en_us";
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final File dataFolder;
    private final PluginConfig config;
    private final Consumer<String> warn;
    private final Consumer<String> info;
    private final Map<String, Configuration> languages = new HashMap<String, Configuration>();
    private String currentLanguage;
    private String prefix = "";

    public YamlLangManager(File dataFolder, PluginConfig config, Consumer<String> warn, Consumer<String> info) {
        this.dataFolder = dataFolder;
        this.config = config;
        this.warn = warn;
        this.info = info;
    }

    @Override
    public void load() {
        this.languages.clear();
        File langDir = new File(this.dataFolder, "lang");
        if (!langDir.exists() || !langDir.isDirectory()) {
            this.warn.accept("Language directory not found: " + langDir.getAbsolutePath());
            return;
        }
        File[] files = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String fileName = file.getName();
            String key = fileName.substring(0, fileName.length() - 4);
            this.languages.put(key, (Configuration)YamlConfiguration.loadConfiguration((File)file));
        }
        this.currentLanguage = this.config.getLanguage();
        if (!this.languages.containsKey(this.currentLanguage)) {
            this.warn.accept("Configured language not found: " + this.currentLanguage + ", falling back to en_us");
            this.currentLanguage = FALLBACK_LANGUAGE;
        }
        if (!this.languages.containsKey(FALLBACK_LANGUAGE)) {
            this.warn.accept("Fallback language en_us not found in lang directory");
        }
        this.prefix = this.getRawValue("prefix");
        this.info.accept("Loaded " + this.languages.size() + " language(s), current: " + this.currentLanguage);
    }

    @Override
    public void reload() {
        this.load();
    }

    @Override
    public String getCurrentLanguage() {
        return this.currentLanguage;
    }

    private String getRawValue(String key) {
        Configuration current = this.languages.get(this.currentLanguage);
        if (current != null && current.contains(key)) {
            return current.getString(key);
        }
        Configuration fallback = this.languages.get(FALLBACK_LANGUAGE);
        if (fallback != null && fallback.contains(key)) {
            return fallback.getString(key);
        }
        this.warn.accept("Missing language key: " + key);
        return key;
    }

    @Override
    public String getRaw(String key) {
        return this.getRawValue(key);
    }

    @Override
    public String get(String key, Object ... placeholders) {
        return this.replacePlaceholders(this.getRawValue(key), placeholders);
    }

    @Override
    public Component getPrefix() {
        return MINI.deserialize(this.prefix);
    }

    @Override
    public Component getComponent(String key, Object ... placeholders) {
        String value = this.replacePlaceholders(this.getRawValue(key), placeholders);
        Component body = MINI.deserialize(value);
        if ("prefix".equals(key)) {
            return body;
        }
        return ((TextComponent)Component.empty().append(this.getPrefix())).append(body);
    }

    @Override
    public Component getComponentWithoutPrefix(String key, Object ... placeholders) {
        String value = this.replacePlaceholders(this.getRawValue(key), placeholders);
        return MINI.deserialize(value);
    }

    @Override
    public String toPlain(String miniMessageInput) {
        if (miniMessageInput == null) {
            return "";
        }
        return PlainTextComponentSerializer.plainText().serialize(MINI.deserialize(miniMessageInput));
    }

    private String replacePlaceholders(String input, Object ... placeholders) {
        if (input == null || placeholders == null || placeholders.length == 0) {
            return input;
        }
        String result = input;
        int i = 0;
        while (i + 1 < placeholders.length) {
            String placeholderKey = String.valueOf(placeholders[i]);
            String placeholderValue = String.valueOf(placeholders[i + 1]);
            result = result.replace("{" + placeholderKey + "}", placeholderValue);
            i += 2;
        }
        return result;
    }
}
