import * as React from "react";
import { t } from "i18next";
import { ScheduleAction, ScheduleTask } from "@/types/schedule";
import { Button } from "@/components/ui/button";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import { jsonRequest, postRequest, putRequest } from "@/lib/RequestUtil";
import { toast } from "@/hooks/use-toast";
import { BackupOption } from "@/types/backup";
import BackupPartsSelector, { fromBackupBit, toBackupBit } from "@/states/Root/pages/Backups/components/BackupPartsSelector";

interface TaskFormDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    scheduleId: number;
    actions: ScheduleAction[];
    onSuccess: () => Promise<void>;
    task?: ScheduleTask;
}

const TaskFormDialog = ({ open, onOpenChange, scheduleId, actions, onSuccess, task }: TaskFormDialogProps) => {
    const isEdit = !!task;
    
    const [selectedActionId, setSelectedActionId] = React.useState<string>(task?.actionId ?? "");
    const [metadata, setMetadata] = React.useState(task?.metadata ?? "");
    const [isSubmitting, setIsSubmitting] = React.useState(false);

    const [backupOptions, setBackupOptions] = React.useState<BackupOption[]>([]);
    const [backupSelected, setBackupSelected] = React.useState<number[]>([]);
    const [backupName, setBackupName] = React.useState("");

    const selectedAction = actions.find(a => a.id === selectedActionId);
    const isBackup = selectedActionId === "backup";

    const applyBackupMetadata = (raw: string) => {
        const [bitPart, ...nameParts] = (raw || "").split("|");
        const bit = parseInt(bitPart, 10);
        setBackupSelected(isNaN(bit) ? [] : fromBackupBit(bit));
        setBackupName(nameParts.join("|"));
    };

    React.useEffect(() => {
        if (task) {
            setSelectedActionId(task.actionId);
            setMetadata(task.metadata || "");
            if (task.actionId === "backup") applyBackupMetadata(task.metadata || "");
        } else if (actions.length > 0 && !selectedActionId) {
            setSelectedActionId(actions[0].id);
            setMetadata("");
        }
    }, [task, actions, open]);

    React.useEffect(() => {
        if (isBackup && backupOptions.length === 0) {
            jsonRequest("backups/options").then((data) => setBackupOptions(data.options ?? []));
        }
    }, [isBackup]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsSubmitting(true);
        
        try {
            let finalMetadata = selectedAction?.inputType !== "NONE" ? metadata : "";
            if (isBackup) {
                const trimmedName = backupName.trim();
                finalMetadata = trimmedName ? `${toBackupBit(backupSelected)}|${trimmedName}` : `${toBackupBit(backupSelected)}`;
            }

            const data = {
                actionId: selectedActionId,
                metadata: finalMetadata,
                ...(isEdit && task ? { executionOrder: task.executionOrder } : {})
            };
            
            if (isEdit && task) {
                await putRequest(`schedules/${scheduleId}/tasks/${task.id}`, data);
                toast({ description: t("schedules.task_updated") });
            } else {
                await postRequest(`schedules/${scheduleId}/tasks`, data);
                toast({ description: t("schedules.task_created") });
            }
            
            await onSuccess();
            onOpenChange(false);

            if (!isEdit && actions.length > 0) {
                setSelectedActionId(actions[0].id);
                setMetadata("");
                setBackupSelected([]);
                setBackupName("");
            }
        } catch {
            toast({ 
                description: t(isEdit ? "schedules.error.update_task" : "schedules.error.create_task"), 
                variant: "destructive" 
            });
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleActionChange = (value: string) => {
        setSelectedActionId(value);
        const newAction = actions.find(a => a.id === value);
        if (newAction?.inputType === "NONE") {
            setMetadata("");
        }
        if (value === "backup") {
            setBackupSelected([]);
            setBackupName("");
        }
    };

    const needsInput = selectedAction && selectedAction.inputType !== "NONE" && !isBackup;
    const isTextarea = selectedAction?.inputType === "TEXTAREA";
    const isNumber = selectedAction?.inputType === "NUMBER";

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[425px] rounded-xl">
                <form onSubmit={handleSubmit}>
                    <DialogHeader>
                        <DialogTitle className="text-lg">
                            {isEdit ? t("schedules.edit_task") : t("schedules.create_task")}
                        </DialogTitle>
                        <DialogDescription>
                            {isEdit ? t("schedules.edit_task_description") : t("schedules.create_task_description")}
                        </DialogDescription>
                    </DialogHeader>
                    <div className="grid gap-4 py-6">
                        <div className="grid gap-2">
                            <Label>{t("schedules.task.type")}</Label>
                            <Select value={selectedActionId} onValueChange={handleActionChange}>
                                <SelectTrigger className="rounded-xl">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {actions.map((action) => (
                                        <SelectItem key={action.id} value={action.id}>
                                            {t(action.translationKey)}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                        
                        {isBackup && (
                            <>
                                <div className="grid gap-2">
                                    <Label>{t("backup.name_label")}</Label>
                                    <Input
                                        value={backupName}
                                        onChange={(e) => setBackupName(e.target.value)}
                                        placeholder={t("backup.name_placeholder")}
                                        className="rounded-xl"
                                    />
                                    <p className="text-xs text-muted-foreground">{t("backup.name_hint")}</p>
                                </div>
                                <BackupPartsSelector
                                    options={backupOptions}
                                    selected={backupSelected}
                                    onSelectedChange={setBackupSelected}
                                />
                            </>
                        )}

                        {needsInput && selectedAction.inputTranslationKey && (
                            <div className="grid gap-2">
                                <Label>{t(selectedAction.inputTranslationKey)}</Label>
                                {isTextarea ? (
                                    <Textarea
                                        value={metadata}
                                        onChange={(e) => setMetadata(e.target.value)}
                                        placeholder={t(selectedAction.inputTranslationKey)}
                                        className="rounded-xl resize-none"
                                        rows={3}
                                    />
                                ) : (
                                    <Input
                                        type={isNumber ? "number" : "text"}
                                        value={metadata}
                                        onChange={(e) => setMetadata(e.target.value)}
                                        placeholder={t(selectedAction.inputTranslationKey)}
                                        className="rounded-xl"
                                    />
                                )}
                            </div>
                        )}
                    </div>
                    <DialogFooter>
                        <Button 
                            type="submit" 
                            disabled={isSubmitting || !selectedActionId || (isBackup && backupSelected.length === 0)}
                            size="lg" 
                            className="w-full h-12 rounded-xl text-base"
                        >
                            {isEdit ? t("action.save") : t("action.create")}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
};

export default TaskFormDialog;
