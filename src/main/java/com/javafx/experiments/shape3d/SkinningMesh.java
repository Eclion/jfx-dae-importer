/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.javafx.experiments.shape3d;

import com.javafx.experiments.importers.dae.structures.Joint;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableFloatArray;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;
import javafx.scene.transform.MatrixType;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Transform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PolygonMesh that knows how to update itself given changes in joint transforms.
 * The mesh can be updated with an AnimationTimer.
 */
public final class SkinningMesh extends TriangleMesh {
    private static final Logger LOGGER = Logger.getLogger(SkinningMesh.class.getSimpleName());
    private final float[][] relativePoints; // nJoints x nPoints*3
    private final float[][] weights; // nJoints x nPoints
    private final List<Integer>[] weightIndices;
    private final List<JointIndex> jointIndexForest = new ArrayList<>();
    private boolean jointsTransformDirty = true;
    private Transform bindGlobalInverseTransform;
    private final Transform[] jointToRootTransforms; // the root refers to the group containing all the mesh skinning nodes (i.e. the parent of jointForest)
    private final int nPoints;
    private final int nJoints;


    /**
     * SkinningMesh constructor.
     *
     * @param mesh                The binding mesh
     * @param jointsWeights       A two-dimensional array (nJoints x nPoints) of the influence weights used for skinning
     * @param bindTransforms      The binding transforms for every joint
     * @param bindGlobalTransform The global binding transform; all binding transforms are defined with respect to this frame
     * @param joints              A list of joints used for skinning; the order of these are associated with the respective attributes of @weights and @bindPoses
     * @param jointForest         A list of the top level trees that contain the joints; all the @joints should be contained in this forest
     */
    public SkinningMesh(final TriangleMesh mesh, final float[][] jointsWeights, final Affine[] bindTransforms,
                        final Affine bindGlobalTransform, final List<Joint> joints, final List<Parent> jointForest) {
        this.copyMesh(mesh);

        this.weights = jointsWeights;

        this.nJoints = joints.size();
        this.nPoints = getPoints().size() / getPointElementSize();

        this.initializeJointIndexForest(joints, jointForest);

        try {
            this.bindGlobalInverseTransform = bindGlobalTransform.createInverse();
        } catch (NonInvertibleTransformException ex) {
            LOGGER.log(Level.SEVERE, "Caught NonInvertibleTransformException: " + ex.getMessage());
        }

        this.jointToRootTransforms = new Transform[nJoints];

        this.weightIndices = this.initializeWeightIndices();

        this.relativePoints = this.initializeRelativePoints(bindTransforms, bindGlobalTransform);

        this.processJoints(joints, jointForest);

        jointsTransformDirty = true;
        update();
    }

    // Create the jointIndexForest forest. Its structure is the same as jointForest, except that this forest have
    // indices information and some branches are pruned if they don't contain joints.
    private void initializeJointIndexForest(final List<Joint> joints, final List<Parent> jointForest) {
        for (final Parent jointRoot : jointForest) {
            jointIndexForest.add(new JointIndex(jointRoot, joints.indexOf(jointRoot), joints));
        }
    }

    // For optimization purposes, store the indices of the non-zero weights
    private List<Integer>[] initializeWeightIndices() {
        final List<Integer>[] weightIndices = new List[nJoints];
        for (int j = 0; j < nJoints; j++) {
            weightIndices[j] = new ArrayList<>();
            for (int i = 0; i < nPoints; i++) {
                if (weights[j][i] != 0.0f) {
                    weightIndices[j].add(i);
                }
            }
        }
        return weightIndices;
    }

    // Compute the points of the binding mesh relative to the binding transforms
    private float[][] initializeRelativePoints(final Affine[] bindTransforms, final Affine bindGlobalTransform) {
        final ObservableFloatArray points = getPoints();
        final float[][] relativePoints = new float[nJoints][nPoints * 3];
        for (int j = 0; j < nJoints; j++) {
            final Transform postBindTransform = bindTransforms[j].createConcatenation(bindGlobalTransform);
            for (int i = 0; i < nPoints; i++) {
                final Point3D relativePoint = postBindTransform.transform(points.get(3 * i), points.get(3 * i + 1), points.get(3 * i + 2));
                relativePoints[j][3 * i] = (float) relativePoint.getX();
                relativePoints[j][3 * i + 1] = (float) relativePoint.getY();
                relativePoints[j][3 * i + 2] = (float) relativePoint.getZ();
            }
        }
        return relativePoints;
    }

    // Add a listener to all the joints (and their parents nodes) so that we can track when any of their transforms have changed
    // Set of joints that already have a listener (so we don't attach a listener to the same node more than once)
    private void processJoints(final List<Joint> joints, final List<Parent> jointForest) {
        final Set<Node> processedNodes = new HashSet<>(joints.size());
        final InvalidationListener invalidationListener = observable -> jointsTransformDirty = true;
        for (Joint joint : joints) {
            Node node = joint;
            while (!processedNodes.contains(node)) {
                node.localToParentTransformProperty().addListener(invalidationListener);
                processedNodes.add(node);
                // Don't check for nodes above the jointForest
                if (jointForest.contains(node) || node.parentProperty().isNull().get()) {
                    break;
                }
                node = node.getParent();
            }
        }
    }

