package me.mattstudios.config.properties;

import me.mattstudios.config.internal.yaml.YamlManager;
import org.jetbrains.annotations.NotNull;

public final class StringProperty extends BaseProperty<String> {

    public StringProperty(@NotNull final String defaultValue) {
        super(defaultValue);

    }

    @NotNull
    @Override
    public String determineValue(@NotNull final YamlManager yamlManager) {
        final String value = yamlManager.getValue(getPath(), String.class);
        if (value == null) return getDefaultValue();
        return value;
    }

    @NotNull
    @Override
    public String getExportValue(@NotNull final String key, @NotNull final Object value) {
        return key + ": \"" + value + '"';
    }

}
