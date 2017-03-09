package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.importers.dae.structures.DaeAnimation;
import com.javafx.experiments.importers.dae.utils.ParserUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Eclion.
 */
final class LibraryAnimationsParser extends AbstractParser {
    private static final String ANIMATION_TAG = "animation";
    private static final String CHANNEL_TAG = "channel";
    private static final String FLOAT_ARRAY_TAG = "float_array";
    private static final String NAME_ARRAY_TAG = "Name_array";
    private static final String SOURCE_TAG = "source";

    private final Map<String, String> currentId = new HashMap<>();
    private String currentAnimationId = "";
    public final Map<String, DaeAnimation> animations = new HashMap<>();
    private final LinkedList<DaeAnimation> currentAnimations = new LinkedList<>();

    LibraryAnimationsParser() {
        /*final Map<String, Consumer<StartElement>> startElementConsumer = new HashMap<>();
        final Map<String, Consumer<LibraryHandler.EndElement>> endElementConsumer = new HashMap<>();
        handler = new LibraryHandler(startElementConsumer, endElementConsumer);*/
        final Map<String, Consumer<StartElement>> startElementConsumer = new HashMap<>();

        startElementConsumer.put("*", startElement -> currentId.put(startElement.qName, startElement.getAttributeValue("id")));
        startElementConsumer.put(ANIMATION_TAG, startElement -> {
            currentAnimationId = currentId.get(startElement.qName);
            currentAnimations.push(new DaeAnimation(currentAnimationId));
        });
        startElementConsumer.put(CHANNEL_TAG, startElement -> currentAnimations.peek().target = startElement.getAttributeValue("target"));

        final Map<String, Consumer<EndElement>> endElementConsumer = new HashMap<>();

        endElementConsumer.put(ANIMATION_TAG, endElement -> {
            DaeAnimation animation = currentAnimations.pop();
            if (currentAnimations.isEmpty()) {
                animations.put(currentAnimationId, animation);
            } else {
                currentAnimations.peek().addChild(animation);
            }
        });
        endElementConsumer.put(FLOAT_ARRAY_TAG, endElement -> {
            String sourceId = currentId.get(SOURCE_TAG);
            if (sourceId.equalsIgnoreCase(currentAnimationId + "-input")) {
                currentAnimations.peek().input = ParserUtils.extractFloatArray(endElement.content);
            } else if (sourceId.equalsIgnoreCase(currentAnimationId + "-output")) {
                currentAnimations.peek().output = ParserUtils.extractDoubleArray(endElement.content);
            }
        });
        endElementConsumer.put(NAME_ARRAY_TAG, endElement -> currentAnimations.peek().setInterpolations(endElement.content.split("\\s+")
        ));

        handler = new LibraryHandler(startElementConsumer, endElementConsumer);
    }
}
