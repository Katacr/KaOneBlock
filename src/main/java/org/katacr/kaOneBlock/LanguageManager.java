package org.katacr.kaOneBlock;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> languages = new HashMap<>();
    private FileConfiguration currentLang;

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadLanguageFiles() {
        // 清空现有语言
        languages.clear();

        // 从lang目录加载所有语言文件
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (langDir.exists() && langDir.isDirectory()) {
            File[] langFiles = langDir.listFiles((dir, name) -> name.startsWith("lang_") && name.endsWith(".yml"));

            if (langFiles != null) {
                for (File langFile : langFiles) {
                    String fileName = langFile.getName();
                    // 从文件名提取语言代码: lang_xx_XX.yml -> xx_XX
                    String langCode = fileName.substring(5, fileName.length() - 4);

                    FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
                    languages.put(langCode, langConfig);
                    plugin.getLogger().info("Loaded language file: " + fileName + " -> " + langCode);
                }
            }
        }

        // 根据配置设置当前语言
        String language = plugin.getConfig().getString("language", "en_US");
        setLanguage(language);
    }

    private void setLanguage(String langCode) {
        if (languages.containsKey(langCode)) {
            currentLang = languages.get(langCode);
            plugin.getLogger().info(getMessage("language-loaded").replace("%language%", langCode));
        } else {
            // 如果指定的语言未找到，回退到英语
            if (languages.containsKey("en_US")) {
                currentLang = languages.get("en_US");
                plugin.getLogger().warning("Language '" + langCode + "' not found. Falling back to en_US.");
            } else if (!languages.isEmpty()) {
                // 回退到任何可用的语言
                currentLang = languages.values().iterator().next();
                plugin.getLogger().warning("Language '" + langCode + "' not found. Falling back to first available language.");
            } else {
                // 未找到语言文件，创建空配置以防止NPE
                currentLang = new YamlConfiguration();
                plugin.getLogger().severe("No language files found! Please check your lang directory.");
            }
        }
    }

    public String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', currentLang.getString(key, "Message not found: " + key));
    }
}