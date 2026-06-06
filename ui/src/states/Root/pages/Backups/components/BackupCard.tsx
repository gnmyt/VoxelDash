import {ArchiveIcon, DownloadSimpleIcon, GearIcon, FileTextIcon, ArrowCounterClockwiseIcon, TrashIcon, CalendarIcon, PlanetIcon, PuzzlePieceIcon} from "@phosphor-icons/react";
import {Button} from "@/components/ui/button.tsx";
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
} from "@/components/ui/alert-dialog.tsx";
import {Backup, BackupType} from "@/types/backup.ts";
import {ReactNode} from "react";
import {convertSize} from "@/lib/FileUtil.ts";
import {t} from "i18next";

const TYPE_ICONS: Record<BackupType, ReactNode> = {
    ROOT: <ArchiveIcon className="h-4 w-4"/>,
    WORLDS: <PlanetIcon className="h-4 w-4"/>,
    PLUGINS: <GearIcon className="h-4 w-4"/>,
    MODS: <PuzzlePieceIcon className="h-4 w-4"/>,
    CONFIGS: <GearIcon className="h-4 w-4"/>,
    LOGS: <FileTextIcon className="h-4 w-4"/>,
}

const FALLBACK_ICON = <ArchiveIcon className="h-4 w-4"/>;

interface BackupCardProps {
    backup: Backup;
    onRestore: (id: number, haltAfterRestore: boolean) => void;
    onDelete: (id: number) => void;
    onDownload: (id: number) => void;
}

const BackupCard = ({backup, onRestore, onDelete, onDownload}: BackupCardProps) => {
    return (
        <div className="flex items-center justify-between p-4 rounded-xl border bg-card hover:bg-accent/50 transition-colors">
            <div className="flex items-center gap-4 min-w-0 flex-1">
                <div className="h-12 w-12 rounded-xl bg-muted flex items-center justify-center shrink-0">
                    <ArchiveIcon className="h-6 w-6 text-muted-foreground"/>
                </div>
                <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-3">
                        <h3 className="text-base font-semibold">{t("backup.name")}</h3>
                        <span className="text-sm text-muted-foreground bg-muted px-2 py-0.5 rounded-lg">
                            {convertSize(backup.size)}
                        </span>
                    </div>
                    <div className="flex items-center gap-4 mt-1">
                        <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
                            <CalendarIcon className="h-4 w-4"/>
                            <span>{new Date(backup.id).toLocaleString()}</span>
                        </div>
                        <div className="flex items-center gap-1.5">
                            {backup.modes.map((type) => (
                                <div className="flex items-center gap-1 rounded-lg bg-muted px-2 py-1 text-xs" key={type}>
                                    {TYPE_ICONS[type] ?? FALLBACK_ICON}
                                    <span>{t(`backup.mapping.${type.toLowerCase()}`)}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
            
            <div className="flex items-center gap-2 shrink-0 ml-4">
                <AlertDialog>
                    <AlertDialogTrigger asChild>
                        <Button variant="outline" size="lg" className="h-10 px-4 rounded-xl">
                            <ArrowCounterClockwiseIcon className="h-4 w-4 mr-2"/>
                            {t("backup.restore")}
                        </Button>
                    </AlertDialogTrigger>
                    <AlertDialogContent className="rounded-xl">
                        <AlertDialogHeader>
                            <AlertDialogTitle className="text-lg">{t("backup.restoring")}</AlertDialogTitle>
                            <AlertDialogDescription>
                                {t("backup.restore_text")}
                            </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                            <AlertDialogCancel className="rounded-xl">{t("action.cancel")}</AlertDialogCancel>
                            <AlertDialogCancel onClick={() => onRestore(backup.id, false)} className="rounded-xl">
                                {t("backup.only_restore")}
                            </AlertDialogCancel>
                            <AlertDialogAction onClick={() => onRestore(backup.id, true)} className="rounded-xl">
                                {t("backup.restore_and_stop")}
                            </AlertDialogAction>
                        </AlertDialogFooter>
                    </AlertDialogContent>
                </AlertDialog>
                
                <Button variant="outline" size="lg" onClick={() => onDownload(backup.id)} className="h-10 px-4 rounded-xl">
                    <DownloadSimpleIcon className="h-4 w-4 mr-2"/>
                    {t("backup.download")}
                </Button>
                
                <AlertDialog>
                    <AlertDialogTrigger asChild>
                        <Button variant="destructive" size="lg" className="h-10 px-4 rounded-xl">
                            <TrashIcon className="h-4 w-4 mr-2"/>
                            {t("backup.delete.button")}
                        </Button>
                    </AlertDialogTrigger>
                    <AlertDialogContent className="rounded-xl">
                        <AlertDialogHeader>
                            <AlertDialogTitle className="text-lg">{t("backup.delete.title")}</AlertDialogTitle>
                            <AlertDialogDescription>
                                {t("backup.delete.text")}
                            </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                            <AlertDialogCancel className="rounded-xl">
                                {t("action.cancel")}
                            </AlertDialogCancel>
                            <AlertDialogAction onClick={() => onDelete(backup.id)} className="rounded-xl">
                                {t("backup.delete.yes")}
                            </AlertDialogAction>
                        </AlertDialogFooter>
                    </AlertDialogContent>
                </AlertDialog>
            </div>
        </div>
    );
}

export default BackupCard;