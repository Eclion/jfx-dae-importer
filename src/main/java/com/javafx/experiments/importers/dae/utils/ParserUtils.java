package com.javafx.experiments.importers.dae.utils;

import com.javafx.experiments.importers.dae.structures.Input;
import org.xml.sax.Attributes;

import java.util.stream.IntStream;

/**
 * @author Eclion
 */
public final class ParserUtils {

    public static float[] extractFloatArray(StringBuilder charBuf) {
        String[] numbers = charBuf.toString().trim().split("\\s+");
        float[] array = new float[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            array[i] = Float.parseFloat(numbers[i].trim());
        }
        return array;
    }

    public static String[] extractNameArray(StringBuilder charBuf) {
        return charBuf.toString().trim().split("\\s+");
    }

    public static Input createInput(Attributes attributes) {
        return new Input(
                attributes.getValue("offset") != null ? Integer.parseInt(attributes.getValue("offset")) : 0,
                attributes.getValue("semantic"),
                attributes.getValue("source"));
    }

    public static double[] extractDoubleArray(StringBuilder charBuf) {
        final float[] floatArray = extractFloatArray(charBuf);
        return IntStream.range(0, floatArray.length).mapToDouble(i -> floatArray[i]).toArray();
    }
}
