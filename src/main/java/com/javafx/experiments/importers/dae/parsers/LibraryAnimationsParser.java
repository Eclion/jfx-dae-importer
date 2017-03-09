package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.importers.dae.structures.DaeAnimation;
import com.javafx.experiments.importers.dae.utils.ParserUtils;
import org.xml.sax.Attributes;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.BiConsumer;

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
        final HashMap<String, BiConsumer<String, Attributes>> startElementConsumer = new HashMap<>();

        startElementConsumer.put("*", (qName, attributes) -> currentId.put(qName, attributes.getValue("id")));
        startElementConsumer.put(ANIMATION_TAG, (qName, attributes) -> {
            currentAnimationId = currentId.get(qName);
            currentAnimations.push(new DaeAnimation(currentAnimationId));
        });
        startElementConsumer.put(CHANNEL_TAG, (qName, attributes) -> currentAnimations.peek().target = attributes.getValue("target"));

        final HashMap<String, BiConsumer<String, String>> endElementConsumer = new HashMap<>();

        endElementConsumer.put(ANIMATION_TAG, (qName, content) -> {
            DaeAnimation animation = currentAnimations.pop();
            if (currentAnimations.isEmpty()) {
                animations.put(currentAnimationId, animation);
            } else {
                currentAnimations.peek().addChild(animation);
            }
        });
        endElementConsumer.put(FLOAT_ARRAY_TAG, (qName, content) -> {
            String sourceId = currentId.get(SOURCE_TAG);
            if (sourceId.equalsIgnoreCase(currentAnimationId + "-input")) {
                currentAnimations.peek().input = ParserUtils.extractFloatArray(content);
            } else if (sourceId.equalsIgnoreCase(currentAnimationId + "-output")) {
                currentAnimations.peek().output = ParserUtils.extractDoubleArray(content);
            }
        });
        endElementConsumer.put(NAME_ARRAY_TAG, (qName, content) -> currentAnimations.peek().setInterpolations(content.split("\\s+")
        ));

        handler = new LibraryHandler(startElementConsumer, endElementConsumer);
    }
}
