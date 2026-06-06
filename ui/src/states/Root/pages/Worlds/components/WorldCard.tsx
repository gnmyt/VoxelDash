import { 
    UsersIcon, 
    ClockIcon, 
    CloudIcon, 
    SunIcon,
    MoonIcon,
    CloudRainIcon,
    CloudLightningIcon,
    FloppyDiskIcon,
    SkullIcon,
    ShieldIcon,
    TrashIcon
} from "@phosphor-icons/react";
import { Button } from "@/components/ui/button";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { World } from "@/types/world";
import { t } from "i18next";
import { ReactNode } from "react";

interface WorldCardProps {
    world: World;
    onSetTime: (worldName: string, time: string) => void;
    onSetWeather: (worldName: string, weather: string) => void;
    onSetDifficulty: (worldName: string, difficulty: string) => void;
    onSave: (worldName: string) => void;
    onDelete: (worldName: string) => void;
    isMainWorld: boolean;
}

const ENVIRONMENT_IMAGES: Record<string, string> = {
    NORMAL: "/assets/images/overworld.webp",
    NETHER: "/assets/images/nether.webp",
    THE_END: "/assets/images/end.webp",
};

const ENVIRONMENT_BADGE: Record<string, string> = {
    NORMAL: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400",
    NETHER: "bg-orange-500/10 text-orange-600 dark:text-orange-400",
    THE_END: "bg-violet-500/10 text-violet-600 dark:text-violet-400",
};

const getTimeOfDay = (time: number): string => {
    const normalizedTime = time % 24000;
    if (normalizedTime >= 0 && normalizedTime < 6000) return "morning";
    if (normalizedTime >= 6000 && normalizedTime < 12000) return "day";
    if (normalizedTime >= 12000 && normalizedTime < 18000) return "evening";
    return "night";
};

const getWeatherIcon = (weather: string): ReactNode => {
    switch (weather.toLowerCase()) {
        case "rain": return <CloudRainIcon className="h-3.5 w-3.5" />;
        case "thunder": return <CloudLightningIcon className="h-3.5 w-3.5" />;
        default: return <SunIcon className="h-3.5 w-3.5" />;
    }
};

