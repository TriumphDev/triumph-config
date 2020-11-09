package me.mattstudios.config.properties;

import me.mattstudios.config.properties.convertresult.PropertyValue;
import me.mattstudios.config.resource.PropertyReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static me.mattstudios.config.TestUtils.isErrorValueOf;
import static me.mattstudios.config.TestUtils.isValidValueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link BooleanProperty}.
 */
class BooleanPropertyTest {

    private static PropertyReader reader;

    @BeforeAll
    static void setUpConfiguration() {
        reader = mock(PropertyReader.class);
        when(reader.getObject("bool.path.test")).thenReturn(true);
        when(reader.getObject("bool.path.wrong")).thenReturn(null);
    }

    @Test
    void shouldGetBoolValue() {
        // given
        Property<Boolean> property = new BooleanProperty("bool.path.test", false);

        // when
        PropertyValue<Boolean> result = property.determineValue(reader);

        // then
        assertThat(result, isValidValueOf(true));
    }

    @Test
    void shouldGetBoolDefault() {
        // given
        Property<Boolean> property = new BooleanProperty("bool.path.wrong", true);

        // when
        PropertyValue<Boolean> result = property.determineValue(reader);

        // then
        assertThat(result, isErrorValueOf(true));
    }

    @Test
    void shouldReturnExportRepresentation() {
        // given
        Property<Boolean> property = new BooleanProperty("bool.path.test", true);

        // when
        Object exportValue = property.toExportValue(true);

        // then
        assertThat(exportValue, equalTo(true));
    }
}
