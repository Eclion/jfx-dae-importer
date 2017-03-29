package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.importers.dae.structures.Input;
import com.javafx.experiments.importers.dae.utils.ParserUtils;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * @author Eclion
 */
final class LibraryGeometriesParser extends AbstractParser {
    private static final Logger LOGGER = Logger.getLogger(LibraryGeometriesParser.class.getSimpleName());
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
    final Map<String, List<TriangleMesh>> meshes = new HashMap<>();
    final Map<String, List<String>> materials = new HashMap<>();
    private int[] vCounts;
    private boolean triangulated = true;

    List<TriangleMesh> getMeshes(final String meshId) {
        return meshes.getOrDefault(meshId, new ArrayList<>());
    }

    List<String> getMaterialIds(final String meshId) {
        return materials.getOrDefault(meshId, new ArrayList<>());
    }

    LibraryGeometriesParser() {
        addStartElementBiConsumer("*", (qName, attributes) -> currentId.put(qName, attributes.getValue("id")));
        addStartElementBiConsumer(INPUT_TAG, (qName, attributes) -> {
            Input input = ParserUtils.createInput(qName, attributes);
            this.inputs.put(input.semantic, input);
        });
        addStartElementBiConsumer(POLYLIST_TAG, (qName, attributes) -> {
            Optional.ofNullable(attributes.getValue("material")).
                    ifPresent(materialId -> {
                        final String geometryId = currentId.get("geometry");
                        if (!materials.containsKey(geometryId)) {
                            materials.put(geometryId, new ArrayList<>());
                        }
                        materials.get(geometryId).add(materialId);
                    });
            this.inputs.clear();
            this.pLists.clear();
        });

        addEndElementBiConsumer(FLOAT_ARRAY_TAG, (qName, content) ->
                floatArrays.put(currentId.get(SOURCE_TAG), ParserUtils.extractFloatArray(content)));
        addEndElementBiConsumer(P_TAG, (qName, content) -> pLists.add(ParserUtils.extractIntArray(content)));
        addEndElementBiConsumer(POLYGONS_TAG, (qName, content) -> createPolygonsTriangleMesh());
        addEndElementBiConsumer(POLYLIST_TAG, (qName, content) -> createPolylistTriangleMesh());
        addEndElementBiConsumer(VCOUNT_TAG, (qName, content) -> {
            setTriangulatedFromContent(content);
            saveVerticesCounts(content);
        });
        addEndElementBiConsumer(VERTICES_TAG, (qName, content) -> saveVertices());
    }

    private void createPolygonsTriangleMesh() {
/*        final TriangleMesh mesh = new TriangleMesh();
        final String geometryId = currentId.get("geometry");
        if (!meshes.containsKey(geometryId)) {
            meshes.put(geometryId, new ArrayList<>());
        }
        meshes.get(geometryId).add(mesh);*/
        throw new UnsupportedOperationException("Need to implement TriangleMesh Polygons creation");
    }

    private void createPolylistTriangleMesh() {
        if (!triangulated) {
            LOGGER.warning("Not triangulated meshes aren't supported (yet)");
        }

        final TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);

        int faceStep = 1;

        final String geometryId = currentId.get("geometry");

        final Input vertexInput = inputs.get("VERTEX");
        final Input texInput = inputs.get("TEXCOORD");
        final Input normalInput = inputs.get("NORMAL");

        if (vertexInput != null && (vertexInput.offset + 1) > faceStep) {
            faceStep = vertexInput.offset + 1;
        } else if (vertexInput == null) {
            LOGGER.log(Level.SEVERE, "Incorrect vertex inputs for " + geometryId);
            return;
        }

        if (texInput != null && (texInput.offset + 1) > faceStep) {
            faceStep = texInput.offset + 1;
        }

        if (normalInput != null) {
            mesh.setVertexFormat(VertexFormat.POINT_NORMAL_TEXCOORD);
            if ((normalInput.offset + 1) > faceStep) {
                faceStep = normalInput.offset + 1;
            }
        }

        final float[] points = floatArrays.get(vertexInput.source.substring(1));
        final int[] faces = calcFaces(faceStep, vertexInput, texInput, normalInput);

        mesh.getFaces().setAll(faces);
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(calcTexCoords(texInput));
        mesh.getNormals().setAll(calcNormals(normalInput));

        if (!meshes.containsKey(geometryId)) {
            meshes.put(geometryId, new ArrayList<>());
        }

        meshes.get(geometryId).add(mesh);
    }

    private float[] calcTexCoords(final Input texInput) {
        return Optional.ofNullable(texInput).
                map(input -> floatArrays.get(input.source.substring(1))).
                orElse(new float[]{0, 0});
    }

    private float[] calcNormals(final Input normalInput) {
        return Optional.ofNullable(normalInput).
                map(input -> floatArrays.get(input.source.substring(1))).
                orElse(new float[]{});
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

    private void setTriangulatedFromContent(final String content) {
        triangulated = content.replaceAll("[3 ]", "").isEmpty();
    }

    private void saveVerticesCounts(final String content) {
        vCounts = ParserUtils.extractIntArray(content);
    }

    private void saveVertices() {
        final String sourceId = inputs.get("POSITION").source.substring(1);
        float[] points = floatArrays.get(sourceId);
        floatArrays.put(currentId.get(VERTICES_TAG), points);
    }
}
