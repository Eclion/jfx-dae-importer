package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.importers.dae.utils.ParserUtils;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Eclion
 */
final class LibraryEffectsParser extends DefaultHandler {
    private static final Logger LOGGER = Logger.getLogger(LibraryEffectsParser.class.getSimpleName());
    private StringBuilder charBuf = new StringBuilder();
    private final HashMap<String, String> currentId = new HashMap<>();
    private final HashMap<String, String> currentSid = new HashMap<>();

    private final HashMap<String, Material> materials = new HashMap<>();
    private final ArrayList<DaeEffect> effects = new ArrayList<>();

    private DaeEffect currentEffect;
    private String tempTexture;

    private Float tempFloat;
    private Color tempColor;

    private enum State {
        UNKNOWN,
        ambient,
        color,
        diffuse,
        effect,
        emission,
        _float,
        index_of_refraction,
        init_from,
        phong,
        shininess,
        specular,
        source,
        texture,

        // ignored, unsupported states:
        blinn,
        double_sided,
        extra,
        format,
        magfilter,
        minfilter,
        newparam,
        profile_COMMON,
        reflective,
        reflectivity,
        sampler2D,
        surface,
        technique,
        transparency,
        transparent
    }

    void buildEffects(final LibraryImagesParser imagesParser) {
        effects.stream()
                .filter(effect -> effect.type != null)
                .forEach(effect -> materials.put(effect.id, effect.build(imagesParser))
                );
    }

    Material getEffectMaterial(final String effectId) {
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
        this.currentSid.put(qName, attributes.getValue("sid"));
        this.charBuf = new StringBuilder();
        switch (state(qName)) {
            case UNKNOWN:
                LOGGER.log(Level.WARNING, "Unknown element: " + qName);
                break;
            case effect:
                currentEffect = new DaeEffect(this.currentId.get("effect"));
                break;
            case phong:
                currentEffect.type = state(qName);
                break;
            case color:
                tempColor = null;
                tempTexture = null;
                break;
            case _float:
                tempFloat = null;
                break;
            case texture:
                tempColor = null;
                tempTexture = attributes.getValue("texture");
                break;
            default:
                break;
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        final State currentState = state(qName);
        switch (currentState) {
            case color:
                tempColor = extractColor(charBuf);
                break;
            case ambient:
            case diffuse:
            case emission:
            case specular:
                if (tempColor != null) {
                    currentEffect.colors.put(currentState, tempColor);
                } else if (tempTexture != null) {
                    currentEffect.textureIds.put(currentState, tempTexture);
                }
                break;
            case effect:
                effects.add(currentEffect);
                break;
            case _float:
                tempFloat = Float.parseFloat(charBuf.toString());
                break;
            case index_of_refraction:
                currentEffect.refractionIndex = tempFloat;
                break;
            case init_from:
                currentEffect.surfaces.put(currentSid.get("newparam"), charBuf.toString());
                break;
            case shininess:
                currentEffect.shininess = tempFloat;
                break;
            case source:
                currentEffect.samplers.put(currentSid.get("newparam"), charBuf.toString());
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

    private final class DaeEffect {

        private String id;

        private HashMap<String, String> surfaces = new HashMap<>();
        private HashMap<String, String> samplers = new HashMap<>();
        private HashMap<State, Color> colors = new HashMap<>();
        private HashMap<State, String> textureIds = new HashMap<>();
        private float shininess;
        private float refractionIndex;

        private State type;

        DaeEffect(final String id) {
            this.id = id;
        }

        Material build(final LibraryImagesParser imagesParser) {
            Material material = null;
            switch (this.type) {
                case phong:
                    material = buildPhongMaterial(imagesParser);
                    break;
                default:
                    break;
            }
            return material;
        }

        PhongMaterial buildPhongMaterial(final LibraryImagesParser imagesParser) {
            final PhongMaterial material = new PhongMaterial();

            colors.entrySet().forEach(entry -> {
                switch (entry.getKey()) {
                    case ambient:
                        break;
                    case diffuse:
                        material.setDiffuseColor(entry.getValue());
                        break;
                    case emission:
                        break;
                    case specular:
                        material.setSpecularColor(entry.getValue());
                        break;
                    default:
                        break;
                }
            });

            textureIds.entrySet().stream()
                    .filter(entry -> samplers.containsKey(entry.getValue()))
                    .filter(entry -> surfaces.containsKey(samplers.get(entry.getValue())))
                    .filter(entry -> imagesParser.getImage(surfaces.get(samplers.get(entry.getValue()))) != null)
                    .forEach(entry -> {
                        final Image image = imagesParser.getImage(surfaces.get(samplers.get(entry.getValue())));
                        switch (entry.getKey()) {
                            case ambient:
                                break;
                            case diffuse:
                                material.setDiffuseMap(image);
                                break;
                            case emission:
                                break;
                            case specular:
                                material.setSpecularMap(image);
                                break;
                            default:
                                break;
                        }
                    });
            return material;
        }
    }
}
