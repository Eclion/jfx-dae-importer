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

    final Map<String, Joint> joints = new LinkedHashMap<>();
    private final Map<String, Affine> bindTransforms = new LinkedHashMap<>();

    private DaeSkeleton(final String id) {
        setId(id);
    }

    public static DaeSkeleton fromDaeNode(final DaeNode rootNode) {
        final DaeSkeleton skeleton = new DaeSkeleton(rootNode.getId());

        //TODO should the rootNode transforms be local to the parent or global?
        skeleton.getTransforms().addAll(rootNode.getTransforms());

        final List<DaeNode> rootDaeNodes = new ArrayList<>();
        rootDaeNodes.addAll(rootNode.getDaeNodeChildStream().
                filter(DaeNode::isJoint).
                collect(Collectors.toList()));

        skeleton.getChildren().addAll(buildBone(rootDaeNodes, skeleton.joints, skeleton.bindTransforms));

        return skeleton;
    }

    private static List<Joint> buildBone(final List<DaeNode> daeNodes, final Map<String, Joint> joints, final Map<String, Affine> bindTransforms) {
        return daeNodes.stream().
                map(node -> {
                    final Joint joint = createJointFromNode(node);

                    joints.put(joint.getId(), joint);
                    bindTransforms.put(joint.getId(), joint.a);

                    final List<DaeNode> children = node.getDaeNodeChildStream().
                            filter(DaeNode::isJoint).
                            collect(Collectors.toList());
                    joint.getChildren().addAll(buildBone(children, joints, bindTransforms));
                    return joint;
                }).
                collect(Collectors.toList());
    }

    private static Joint createJointFromNode(final DaeNode node) {
        final Joint joint = new Joint();
        joint.setId(node.getId());

        node.getTransforms().stream().
                filter(transform -> transform instanceof Affine).
                findFirst().
                ifPresent(joint.a::setToTransform);

        return joint;
    }
}
