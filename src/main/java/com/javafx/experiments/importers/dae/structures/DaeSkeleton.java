package com.javafx.experiments.importers.dae.structures;

import javafx.scene.Parent;
import javafx.scene.transform.Affine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Eclion
 */
public final class DaeSkeleton extends Parent {

    private final String name;
    public final Map<String, Joint> joints = new LinkedHashMap<>();
    private final Map<String, Affine> bindTransforms = new LinkedHashMap<>();

    DaeSkeleton(final String id, final String name) {
        setId(id);
        this.name = name;
    }

    public static DaeSkeleton fromDaeNode(final DaeNode rootNode) {
        final DaeSkeleton skeleton = new DaeSkeleton(rootNode.getId(), rootNode.name);

        skeleton.getTransforms().addAll(rootNode.getTransforms());

        List<DaeNode> rootDaeNodes = new ArrayList<>();
        rootDaeNodes.addAll(rootNode.getChildren().stream()
                .filter(node -> node instanceof DaeNode)
                .map(node -> (DaeNode) node)
                .filter(DaeNode::isJoint)
                .collect(Collectors.toList()));

        skeleton.getChildren().addAll(buildBone(rootDaeNodes, skeleton.joints, skeleton.bindTransforms));

        return skeleton;
    }

    private static List<Joint> buildBone(final List<DaeNode> daeNodes, final Map<String, Joint> joints, final Map<String, Affine> bindTransforms) {
        return daeNodes.stream().
                map(node -> {
                    final Joint joint = new Joint();
                    joint.setId(node.getId());

                    joints.put(joint.getId(), joint);

                    node.getTransforms().stream().
                            filter(transform -> transform instanceof Affine).
                            findFirst().
                            ifPresent(joint.a::setToTransform);

                    bindTransforms.put(joint.getId(), joint.a);

                    final List<DaeNode> children = node.getChildren().stream()
                            .filter(n -> n instanceof DaeNode)
                            .map(n -> (DaeNode) n)
                            .filter(DaeNode::isJoint)
                            .collect(Collectors.toList());
                    joint.getChildren().addAll(buildBone(children, joints, bindTransforms));
                    return joint;
                }).
                collect(Collectors.toList());
    }
}
