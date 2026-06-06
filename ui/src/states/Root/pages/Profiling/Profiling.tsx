import {ReactNode, useCallback, useEffect, useRef, useState} from "react";
import {
    CircleNotchIcon,
    CpuIcon,
    CubeIcon,
    FireIcon,
    GaugeIcon,
    LightningIcon,
    PlayIcon,
    StackIcon,
    StopIcon,
    TimerIcon,
    WarningCircleIcon,
} from "@phosphor-icons/react";
import {t} from "i18next";
import {jsonRequest, postRequest} from "@/lib/RequestUtil.ts";
import {Button} from "@/components/ui/button.tsx";
import {Alert, AlertDescription, AlertTitle} from "@/components/ui/alert.tsx";
import {ScrollArea} from "@/components/ui/scroll-area.tsx";
import {Badge} from "@/components/ui/badge.tsx";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select.tsx";
import {LiveMetrics, ProfilingCapabilities, ProfilingResult, ProfilingStatus} from "@/types/profiling";
import {colorForResource} from "@/states/Root/pages/Profiling/components/colors";
import CallTreeView from "@/states/Root/pages/Profiling/components/CallTreeView.tsx";

const DURATIONS = [
    {value: "30", labelKey: "profiling.duration.30s"},
    {value: "60", labelKey: "profiling.duration.60s"},
    {value: "120", labelKey: "profiling.duration.120s"},
    {value: "0", labelKey: "profiling.duration.manual"},
];

const MetricTile = ({icon, label, value, accent, ratio}: {
    icon: ReactNode;
    label: string;
    value: string;
    accent?: string;
    ratio?: number;
}) => (
    <div className="flex min-w-[7.5rem] flex-1 flex-col gap-1.5 rounded-xl border bg-card p-3">
        <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            {icon}
            <span>{label}</span>
        </div>
        <span className="text-xl font-semibold tabular-nums" style={accent ? {color: accent} : undefined}>{value}</span>
        {ratio !== undefined && (
            <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
                <div className="h-full rounded-full transition-all"
                     style={{width: `${Math.min(100, Math.max(0, ratio * 100))}%`, backgroundColor: accent ?? "hsl(var(--primary))"}}/>
            </div>
        )}
    </div>
);

