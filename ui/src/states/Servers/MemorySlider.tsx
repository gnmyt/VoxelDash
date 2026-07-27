import {useEffect, useState} from "react";
import {t} from "i18next";
import {masterJson} from "@/lib/RequestUtil.ts";
import {Label} from "@/components/ui/label.tsx";
import {Slider} from "@/components/ui/slider.tsx";

export const MEMORY_MIN = 1024;
export const MEMORY_STEP = 512;
const MEMORY_FALLBACK_MAX = 16384;

export const formatGb = (mb: number) => `${Number.isInteger(mb / 1024) ? mb / 1024 : (mb / 1024).toFixed(1)} GB`;

export const snapToStep = (mb: number) => Math.max(MEMORY_MIN, Math.round(mb / MEMORY_STEP) * MEMORY_STEP);

const memoryMarks = (max: number) => [
    {mb: snapToStep(MEMORY_MIN + (max - MEMORY_MIN) * 0.25), label: t("create_server.memory_low")},
    {mb: snapToStep(MEMORY_MIN + (max - MEMORY_MIN) * 0.5), label: t("create_server.memory_medium")},
    {mb: snapToStep(MEMORY_MIN + (max - MEMORY_MIN) * 0.75), label: t("create_server.memory_high")},
];

export const useSystemInfo = (enabled: boolean) => {
    const [memoryMax, setMemoryMax] = useState(MEMORY_FALLBACK_MAX);
    const [javaMajors, setJavaMajors] = useState<number[]>([]);

    useEffect(() => {
        if (!enabled) return;
        masterJson("system").then((d) => {
            if (d.totalMemoryMb) setMemoryMax(Math.max(MEMORY_MIN + MEMORY_STEP * 2, snapToStep(d.totalMemoryMb)));
            if (Array.isArray(d.javaMajors)) setJavaMajors(d.javaMajors);
        }).catch(() => {});
    }, [enabled]);

    return {memoryMax, javaMajors};
};

export const useMemoryMax = (enabled: boolean) => useSystemInfo(enabled).memoryMax;

export const MemorySlider = ({value, max, onChange}: { value: number; max: number; onChange: (mb: number) => void }) => (
    <div className="space-y-2">
        <div className="flex items-center justify-between">
            <Label>{t("create_server.memory")}</Label>
            <span className="rounded-md bg-primary/10 px-2 py-0.5 font-mono text-sm font-semibold text-primary">
                {formatGb(value)}
            </span>
        </div>
        <Slider value={[value]} min={MEMORY_MIN} max={max} step={MEMORY_STEP}
                onValueChange={([v]) => onChange(v)} aria-label={t("create_server.memory")}/>
        <div className="relative h-7">
            {memoryMarks(max).map((mark) => {
                const pct = ((mark.mb - MEMORY_MIN) / (max - MEMORY_MIN)) * 100;
                const active = Math.abs(value - mark.mb) < MEMORY_STEP;
                return (
                    <div key={mark.mb} style={{left: `${pct}%`}}
                         className="absolute flex -translate-x-1/2 flex-col items-center gap-1">
                        <span className={`h-1.5 w-px ${active ? "bg-primary" : "bg-border"}`}/>
                        <span className={`whitespace-nowrap text-[10px] leading-none transition-colors
                            ${active ? "font-medium text-primary" : "text-muted-foreground"}`}>
                            {mark.label} · {formatGb(mark.mb)}
                        </span>
                    </div>
                );
            })}
        </div>
    </div>
);
