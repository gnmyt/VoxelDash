import { PermissionLevel } from "@/types/user";
import { cn } from "@/lib/utils";
import { ProhibitIcon, EyeIcon, CheckIcon } from "@phosphor-icons/react";
import { t } from "i18next";

interface PermissionToggleProps {
    value: PermissionLevel;
    onChange: (value: PermissionLevel) => void;
    disabled?: boolean;
}

const PermissionToggle = ({ value, onChange, disabled }: PermissionToggleProps) => {
    const handleClick = (level: PermissionLevel) => {
        if (disabled) return;
        onChange(level);
    };

    return (
        <div className={cn(
            "inline-flex items-center rounded-lg border p-0.5 gap-0.5",
            disabled && "opacity-50 cursor-not-allowed"
        )}>
            <button
                type="button"
                onClick={() => handleClick(0)}
                disabled={disabled}
                className={cn(
                    "flex items-center justify-center h-7 w-7 rounded-md transition-all",
                    value === 0 
                        ? "bg-muted text-muted-foreground" 
                        : "text-muted-foreground/50 hover:text-muted-foreground hover:bg-muted/50",
                    disabled && "pointer-events-none"
                )}
                title={t("users.level.none")}
            >
                <ProhibitIcon className="h-4 w-4" weight={value === 0 ? "fill" : "regular"} />
            </button>

            <button
                type="button"
                onClick={() => handleClick(1)}
                disabled={disabled}
                className={cn(
                    "flex items-center justify-center h-7 w-7 rounded-md transition-all",
                    value === 1 
                        ? "bg-yellow-500/20 text-yellow-600 dark:text-yellow-400" 
                        : "text-muted-foreground/50 hover:text-yellow-600 dark:hover:text-yellow-400 hover:bg-yellow-500/10",
                    disabled && "pointer-events-none"
                )}
                title={t("users.level.read")}
            >
                <EyeIcon className="h-4 w-4" weight={value === 1 ? "fill" : "regular"} />
            </button>

            <button
                type="button"
                onClick={() => handleClick(2)}
                disabled={disabled}
                className={cn(
                    "flex items-center justify-center h-7 w-7 rounded-md transition-all",
                    value === 2 
                        ? "bg-green-500/20 text-green-600 dark:text-green-400" 
                        : "text-muted-foreground/50 hover:text-green-600 dark:hover:text-green-400 hover:bg-green-500/10",
                    disabled && "pointer-events-none"
                )}
                title={t("users.level.full")}
            >
                <CheckIcon className="h-4 w-4" weight={value === 2 ? "bold" : "regular"} />
            </button>
        </div>
    );
};

export default PermissionToggle;
