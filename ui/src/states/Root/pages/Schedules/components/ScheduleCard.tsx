import { useState } from "react";
import { 
    PencilSimpleIcon, 
    TrashIcon, 
    CaretDownIcon,
    CaretUpIcon,
    PlusIcon,
    CheckCircleIcon,
    PauseCircleIcon
} from "@phosphor-icons/react";
import { t } from "i18next";
import { Schedule, ScheduleAction, CreateScheduleRequest } from "@/types/schedule";
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
    Collapsible,
    CollapsibleContent,
    CollapsibleTrigger,
} from "@/components/ui/collapsible";
import TaskList from "./TaskList";
import TaskFormDialog from "./TaskFormDialog";
import ScheduleFormDialog from "./ScheduleFormDialog";

const DAYS = [
    "schedules.days.sunday",
    "schedules.days.monday",
    "schedules.days.tuesday",
    "schedules.days.wednesday",
    "schedules.days.thursday",
    "schedules.days.friday",
    "schedules.days.saturday"
];

interface ScheduleCardProps {
    schedule: Schedule;
    actions: ScheduleAction[];
    onUpdate: (id: number, data: CreateScheduleRequest) => Promise<void>;
    onDelete: (id: number) => Promise<void>;
    onToggle: (id: number, enabled: boolean) => Promise<void>;
    onRefresh: () => Promise<void>;
}

