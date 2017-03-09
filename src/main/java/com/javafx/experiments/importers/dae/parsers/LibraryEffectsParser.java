package com.javafx.experiments.importers.dae.parsers;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author Eclion
 */
final class LibraryEffectsParser extends AbstractParser {
    private static final String AMBIENT_TAG = "ambient";
    private static final String COLOR_TAG = "color";
    private static final String DIFFUSE_TAG = "diffuse";
    private static final String EFFECT_TAG = "effect";
    private static final String EMISSION_TAG = "emission";
    private static final String FLOAT_TAG = "float";
    private static final String INDEX_OF_REFRACTION_TAG = "index_of_refraction";
    private static final String INIT_FROM_TAG = "init_from";
    private static final String PHONG_TAG = "phong";
    private static final String SHININESS_TAG = "shininess";
    private static final String SPECULAR_TAG = "specular";
    private static final String SOURCE_TAG = "source";
    private static final String TEXTURE_TAG = "texture";

    private final HashMap<String, String> currentId = new HashMap<>();
    private final HashMap<String, String> currentSid = new HashMap<>();

    private final HashMap<String, Material> materials = new HashMap<>();
    private final ArrayList<DaeEffect> effects = new ArrayList<>();

    private DaeEffect currentEffect;
    private String tempTexture;

    private Float tempFloat;
    private Color tempColor;

    LibraryEffectsParser() {
        final Map<String, Consumer<StartElement>> startElementConsumer = new HashMap<>();

        startElementConsumer.put("*", startElement -> {
            currentId.put(startElement.qName, startElement.getAttributeValue("id"));
            currentSid.put(startElement.qName, startElement.getAttributeValue("sid"));
        });
        startElementConsumer.put(EFFECT_TAG, startElement -> currentEffect = new DaeEffect(this.currentId.get("effect")));
        startElementConsumer.put(PHONG_TAG, startElement -> currentEffect.type = startElement.qName);
        startElementConsumer.put(COLOR_TAG, startElement -> {
            tempColor = null;
            tempTexture = null;
        });
        startElementConsumer.put(FLOAT_TAG, startElement -> tempFloat = null);
        startElementConsumer.put(TEXTURE_TAG, startElement -> {
            tempColor = null;
            tempTexture = startElement.getAttributeValue("texture");
        });

        final Map<String, Consumer<LibraryHandler.EndElement>> endElementConsumer = new HashMap<>();

        endElementConsumer.put(COLOR_TAG, endElement -> tempColor = extractColor(endElement.content));
        Stream.of(AMBIENT_TAG, DIFFUSE_TAG, EMISSION_TAG, SPECULAR_TAG).forEach(tag ->
                endElementConsumer.put(tag, endElement -> {
                    if (tempColor != null) {
                        currentEffect.colors.put(endElement.qName, tempColor);
                    } else if (tempTexture != null) {
                        currentEffect.textureIds.put(endElement.qName, tempTexture);
                    }
                }));
        endElementConsumer.put(EFFECT_TAG, endElement -> effects.add(currentEffect));
        endElementConsumer.put(FLOAT_TAG, endElement -> tempFloat = Float.parseFloat(endElement.content));
        endElementConsumer.put(INDEX_OF_REFRACTION_TAG, endElement -> currentEffect.refractionIndex = tempFloat);
        endElementConsumer.put(INIT_FROM_TAG, endElement -> currentEffect.surfaces.put(currentSid.get("newparam"), endElement.content));
        endElementConsumer.put(SHININESS_TAG, endElement -> currentEffect.shininess = tempFloat);
        endElementConsumer.put(SOURCE_TAG, endElement -> currentEffect.samplers.put(currentSid.get("newparam"), endElement.content));

        handler = new LibraryHandler(startElementConsumer, endElementConsumer);
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

    private Color extractColor(final String content) {
        try {
            final String[] colors = content.split("\\s+");
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
        private HashMap<String, Color> colors = new HashMap<>();
        private HashMap<String, String> textureIds = new HashMap<>();
        private float shininess;
        private float refractionIndex;

        private String type;

        DaeEffect(final String id) {
            this.id = id;
        }

        Material build(final LibraryImagesParser imagesParser) {
            Material material = null;
            switch (this.type) {
                case PHONG_TAG:
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
                    case AMBIENT_TAG:
                        break;
                    case DIFFUSE_TAG:
                        material.setDiffuseColor(entry.getValue());
                        break;
                    case EMISSION_TAG:
                        break;
                    case SPECULAR_TAG:
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
                            case AMBIENT_TAG:
                                break;
                            case DIFFUSE_TAG:
                                material.setDiffuseMap(image);
                                break;
                            case EMISSION_TAG:
                                break;
                            case SPECULAR_TAG:
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
