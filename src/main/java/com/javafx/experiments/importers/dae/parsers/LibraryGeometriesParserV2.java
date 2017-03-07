package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.importers.dae.structures.Input;
import com.javafx.experiments.importers.dae.utils.ParserUtils;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * @author Eclion
 */
final class LibraryGeometriesParserV2 extends AbstractParser {
    private static final Logger LOGGER = Logger.getLogger(LibraryGeometriesParserV2.class.getSimpleName());
    private static final String FLOAT_ARRAY_TAG = "float_array";
    private static final String INPUT_TAG = "input";
    private static final String P_TAG = "p";
    private static final String POLYGONS_TAG = "polygons";
    private static final String POLYLIST_TAG = "polylist";
    private static final String SOURCE_TAG = "source";
    private static final String VCOUNT_TAG = "vcount";
    private static final String VERTICES_TAG = "vertices";

    private final Map<String, String> currentId = new HashMap<>();
    private final Map<String, float[]> floatArrays = new HashMap<>();
    private final Map<String, Input> inputs = new HashMap<>();
    private final List<int[]> pLists = new ArrayList<>();
    private final Map<String, List<TriangleMesh>> meshes = new HashMap<>();
    private final Map<String, List<String>> materials = new HashMap<>();
    private int[] vCounts;
    private boolean triangulated = true;

    List<TriangleMesh> getMeshes(final String meshId) {
        return meshes.getOrDefault(meshId, new ArrayList<>());
    }

    List<String> getMaterialIds(final String meshId) {
        return materials.getOrDefault(meshId, new ArrayList<>());
    }

    LibraryGeometriesParserV2() {
        final Map<String, Consumer<StartElement>> startElementConsumer = new HashMap<>();

        startElementConsumer.put("*", startElement -> currentId.put(startElement.qName, startElement.getAttributeValue("id")));
        startElementConsumer.put(INPUT_TAG, startElement -> {
            Input input = ParserUtils.createInput(startElement);
            this.inputs.put(input.semantic, input);
        });
        startElementConsumer.put(POLYLIST_TAG, startElement -> {
            final String materialId = startElement.getAttributeValue("material");
            if (materialId != null) {
                final String geometryId = currentId.get("geometry");
                if (!materials.containsKey(geometryId)) {
                    materials.put(geometryId, new ArrayList<>());
                }
                materials.get(geometryId).add(materialId);
            }
            this.inputs.clear();
            this.pLists.clear();
        });

        final Map<String, Consumer<LibraryHandler.EndElement>> endElementConsumer = new HashMap<>();

        endElementConsumer.put(FLOAT_ARRAY_TAG, endElement ->
                floatArrays.put(currentId.get(SOURCE_TAG), ParserUtils.extractFloatArray(endElement.content)));
        endElementConsumer.put(P_TAG, this::savePoints);
        endElementConsumer.put(POLYGONS_TAG, endElement -> createPolygonsTriangleMesh());
        endElementConsumer.put(POLYLIST_TAG, endElement -> createPolylistTriangleMesh());
        endElementConsumer.put(VCOUNT_TAG, this::saveVerticesCounts);
        endElementConsumer.put(VERTICES_TAG, endElement -> saveVertices());


        handler = new LibraryHandler(startElementConsumer, endElementConsumer);
    }

    private void savePoints(LibraryHandler.EndElement endElement) {
        String[] numbers = endElement.content.split("\\s+");
        int[] iArray = new int[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            iArray[i] = Integer.parseInt(numbers[i].trim());
        }
        pLists.add(iArray);
    }

    private void createPolygonsTriangleMesh() {
        // create mesh put in map
        final TriangleMesh mesh = new TriangleMesh();
        final String geometryId = currentId.get("geometry");
        if (!meshes.containsKey(geometryId)) {
            meshes.put(geometryId, new ArrayList<>());
        }
        meshes.get(geometryId).add(mesh);
        throw new UnsupportedOperationException("Need to implement TriangleMesh creation");
    }

    private void createPolylistTriangleMesh() {
        if (!triangulated) {
            LOGGER.warning("Not triangulated meshes aren't supported (yet)");
        }

        final TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);

        // create mesh put in map
        int faceStep = 1;

        final Input vertexInput = inputs.get("VERTEX");
        final Input texInput = inputs.get("TEXCOORD");
        final Input normalInput = inputs.get("NORMAL");

        if (vertexInput != null && (vertexInput.offset + 1) > faceStep)
            faceStep = vertexInput.offset + 1;

        if (texInput != null && (texInput.offset + 1) > faceStep)
            faceStep = texInput.offset + 1;

        if (normalInput != null) {
            mesh.setVertexFormat(VertexFormat.POINT_NORMAL_TEXCOORD);
            if ((normalInput.offset + 1) > faceStep)
                faceStep = normalInput.offset + 1;
        }

        final float[] points = floatArrays.get(vertexInput.source.substring(1));
        final int[] faces = calcFaces(faceStep, vertexInput, texInput, normalInput);

        mesh.getFaces().setAll(faces);
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(calcTexCoords(texInput));
        mesh.getNormals().setAll(calcNormals(normalInput));

        final String geometryId = currentId.get("geometry");
        if (!meshes.containsKey(geometryId)) {
            meshes.put(geometryId, new ArrayList<>());
        }
        meshes.get(geometryId).add(mesh);
    }

    private float[] calcTexCoords(final Input texInput) {
        return (texInput == null)
                ? new float[]{0, 0}
                : floatArrays.get(texInput.source.substring(1));
    }

    private float[] calcNormals(final Input normalInput) {
        return (normalInput == null)
                ? new float[]{}
                : floatArrays.get(normalInput.source.substring(1));
    }

    private int[] calcFaces(final int faceStep, final Input vertexInput, final Input texInput, final Input normalInput) {
        final int inputCount = (normalInput == null) ? 2 : 3;
        final int[] faces = new int[IntStream.of(vCounts).sum() * inputCount];
        final int[] p = pLists.get(0);
        int pIndex = 0;

        int faceIndex = 0;
        for (int vCount : vCounts) {
            for (int v = 0; v < vCount; v++) {
                faces[faceIndex + v * inputCount] = p[pIndex + vertexInput.offset];
                if (inputCount == 3)
                    faces[faceIndex + v * inputCount + 1] = p[pIndex + normalInput.offset];
                faces[faceIndex + v * inputCount + inputCount - 1] = (texInput == null) ? 0 : p[pIndex + texInput.offset];
                pIndex += faceStep;
            }
            faceIndex += vCount * inputCount;
        }
        return faces;
    }

    private void saveVerticesCounts(LibraryHandler.EndElement endElement) {
        final String[] numbers = endElement.content.split("\\s+");
        triangulated = true;
        vCounts = new int[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            vCounts[i] = Integer.parseInt(numbers[i].trim());
            if (vCounts[i] != 3 && triangulated) triangulated = false;
        }
    }

    private void saveVertices() {
        // put vertex float into map again with new ID
        String sourceId = inputs.get("POSITION").source.substring(1);
        float[] points = floatArrays.get(sourceId);
        floatArrays.put(
                currentId.get("vertices"),
                points);
    }

}
