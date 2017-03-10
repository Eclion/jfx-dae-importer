package com.javafx.experiments.importers.dae.parsers;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final Map<String, String> currentId = new HashMap<>();
    private final Map<String, String> currentSid = new HashMap<>();

    private final Map<String, Material> materials = new HashMap<>();
    private final List<DaeEffect> effects = new ArrayList<>();

    private DaeEffect currentEffect;
    private String tempTexture;

    private Float tempFloat;
    private Color tempColor;

    LibraryEffectsParser() {
        addStartElementBiConsumer("*", (qName, attributes) -> {
            currentId.put(qName, attributes.getValue("id"));
            currentSid.put(qName, attributes.getValue("sid"));
        });
        addStartElementBiConsumer(EFFECT_TAG, (qName, attributes) -> currentEffect = new DaeEffect(this.currentId.get("effect")));
        addStartElementBiConsumer(PHONG_TAG, (qName, attributes) -> currentEffect.type = qName);
        addStartElementBiConsumer(COLOR_TAG, (qName, attributes) -> {
            tempColor = null;
            tempTexture = null;
        });
        addStartElementBiConsumer(FLOAT_TAG, (qName, attributes) -> tempFloat = null);
        addStartElementBiConsumer(TEXTURE_TAG, (qName, attributes) -> {
            tempColor = null;
            tempTexture = attributes.getValue("texture");
        });

        addEndElementBiConsumer(COLOR_TAG, (qName, content) -> tempColor = extractColor(content));
        Stream.of(AMBIENT_TAG, DIFFUSE_TAG, EMISSION_TAG, SPECULAR_TAG).forEach(tag ->
                addEndElementBiConsumer(tag, (qName, content) -> {
                    if (tempColor != null) {
                        currentEffect.colors.put(qName, tempColor);
                    } else if (tempTexture != null) {
                        currentEffect.textureIds.put(qName, tempTexture);
                    }
                }));
        addEndElementBiConsumer(EFFECT_TAG, (qName, content) -> effects.add(currentEffect));
        addEndElementBiConsumer(FLOAT_TAG, (qName, content) -> tempFloat = Float.parseFloat(content));
        addEndElementBiConsumer(INDEX_OF_REFRACTION_TAG, (qName, content) -> currentEffect.refractionIndex = tempFloat);
        addEndElementBiConsumer(INIT_FROM_TAG, (qName, content) -> currentEffect.surfaces.put(currentSid.get("newparam"), content));
        addEndElementBiConsumer(SHININESS_TAG, (qName, content) -> currentEffect.shininess = tempFloat);
        addEndElementBiConsumer(SOURCE_TAG, (qName, content) -> currentEffect.samplers.put(currentSid.get("newparam"), content));
    }

    void buildEffects(final LibraryImagesParser imagesParser) {
        effects.stream().
                filter(effect -> effect.type != null).
                forEach(effect -> materials.put(effect.id, effect.build(imagesParser))
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

        private final String id;

        private final Map<String, String> surfaces = new HashMap<>();
        private final Map<String, String> samplers = new HashMap<>();
        private final Map<String, Color> colors = new HashMap<>();
        private final Map<String, String> textureIds = new HashMap<>();
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

            textureIds.entrySet().stream().
                    filter(entry -> samplers.containsKey(entry.getValue())).
                    filter(entry -> surfaces.containsKey(samplers.get(entry.getValue()))).
                    filter(entry -> imagesParser.getImage(surfaces.get(samplers.get(entry.getValue()))) != null).
                    forEach(entry -> {
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
