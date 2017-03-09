package com.javafx.experiments.importers.dae.parsers;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Eclion
 */
final class LibraryMaterialsParser extends AbstractParser {
    private static final String INSTANCE_EFFECT_TAG = "instance_effect";
    private static final String MATERIAL_TAG = "material";

    private final Map<String, String> currentId = new HashMap<>();

    private final Map<String, String> materialEffectMap = new HashMap<>();

    LibraryMaterialsParser() {
        final Map<String, Consumer<StartElement>> startElementConsumer = new HashMap<>();

        startElementConsumer.put("*", startElement -> currentId.put(startElement.qName, startElement.getAttributeValue("id")));
        startElementConsumer.put(INSTANCE_EFFECT_TAG, startElement -> {
            final String effectUrl = startElement.getAttributeValue("url");
            if (effectUrl != null) {
                materialEffectMap.put(currentId.get("material"), effectUrl.substring(1));
            }
        });

        final Map<String, Consumer<EndElement>> endElementConsumer = new HashMap<>();
        handler = new LibraryHandler(startElementConsumer, endElementConsumer);
    }

    String getEffectId(String materialId) {
        return materialEffectMap.get(materialId);
    }
}