    private void copyMesh(final TriangleMesh mesh) {
        this.getPoints().addAll(mesh.getPoints());
        this.getTexCoords().addAll(mesh.getTexCoords());
        this.getFaces().addAll(mesh.getFaces());
        this.getFaceSmoothingGroups().addAll(mesh.getFaceSmoothingGroups());
        this.getNormals().addAll(mesh.getNormals());
        this.setVertexFormat(mesh.getVertexFormat());
    }

    private final class JointIndex {
        private final Node node;
        private final int index;
        private final List<JointIndex> children = new ArrayList<>();
        private JointIndex parent = null;
        private Transform localToGlobalTransform;

        JointIndex(final Node n, final int ind, final List<Joint> orderedJoints) {
            node = n;
            index = ind;
            if (!(node instanceof Parent)) {
                return;
            }
            ((Parent) node)
                    .getChildrenUnmodifiable()
                    .stream()
                    .filter(childJoint -> childJoint instanceof Parent)
                    .forEach(childJoint -> {
                                final int childInd = orderedJoints.indexOf(childJoint);
                                final JointIndex childJointIndex = new JointIndex(childJoint, childInd, orderedJoints);
                                childJointIndex.parent = this;
                                children.add(childJointIndex);
                            }
                    );
        }
    }

    // Updates the jointToRootTransforms by doing a a depth-first search of the jointIndexForest
    private void updateLocalToGlobalTransforms(final List<JointIndex> jointIndexForest) {
        for (final JointIndex jointIndex : jointIndexForest) {
            if (jointIndex.parent == null) {
                jointIndex.localToGlobalTransform = bindGlobalInverseTransform.createConcatenation(jointIndex.node.getLocalToParentTransform());
            } else {
                jointIndex.localToGlobalTransform = jointIndex.parent.localToGlobalTransform.createConcatenation(jointIndex.node.getLocalToParentTransform());
            }
            if (jointIndex.index != -1) {
                jointToRootTransforms[jointIndex.index] = jointIndex.localToGlobalTransform;
            }
            updateLocalToGlobalTransforms(jointIndex.children);
        }
    }

    // Updates its points only if any of the joints' transforms have changed
    public final void update() {
        if (!jointsTransformDirty) {
            return;
        }

        updateLocalToGlobalTransforms(jointIndexForest);

        updatePoints();
        updateNormals();

        jointsTransformDirty = false;
    }

    private void updatePoints() {
        final float[] points = new float[nPoints * 3];
        final double[] t = new double[12];
        float[] relativePoint;
        for (int j = 0; j < nJoints; j++) {
            jointToRootTransforms[j].toArray(MatrixType.MT_3D_3x4, t);
            relativePoint = relativePoints[j];
            for (Integer i : weightIndices[j]) {
                points[3 * i] += weights[j][i] * (t[0] * relativePoint[3 * i] + t[1] * relativePoint[3 * i + 1] + t[2] * relativePoint[3 * i + 2] + t[3]);
                points[3 * i + 1] += weights[j][i] * (t[4] * relativePoint[3 * i] + t[5] * relativePoint[3 * i + 1] + t[6] * relativePoint[3 * i + 2] + t[7]);
                points[3 * i + 2] += weights[j][i] * (t[8] * relativePoint[3 * i] + t[9] * relativePoint[3 * i + 1] + t[10] * relativePoint[3 * i + 2] + t[11]);
            }
        }
        this.getPoints().set(0, points, 0, points.length);
    }

    private void updateNormals() {
        float[] normals = this.getNormals().toArray(null);
        final ObservableFaceArray faces = this.getFaces();
        final int faceSize = getFaceElementSize();
        final int pointSize = getPointElementSize();

        for (int i = 0; i < faces.size() / faceSize; i++) {
            int index1 = faces.get(i * faceSize);
            int index2 = faces.get(i * faceSize + pointSize);
            int index3 = faces.get(i * faceSize + pointSize * 2);
            final Point3D p1 = getPoint(index1);
            final Point3D p2 = getPoint(index2);
            final Point3D p3 = getPoint(index3);
            final Point3D newNormal = calculateNormal(p1, p2, p3);
            normals[i * 3] = (float) newNormal.getX();
            normals[i * 3 + 1] = (float) newNormal.getY();
            normals[i * 3 + 2] = (float) newNormal.getZ();
        }

        getNormals().setAll(normals);
    }

    private Point3D getPoint(int index) {
        return new Point3D(getPoints().get(3 * index), getPoints().get(3 * index + 1), getPoints().get(3 * index + 2));
    }

    private Point3D calculateNormal(Point3D p1, Point3D p2, Point3D p3) {
        Point3D u = p2.subtract(p1);
        Point3D v = p3.subtract(p1);
        return u.crossProduct(v).normalize();
    }
}