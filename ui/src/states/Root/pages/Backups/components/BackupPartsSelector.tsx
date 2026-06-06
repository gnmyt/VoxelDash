import { CheckIcon } from "@phosphor-icons/react";
import { t } from "i18next";

import { Button } from "@/components/ui/button.tsx";
import { Label } from "@/components/ui/label.tsx";
import { BackupOption } from "@/types/backup.ts";

interface BackupPartsSelectorProps {
    options: BackupOption[];
    selected: number[];
    onSelectedChange: (selected: number[]) => void;
}

const BackupPartsSelector = ({ options, selected, onSelectedChange }: BackupPartsSelectorProps) => {
    const granular = options.filter(o => o.bit !== 0);

    const toggle = (bit: number) => {
        if (selected.includes(bit)) {
            onSelectedChange(selected.filter(b => b !== bit));
            return;
        }
        if (bit === 0) {
            onSelectedChange([0]);
            return;
        }
        onSelectedChange([...selected.filter(b => b !== 0), bit]);
    };

    const selectAll = () => onSelectedChange(granular.map(o => o.bit));

    return (
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
    );
};

export const toBackupBit = (selected: number[]): number =>
    selected.includes(0) ? 0 : selected.reduce((acc, bit) => acc | bit, 0);

export const fromBackupBit = (bit: number): number[] => {
    if (bit === 0) return [0];
    const result: number[] = [];
    for (let b = 1; b <= bit; b <<= 1) {
        if ((bit & b) === b) result.push(b);
    }
    return result;
};

export default BackupPartsSelector;
