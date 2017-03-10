package com.javafx.experiments.importers.dae.parsers;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eclion
 */
final class LibraryMaterialsParser extends AbstractParser {
    private static final String INSTANCE_EFFECT_TAG = "instance_effect";
    private static final String MATERIAL_TAG = "material";

    private final Map<String, String> currentId = new HashMap<>();

    private final Map<String, String> materialEffectMap = new HashMap<>();

    LibraryMaterialsParser() {
        addStartElementBiConsumer("*", (qName, attributes) -> currentId.put(qName, attributes.getValue("id")));
        addStartElementBiConsumer(INSTANCE_EFFECT_TAG, (qName, attributes) -> {
            final String effectUrl = attributes.getValue("url");
            if (effectUrl != null) {
                materialEffectMap.put(currentId.get("material"), effectUrl.substring(1));
            }
        });
    }

    String getEffectId(String materialId) {
        return materialEffectMap.get(materialId);
    }
}