const ScheduleCard = ({ schedule, actions, onUpdate, onDelete, onToggle, onRefresh }: ScheduleCardProps) => {
    const [isOpen, setIsOpen] = useState(false);
    const [isEditOpen, setIsEditOpen] = useState(false);
    const [isCreateTaskOpen, setIsCreateTaskOpen] = useState(false);

    const getIntervalDescription = () => {
        switch (schedule.interval) {
            case "HOURLY":
                return t("schedules.interval.hourly_at", { minute: schedule.intervalValue });
            case "DAILY":
                return t("schedules.interval.daily_at", { 
                    hour: schedule.intervalValue.toString().padStart(2, '0'), 
                    minute: schedule.timeValue.toString().padStart(2, '0') 
                });
            case "WEEKLY": {
                const hour = Math.floor(schedule.timeValue / 60);
                const minute = schedule.timeValue % 60;
                return t("schedules.interval.weekly_at", { 
                    day: t(DAYS[schedule.intervalValue]), 
                    hour: hour.toString().padStart(2, '0'),
                    minute: minute.toString().padStart(2, '0')
                });
            }
            default:
                return schedule.description;
        }
    };

    const handleToggle = (e: React.MouseEvent) => {
        e.stopPropagation();
        onToggle(schedule.id, !schedule.enabled);
    };

    return (
        <Collapsible open={isOpen} onOpenChange={setIsOpen}>
            <div className={`rounded-xl border bg-card transition-all duration-200 ${!schedule.enabled ? 'opacity-60 border-dashed' : ''}`}>
                <div className="flex items-center p-4 gap-4">
                    <button
                        onClick={handleToggle}
                        className="shrink-0 group relative"
                        title={schedule.enabled ? t("schedules.status.click_to_disable") : t("schedules.status.click_to_enable")}
                    >
                        <div className={`h-12 w-12 rounded-xl flex items-center justify-center transition-colors ${
                            schedule.enabled 
                                ? 'bg-green-500/10 text-green-500 group-hover:bg-green-500/20' 
                                : 'bg-muted text-muted-foreground group-hover:bg-muted/80'
                        }`}>
                            {schedule.enabled ? (
                                <CheckCircleIcon className="h-6 w-6" weight="fill" />
                            ) : (
                                <PauseCircleIcon className="h-6 w-6" weight="fill" />
                            )}
                        </div>
                    </button>

                    <CollapsibleTrigger asChild>
                        <div className="flex-1 min-w-0 cursor-pointer hover:bg-accent/30 rounded-lg p-2 -m-2 transition-colors">
                            <div className="flex items-center gap-2 mb-1">
                                <h3 className="text-base font-semibold truncate">{schedule.name}</h3>
                            </div>
                            <p className="text-sm text-muted-foreground">{getIntervalDescription()}</p>
                            {schedule.lastRun > 0 && (
                                <p className="text-xs text-muted-foreground mt-1">
                                    {t("schedules.last_run", { time: new Date(schedule.lastRun).toLocaleString(), interpolation: { escapeValue: false } })}
                                </p>
                            )}
                        </div>
                    </CollapsibleTrigger>

                    <CollapsibleTrigger asChild>
                        <Button variant="ghost" size="icon" className="shrink-0 rounded-xl">
                            {isOpen ? (
                                <CaretUpIcon className="h-5 w-5 text-muted-foreground" />
                            ) : (
                                <CaretDownIcon className="h-5 w-5 text-muted-foreground" />
                            )}
                        </Button>
                    </CollapsibleTrigger>
                </div>

                <CollapsibleContent>
                    <div className="border-t px-4 py-3">
                        <div className="flex flex-wrap items-center justify-between gap-2 mb-4">
                            <span className="text-sm text-muted-foreground">
                                {t("schedules.tasks_count", { count: schedule.tasks.length })}
                            </span>
                            <div className="flex flex-wrap items-center gap-2">
                                <Button 
                                    variant="outline" 
                                    size="sm" 
                                    className="rounded-xl"
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        setIsEditOpen(true);
                                    }}
                                >
                                    <PencilSimpleIcon className="h-4 w-4 mr-1" />
                                    {t("action.edit")}
                                </Button>
                                <AlertDialog>
                                    <AlertDialogTrigger asChild>
                                        <Button 
                                            variant="destructive" 
                                            size="sm" 
                                            className="rounded-xl"
                                            onClick={(e) => e.stopPropagation()}
                                        >
                                            <TrashIcon className="h-4 w-4 mr-1" />
                                            {t("action.delete")}
                                        </Button>
                                    </AlertDialogTrigger>
                                    <AlertDialogContent className="rounded-xl">
                                        <AlertDialogHeader>
                                            <AlertDialogTitle>{t("schedules.delete.title")}</AlertDialogTitle>
                                            <AlertDialogDescription>
                                                {t("schedules.delete.description")}
                                            </AlertDialogDescription>
                                        </AlertDialogHeader>
                                        <AlertDialogFooter>
                                            <AlertDialogCancel className="rounded-xl">{t("action.cancel")}</AlertDialogCancel>
                                            <AlertDialogAction 
                                                className="rounded-xl" 
                                                onClick={() => onDelete(schedule.id)}
                                            >
                                                {t("action.delete")}
                                            </AlertDialogAction>
                                        </AlertDialogFooter>
                                    </AlertDialogContent>
                                </AlertDialog>
                                <Button 
                                    variant="outline" 
                                    size="sm" 
                                    className="rounded-xl"
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        setIsCreateTaskOpen(true);
                                    }}
                                >
                                    <PlusIcon className="h-4 w-4 mr-1" />
                                    {t("schedules.create_task")}
                                </Button>
                            </div>
                        </div>

                        <TaskList 
                            tasks={schedule.tasks} 
                            scheduleId={schedule.id}
                            actions={actions}
                            onRefresh={onRefresh}
                        />
                    </div>
                </CollapsibleContent>
            </div>

            <ScheduleFormDialog
                open={isEditOpen}
                onOpenChange={setIsEditOpen}
                schedule={schedule}
                onSubmit={(data: CreateScheduleRequest) => onUpdate(schedule.id, data)}
            />

            <TaskFormDialog
                open={isCreateTaskOpen}
                onOpenChange={setIsCreateTaskOpen}
                scheduleId={schedule.id}
                actions={actions}
                onSuccess={onRefresh}
            />
        </Collapsible>
    );
};

export default ScheduleCard;
