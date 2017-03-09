package com.javafx.experiments.importers.dae.parsers;

import org.xml.sax.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author Eclion
 */
final class LibraryMaterialsParser extends AbstractParser {
    private static final String INSTANCE_EFFECT_TAG = "instance_effect";
    private static final String MATERIAL_TAG = "material";

    private final Map<String, String> currentId = new HashMap<>();

    private final Map<String, String> materialEffectMap = new HashMap<>();

    LibraryMaterialsParser() {
        final HashMap<String, BiConsumer<String, Attributes>> startElementConsumer = new HashMap<>();

        startElementConsumer.put("*", (qName, attributes) -> currentId.put(qName, attributes.getValue("id")));
        startElementConsumer.put(INSTANCE_EFFECT_TAG, (qName, attributes) -> {
            final String effectUrl = attributes.getValue("url");
            if (effectUrl != null) {
                materialEffectMap.put(currentId.get("material"), effectUrl.substring(1));
            }
        });

        final HashMap<String, BiConsumer<String, String>> endElementConsumer = new HashMap<>();
        handler = new LibraryHandler(startElementConsumer, endElementConsumer);
    }

    String getEffectId(String materialId) {
        return materialEffectMap.get(materialId);
    }
}