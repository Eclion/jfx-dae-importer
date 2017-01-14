package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.importers.dae.utils.ParserUtils;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Eclion
 */
final class LibraryEffectsParser extends DefaultHandler {
    private static final Logger LOGGER = Logger.getLogger(LibraryEffectsParser.class.getSimpleName());
    private StringBuilder charBuf = new StringBuilder();
    private final Map<String, String> currentId = new HashMap<>();

    private final Map<String, Material> materials = new HashMap<>();

    //private Color ambient;
    private Color diffuse;
    //private Color emission;
    private Color specular;

    //private Float shininess;
    //private Float refractionIndex;

    //private Float tempFloat;
    private Color tempColor;

    private enum State {
        UNKNOWN,
        color,
        diffuse,
        phong,
        specular,

        // ignored, unsupported states:
        ambient,
        double_sided,
        effect,
        emission,
        extra,
        _float,
        index_of_refraction,
        profile_COMMON,
        shininess,
        technique
    }

    Material getEffectMaterial(String effectId) {
        return materials.get(effectId);
    }

    private static State state(final String name) {
        try {
            return (!Objects.equals(name, "float"))
                    ? State.valueOf(name)
                    : State._float;
        } catch (Exception e) {
            return State.UNKNOWN;
        }
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
        this.currentId.put(qName, attributes.getValue("id"));
        this.charBuf = new StringBuilder();
        switch (state(qName)) {
            case UNKNOWN:
                LOGGER.log(Level.WARNING, "Unknown element: " + qName);
                break;
            case phong:
                diffuse = specular = null;
                //ambient = diffuse = emission = null;
                //shininess = refractionIndex = null;
                break;
            case color:
                tempColor = null;
                break;
            /*case _float:
                tempFloat = null;
                break;*/
            default:
                break;
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        switch (state(qName)) {
            /*case ambient:
                ambient = tempColor;
                break;*/
            case color:
                tempColor = extractColor(charBuf);
                break;
            case diffuse:
                diffuse = tempColor;
                break;
            /*case emission:
                emission = tempColor;
                break;
            case _float:
                tempFloat = extractFloat(charBuf);
                break;
            case index_of_refraction:
                refractionIndex = tempFloat;
                break;*/
            case phong:
                PhongMaterial material = new PhongMaterial(diffuse);
                if (specular != null) material.setSpecularColor(specular);
                materials.put(currentId.get("effect"), material);
                // commented nearly all parameters as only diffuse is used in the JavaFX's phong impl
                break;
            /*case shininess:
                shininess = tempFloat;
                break;*/
            case specular:
                specular = tempColor;
                break;
            default:
                break;
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        this.charBuf.append(ch, start, length);
    }

    private Color extractColor(final StringBuilder charBuf) {
        try {
            final String[] colors = ParserUtils.splitCharBuffer(charBuf);
            return new Color(
                    Double.parseDouble(colors[0]),
                    Double.parseDouble(colors[1]),
                    Double.parseDouble(colors[2]),
                    Double.parseDouble(colors[3])
            );
        } catch (Exception ignored) {

        }
        return null;
    }

}
