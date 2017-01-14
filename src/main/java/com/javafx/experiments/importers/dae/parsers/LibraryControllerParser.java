package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.importers.dae.structures.DaeController;
import com.javafx.experiments.importers.dae.structures.Input;
import com.javafx.experiments.importers.dae.structures.Param;
import com.javafx.experiments.importers.dae.utils.ParserUtils;
import javafx.scene.transform.Affine;
import javafx.scene.transform.MatrixType;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Eclion
 */
final class LibraryControllerParser extends DefaultHandler {
    private static final Logger LOGGER = Logger.getLogger(LibraryControllerParser.class.getSimpleName());
    private StringBuilder charBuf = new StringBuilder();
    private String currentControllerId = "";
    private final Map<String, String> currentId = new HashMap<>();
    private final Map<String, Input> inputs = new HashMap<>();
    private final Map<String, Param> params = new HashMap<>();
    private final Map<String, float[]> floatArrays = new HashMap<>();
    final Map<String, DaeController> controllers = new HashMap<>();
    private int[] vCounts;
    private int[] v;
    private int nbPoints;

    private enum State {
        UNKNOWN,
        bind_shape_matrix,
        controller,
        float_array,
        input,
        Name_array,
        param,
        skin,
        v,
        vcount,
        vertex_weights,

        // ignored, unsupported states:
        accessor,
        joints,
        source,
        technique_common

    }

    private void init() {
        charBuf = new StringBuilder();
        currentControllerId = "";
        currentId.clear();
        inputs.clear();
        params.clear();
        floatArrays.clear();
        vCounts = new int[0];
        v = new int[0];
        nbPoints = 0;
    }

    private static State state(final String name) {
        try {
            return State.valueOf(name);
        } catch (Exception e) {
            return State.UNKNOWN;
        }
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
        currentId.put(qName, attributes.getValue("id"));
        charBuf = new StringBuilder();
        switch (state(qName)) {
            case UNKNOWN:
                LOGGER.log(Level.WARNING, "Unknown element: " + qName);
                break;
            case controller:
                currentControllerId = currentId.get(qName);
                controllers.put(currentControllerId, new DaeController(currentControllerId, attributes.getValue("name")));
                break;
            case input:
                Input input = ParserUtils.createInput(attributes);
                inputs.put(input.semantic, input);
                break;
            case param:
                String sourceId = currentId.get("source");
                params.put(sourceId, new Param(attributes.getValue("name"), attributes.getValue("type")));
                break;
            case skin:
                controllers.get(currentControllerId).skinId = attributes.getValue("source").substring(1);
                break;
            case vertex_weights:
                nbPoints = Integer.parseInt(attributes.getValue("count"));
                break;
            default:
                break;
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        switch (state(qName)) {
            case UNKNOWN:
                break;
            case bind_shape_matrix:
                String[] matrixValues = charBuf.toString().trim().split("\\s+");
                controllers.get(currentControllerId).bindShapeMatrix = extractMatrixTransformation(matrixValues);
                break;
            case controller:
                init();
                break;
            case float_array:
                floatArrays.put(currentId.get("source"), ParserUtils.extractFloatArray(charBuf));
                if (currentId.get("source").contains("bind_poses"))
                {
                    double[] doubleArray = ParserUtils.extractDoubleArray(charBuf);
                    for (int i = 0; i < doubleArray.length / 16; i++) {
                        controllers.get(currentControllerId).bindPoses.add(new Affine(doubleArray, MatrixType.MT_3D_4x4, i * 16));
                    }
                }
                break;
            case Name_array:
                controllers.get(currentControllerId).jointNames = charBuf.toString().trim().split("\\s+");
                break;
            case v:
                saveVertices();
                break;
            case vcount:
                saveVerticesCounts();
                break;
            case vertex_weights:
                saveWeights();
                break;
            default:
                break;
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        charBuf.append(ch, start, length);
    }

    private void saveVerticesCounts() {
        String[] numbers = charBuf.toString().trim().split("\\s+");
        vCounts = new int[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            vCounts[i] = Integer.parseInt(numbers[i].trim());
        }
    }

    private void saveVertices() {
        String[] numbers = charBuf.toString().trim().split("\\s+");
        v = new int[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            v[i] = Integer.parseInt(numbers[i].trim());
        }
    }

    private void saveWeights() {
        Input jointInput = inputs.get("JOINT");
        int jointOffset = jointInput.offset;
        Input weightInput = inputs.get("WEIGHT");
        int weightOffset = weightInput.offset;
        float[] weightValues = floatArrays.get(weightInput.source.substring(1));

        int nbJoints = controllers.get(currentControllerId).jointNames.length;
        float[][] weights = new float[nbJoints][nbPoints];

        int index = 0;
        for (int i = 0; i < vCounts.length; i++) {
            for (int _v = 0; _v < vCounts[i]; _v++) {
                int jointIndex = v[index + jointOffset];
                int weightIndex = v[index + weightOffset];
                weights[jointIndex][i] = weightValues[weightIndex];
                index += 2;
            }
        }

        controllers.get(currentControllerId).vertexWeights = weights;
    }

    private Affine extractMatrixTransformation(final String[] matrixStringValues) {
        double[] matrixValues = new double[matrixStringValues.length];
        for (int i = 0; i < matrixStringValues.length; i++) {
            matrixValues[i] = Double.valueOf(matrixStringValues[i]);
        }
        return new Affine(matrixValues, MatrixType.MT_3D_4x4, 0);
    }
}
