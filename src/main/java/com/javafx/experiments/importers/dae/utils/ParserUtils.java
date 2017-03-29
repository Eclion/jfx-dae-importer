package com.javafx.experiments.importers.dae.utils;

import com.javafx.experiments.importers.dae.structures.DaeNode;
import com.javafx.experiments.importers.dae.structures.Input;
import javafx.scene.Group;
import org.xml.sax.Attributes;

import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Eclion
 */
public final class ParserUtils {

    private ParserUtils() {
    }

    public static float[] extractFloatArray(final String content) {
        final String[] numbers = content.split("\\s+");
        final float[] array = new float[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            array[i] = Float.parseFloat(numbers[i].trim());
        }
        return array;
    }

    public static Input createInput(final String qName, final Attributes attributes) {
        final int offset = Optional.ofNullable(attributes.getValue("offset")).
                map(Integer::parseInt).
                orElse(0);

        return new Input(
                offset,
                attributes.getValue("semantic"),
                attributes.getValue("source")
        );
    }

    public static double[] extractDoubleArray(final String content) {
        final float[] floatArray = extractFloatArray(content);
        return IntStream.range(0, floatArray.length).mapToDouble(i -> floatArray[i]).toArray();
    }

    public static int[] extractIntArray(final String content) {
        final float[] floatArray = extractFloatArray(content);
        final int[] intArray = new int[floatArray.length];
        for (int i = 0; i < floatArray.length; ++i) {
            intArray[i] = (int) floatArray[i];
        }
        return intArray;
    }

    public static Stream<DaeNode> getDaeNodeChildStream(Group group) {
        return group.getChildren().stream().
                filter(child -> child instanceof DaeNode).
                map(child -> (DaeNode) child);
    }
}
