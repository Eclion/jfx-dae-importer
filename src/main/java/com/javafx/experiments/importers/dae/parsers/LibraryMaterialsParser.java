package com.javafx.experiments.importers.dae.parsers;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eclion
 */
final class LibraryMaterialsParser extends AbstractParser {
    private static final String INSTANCE_EFFECT_TAG = "instance_effect";

    private final Map<String, String> currentId = new HashMap<>();

    final Map<String, String> materialIdToEffectIdMap = new HashMap<>();

    LibraryMaterialsParser() {
        addStartElementBiConsumer("*", (qName, attributes) -> currentId.put(qName, attributes.getValue("id")));
        addStartElementBiConsumer(INSTANCE_EFFECT_TAG, (qName, attributes) -> {
            final String effectUrl = attributes.getValue("url");
            if (effectUrl != null) {
                materialIdToEffectIdMap.put(currentId.get("material"), effectUrl.substring(1));
            }
        });
    }

    String getEffectId(final String materialId) {
        return materialIdToEffectIdMap.get(materialId);
    }
}