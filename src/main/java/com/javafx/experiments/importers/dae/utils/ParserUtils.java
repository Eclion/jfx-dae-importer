package com.javafx.experiments.importers.dae.utils;

import com.javafx.experiments.importers.dae.parsers.StartElement;
import com.javafx.experiments.importers.dae.structures.Input;
import org.xml.sax.Attributes;

import java.util.stream.IntStream;

/**
 * @author Eclion
 */
public final class ParserUtils {

    private ParserUtils() {
    }

    public static String[] splitCharBuffer(final StringBuilder charBuf) {
        return charBuf.toString().trim().split("\\s+");
    }

    public static float[] extractFloatArray(final StringBuilder charBuf) {
        final String[] numbers = splitCharBuffer(charBuf);
        final float[] array = new float[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            array[i] = Float.parseFloat(numbers[i].trim());
        }
        return array;
    }

    public static float[] extractFloatArray(final String content) {
        final String[] numbers = content.split("\\s+");
        final float[] array = new float[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            array[i] = Float.parseFloat(numbers[i].trim());
        }
        return array;
    }

    public static String[] extractNameArray(final StringBuilder charBuf) {
        return splitCharBuffer(charBuf);
    }

    public static Input createInput(final Attributes attributes) {
        final String offset = attributes.getValue("offset");
        return new Input(
                offset == null
                        ? 0
                        : Integer.parseInt(offset),
                attributes.getValue("semantic"),
                attributes.getValue("source"));
    }

    public static Input createInput(final StartElement startElement) {
        final String offset = startElement.getAttributeValue("offset");
        return new Input(
                offset == null
                        ? 0
                        : Integer.parseInt(offset),
                startElement.getAttributeValue("semantic"),
                startElement.getAttributeValue("source"));
    }

    public static double[] extractDoubleArray(final StringBuilder charBuf) {
        final float[] floatArray = extractFloatArray(charBuf);
        return IntStream.range(0, floatArray.length).mapToDouble(i -> floatArray[i]).toArray();
    }

    public static double[] extractDoubleArray(final String content) {
        final float[] floatArray = extractFloatArray(content);
        return IntStream.range(0, floatArray.length).mapToDouble(i -> floatArray[i]).toArray();
    }
}
