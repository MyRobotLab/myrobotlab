package org.myrobotlab.codec.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;

import java.io.IOException;

/**
 * Custom Jackson pretty printer that should perfectly
 * emulate GSON's pretty printing style.
 * <p>
 * Copied from
 * <a href="https://stackoverflow.com/questions/64669932/how-to-configure-jackson-prettyprinter-format-json-as-gson">
 * StackOverflow (answer by anton)</a> with minor adjustments by AutonomicPerfectionist.
 * </p>
 */
public class JacksonPrettyPrinter extends DefaultPrettyPrinter {

    public JacksonPrettyPrinter() {
        _arrayIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
        _objectIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
    }

    public JacksonPrettyPrinter(DefaultPrettyPrinter base) {
        super(base);
    }

    @Override
    public JacksonPrettyPrinter createInstance() {
        if (getClass() != JacksonPrettyPrinter.class) {
            throw new IllegalStateException("Failed `createInstance()`: " + getClass().getName()
                    + " does not override method; it has to");
        }
        return new JacksonPrettyPrinter(this);
    }

    @Override
    public JacksonPrettyPrinter withSeparators(Separators separators) {
        this._separators = separators;
        this._objectFieldValueSeparatorWithSpaces = separators.getObjectFieldValueSeparator() + " ";
        return this;
    }

    @Override
    public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException {
        if (!_arrayIndenter.isInline()) {
            _nesting--;
        }
        if (nrOfValues > 0) {
            _arrayIndenter.writeIndentation(g, _nesting);
        }
        g.writeRaw(']');
    }

    @Override
    public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException {
        if (!_objectIndenter.isInline()) {
            _nesting--;
        }
        if (nrOfEntries > 0) {
            _objectIndenter.writeIndentation(g, _nesting);
        }
        g.writeRaw('}');
    }
}
