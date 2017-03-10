package com.javafx.experiments.importers.dae.parsers;

import javafx.scene.Camera;
import javafx.scene.PerspectiveCamera;

import java.util.HashMap;
import java.util.Map;

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
    private Double aspectRatio;
    private Double xfov;
    private Double yfov;
    private Double znear;
    private Double zfar;
    Camera firstCamera = null;
    final Map<String, Camera> cameras = new HashMap<>();
    double firstCameraAspectRatio = DEFAULT_ASPECT_RATIO;

    LibraryCamerasParser() {
        addStartElementBiConsumer("*", (qName, attributes) -> currentId.put(qName, attributes.getValue("id")));
        addStartElementBiConsumer(CAMERA_TAG, (qName, attributes) -> aspectRatio = xfov = yfov = znear = zfar = null);

        addEndElementBiConsumer(ASPECT_RATIO_TAG, (qName, content) -> aspectRatio = Double.parseDouble(content));
        addEndElementBiConsumer(CAMERA_TAG, (qName, content) -> saveCamera());
        addEndElementBiConsumer(XFOV_TAG, (qName, content) -> xfov = Double.parseDouble(content));
        addEndElementBiConsumer(YFOV_TAG, (qName, content) -> yfov = Double.parseDouble(content));
        addEndElementBiConsumer(ZFAR_TAG, (qName, content) -> zfar = Double.parseDouble(content));
        addEndElementBiConsumer(ZNEAR_TAG, (qName, content) -> znear = Double.parseDouble(content));
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
        cameras.put(currentId.get(CAMERA_TAG), camera);
        if (firstCamera == null) {
            firstCamera = camera;
            if (aspectRatio != null) firstCameraAspectRatio = aspectRatio;
        }
    }
}
