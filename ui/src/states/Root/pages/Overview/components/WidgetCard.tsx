import { Card, CardContent } from "@/components/ui/card";
import { Widget } from "@/types/widget";
import { t } from "i18next";
import AreaChartWidget from "./AreaChartWidget";
import LineChartWidget from "./LineChartWidget";
import BarChartWidget from "./BarChartWidget";
import StatCardWidget from "./StatCardWidget";
import ProgressWidget from "./ProgressWidget";
import InfoCardWidget from "./InfoCardWidget";
import { 
    DotsSixVerticalIcon, 
    UsersIcon, 
    ClockIcon, 
    GaugeIcon, 
    MemoryIcon, 
    HardDrivesIcon,
    CpuIcon,
    GlobeIcon,
    InfoIcon,
    ChartLineUpIcon,
    GameControllerIcon,
    type Icon
} from "@phosphor-icons/react";

interface WidgetCardProps {
    widget: Widget;
}

const cardInteract = "transition-[transform,box-shadow,border-color] duration-200 ease-out hover:-translate-y-0.5 hover:border-border/80 hover:shadow-lg motion-reduce:transition-none motion-reduce:hover:translate-y-0 animate-in fade-in-0 zoom-in-95";

const getWidgetIcon = (widgetId: string): Icon => {
    const iconMap: Record<string, Icon> = {
        'online_players': UsersIcon,
        'player_count': UsersIcon,
        'player_history': UsersIcon,
        'uptime': ClockIcon,
        'tps': GaugeIcon,
        'tps_stat': GaugeIcon,
        'memory': MemoryIcon,
        'memory_stat': MemoryIcon,
        'memory_usage': MemoryIcon,
        'disk': HardDrivesIcon,
        'disk_usage': HardDrivesIcon,
        'cpu': CpuIcon,
        'cpu_usage': CpuIcon,
        'worlds': GlobeIcon,
        'world_info': GlobeIcon,
        'server_info': InfoIcon,
        'system_info': CpuIcon,
        'game_settings': GameControllerIcon,
    };
    return iconMap[widgetId] || ChartLineUpIcon;
};

const WidgetCard = ({ widget }: WidgetCardProps) => {
    const renderWidgetContent = () => {
        switch (widget.type) {
            case 'AREA_CHART':
                return <AreaChartWidget widget={widget} />;
            case 'LINE_CHART':
                return <LineChartWidget widget={widget} />;
            case 'BAR_CHART':
                return <BarChartWidget widget={widget} />;
            case 'STAT_CARD':
                return <StatCardWidget widget={widget} />;
            case 'PROGRESS':
                return <ProgressWidget widget={widget} />;
            case 'INFO_CARD':
                return <InfoCardWidget widget={widget} />;
            default:
                return <div className="text-muted-foreground text-center">Unknown widget type</div>;
        }
    };

    const isChartWidget = ['AREA_CHART', 'LINE_CHART', 'BAR_CHART'].includes(widget.type);
    const isStatCard = widget.type === 'STAT_CARD';
    const isInfoCard = widget.type === 'INFO_CARD';
    const isProgress = widget.type === 'PROGRESS';
    
    const WidgetIcon = getWidgetIcon(widget.id);

    if (isStatCard || isProgress) {
        return (
            <Card className={`h-full flex flex-col overflow-hidden group relative ${cardInteract}`}>
                <DotsSixVerticalIcon 
                    className="absolute top-2 right-2 h-4 w-4 text-muted-foreground/50 opacity-0 group-hover:opacity-100 transition-opacity cursor-grab drag-handle z-10" 
                    weight="bold"
                />
                <CardContent className="flex-1 flex flex-col justify-center p-4">
                    <div className="flex items-center gap-2 mb-2">
                        <div 
                            className="h-6 w-6 rounded-md flex items-center justify-center"
                            style={{ backgroundColor: `${widget.color}15` }}
                        >
                            <WidgetIcon 
                                className="h-3.5 w-3.5" 
                                weight="fill"
                                style={{ color: widget.color }}
                            />
                        </div>
                        <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
                            {t(widget.translationKey)}
                        </span>
                    </div>
                    {renderWidgetContent()}
                </CardContent>
            </Card>
        );
    }

    if (isInfoCard) {
        return (
            <Card className={`h-full flex flex-col overflow-hidden group relative ${cardInteract}`}>
                <DotsSixVerticalIcon 
                    className="absolute top-2 right-2 h-4 w-4 text-muted-foreground/50 opacity-0 group-hover:opacity-100 transition-opacity cursor-grab drag-handle z-10" 
                    weight="bold"
                />
                <CardContent className="flex-1 flex flex-col p-4">
                    <div className="flex items-center gap-2 mb-3">
                        <div 
                            className="h-6 w-6 rounded-md flex items-center justify-center"
                            style={{ backgroundColor: `${widget.color}15` }}
                        >
                            <WidgetIcon 
                                className="h-3.5 w-3.5" 
                                weight="fill"
                                style={{ color: widget.color }}
                            />
                        </div>
                        <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
                            {t(widget.translationKey)}
                        </span>
                    </div>
                    {renderWidgetContent()}
                </CardContent>
            </Card>
        );
    }

    return (
        <Card className={`h-full flex flex-col overflow-hidden ${cardInteract}`}>
            <div className="flex items-center justify-between py-3 px-4 flex-shrink-0">
                <div className="flex items-center gap-2">
                    <div 
                        className="h-6 w-6 rounded-md flex items-center justify-center"
                        style={{ backgroundColor: `${widget.color}15` }}
                    >
                        <WidgetIcon 
                            className="h-3.5 w-3.5" 
                            weight="fill"
                            style={{ color: widget.color }}
                        />
                    </div>
                    <span className="text-sm font-medium">
                        {t(widget.translationKey)}
                    </span>
                </div>
                <DotsSixVerticalIcon 
                    className="h-4 w-4 text-muted-foreground cursor-grab drag-handle" 
                    weight="bold"
                />
            </div>
            <CardContent className={`flex-1 min-h-0 ${isChartWidget ? 'pb-2 px-2' : 'pb-4 px-4'}`}>
                {renderWidgetContent()}
            </CardContent>
        </Card>
    );
};

export default WidgetCard;
