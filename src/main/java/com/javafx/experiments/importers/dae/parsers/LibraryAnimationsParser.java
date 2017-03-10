package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.importers.dae.structures.DaeAnimation;
import com.javafx.experiments.importers.dae.utils.ParserUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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
        addStartElementBiConsumer("*", (qName, attributes) -> currentId.put(qName, attributes.getValue("id")));
        addStartElementBiConsumer(ANIMATION_TAG, (qName, attributes) -> {
            currentAnimationId = currentId.get(qName);
            currentAnimations.push(new DaeAnimation(currentAnimationId));
        });
        addStartElementBiConsumer(CHANNEL_TAG, (qName, attributes) -> currentAnimations.peek().target = attributes.getValue("target"));

        addEndElementBiConsumer(ANIMATION_TAG, (qName, content) -> {
            DaeAnimation animation = currentAnimations.pop();
            if (currentAnimations.isEmpty()) {
                animations.put(currentAnimationId, animation);
            } else {
                currentAnimations.peek().addChild(animation);
            }
        });
        addEndElementBiConsumer(FLOAT_ARRAY_TAG, (qName, content) -> {
            String sourceId = currentId.get(SOURCE_TAG);
            if (sourceId.equalsIgnoreCase(currentAnimationId + "-input")) {
                currentAnimations.peek().input = ParserUtils.extractFloatArray(content);
            } else if (sourceId.equalsIgnoreCase(currentAnimationId + "-output")) {
                currentAnimations.peek().output = ParserUtils.extractDoubleArray(content);
            }
        });
        addEndElementBiConsumer(NAME_ARRAY_TAG, (qName, content) -> currentAnimations.peek().setInterpolations(content.split("\\s+")
        ));
    }
}
