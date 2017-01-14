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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * @author Eclion
 */
final class LibraryGeometriesParser extends DefaultHandler {
    private static final Logger LOGGER = Logger.getLogger(LibraryGeometriesParser.class.getSimpleName());
    private StringBuilder charBuf = new StringBuilder();
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

    private enum State {
        UNKNOWN,
        float_array,
        input,
        p,
        polygons,
        polylist,
        source,
        vcount,
        vertices,

        // ignored, unsupported states:
        accessor,
        double_sided,
        extra,
        geometry,
        mesh,
        param,
        technique,
        technique_common
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
        this.currentId.put(qName, attributes.getValue("id"));
        this.charBuf = new StringBuilder();
        switch (state(qName)) {
            case UNKNOWN:
                LOGGER.log(Level.WARNING, "Unknown element: " + qName);
                break;
            case input:
                Input input = ParserUtils.createInput(attributes);
                this.inputs.put(input.semantic, input);
                break;
            case polylist:
                final String materialId = attributes.getValue("material");
                if (materialId != null)
                {
                    final String geometryId = currentId.get("geometry");
                    if (!materials.containsKey(geometryId)){
                        materials.put(geometryId, new ArrayList<>());
                    }
                    materials.get(geometryId).add(materialId);
                }
                this.inputs.clear();
                this.pLists.clear();
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
            case float_array:
                floatArrays.put(currentId.get("source"),
                        ParserUtils.extractFloatArray(charBuf));
                break;
            case p:
                savePoints();
                break;
            case polygons:
                createPolygonsTriangleMesh();
                break;
            case polylist:
                createPolylistTriangleMesh();
                break;
            case vcount:
                saveVerticesCounts();
                break;
            case vertices:
                saveVertices();
                break;
            default:
                break;
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        charBuf.append(ch, start, length);
    }

    private void savePoints() {
        String[] numbers = charBuf.toString().trim().split("\\s+");
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
        if (!meshes.containsKey(geometryId)){
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
        if (!meshes.containsKey(geometryId)){
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

    private void saveVerticesCounts() {
        final String[] numbers = ParserUtils.splitCharBuffer(charBuf);
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
