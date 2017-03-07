package com.javafx.experiments.importers.dae.parsers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * @author Eclion
 */
final class AssetParserV2 {
    String author;
    String authoringTool;
    String unit;
    float scale;
    final LibraryHandler libraryHandler;
    String upAxis;

    public AssetParserV2() {

        final Map<String, Consumer<LibraryHandler.StartElement>> startElementConsumer = new HashMap<>();

        startElementConsumer.put("unit", startElement -> {
            unit = startElement.attributes.getValue("name");
            scale = Float.parseFloat(startElement.attributes.getValue("meter"));
        });

        final Map<String, Consumer<LibraryHandler.EndElement>> endElementConsumer = new HashMap<>();

        endElementConsumer.put("author", endElement -> author = endElement.content);
        endElementConsumer.put("authoring_tool", endElement -> authoringTool = endElement.content);
        endElementConsumer.put("up_axis", endElement -> upAxis = endElement.content);

        libraryHandler = new LibraryHandler(startElementConsumer, endElementConsumer);
    }
}
