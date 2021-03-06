package me.mattstudios.config.configurationdata;

import me.mattstudios.config.SettingsHolder;
import me.mattstudios.config.annotations.Comment;
import me.mattstudios.config.annotations.Description;
import me.mattstudios.config.annotations.Path;
import me.mattstudios.config.exception.ConfigMeException;
import me.mattstudios.config.properties.BaseProperty;
import me.mattstudios.config.properties.OptionalProperty;
import me.mattstudios.config.properties.Property;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utility class responsible for retrieving all {@link Property} fields
 * from {@link SettingsHolder} implementations via reflection.
 * <p>
 * Properties must be declared as {@code public static} fields or they are ignored.
 */
public class ConfigurationDataBuilder {

    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected PropertyListBuilder propertyListBuilder = new PropertyListBuilder();
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected CommentsConfiguration commentsConfiguration = new CommentsConfiguration();

    protected ConfigurationDataBuilder() {
    }

    /**
     * Collects all properties and comment data from the provided classes.
     * Properties are sorted by their group, and each group is sorted by order of encounter.
     *
     * @param classes the classes to scan for their property data
     * @return collected configuration data
     */
    @SafeVarargs
    public static ConfigurationData createConfiguration(Class<? extends SettingsHolder>... classes) {
        return createConfiguration(Arrays.asList(classes));
    }

    /**
     * Collects all properties and comment data from the provided classes.
     * Properties are sorted by their group, and each group is sorted by order of encounter.
     *
     * @param classes the classes to scan for their property data
     * @return collected configuration data
     */
    public static ConfigurationData createConfiguration(Iterable<Class<? extends SettingsHolder>> classes) {
        ConfigurationDataBuilder builder = new ConfigurationDataBuilder();
        return builder.collectData(classes);
    }

    public static ConfigurationData createConfiguration(List<? extends Property<?>> properties) {
        return new ConfigurationDataImpl(properties, Collections.emptyMap());
    }

    public static ConfigurationData createConfiguration(List<? extends Property<?>> properties,
            CommentsConfiguration commentsConfiguration) {
        return new ConfigurationDataImpl(properties, commentsConfiguration.getAllComments());
    }

    /**
     * Collects property data and comment info from the given class and creates a configuration data
     * instance with it.
     *
     * @param classes the classes to process
     * @return configuration data with the classes' data
     */
    protected ConfigurationData collectData(Iterable<Class<? extends SettingsHolder>> classes) {
        for (Class<? extends SettingsHolder> clazz : classes) {
            collectProperties(clazz);
            collectSectionComments(clazz);
        }
        return new ConfigurationDataImpl(propertyListBuilder.create(), commentsConfiguration.getAllComments());
    }

    /**
     * Registers all property fields of the given class to this instance's property list builder.
     *
     * @param clazz the class to process
     */
    protected void collectProperties(Class<?> clazz) {
        findFieldsToProcess(clazz).forEach(field -> {
            Property<?> property = getPropertyField(field);
            if (property != null) {
                propertyListBuilder.add(property);
                setCommentForPropertyField(field, property.getPath());
            }
        });
    }

    protected void setCommentForPropertyField(Field field, String path) {
        Comment commentAnnotation = field.getAnnotation(Comment.class);
        if (commentAnnotation != null) {
            commentsConfiguration.setComment(path, commentAnnotation.value());
        }
    }

    /**
     * Returns the given field's value if it is a static {@link Property}.
     *
     * @param field the field's value to return
     * @return the property the field defines, or null if not applicable
     */
    @Nullable
    protected Property<?> getPropertyField(Field field) {
        field.setAccessible(true);
        if (Property.class.isAssignableFrom(field.getType()) && Modifier.isStatic(field.getModifiers())) {
            try {

                if (!field.isAnnotationPresent(Path.class)) {
                    throw new ConfigMeException("Missing path annotation for field '" + field.getName() + "' from class '"
                                                        + field.getDeclaringClass().getSimpleName() + "'.");
                }
                final String path = field.getAnnotation(Path.class).value();
                final Property<?> property = (Property<?>) field.get(null);

                if (property instanceof BaseProperty) {
                    final BaseProperty<?> baseProperty = (BaseProperty<?>) field.get(null);
                    baseProperty.setPath(path);
                    return baseProperty;
                }

                if (property instanceof OptionalProperty) {
                    final BaseProperty<?> baseProperty = (BaseProperty<?>) ((OptionalProperty<?>) field.get(null)).getBaseProperty();
                    baseProperty.setPath(path);
                    return baseProperty;
                }

                return property;
            } catch (IllegalAccessException e) {
                throw new ConfigMeException("Could not fetch field '" + field.getName() + "' from class '"
                                                    + field.getDeclaringClass().getSimpleName() + "'.", e);
            }
        }
        return null;
    }

    protected void collectSectionComments(final Class<? extends SettingsHolder> clazz) {
        final SettingsHolder settingsHolder = createSettingsHolderInstance(clazz);
        settingsHolder.registerComments(commentsConfiguration);

        if (!clazz.isAnnotationPresent(Description.class)) return;
        final String[] description = clazz.getDeclaredAnnotation(Description.class).value();
        if (description.length == 0) return;
        commentsConfiguration.setComment("TH-CONFIG-DESCRIPTION", description);
    }

    /**
     * Creates an instance of the given settings holder class.
     *
     * @param clazz the class to instantiate
     * @param <T>   the class type
     * @return instance of the class
     */
    protected <T extends SettingsHolder> T createSettingsHolderInstance(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new ConfigMeException("Expected no-args constructor to be available for " + clazz, e);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new ConfigMeException("Could not create instance of " + clazz, e);
        }
    }

    /**
     * Returns all fields of the class which should be considered as potential {@link Property} definitions.
     * Considers the class' parents.
     *
     * @param clazz the class whose fields should be returned
     * @return stream of all the fields to process
     */
    protected Stream<Field> findFieldsToProcess(Class<?> clazz) {
        // In most cases we expect the class not to have any parent, so we check here and "fast track" this case
        if (Object.class.equals(clazz.getSuperclass())) {
            return Arrays.stream(clazz.getDeclaredFields());
        }

        List<Class<?>> classes = new ArrayList<>();
        Class<?> currentClass = clazz;
        while (currentClass != null && !currentClass.equals(Object.class)) {
            classes.add(currentClass);
            currentClass = currentClass.getSuperclass();
        }
        Collections.reverse(classes);

        return classes.stream()
                .map(Class::getDeclaredFields)
                .flatMap(Arrays::stream);
    }
}
