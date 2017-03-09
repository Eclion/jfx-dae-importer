package com.javafx.experiments.importers.dae.parsers;

import javafx.scene.Camera;
import javafx.scene.PerspectiveCamera;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Eclion
 */
final class LibraryCamerasParser extends AbstractParser {
    private static final double DEFAULT_ASPECT_RATIO = 4d / 3d;
    private static final String ASPECT_RATIO_TAG = "aspect_ratio";
    private static final String CAMERA_TAG = "camera";
    private static final String XFOV_TAG = "xfov";
    private static final String YFOV_TAG = "yfov";
    private static final String ZFAR_TAG = "zfar";
    private static final String ZNEAR_TAG = "znear";

    private final Map<String, String> currentId = new HashMap<>();
    private Double aspectRatio, xfov, yfov, znear, zfar;
    Camera firstCamera = null;
    final Map<String, Camera> cameras = new HashMap<>();
    double firstCameraAspectRatio = DEFAULT_ASPECT_RATIO;

    LibraryCamerasParser(){
        final Map<String, Consumer<StartElement>> startElementConsumer = new HashMap<>();

        startElementConsumer.put("*", startElement -> currentId.put(startElement.qName, startElement.getAttributeValue("id")));
        startElementConsumer.put(CAMERA_TAG, startElement -> aspectRatio = xfov = yfov = znear = zfar = null);

        final Map<String, Consumer<LibraryHandler.EndElement>> endElementConsumer = new HashMap<>();

        endElementConsumer.put(ASPECT_RATIO_TAG, endElement -> aspectRatio = Double.parseDouble(endElement.content));
        endElementConsumer.put(CAMERA_TAG, endElement -> saveCamera());
        endElementConsumer.put(XFOV_TAG, endElement -> xfov = Double.parseDouble(endElement.content));
        endElementConsumer.put(YFOV_TAG, endElement -> yfov = Double.parseDouble(endElement.content));
        endElementConsumer.put(ZFAR_TAG, endElement -> zfar = Double.parseDouble(endElement.content));
        endElementConsumer.put(ZNEAR_TAG, endElement -> znear = Double.parseDouble(endElement.content));

        handler = new LibraryHandler(startElementConsumer, endElementConsumer);
    }

    private void saveCamera() {
        PerspectiveCamera camera = new PerspectiveCamera(true);
        if (yfov != null) {
            camera.setVerticalFieldOfView(true);
            camera.setFieldOfView(yfov);
        } else if (xfov != null) {
            camera.setVerticalFieldOfView(false);
            camera.setFieldOfView(xfov);
        }
        if (znear != null) camera.setNearClip(znear);
        if (zfar != null) camera.setFarClip(zfar);
        cameras.put(currentId.get("camera"), camera);
        if (firstCamera == null) {
            firstCamera = camera;
            if (aspectRatio != null) firstCameraAspectRatio = aspectRatio;
        }
    }
}
