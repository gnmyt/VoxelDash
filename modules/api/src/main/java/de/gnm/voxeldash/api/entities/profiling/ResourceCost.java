package de.gnm.voxeldash.api.entities.profiling;

public class ResourceCost {

    /**
     * Display name of the resource (plugin/mod name, or "Minecraft" etc.).
     */
    public String resource;

    /**
     * The kind of resource: {@code plugin}, {@code mod}, {@code minecraft},
     * {@code jvm}, {@code shared} or {@code unknown}.
     */
    public String resourceType;

    /**
     * Samples where a frame of this resource was on top of the stack.
     */
    public long selfSamples;

    /**
     * Samples whose stack contained this resource at least once (counted once
     * per sample even if the resource appears multiple times).
     */
    public long totalSamples;

    /**
     * {@link #selfSamples} as a percentage of all samples.
     */
    public double selfPct;

    /**
     * {@link #totalSamples} as a percentage of all samples.
     */
    public double totalPct;

    public ResourceCost() {
    }

    public ResourceCost(String resource, String resourceType) {
        this.resource = resource;
        this.resourceType = resourceType;
    }
}
