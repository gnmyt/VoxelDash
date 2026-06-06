package de.gnm.voxeldash.api.entities.profiling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallNode {

    /**
     * Fully-qualified declaring class name of this frame.
     */
    public String className;

    /**
     * Method name of this frame.
     */
    public String method;

    /**
     * The plugin/mod (or "Minecraft"/"JVM"/"unknown") this frame is attributed
     * to, see {@link de.gnm.voxeldash.api.profiling.ResourceAttributor}.
     */
    public String resource;

    /**
     * The kind of resource: {@code plugin}, {@code mod}, {@code minecraft},
     * {@code jvm}, {@code shared} or {@code unknown}.
     */
    public String resourceType;

    /**
     * Samples where this frame was on top of the stack (self time).
     */
    public long selfSamples;

    /**
     * Samples that passed through this frame (self + descendants).
     */
    public long totalSamples;

    /**
     * Child frames called from this one.
     */
    public List<CallNode> children = new ArrayList<>();

    /**
     * Fast lookup index for {@link #child}. Not serialized (Jackson ignores
     * non-public fields by default).
     */
    private transient Map<String, CallNode> index;

    public CallNode() {
    }

    public CallNode(String className, String method, String resource, String resourceType) {
        this.className = className;
        this.method = method;
        this.resource = resource;
        this.resourceType = resourceType;
    }

    /**
     * Returns the existing child for the given class+method, creating it if it
     * does not exist yet. Used only while building the tree.
     */
    public CallNode child(String className, String method, String resource, String resourceType) {
        if (index == null) {
            index = new HashMap<>();
            for (CallNode existing : children) {
                index.put(existing.className + '#' + existing.method, existing);
            }
        }
        String key = className + '#' + method;
        CallNode node = index.get(key);
        if (node == null) {
            node = new CallNode(className, method, resource, resourceType);
            children.add(node);
            index.put(key, node);
        }
        return node;
    }
}