const Profiling = () => {
    const [capabilities, setCapabilities] = useState<ProfilingCapabilities | null>(null);
    const [metrics, setMetrics] = useState<LiveMetrics | null>(null);
    const [status, setStatus] = useState<ProfilingStatus | null>(null);
    const [result, setResult] = useState<ProfilingResult | null>(null);
    const [running, setRunning] = useState(false);
    const [duration, setDuration] = useState("60");
    const [busy, setBusy] = useState(false);
    const [selected, setSelected] = useState<string | null>(null);

    const statusTimer = useRef<ReturnType<typeof setInterval> | null>(null);
    const runningRef = useRef(false);

    const stopPolling = useCallback(() => {
        if (statusTimer.current) {
            clearInterval(statusTimer.current);
            statusTimer.current = null;
        }
    }, []);

    const finish = useCallback(async () => {
        stopPolling();
        runningRef.current = false;
        const response = await postRequest("profiling/stop", {});
        if (response?.result) {
            setResult(response.result);
            setSelected(null);
        }
        setRunning(false);
        setBusy(false);
    }, [stopPolling]);

    const pollStatus = useCallback(async () => {
        const response = await jsonRequest("profiling/status");
        const next: ProfilingStatus | undefined = response?.status;
        if (!next) return;
        setStatus(next);
        if (runningRef.current && !next.running) void finish();
    }, [finish]);

    useEffect(() => {
        jsonRequest("profiling/capabilities").then((response) => {
            if (response?.capabilities) setCapabilities(response.capabilities);
        });
        const fetchMetrics = () => {
            jsonRequest("profiling/metrics").then((response) => {
                if (response?.metrics) setMetrics(response.metrics);
            }).catch(() => {});
        };
        fetchMetrics();
        const metricsTimer = setInterval(fetchMetrics, 2000);
        return () => {
            clearInterval(metricsTimer);
            stopPolling();
        };
    }, [stopPolling]);

    const start = useCallback(async () => {
        setBusy(true);
        setResult(null);
        setSelected(null);
        const response = await postRequest("profiling/start", {intervalMs: 10, durationSec: Number(duration)});
        const next: ProfilingStatus | undefined = response?.status;
        if (next && next.supported === false) {
            setBusy(false);
            return;
        }
        runningRef.current = true;
        setRunning(true);
        setBusy(false);
        setStatus(next ?? null);
        stopPolling();
        statusTimer.current = setInterval(() => void pollStatus(), 1000);
    }, [duration, pollStatus, stopPolling]);

    const stop = useCallback(() => {
        setBusy(true);
        void finish();
    }, [finish]);

    const sampling = capabilities?.sampling ?? false;
    const resources = result?.byResource ?? [];

    const tps = metrics?.tps ?? 0;
    const tpsAccent = tps >= 19 ? "hsl(142 60% 45%)" : tps >= 15 ? "hsl(38 92% 50%)" : "hsl(0 72% 55%)";
    const mspt = metrics?.mspt ?? 0;
    const msptAccent = mspt > 50 ? "hsl(0 72% 55%)" : mspt > 40 ? "hsl(38 92% 50%)" : undefined;

    const hasHeap = (metrics?.heapMaxMb ?? 0) > 0;
    const memoryValue = hasHeap
        ? `${metrics?.heapUsedMb ?? 0} / ${metrics?.heapMaxMb} MB`
        : metrics ? `${metrics.heapUsedMb} MB` : "—";
    const memRatio = hasHeap ? (metrics!.heapUsedMb / metrics!.heapMaxMb) : undefined;
    const memAccent = memRatio === undefined ? undefined
        : memRatio > 0.9 ? "hsl(0 72% 55%)" : memRatio > 0.75 ? "hsl(38 92% 50%)" : "hsl(var(--primary))";

    return (
        <div className="flex min-w-0 flex-col p-6 pt-0 gap-6" style={{height: "calc(var(--app-vh) - 5.5rem)"}}>
            <div className="flex items-center justify-between p-4 rounded-xl border bg-card shrink-0">
                <div className="flex items-center gap-4">
                    <GaugeIcon className="h-6 w-6 text-primary" weight="fill"/>
                    <div>
                        <h1 className="text-lg font-semibold">{t("profiling.title")}</h1>
                        <p className="text-xs text-muted-foreground">{t("profiling.subtitle")}</p>
                    </div>
                </div>
                {sampling && (
                    <div className="flex items-center gap-3">
                        <Select value={duration} onValueChange={setDuration} disabled={running}>
                            <SelectTrigger className="w-[150px]">
                                <SelectValue/>
                            </SelectTrigger>
                            <SelectContent>
                                {DURATIONS.map((option) => (
                                    <SelectItem key={option.value} value={option.value}>{t(option.labelKey)}</SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        {running ? (
                            <Button variant="destructive" onClick={stop} disabled={busy}>
                                <StopIcon className="h-4 w-4" weight="fill"/>
                                {t("profiling.stop")}
                            </Button>
                        ) : (
                            <Button onClick={start} disabled={busy}>
                                {busy
                                    ? <CircleNotchIcon className="h-4 w-4 animate-spin"/>
                                    : <PlayIcon className="h-4 w-4" weight="fill"/>}
                                {t("profiling.start")}
                            </Button>
                        )}
                    </div>
                )}
            </div>

            <ScrollArea className="h-full">
                <div className="flex min-w-0 flex-col gap-6">
                    <div className="flex flex-wrap gap-3">
                        <MetricTile icon={<LightningIcon className="h-3.5 w-3.5"/>} label={t("profiling.metrics.tps")}
                                    value={tps.toFixed(1)} accent={tpsAccent} ratio={metrics ? tps / 20 : undefined}/>
                        <MetricTile icon={<TimerIcon className="h-3.5 w-3.5"/>} label={t("profiling.metrics.mspt")}
                                    value={mspt > 0 ? `${mspt.toFixed(1)} ms` : "—"} accent={msptAccent}
                                    ratio={mspt > 0 ? mspt / 50 : undefined}/>
                        <MetricTile icon={<CpuIcon className="h-3.5 w-3.5"/>} label={t("profiling.metrics.memory")}
                                    value={memoryValue} accent={memAccent} ratio={memRatio}/>
                        <MetricTile icon={<FireIcon className="h-3.5 w-3.5"/>} label={t("profiling.metrics.gc")}
                                    value={metrics ? `${metrics.gcCount} · ${metrics.gcTimeMs} ms` : "—"}/>
                        <MetricTile icon={<StackIcon className="h-3.5 w-3.5"/>} label={t("profiling.metrics.threads")}
                                    value={metrics ? String(metrics.threadCount) : "—"}/>
                        <MetricTile icon={<CubeIcon className="h-3.5 w-3.5"/>} label={t("profiling.metrics.entities")}
                                    value={metrics && metrics.entityCount >= 0 ? String(metrics.entityCount) : "—"}/>
                    </div>

                    {!sampling && (
                        <Alert className="rounded-xl">
                            <WarningCircleIcon className="h-5 w-5"/>
                            <AlertTitle>{t("profiling.unsupported.title")}</AlertTitle>
                            <AlertDescription>{t("profiling.unsupported.description")}</AlertDescription>
                        </Alert>
                    )}

                    {sampling && running && (
                        <div className="flex items-center gap-3 rounded-xl border bg-card p-6 text-sm">
                            <CircleNotchIcon className="h-5 w-5 animate-spin text-primary"/>
                            <span>{t("profiling.running", {
                                seconds: Math.round((status?.elapsedMs ?? 0) / 1000),
                                samples: status?.sampleCount ?? 0,
                            })}</span>
                        </div>
                    )}

                    {sampling && !running && !result && (
                        <div className="flex flex-col items-center justify-center gap-2 rounded-xl border border-dashed bg-card/50 p-12 text-center">
                            <FireIcon className="h-8 w-8 text-muted-foreground" weight="duotone"/>
                            <p className="text-sm font-medium">{t("profiling.empty.title")}</p>
                            <p className="text-xs text-muted-foreground">{t("profiling.empty.description")}</p>
                        </div>
                    )}

                    {sampling && result && result.sampleCount > 0 && (
                        <div className="flex flex-col gap-6">
                            <div className="min-w-0 overflow-hidden rounded-xl border bg-card p-4">
                                <div className="mb-3 flex items-center justify-between gap-2">
                                    <h2 className="truncate text-sm font-semibold">{t("profiling.byResource")}</h2>
                                    <span className="shrink-0 text-xs text-muted-foreground">{t("profiling.summary", {
                                        samples: result.sampleCount,
                                        seconds: Math.round(result.durationMs / 1000),
                                    })}</span>
                                </div>
                                <div className="flex flex-col gap-1">
                                    {resources.map((cost, index) => {
                                        const active = selected === cost.resource;
                                        const color = colorForResource(cost.resource, cost.resourceType);
                                        return (
                                            <button
                                                key={cost.resource}
                                                type="button"
                                                onClick={() => setSelected(active ? null : cost.resource)}
                                                className={`flex items-center gap-3 rounded-lg px-2 py-2 text-left transition hover:bg-muted ${active ? "bg-muted ring-1 ring-border" : ""}`}
                                            >
                                                <span className="w-4 shrink-0 text-right text-xs tabular-nums text-muted-foreground">{index + 1}</span>
                                                <span className="flex w-40 shrink-0 items-center gap-2 sm:w-48">
                                                    <span className="h-2.5 w-2.5 shrink-0 rounded-full" style={{backgroundColor: color}}/>
                                                    <span className="truncate text-sm font-medium">{cost.resource}</span>
                                                    <Badge variant="secondary" className="hidden shrink-0 text-[10px] sm:inline-flex">{cost.resourceType}</Badge>
                                                </span>
                                                <div className="relative h-3 flex-1 overflow-hidden rounded-full bg-muted">
                                                    <div className="absolute inset-y-0 left-0 rounded-full transition-all"
                                                         style={{width: `${Math.min(100, cost.selfPct)}%`, backgroundColor: color}}/>
                                                </div>
                                                <span className="w-12 shrink-0 text-right text-sm font-semibold tabular-nums">{cost.selfPct.toFixed(1)}%</span>
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>

                            <div className="min-w-0 overflow-hidden rounded-xl border bg-card p-4">
                                <h2 className="text-sm font-semibold">{t("profiling.tree.title")}</h2>
                                <p className="mb-3 text-xs text-muted-foreground">{t("profiling.tree.hint")}</p>
                                {result.root && <CallTreeView root={result.root} highlight={selected}/>}
                            </div>
                        </div>
                    )}

                    {sampling && result && result.sampleCount === 0 && !running && (
                        <Alert className="rounded-xl">
                            <WarningCircleIcon className="h-5 w-5"/>
                            <AlertTitle>{t("profiling.noSamples.title")}</AlertTitle>
                            <AlertDescription>{t("profiling.noSamples.description")}</AlertDescription>
                        </Alert>
                    )}
                </div>
            </ScrollArea>
        </div>
    );
};

export default Profiling;