const WorldCard = ({ 
    world, 
    onSetTime, 
    onSetWeather, 
    onSetDifficulty, 
    onSave,
    onDelete,
    isMainWorld,
}: WorldCardProps) => {
    const timeOfDay = getTimeOfDay(world.time);
    const envImage = ENVIRONMENT_IMAGES[world.environment] || ENVIRONMENT_IMAGES.NORMAL;
    const envBadge = ENVIRONMENT_BADGE[world.environment] || ENVIRONMENT_BADGE.NORMAL;

    return (
        <div className="p-4 rounded-xl border bg-card hover:bg-accent/50 transition-colors">
            <div className="flex items-center gap-3 mb-3">
                <img 
                    src={envImage} 
                    alt={world.environment} 
                    className="h-10 w-10 object-contain shrink-0"
                />
                <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                        <h3 className="font-semibold truncate">{world.name}</h3>
                        {world.hardcore && (
                            <span title={t("worlds.hardcore")}>
                                <SkullIcon className="h-4 w-4 text-red-500 shrink-0" weight="fill" />
                            </span>
                        )}
                    </div>
                    <span className={`inline-flex items-center text-xs px-1.5 py-0.5 rounded-md ${envBadge}`}>
                        {t(`worlds.environment.${world.environment.toLowerCase()}`)}
                    </span>
                </div>
                <Button 
                    variant="ghost" 
                    size="icon"
                    className="h-8 w-8 shrink-0"
                    onClick={() => onSave(world.name)}
                    title={t("worlds.save")}
                >
                    <FloppyDiskIcon className="h-4 w-4" />
                </Button>
                {!isMainWorld && (
                    <AlertDialog>
                        <AlertDialogTrigger asChild>
                            <Button 
                                variant="ghost" 
                                size="icon"
                                className="h-8 w-8 shrink-0 text-destructive hover:text-destructive"
                                title={t("action.delete")}
                            >
                                <TrashIcon className="h-4 w-4" />
                            </Button>
                        </AlertDialogTrigger>
                        <AlertDialogContent className="rounded-xl">
                            <AlertDialogHeader>
                                <AlertDialogTitle>{t("worlds.delete.title")}</AlertDialogTitle>
                                <AlertDialogDescription>
                                    {t("worlds.delete.description", { name: world.name })}
                                </AlertDialogDescription>
                            </AlertDialogHeader>
                            <AlertDialogFooter>
                                <AlertDialogCancel className="rounded-xl">{t("action.cancel")}</AlertDialogCancel>
                                <AlertDialogAction 
                                    onClick={() => onDelete(world.name)} 
                                    className="rounded-xl bg-destructive text-destructive-foreground hover:bg-destructive/90"
                                >
                                    {t("action.delete")}
                                </AlertDialogAction>
                            </AlertDialogFooter>
                        </AlertDialogContent>
                    </AlertDialog>
                )}
            </div>

            <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5 text-sm text-muted-foreground mb-3">
                <div className="flex items-center gap-1.5">
                    <UsersIcon className="h-3.5 w-3.5" />
                    <span>{world.playerCount}</span>
                </div>
                <div className="flex items-center gap-1.5">
                    <ClockIcon className="h-3.5 w-3.5" />
                    <span className="capitalize">{t(`worlds.time_of_day.${timeOfDay}`)}</span>
                </div>
                <div className="flex items-center gap-1.5">
                    {getWeatherIcon(world.weather)}
                    <span className="capitalize">{t(`worlds.weather.${world.weather.toLowerCase()}`)}</span>
                </div>
                <div className="flex items-center gap-1.5">
                    <ShieldIcon className="h-3.5 w-3.5" />
                    <span className="capitalize">{t(`worlds.difficulty.${world.difficulty.toLowerCase()}`)}</span>
                </div>
            </div>

            <div className="flex flex-wrap items-center gap-1.5">
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button variant="outline" size="sm" className="h-8 px-2.5 rounded-lg text-xs">
                            <SunIcon className="h-3.5 w-3.5 mr-1" />
                            {t("worlds.set_time")}
                        </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="start" className="rounded-xl">
                        <DropdownMenuItem onClick={() => onSetTime(world.name, "day")} className="rounded-lg">
                            <SunIcon className="h-4 w-4 mr-2" />
                            {t("worlds.time_options.day")}
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => onSetTime(world.name, "noon")} className="rounded-lg">
                            <SunIcon className="h-4 w-4 mr-2" weight="fill" />
                            {t("worlds.time_options.noon")}
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => onSetTime(world.name, "sunset")} className="rounded-lg">
                            <SunIcon className="h-4 w-4 mr-2" />
                            {t("worlds.time_options.sunset")}
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => onSetTime(world.name, "night")} className="rounded-lg">
                            <MoonIcon className="h-4 w-4 mr-2" />
                            {t("worlds.time_options.night")}
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => onSetTime(world.name, "midnight")} className="rounded-lg">
                            <MoonIcon className="h-4 w-4 mr-2" weight="fill" />
                            {t("worlds.time_options.midnight")}
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button variant="outline" size="sm" className="h-8 px-2.5 rounded-lg text-xs">
                            <CloudIcon className="h-3.5 w-3.5 mr-1" />
                            {t("worlds.set_weather")}
                        </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="start" className="rounded-xl">
                        <DropdownMenuItem onClick={() => onSetWeather(world.name, "clear")} className="rounded-lg">
                            <SunIcon className="h-4 w-4 mr-2" />
                            {t("worlds.weather.clear")}
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => onSetWeather(world.name, "rain")} className="rounded-lg">
                            <CloudRainIcon className="h-4 w-4 mr-2" />
                            {t("worlds.weather.rain")}
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => onSetWeather(world.name, "thunder")} className="rounded-lg">
                            <CloudLightningIcon className="h-4 w-4 mr-2" />
                            {t("worlds.weather.thunder")}
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button variant="outline" size="sm" className="h-8 px-2.5 rounded-lg text-xs">
                            <ShieldIcon className="h-3.5 w-3.5 mr-1" />
                            {t("worlds.set_difficulty")}
                        </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="start" className="rounded-xl">
                        <DropdownMenuItem onClick={() => onSetDifficulty(world.name, "peaceful")} className="rounded-lg">
                            {t("worlds.difficulty.peaceful")}
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => onSetDifficulty(world.name, "easy")} className="rounded-lg">
                            {t("worlds.difficulty.easy")}
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => onSetDifficulty(world.name, "normal")} className="rounded-lg">
                            {t("worlds.difficulty.normal")}
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => onSetDifficulty(world.name, "hard")} className="rounded-lg">
                            {t("worlds.difficulty.hard")}
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>
        </div>
    );
};

export default WorldCard;
