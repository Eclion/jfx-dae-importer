package com.javafx.experiments.importers.dae.parsers;

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

        addStartElementBiConsumer(UNIT_TAG, (qName, attributes) -> {
            unit = attributes.getValue("name");
            scale = Float.parseFloat(attributes.getValue("meter"));
        });

        addEndElementBiConsumer(AUTHOR_TAG, (qName, content) -> author = content);
        addEndElementBiConsumer(AUTHORING_TOOL_TAG, (qName, content) -> authoringTool = content);
        addEndElementBiConsumer(UP_AXIS_TAG, (qName, content) -> upAxis = content);
    }
}
