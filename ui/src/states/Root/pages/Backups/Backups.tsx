import * as React from "react";
import {SpinnerGapIcon, ArchiveIcon} from "@phosphor-icons/react";
import {Backup, BackupOption} from "@/types/backup.ts";
import BackupCard from "@/states/Root/pages/Backups/components/BackupCard.tsx";
import CreateBackupDialog from "@/states/Root/pages/Backups/components/CreateBackupDialog.tsx";
import {deleteRequest, downloadRequest, jsonRequest, postRequest} from "@/lib/RequestUtil.ts";
import {useEffect} from "react";
import {toast} from "@/hooks/use-toast.ts";
import {t} from "i18next";
import {ScrollArea} from "@/components/ui/scroll-area.tsx";

const Backups = () => {
    const [backups, setBackups] = React.useState<Backup[]>([]);
    const [options, setOptions] = React.useState<BackupOption[]>([]);
    const [isCreating, setIsCreating] = React.useState(false);

    const handleCreateBackup = async (backupMode: number, name: string) => {
        setIsCreating(true);

        await postRequest("backups/create", {backupMode, backupName: name});

        await fetchBackups();
        setIsCreating(false);

        toast({description: t("backup.created")});
    }

    const handleRestore = async (id: number, haltAfterRestore: boolean) => {
        await postRequest(`backups/restore`, {backupName: id, haltAfterRestore});

        toast({description: t("backup.restored")});
    }

    const handleDelete = (id: number) => deleteRequest(`backups/${id}`).then(async () => {
        await fetchBackups();

        toast({description: t("backup.delete.success")});
    });

    const handleDownload = (id: number) => downloadRequest(`backups/download/${id}`)

    const fetchBackups = async () => jsonRequest("backups/list").then((data) => {
        setBackups(data.backups.sort((a: { id: number }, b: { id: number }) => b.id - a.id));
    });

    const fetchOptions = async () => jsonRequest("backups/options").then((data) => {
        setOptions(data.options ?? []);
    });

    useEffect(() => {
        fetchBackups();
        fetchOptions();
    }, []);

    return (
        <div className="flex flex-col p-4 md:p-6 pt-0 gap-6" style={{ height: 'calc(var(--app-vh) - 5.5rem)' }}>
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 p-4 rounded-xl border bg-card shrink-0">
                <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center">
                        <ArchiveIcon className="h-6 w-6 text-primary" weight="fill"/>
                    </div>
                    <div>
                        <h1 className="text-lg font-semibold">{t("backup.title")}</h1>
                        <p className="text-sm text-muted-foreground">{t("backup.subtitle")}</p>
                    </div>
                </div>
                <CreateBackupDialog options={options} onBackup={handleCreateBackup} disabled={isCreating}/>
            </div>

            {isCreating && (
                <div className="flex items-center gap-3 p-4 rounded-xl border bg-card shrink-0">
                    <div className="h-10 w-10 rounded-xl bg-primary/10 flex items-center justify-center">
                        <SpinnerGapIcon className="h-5 w-5 text-primary animate-spin"/>
                    </div>
                    <span className="text-base font-medium">{t("backup.creating")}</span>
                </div>
            )}

            <div className="flex-1 min-h-0">
                <ScrollArea className="h-full">
                    <div className="space-y-3">
                        {backups.length === 0 && (
                            <div className="flex flex-col items-center justify-center py-16 text-center">
                                <div className="h-16 w-16 rounded-2xl bg-muted flex items-center justify-center mb-4">
                                    <ArchiveIcon className="h-8 w-8 text-muted-foreground"/>
                                </div>
                                <p className="text-lg font-medium text-muted-foreground">{t("backup.none_found")}</p>
                                <p className="text-sm text-muted-foreground mt-1">{t("backup.none_found_hint")}</p>
                            </div>
                        )}
                        {backups.map((backup) => (
                            <BackupCard key={backup.id} backup={backup} onRestore={handleRestore}
                                        onDelete={handleDelete} onDownload={handleDownload}/>
                        ))}
                    </div>
                </ScrollArea>
            </div>
        </div>
    );
}

export default Backups;