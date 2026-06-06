import * as React from "react";
import { PlusIcon } from "@phosphor-icons/react";

import { Button } from "@/components/ui/button.tsx";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog.tsx";
import { Input } from "@/components/ui/input.tsx";
import { Label } from "@/components/ui/label.tsx";
import { BackupOption } from "@/types/backup.ts";
import BackupPartsSelector, { toBackupBit } from "@/states/Root/pages/Backups/components/BackupPartsSelector.tsx";
import {t} from "i18next";

interface CreateBackupDialogProps {
    options: BackupOption[];
    onBackup: (backupMode: number, name: string) => Promise<void>;
    disabled?: boolean;
}

const CreateBackupDialog = ({ options, onBackup, disabled }: CreateBackupDialogProps) => {
    const [open, setOpen] = React.useState(false);
    const [selected, setSelected] = React.useState<number[]>([]);
    const [name, setName] = React.useState("");

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setOpen(false);

        await onBackup(toBackupBit(selected), name.trim());
        setSelected([]);
        setName("");
    }

    return (
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
                <Button disabled={disabled} size="lg" className="h-12 px-6 rounded-xl text-base w-full sm:w-auto">
                    <PlusIcon className="h-5 w-5 mr-2" weight="bold" />
                    {t("backup.create")}
                </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-[425px] rounded-xl">
                <form onSubmit={handleSubmit}>
                    <DialogHeader>
                        <DialogTitle className="text-lg">{t("backup.create")}</DialogTitle>
                        <DialogDescription>
                            {t("backup.description")}
                        </DialogDescription>
                    </DialogHeader>
                    <div className="grid gap-4 py-6">
                        <div className="grid gap-2">
                            <Label className="text-base">{t("backup.name_label")}</Label>
                            <Input
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder={t("backup.name_placeholder")}
                                className="rounded-xl"
                            />
                            <p className="text-xs text-muted-foreground">{t("backup.name_hint")}</p>
                        </div>
                        <BackupPartsSelector options={options} selected={selected} onSelectedChange={setSelected} />
                    </div>
                    <DialogFooter>
                        <Button type="submit" disabled={selected.length === 0} size="lg" className="w-full h-12 rounded-xl text-base">
                            {t("action.create")}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}

export default CreateBackupDialog;
