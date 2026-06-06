import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import GridLayout from "react-grid-layout";
import { jsonRequest } from "@/lib/RequestUtil";
import { Widget, SavedLayout } from "@/types/widget";
import WidgetCard from "./components/WidgetCard";
import { useResizeObserver } from "@/hooks/useResizeObserver";
import { useIsMobile } from "@/hooks/use-mobile";
import { DotsThreeIcon, ArrowsClockwiseIcon, GridFourIcon } from "@phosphor-icons/react";
import { Button } from "@/components/ui/button";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { t } from "i18next";
import "react-grid-layout/css/styles.css";

interface LayoutItem {
    i: string;
    x: number;
    y: number;
    w: number;
    h: number;
    minW?: number;
    minH?: number;
}

const LAYOUT_STORAGE_KEY = "voxeldash-overview-layout";
const LAYOUT_VERSION = 1;
const GRID_COLS = 6;
const ROW_HEIGHT = 80;

const Overview = () => {
    const [widgets, setWidgets] = useState<Widget[]>([]);
    const [layout, setLayout] = useState<LayoutItem[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [containerRef, containerSize] = useResizeObserver<HTMLDivElement>();
    const isMobile = useIsMobile();

    const fetchWidgets = useCallback(async () => {
        try {
            const response = await jsonRequest("widgets/data");
            if (response.widgets) {
                setWidgets(response.widgets);
            }
        } catch (error) {
            console.error("Failed to fetch widgets:", error);
        }
    }, []);

    const loadSavedLayout = useCallback((widgetList: Widget[]): LayoutItem[] => {
        try {
            const saved = localStorage.getItem(LAYOUT_STORAGE_KEY);
            if (saved) {
                const parsed: SavedLayout = JSON.parse(saved);
                if (parsed.version === LAYOUT_VERSION && parsed.layouts) {
                    const validIds = new Set(widgetList.map(w => w.id));
                    return parsed.layouts.filter(l => validIds.has(l.i));
                }
            }
        } catch (error) {
            console.error("Failed to load saved layout:", error);
        }
        return [];
    }, []);

    const generateDefaultLayout = useCallback((widget: Widget, index: number, existingLayouts: LayoutItem[]): LayoutItem => {
        let x = (index * widget.defaultSize.width) % GRID_COLS;
        let y = 0;

        if (existingLayouts.length > 0) {
            const maxY = Math.max(...existingLayouts.map(l => l.y + l.h));
            if (x + widget.defaultSize.width > GRID_COLS) {
                x = 0;
                y = maxY;
            } else {
                const lastLayout = existingLayouts[existingLayouts.length - 1];
                if (lastLayout) {
                    y = lastLayout.y;
                    x = lastLayout.x + lastLayout.w;
                    if (x + widget.defaultSize.width > GRID_COLS) {
                        x = 0;
                        y = maxY;
                    }
                }
            }
        }

        return {
            i: widget.id,
            x,
            y,
            w: widget.defaultSize.width,
            h: widget.defaultSize.height,
            minW: widget.defaultSize.minWidth,
            minH: widget.defaultSize.minHeight,
        };
    }, []);

    const widgetIds = useMemo(() => widgets.map(w => w.id).sort().join(','), [widgets]);
    const layoutInitialized = useRef(false);
    const prevWidgetIds = useRef<string>('');

    useEffect(() => {
        if (widgets.length === 0) return;
        
        const currentWidgetIds = widgets.map(w => w.id).sort().join(',');
        if (prevWidgetIds.current === currentWidgetIds && layoutInitialized.current) {
            return;
        }
        prevWidgetIds.current = currentWidgetIds;

        setLayout(currentLayout => {
            if (layoutInitialized.current && currentLayout.length > 0) {
                const currentIds = new Set(currentLayout.map(l => l.i));
                const newWidgets = widgets.filter(w => !currentIds.has(w.id));
                
                if (newWidgets.length === 0) {
                    return currentLayout;
                }
                
                const newLayouts = [...currentLayout];
                newWidgets.forEach((widget) => {
                    newLayouts.push(generateDefaultLayout(widget, widgets.indexOf(widget), newLayouts));
                });
                return newLayouts;
            }

            const savedLayouts = loadSavedLayout(widgets);
            const savedIds = new Set(savedLayouts.map(l => l.i));
            
            const newLayouts: LayoutItem[] = [...savedLayouts];
            widgets.forEach((widget, index) => {
                if (!savedIds.has(widget.id)) {
                    newLayouts.push(generateDefaultLayout(widget, index, newLayouts));
                }
            });

            layoutInitialized.current = true;
            return newLayouts;
        });
    }, [widgets, widgetIds, loadSavedLayout, generateDefaultLayout]);

    useEffect(() => {
        const loadData = async () => {
            setIsLoading(true);
            await fetchWidgets();
            setIsLoading(false);
        };
        loadData();

        const interval = setInterval(fetchWidgets, 10000);
        return () => clearInterval(interval);
    }, [fetchWidgets]);

    const saveLayout = useCallback((newLayout: LayoutItem[]) => {
        const layoutData: SavedLayout = {
            layouts: newLayout.map(l => ({
                i: l.i,
                x: l.x,
                y: l.y,
                w: l.w,
                h: l.h,
                minW: l.minW,
                minH: l.minH,
            })),
            version: LAYOUT_VERSION,
        };
        localStorage.setItem(LAYOUT_STORAGE_KEY, JSON.stringify(layoutData));
    }, []);

    const handleLayoutChange = useCallback((newLayout: readonly LayoutItem[]) => {
        setLayout([...newLayout]);
        saveLayout([...newLayout]);
    }, [saveLayout]);

    const resetLayout = useCallback(() => {
        localStorage.removeItem(LAYOUT_STORAGE_KEY);
        layoutInitialized.current = false;
        prevWidgetIds.current = '';
        const defaultLayouts: LayoutItem[] = [];
        widgets.forEach((widget, index) => {
            defaultLayouts.push(generateDefaultLayout(widget, index, defaultLayouts));
        });
        setLayout(defaultLayouts);
        saveLayout(defaultLayouts);
        layoutInitialized.current = true;
        prevWidgetIds.current = widgets.map(w => w.id).sort().join(',');
    }, [widgets, generateDefaultLayout, saveLayout]);

    const gridWidth = containerSize.width || 800;

    if (isLoading && widgets.length === 0) {
        return (
            <div className="flex flex-col flex-1 p-4 md:p-6 pt-0">
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    {[1, 2, 3, 4, 5, 6].map((i) => (
                        <div key={i} className="aspect-video rounded-xl bg-muted/50 animate-pulse" />
                    ))}
                </div>
            </div>
        );
    }

    return (
        <div className="flex flex-col flex-1 p-4 md:p-6 pt-0 min-h-0 overflow-hidden">
            <div className="flex items-center justify-end mb-4 shrink-0">
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="h-8 w-8 rounded-lg">
                            <DotsThreeIcon className="h-5 w-5" weight="bold" />
                        </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" className="min-w-[160px]">
                        <DropdownMenuItem onClick={resetLayout} className="gap-2">
                            <ArrowsClockwiseIcon className="h-4 w-4" />
                            {t("overview.reset_layout")}
                        </DropdownMenuItem>
                        <DropdownMenuItem className="gap-2" disabled>
                            <GridFourIcon className="h-4 w-4" />
                            {t("overview.subtitle", { count: widgets.length })}
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            <div ref={containerRef} className="flex-1 min-h-0 overflow-auto">
                {isMobile ? (
                    <div className="flex flex-col gap-4">
                        {widgets.map((widget) => {
                            const item = layout.find((l) => l.i === widget.id);
                            const height = item ? item.h * ROW_HEIGHT + (item.h - 1) * 16 : 160;
                            return (
                                <div key={widget.id} style={{ height }}>
                                    <WidgetCard widget={widget} />
                                </div>
                            );
                        })}
                    </div>
                ) : (
                    layout.length > 0 && (
                        <GridLayout
                            className="layout"
                            layout={layout}
                            width={gridWidth}
                            gridConfig={{
                                cols: GRID_COLS,
                                rowHeight: ROW_HEIGHT,
                                margin: [16, 16],
                                containerPadding: [0, 0],
                            }}
                            dragConfig={{
                                enabled: true,
                                handle: ".drag-handle",
                            }}
                            resizeConfig={{
                                enabled: true,
                            }}
                            onLayoutChange={handleLayoutChange}
                        >
                            {widgets.map((widget) => (
                                <div key={widget.id}>
                                    <WidgetCard widget={widget} />
                                </div>
                            ))}
                        </GridLayout>
                    )
                )}
            </div>
        </div>
    );
};

export default Overview;