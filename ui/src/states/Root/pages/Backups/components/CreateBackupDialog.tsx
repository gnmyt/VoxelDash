import * as React from "react";
import { CheckIcon, PlusIcon } from "@phosphor-icons/react";

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
import { Label } from "@/components/ui/label.tsx";
import { BackupOption } from "@/types/backup.ts";
import {t} from "i18next";

interface CreateBackupDialogProps {
    options: BackupOption[];
    onBackup: (data: number) => Promise<void>;
    disabled?: boolean;
}

const CreateBackupDialog = ({ options, onBackup, disabled }: CreateBackupDialogProps) => {
    const [open, setOpen] = React.useState(false);
    const [selected, setSelected] = React.useState<number[]>([]);

    const granular = options.filter(o => o.bit !== 0);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setOpen(false);

        const backupBit = selected.includes(0) ? 0 : selected.reduce((acc, bit) => acc | bit, 0);

        await onBackup(backupBit);
        setSelected([]);
    }

    const toggle = (bit: number) => {
        setSelected(prev => {
            if (prev.includes(bit)) return prev.filter(b => b !== bit);

            if (bit === 0) return [0];
            return [...prev.filter(b => b !== 0), bit];
        });
    }

    const selectAll = () => {
        setSelected(granular.map(o => o.bit));
    }

    return (
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
                <Button disabled={disabled} size="lg" className="h-12 px-6 rounded-xl text-base">
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
                        <div className="grid gap-3">
                            <div className="flex items-center justify-between">
                                <Label className="text-base">{t("backup.include")}</Label>
                                <Button type="button" variant="ghost" size="sm" onClick={selectAll} className="rounded-xl">
                                    {t("backup.select_all")}
                                </Button>
                            </div>
                            <div className="grid grid-cols-1 gap-2">
                                {options.map((option) => (
                                    <Button
                                        key={option.id}
                                        type="button"
                                        variant="outline"
                                        onClick={() => toggle(option.bit)}
                                        className={`flex items-center justify-start gap-3 h-14 px-4 rounded-xl text-left ${selected.includes(option.bit) ? 'border-primary bg-primary/10' : ''}`}
                                    >
                                        <div className={`flex h-5 w-5 items-center justify-center rounded-md border-2 transition-colors ${
                                            selected.includes(option.bit)
                                                ? "bg-primary border-primary"
                                                : "border-muted-foreground"}`}
                                        >
                                            {selected.includes(option.bit) && (
                                                <CheckIcon className="h-3 w-3 text-primary-foreground" weight="bold" />
                                            )}
                                        </div>
                                        <span className="text-base font-medium">
                                            {t("backup.mapping." + option.id)}
                                        </span>
                                    </Button>
                                ))}
                            </div>
                        </div>
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
