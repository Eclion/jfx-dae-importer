package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.importers.dae.structures.DaeEffect;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * @author Eclion
 */
final class LibraryEffectsParser extends AbstractParser {
    private static final Logger LOGGER = Logger.getLogger(LibraryEffectsParser.class.getSimpleName());

    private static final String AMBIENT_TAG = "ambient";
    private static final String COLOR_TAG = "color";
    private static final String DIFFUSE_TAG = "diffuse";
    private static final String EFFECT_TAG = "effect";
    private static final String EMISSION_TAG = "emission";
    private static final String INIT_FROM_TAG = "init_from";
    private static final String PHONG_TAG = "phong";
    private static final String SPECULAR_TAG = "specular";
    private static final String SOURCE_TAG = "source";
    private static final String TEXTURE_TAG = "texture";

    private final Map<String, String> currentId = new HashMap<>();
    private final Map<String, String> currentSid = new HashMap<>();

    final Map<String, Material> effectIdToMaterialMap = new HashMap<>();
    private final List<DaeEffect> effects = new ArrayList<>();

    private DaeEffect currentEffect;
    private String tempTexture;

    private Color tempColor;

    LibraryEffectsParser() {
        addStartElementBiConsumer("*", (qName, attributes) -> {
            currentId.put(qName, attributes.getValue("id"));
            currentSid.put(qName, attributes.getValue("sid"));
        });
        addStartElementBiConsumer(EFFECT_TAG, (qName, attributes) -> currentEffect = new DaeEffect(this.currentId.get("effect")));
        addStartElementBiConsumer(PHONG_TAG, (qName, attributes) -> currentEffect.setType(qName));
        addStartElementBiConsumer(COLOR_TAG, (qName, attributes) -> {
            tempColor = null;
            tempTexture = null;
        });
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
        addEndElementBiConsumer(INIT_FROM_TAG, (qName, content) -> currentEffect.surfaces.put(currentSid.get("newparam"), content));
        addEndElementBiConsumer(SOURCE_TAG, (qName, content) -> currentEffect.samplers.put(currentSid.get("newparam"), content));
    }

    void buildEffects(final Map<String, Image> images) {
        effects.stream().
                filter(DaeEffect::hasType).
                forEach(effect -> effectIdToMaterialMap.put(effect.id, effect.build(images))
                );
    }

    Material getEffectMaterial(final String effectId) {
        return effectIdToMaterialMap.get(effectId);
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
            LOGGER.warning("Couldn't parse the color from \"" + content + "\"");
        }
        return null;
    }

}
