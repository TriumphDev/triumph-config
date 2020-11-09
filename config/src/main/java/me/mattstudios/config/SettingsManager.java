package me.mattstudios.config;

import me.mattstudios.config.migration.MigrationService;
import me.mattstudios.config.properties.Property;
import me.mattstudios.config.configurationdata.ConfigurationData;
import me.mattstudios.config.resource.PropertyResource;

/**
 * Settings manager.
 * <p>
 * The settings manager manages a {@link PropertyResource property resource},
 * {@link ConfigurationData configuration data}, and an optional
 * {@link MigrationService migration service}.
 * <p>
 * The settings manager allows to look up and modify properties. After it is initialized, the settings manager
 * should be the only class from ConfigMe that developers need to interact with.
 *
 * @see <a href="https://github.com/AuthMe/ConfigMe">ConfigMe on Github</a>
 * @see SettingsManagerBuilder
 */
public interface SettingsManager {

    /**
     * Gets the given property from the configuration.
     *
     * @param property The property to retrieve
     * @param <T> The property's type
     * @return The property's value
     */
    <T> T getProperty(Property<T> property);

    /**
     * Sets a new value for the given property.
     *
     * @param property The property to modify
     * @param value The new value to assign to the property
     * @param <T> The property's type
     */
    <T> void setProperty(Property<T> property, T value);

    /**
     * Reloads the configuration.
     */
    void reload();

    /**
     * Saves the properties to the configuration file.
     */
    void save();

}
