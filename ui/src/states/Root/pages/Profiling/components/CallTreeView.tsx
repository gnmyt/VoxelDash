import {ReactNode, useEffect, useMemo, useState} from "react";
import {CaretDownIcon, CaretRightIcon} from "@phosphor-icons/react";
import {t} from "i18next";
import {CallNode} from "@/types/profiling";
import {colorForResource, shortClass} from "./colors";

const MIN_SHARE = 0.01;
const AUTO_EXPAND_SHARE = 0.2;
const AUTO_EXPAND_DEPTH = 4;
const MAX_DEPTH = 30;

const labelFor = (node: CallNode): string => `${shortClass(node.className)}.${node.method}`;

const visibleChildren = (node: CallNode, total: number): CallNode[] =>
    (node.children ?? [])
        .filter((child) => child.totalSamples / total >= MIN_SHARE)
        .sort((a, b) => b.totalSamples - a.totalSamples);

const autoExpanded = (root: CallNode, total: number): Set<string> => {
    const open = new Set<string>();
    const walk = (node: CallNode, parentKey: string | null, depth: number) => {
        if (depth >= AUTO_EXPAND_DEPTH) return;
        visibleChildren(node, total).forEach((child, index) => {
            const key = parentKey === null ? `${index}` : `${parentKey}/${index}`;
            if (child.totalSamples / total >= AUTO_EXPAND_SHARE) {
                open.add(key);
                walk(child, key, depth + 1);
            }
        });
    };
    walk(root, null, 0);
    return open;
};

interface CallTreeViewProps {
    root: CallNode;
    highlight?: string | null;
}

const CallTreeView = ({root, highlight = null}: CallTreeViewProps) => {
    const total = root.totalSamples || 1;
    const initial = useMemo(() => autoExpanded(root, total), [root, total]);
    const [expanded, setExpanded] = useState<Set<string>>(initial);

    useEffect(() => setExpanded(autoExpanded(root, total)), [root, total]);

    const toggle = (key: string) => setExpanded((prev) => {
        const next = new Set(prev);
        next.has(key) ? next.delete(key) : next.add(key);
        return next;
    });

    const renderRow = (node: CallNode, key: string, depth: number): ReactNode => {
        const pct = (node.totalSamples / total) * 100;
        const children = visibleChildren(node, total);
        const hidden = (node.children?.length ?? 0) - children.length;
        const hasChildren = children.length > 0 && depth < MAX_DEPTH;
        const isOpen = expanded.has(key);
        const dim = !!highlight && node.resource !== highlight;
        const color = colorForResource(node.resource, node.resourceType);

        return (
            <div key={key}>
                <button
                    type="button"
                    onClick={() => hasChildren && toggle(key)}
                    className="flex w-full items-center gap-2 rounded-md py-1.5 pr-2 text-left transition hover:bg-muted"
                    style={{paddingLeft: depth * 16 + 8, opacity: dim ? 0.4 : 1, cursor: hasChildren ? "pointer" : "default"}}
                >
                    {hasChildren
                        ? (isOpen
                            ? <CaretDownIcon className="h-3 w-3 shrink-0 text-muted-foreground"/>
                            : <CaretRightIcon className="h-3 w-3 shrink-0 text-muted-foreground"/>)
                        : <span className="w-3 shrink-0"/>}
                    <span className="h-2 w-2 shrink-0 rounded-full" style={{backgroundColor: color}}/>
                    <span className="min-w-0 flex-1 truncate font-mono text-xs">{labelFor(node)}</span>
                    <div className="relative hidden h-1.5 w-20 shrink-0 overflow-hidden rounded-full bg-muted sm:block">
                        <div className="absolute inset-y-0 left-0 rounded-full"
                             style={{width: `${Math.min(100, pct)}%`, backgroundColor: color}}/>
                    </div>
                    <span className="w-12 shrink-0 text-right text-xs font-semibold tabular-nums">{pct.toFixed(1)}%</span>
                </button>
                {isOpen && children.map((child, index) => renderRow(child, `${key}/${index}`, depth + 1))}
                {isOpen && hidden > 0 && (
                    <div className="py-1 text-[11px] text-muted-foreground" style={{paddingLeft: (depth + 1) * 16 + 28}}>
                        {t("profiling.tree.more", {count: hidden})}
                    </div>
                )}
            </div>
        );
    };

    const roots = visibleChildren(root, total);
    return <div className="min-w-0">{roots.map((node, index) => renderRow(node, `${index}`, 0))}</div>;
};

export default CallTreeView;
