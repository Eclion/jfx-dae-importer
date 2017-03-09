package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.importers.dae.structures.DaeController;
import com.javafx.experiments.importers.dae.structures.Input;
import com.javafx.experiments.importers.dae.structures.Param;
import com.javafx.experiments.importers.dae.utils.ParserUtils;
import javafx.scene.transform.Affine;
import javafx.scene.transform.MatrixType;
import org.xml.sax.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author Eclion
 */
final class LibraryControllerParser extends AbstractParser {
    private static final String BIND_SHAPE_MATRIX_TAG = "bind_shape_matrix";
    private static final String CONTROLLER_TAG = "controller";
    private static final String FLOAT_ARRAY_TAG = "float_array";
    private static final String INPUT_TAG = "input";
    private static final String NAME_ARRAY_TAG = "Name_array";
    private static final String PARAM_TAG = "param";
    private static final String SKIN_TAG = "skin";
    private static final String V_TAG = "v";
    private static final String VCOUNT_TAG = "vcount";
    private static final String VERTEX_WEIGTHS_TAG = "vertex_weights";
    private String currentControllerId = "";
    private final Map<String, String> currentId = new HashMap<>();
    private final Map<String, Input> inputs = new HashMap<>();
    private final Map<String, Param> params = new HashMap<>();
    private final Map<String, float[]> floatArrays = new HashMap<>();
    final Map<String, DaeController> controllers = new HashMap<>();
    private int[] vCounts;
    private int[] v;
    private int nbPoints;

    LibraryControllerParser() {
        final HashMap<String, BiConsumer<String, Attributes>> startElementConsumer = new HashMap<>();

        startElementConsumer.put("*", (qName, attributes) -> currentId.put(qName, attributes.getValue("id")));
        startElementConsumer.put(CONTROLLER_TAG, (qName, attributes) -> {
            currentControllerId = currentId.get(qName);
            controllers.put(currentControllerId, new DaeController(currentControllerId, attributes.getValue("name")));
        });
        startElementConsumer.put(INPUT_TAG, (qName, attributes) -> {
            Input input = ParserUtils.createInput(qName, attributes);
            inputs.put(input.semantic, input);
        });
        startElementConsumer.put(PARAM_TAG, (qName, attributes) -> {
            String sourceId = currentId.get("source");
            params.put(sourceId, new Param(attributes.getValue("name"), attributes.getValue("type")));
        });
        startElementConsumer.put(SKIN_TAG, (qName, attributes) -> controllers.get(currentControllerId).skinId = attributes.getValue("source").substring(1));
        startElementConsumer.put(VERTEX_WEIGTHS_TAG, (qName, attributes) -> nbPoints = Integer.parseInt(attributes.getValue("count")));

        final HashMap<String, BiConsumer<String, String>> endElementConsumer = new HashMap<>();

        endElementConsumer.put(BIND_SHAPE_MATRIX_TAG, (qName, content) -> {
            String[] matrixValues = content.split("\\s+");
            controllers.get(currentControllerId).bindShapeMatrix = extractMatrixTransformation(matrixValues);
        });
        endElementConsumer.put(CONTROLLER_TAG, (qName, content) -> init());
        endElementConsumer.put(FLOAT_ARRAY_TAG, (qName, content) -> {
            floatArrays.put(currentId.get("source"), ParserUtils.extractFloatArray(content));
            if (currentId.get("source").contains("bind_poses")) {
                double[] doubleArray = ParserUtils.extractDoubleArray(content);
                for (int i = 0; i < doubleArray.length / 16; i++) {
                    controllers.get(currentControllerId).bindPoses.add(new Affine(doubleArray, MatrixType.MT_3D_4x4, i * 16));
                }
            }
        });
        endElementConsumer.put(NAME_ARRAY_TAG, (qName, content) -> controllers.get(currentControllerId).jointNames = content.split("\\s+"));
        endElementConsumer.put(V_TAG, this::saveVertices);
        endElementConsumer.put(VCOUNT_TAG, this::saveVerticesCounts);
        endElementConsumer.put(VERTEX_WEIGTHS_TAG, (qName, content) -> saveWeights());

        handler = new LibraryHandler(startElementConsumer, endElementConsumer);
    }

    private void init() {
        currentControllerId = "";
        currentId.clear();
        inputs.clear();
        params.clear();
        floatArrays.clear();
        vCounts = new int[0];
        v = new int[0];
        nbPoints = 0;
    }

    private void saveVerticesCounts(final String qName, final String content) {
        String[] numbers = content.split("\\s+");
        vCounts = new int[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            vCounts[i] = Integer.parseInt(numbers[i].trim());
        }
    }

    private void saveVertices(final String qName, final String content) {
        String[] numbers = content.split("\\s+");
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
