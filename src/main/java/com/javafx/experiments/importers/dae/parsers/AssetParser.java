package com.javafx.experiments.importers.dae.parsers;

import org.xml.sax.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author Eclion
 */
final class AssetParser extends AbstractParser {
    private static final String AUTHOR_TAG = "author";
    private static final String AUTHORING_TOOL_TAG = "authoring_tool";
    private static final String UP_AXIS_TAG = "up_axis";
    private static final String UNIT_TAG = "unit";

    String author;
    String authoringTool;
    String unit;
    float scale;
    String upAxis;

    AssetParser() {

        final HashMap<String, BiConsumer<String, Attributes>> startElementConsumer = new HashMap<>();

        startElementConsumer.put(UNIT_TAG, (qName, attributes) -> {
            unit = attributes.getValue("name");
            scale = Float.parseFloat(attributes.getValue("meter"));
        });

        final HashMap<String, BiConsumer<String, String>> endElementConsumer = new HashMap<>();

        endElementConsumer.put(AUTHOR_TAG, (qName, content) -> author = content);
        endElementConsumer.put(AUTHORING_TOOL_TAG, (qName, content) -> authoringTool = content);
        endElementConsumer.put(UP_AXIS_TAG, (qName, content) -> upAxis = content);

        handler = new LibraryHandler(startElementConsumer, endElementConsumer);
    }
}
