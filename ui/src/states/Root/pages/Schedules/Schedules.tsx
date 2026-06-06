import { useEffect, useState } from "react";
import { CalendarIcon, PlusIcon } from "@phosphor-icons/react";
import { t } from "i18next";
import { jsonRequest, deleteRequest, postRequest, putRequest } from "@/lib/RequestUtil";
import { Schedule, ScheduleAction, CreateScheduleRequest } from "@/types/schedule";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Button } from "@/components/ui/button";
import ScheduleCard from "./components/ScheduleCard";
import ScheduleFormDialog from "./components/ScheduleFormDialog";
import { toast } from "@/hooks/use-toast";

const Schedules = () => {
    const [schedules, setSchedules] = useState<Schedule[]>([]);
    const [actions, setActions] = useState<ScheduleAction[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [createDialogOpen, setCreateDialogOpen] = useState(false);

    const fetchSchedules = async () => {
        try {
            const data = await jsonRequest("schedules");
            setSchedules(data.schedules || []);
        } catch (error) {
            toast({ description: t("schedules.error.fetch"), variant: "destructive" });
        } finally {
            setIsLoading(false);
        }
    };

    const fetchActions = async () => {
        try {
            const data = await jsonRequest("schedules/actions");
            setActions(data.actions || []);
        } catch (error) {
            toast({ description: t("schedules.error.fetch_actions"), variant: "destructive" });
        }
    };

    const handleCreateSchedule = async (data: CreateScheduleRequest) => {
        try {
            await postRequest("schedules", data);
            await fetchSchedules();
            setCreateDialogOpen(false);
            toast({ description: t("schedules.created") });
        } catch (error) {
            toast({ description: t("schedules.error.create"), variant: "destructive" });
        }
    };

    const handleUpdateSchedule = async (id: number, data: CreateScheduleRequest) => {
        try {
            await putRequest(`schedules/${id}`, data);
            await fetchSchedules();
            toast({ description: t("schedules.updated") });
        } catch (error) {
            toast({ description: t("schedules.error.update"), variant: "destructive" });
        }
    };

    const handleDeleteSchedule = async (id: number) => {
        try {
            await deleteRequest(`schedules/${id}`);
            await fetchSchedules();
            toast({ description: t("schedules.deleted") });
        } catch (error) {
            toast({ description: t("schedules.error.delete"), variant: "destructive" });
        }
    };

    const handleToggleSchedule = async (id: number, enabled: boolean) => {
        try {
            await postRequest(`schedules/${id}/toggle`, { enabled });
            await fetchSchedules();
            toast({ description: enabled ? t("schedules.enabled") : t("schedules.disabled") });
        } catch (error) {
            toast({ description: t("schedules.error.toggle"), variant: "destructive" });
        }
    };

    useEffect(() => {
        fetchSchedules();
        fetchActions();
    }, []);

    return (
        <div className="flex flex-col p-4 md:p-6 pt-0 gap-6" style={{ height: 'calc(var(--app-vh) - 5.5rem)' }}>
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 p-4 rounded-xl border bg-card shrink-0">
                <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center">
                        <CalendarIcon className="h-6 w-6 text-primary" weight="fill" />
                    </div>
                    <div>
                        <h1 className="text-lg font-semibold">{t("schedules.title")}</h1>
                        <p className="text-sm text-muted-foreground">{t("schedules.subtitle")}</p>
                    </div>
                </div>
                <Button
                    size="lg"
                    className="h-12 px-6 rounded-xl text-base w-full sm:w-auto"
                    onClick={() => setCreateDialogOpen(true)}
                >
                    <PlusIcon className="h-5 w-5 mr-2" weight="bold" />
                    {t("schedules.create")}
                </Button>
            </div>

            <div className="flex-1 min-h-0">
                <ScrollArea className="h-full">
                    {isLoading ? (
                        <div className="flex items-center justify-center py-16">
                            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
                        </div>
                    ) : schedules.length === 0 ? (
                        <div className="flex flex-col items-center justify-center py-16 text-center">
                            <div className="h-16 w-16 rounded-2xl bg-muted flex items-center justify-center mb-4">
                                <CalendarIcon className="h-8 w-8 text-muted-foreground" />
                            </div>
                            <p className="text-lg font-medium text-muted-foreground">{t("schedules.none_found")}</p>
                            <p className="text-sm text-muted-foreground mt-1">{t("schedules.none_found_hint")}</p>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                            {schedules.map((schedule) => (
                                <ScheduleCard
                                    key={schedule.id}
                                    schedule={schedule}
                                    actions={actions}
                                    onUpdate={handleUpdateSchedule}
                                    onDelete={handleDeleteSchedule}
                                    onToggle={handleToggleSchedule}
                                    onRefresh={fetchSchedules}
                                />
                            ))}
                        </div>
                    )}
                </ScrollArea>
            </div>

            <ScheduleFormDialog
                open={createDialogOpen}
                onOpenChange={setCreateDialogOpen}
                onSubmit={handleCreateSchedule}
            />
        </div>
    );
};

export default Schedules;
