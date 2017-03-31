package com.javafx.experiments.importers.dae.structures;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eclion
 */
public final class DaeEffect {

    private static final String PHONG_TAG = "phong";
    private static final String AMBIENT_TAG = "ambient";
    private static final String DIFFUSE_TAG = "diffuse";
    private static final String EMISSION_TAG = "emission";
    private static final String SPECULAR_TAG = "specular";

    public final String id;

    public final Map<String, String> surfaces = new HashMap<>();
    public final Map<String, String> samplers = new HashMap<>();
    public final Map<String, Color> colors = new HashMap<>();
    public final Map<String, String> textureIds = new HashMap<>();

    private String type;

    public DaeEffect(final String id) {
        this.id = id;
    }

    public Material build(final Map<String, Image> images) {
        if (PHONG_TAG.equals(this.type)) {
            return buildPhongMaterial(images);
        }
        return null;
    }

    private PhongMaterial buildPhongMaterial(final Map<String, Image> images) {
        final PhongMaterial material = new PhongMaterial();

        buildPhongMaterialColors(material);

        buildPhongMaterialTextures(material, images);
        return material;
    }

    private void buildPhongMaterialColors(final PhongMaterial material) {
        colors.forEach((key, value) -> {
            switch (key) {
                case DIFFUSE_TAG:
                    material.setDiffuseColor(value);
                    break;
                case SPECULAR_TAG:
                    material.setSpecularColor(value);
                    break;
                case AMBIENT_TAG:
                case EMISSION_TAG:
                default:
                    break;
            }
        });
    }

    private void buildPhongMaterialTextures(final PhongMaterial material, final Map<String, Image> images) {
        textureIds.entrySet().stream().
                filter(entry -> samplers.containsKey(entry.getValue())).
                filter(entry -> surfaces.containsKey(samplers.get(entry.getValue()))).
                filter(entry -> images.get(surfaces.get(samplers.get(entry.getValue()))) != null).
                forEach(entry -> {
                    final Image image = images.get(surfaces.get(samplers.get(entry.getValue())));
                    switch (entry.getKey()) {
                        case DIFFUSE_TAG:
                            material.setDiffuseMap(image);
                            break;
                        case SPECULAR_TAG:
                            material.setSpecularMap(image);
                            break;
                        case AMBIENT_TAG:
                        case EMISSION_TAG:
                        default:
                            break;
                    }
                });
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean hasType() {
        return type != null;
    }
}