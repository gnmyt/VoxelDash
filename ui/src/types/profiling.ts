export interface ProfilingCapabilities {
    sampling: boolean;
}

export interface LiveMetrics {
    tps: number;
    mspt: number;
    heapUsedMb: number;
    heapMaxMb: number;
    nonHeapMb: number;
    gcCount: number;
    gcTimeMs: number;
    threadCount: number;
    entityCount: number;
    chunkCount: number;
}

export interface ProfilingStatus {
    running: boolean;
    elapsedMs: number;
    sampleCount: number;
    supported: boolean;
    message?: string | null;
}

export interface CallNode {
    className: string | null;
    method: string;
    resource: string | null;
    resourceType: string | null;
    selfSamples: number;
    totalSamples: number;
    children: CallNode[];
}

export interface ResourceCost {
    resource: string;
    resourceType: string;
    selfSamples: number;
    totalSamples: number;
    selfPct: number;
    totalPct: number;
}

export interface ProfilingResult {
    root: CallNode | null;
    byResource: ResourceCost[];
    sampleCount: number;
    durationMs: number;
    threadName: string | null;
}
