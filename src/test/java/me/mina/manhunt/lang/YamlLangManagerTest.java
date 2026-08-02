/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.lang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import me.mina.manhunt.config.PluginConfig;
import me.mina.manhunt.lang.YamlLangManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlLangManagerTest {
    @TempDir
    Path tempDir;
    private final PluginConfig config = new PluginConfig();
    private final List<String> warnings = new ArrayList<String>();
    private final List<String> infos = new ArrayList<String>();
    private static final String EN_US = "prefix: \"<gold>[ManHunt] </gold>\"\nteam-joined: \"<green>You joined the {team} team.\"\nteam-hunter: \"Hunter\"\ngame-runner-disconnected: \"<yellow>{player} disconnected! {seconds}s\"\nonly-in-en: \"<blue>English only\"\n";
    private static final String ZH_CN = "prefix: \"<gold>[ManHunt] </gold>\"\nteam-joined: \"<green>\u4f60\u52a0\u5165\u4e86{team}\u961f\u4f0d\u3002\"\nteam-hunter: \"\u730e\u4eba\"\n";

    YamlLangManagerTest() {
    }

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(this.tempDir.resolve("lang"), new FileAttribute[0]);
        Files.writeString(this.tempDir.resolve("lang/en_us.yml"), (CharSequence)EN_US, new OpenOption[0]);
        Files.writeString(this.tempDir.resolve("lang/zh_cn.yml"), (CharSequence)ZH_CN, new OpenOption[0]);
        this.config.setLanguage("zh_cn");
    }

    private YamlLangManager createLangManager() {
        return new YamlLangManager(this.tempDir.toFile(), this.config, this.warnings::add, this.infos::add);
    }

    @Test
    void loadsConfiguredLanguage() {
        YamlLangManager lang = this.createLangManager();
        lang.load();
        Assertions.assertEquals((Object)"zh_cn", (Object)lang.getCurrentLanguage());
        Assertions.assertEquals((Object)"\u730e\u4eba", (Object)lang.getRaw("team-hunter"));
    }

    @Test
    void fallsBackToEnglishForMissingKeyInCurrentLanguage() {
        YamlLangManager lang = this.createLangManager();
        lang.load();
        Assertions.assertEquals((Object)"<blue>English only", (Object)lang.getRaw("only-in-en"));
        Assertions.assertTrue((boolean)this.warnings.isEmpty());
    }

    @Test
    void returnsKeyItselfWhenMissingEverywhere() {
        YamlLangManager lang = this.createLangManager();
        lang.load();
        Assertions.assertEquals((Object)"missing-key", (Object)lang.getRaw("missing-key"));
        Assertions.assertEquals((int)1, (int)this.warnings.size());
        Assertions.assertTrue((boolean)this.warnings.get(0).contains("missing-key"));
    }

    @Test
    void componentIncludesPrefix() {
        YamlLangManager lang = this.createLangManager();
        lang.load();
        String plain = PlainTextComponentSerializer.plainText().serialize(lang.getComponent("team-joined", new Object[]{"team", "\u730e\u4eba"}));
        Assertions.assertTrue((boolean)plain.contains("[ManHunt]"));
        Assertions.assertTrue((boolean)plain.contains("\u730e\u4eba"));
    }

    @Test
    void prefixKeyIsNotDoublePrefixed() {
        YamlLangManager lang = this.createLangManager();
        lang.load();
        String plain = PlainTextComponentSerializer.plainText().serialize(lang.getComponent("prefix", new Object[0]));
        Assertions.assertEquals((Object)"[ManHunt] ", (Object)plain);
    }

    @Test
    void placeholdersAreReplaced() {
        YamlLangManager lang = this.createLangManager();
        lang.load();
        String value = lang.get("game-runner-disconnected", new Object[]{"player", "Steve", "seconds", "300"});
        Assertions.assertTrue((boolean)value.contains("Steve"));
        Assertions.assertTrue((boolean)value.contains("300"));
    }

    @Test
    void fallsBackToDefaultLanguageWhenConfiguredLanguageMissing() {
        this.config.setLanguage("fr_fr");
        YamlLangManager lang = this.createLangManager();
        lang.load();
        Assertions.assertEquals((Object)"en_us", (Object)lang.getCurrentLanguage());
        Assertions.assertTrue((boolean)this.warnings.stream().anyMatch(w -> w.contains("fr_fr")));
    }
}
