package de.gnm.voxeldash.api.routes;

import de.gnm.voxeldash.api.annotations.*;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.entities.profiling.ProfilingConfig;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.pipes.ProfilingPipe;

import static de.gnm.voxeldash.api.http.HTTPMethod.GET;
import static de.gnm.voxeldash.api.http.HTTPMethod.POST;

public class ProfilingRouter extends BaseRoute {

    @ApiDoc(summary = "Get profiling capabilities", description = "Returns what this platform can profile (in-process sampling, deep mode, etc.). The UI adapts to these flags.", tag = "Profiling")
    @AuthenticatedRoute
    @Path("/profiling/capabilities")
    @RequiresFeatures(Feature.Profiling)
    @Method(GET)
    public JSONResponse getCapabilities() {
        return new JSONResponse().add("capabilities", getPipe(ProfilingPipe.class).getCapabilities());
    }

    @ApiDoc(summary = "Get live performance metrics", description = "Returns a cheap snapshot of TPS, MSPT, heap/non-heap memory, GC and entity/chunk counts. Intended to be polled by the live charts.", tag = "Profiling")
    @AuthenticatedRoute
    @Path("/profiling/metrics")
    @RequiresFeatures(Feature.Profiling)
    @Method(GET)
    public JSONResponse getMetrics() {
        return new JSONResponse().add("metrics", getPipe(ProfilingPipe.class).getMetrics());
    }

    @ApiDoc(summary = "Start a profiling session", description = "Begins sampling the server tick thread. Use the status endpoint to poll progress and stop to retrieve the result.", tag = "Profiling")
    @ApiField(name = "intervalMs", description = "Sampling interval in milliseconds (default 10).")
    @ApiField(name = "durationSec", description = "Auto-stop after this many seconds (0 = until stopped).")
    @AuthenticatedRoute
    @Path("/profiling/start")
    @RequiresFeatures(value = Feature.Profiling, level = PermissionLevel.FULL)
    @Method(POST)
    public JSONResponse start(JSONRequest request) {
        ProfilingConfig config = new ProfilingConfig();
        try {
            if (request.has("intervalMs")) config.intervalMs = request.getInt("intervalMs");
            if (request.has("durationSec")) config.durationSec = request.getInt("durationSec");
            if (request.has("allThreads")) config.allThreads = request.getBoolean("allThreads");
        } catch (Exception ignored) {
        }
        return new JSONResponse().add("status", getPipe(ProfilingPipe.class).start(config));
    }

    @ApiDoc(summary = "Get profiling status", description = "Returns whether a session is running, how long it has been running and how many samples were collected.", tag = "Profiling")
    @AuthenticatedRoute
    @Path("/profiling/status")
    @RequiresFeatures(Feature.Profiling)
    @Method(GET)
    public JSONResponse getStatus() {
        return new JSONResponse().add("status", getPipe(ProfilingPipe.class).getStatus());
    }

    @ApiDoc(summary = "Stop a profiling session", description = "Stops the active session and returns the aggregated call tree plus the per-plugin/mod cost breakdown.", tag = "Profiling")
    @AuthenticatedRoute
    @Path("/profiling/stop")
    @RequiresFeatures(value = Feature.Profiling, level = PermissionLevel.FULL)
    @Method(POST)
    public JSONResponse stop() {
        return new JSONResponse().add("result", getPipe(ProfilingPipe.class).stop());
    }
}
