package com.javafx.experiments.importers.dae.structures;

import javafx.scene.Parent;
import javafx.scene.transform.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Eclion
 */
public final class DaeSkeleton extends Parent {

    private final String name;
    public final Map<String, Joint> joints = new LinkedHashMap<>();
    public final Map<String, Affine> bindTransforms = new LinkedHashMap<>();

    DaeSkeleton(String id, String name) {
        setId(id);
        this.name = name;
    }

    public static DaeSkeleton fromDaeNode(DaeNode rootNode) {
        final DaeSkeleton skeleton = new DaeSkeleton(rootNode.id, rootNode.name);

        skeleton.getTransforms().addAll(rootNode.transforms);

        List<DaeNode> rootDaeNodes = new ArrayList<>();
        rootDaeNodes.addAll(rootNode.children.values().stream().filter(DaeNode::isJoint).collect(Collectors.toList()));

        skeleton.getChildren().addAll(buildBone(rootDaeNodes, skeleton.joints, skeleton.bindTransforms));

        return skeleton;
    }

    private static List<Joint> buildBone(final List<DaeNode> daeNodes, final Map<String, Joint> joints, final Map<String, Affine> bindTransforms) {
        return daeNodes.stream()
                .map(node -> {
                    final Joint joint = new Joint();
                    joint.setId(node.id);

                    joints.put(joint.getId(), joint);

                    node.transforms.stream()
                            .filter(transform -> transform instanceof Affine)
                            .findFirst()
                            .ifPresent(joint.a::setToTransform);

                    bindTransforms.put(joint.getId(), joint.a);

                    final List<DaeNode> children = node.children.values().stream().filter(DaeNode::isJoint).collect(Collectors.toList());
                    joint.getChildren().addAll(buildBone(children, joints, bindTransforms));
                    return joint;
                })
                .collect(Collectors.toList());
    }
}
