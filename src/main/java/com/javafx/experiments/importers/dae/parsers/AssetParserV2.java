package com.javafx.experiments.importers.dae.parsers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Eclion
 */
final class AssetParserV2 extends AbstractParser {
    private static final String AUTHOR_TAG = "author";
    private static final String AUTHORING_TOOL_TAG = "authoring_tool";
    private static final String UP_AXIS_TAG = "up_axis";
    private static final String UNIT_TAG = "unit";

    String author;
    String authoringTool;
    String unit;
    float scale;
    String upAxis;

    AssetParserV2() {

        final Map<String, Consumer<LibraryHandler.StartElement>> startElementConsumer = new HashMap<>();

        startElementConsumer.put(UNIT_TAG, startElement -> {
            unit = startElement.attributes.getValue("name");
            scale = Float.parseFloat(startElement.attributes.getValue("meter"));
        });

        final Map<String, Consumer<LibraryHandler.EndElement>> endElementConsumer = new HashMap<>();

        endElementConsumer.put(AUTHOR_TAG, endElement -> author = endElement.content);
        endElementConsumer.put(AUTHORING_TOOL_TAG, endElement -> authoringTool = endElement.content);
        endElementConsumer.put(UP_AXIS_TAG, endElement -> upAxis = endElement.content);

        handler = new LibraryHandler(startElementConsumer, endElementConsumer);
    }
}
