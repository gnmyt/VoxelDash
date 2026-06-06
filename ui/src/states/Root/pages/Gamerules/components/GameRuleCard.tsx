import {useEffect, useState} from "react";
import {ArrowCounterClockwiseIcon, BookOpenTextIcon, HashIcon, ToggleRightIcon} from "@phosphor-icons/react";
import {t} from "i18next";
import {Switch} from "@/components/ui/switch";
import {Input} from "@/components/ui/input";
import {Button} from "@/components/ui/button";
import {GameRule} from "@/types/gamerule";
import {gameRuleWikiUrl, humanizeGameRule} from "@/lib/GameRuleUtil";

interface GameRuleCardProps {
    rule: GameRule;
    onChange: (key: string, value: string) => void;
}

export function GameRuleCard({rule, onChange}: GameRuleCardProps) {
    const [draft, setDraft] = useState(rule.value);

    useEffect(() => {
        setDraft(rule.value);
    }, [rule.value]);

    const isBoolean = rule.type === "BOOLEAN";
    const hasDefault = rule.defaultValue !== undefined && rule.defaultValue !== null && rule.defaultValue !== "";
    const isModified = hasDefault && rule.value !== rule.defaultValue;

    const commitDraft = () => {
        const trimmed = draft.trim();
        if (trimmed !== rule.value && trimmed !== "") onChange(rule.key, trimmed);
        else setDraft(rule.value);
    };

    const TypeIcon = isBoolean ? ToggleRightIcon : HashIcon;

    return (
        <div className="flex flex-wrap items-center gap-4 p-4 rounded-xl border bg-card hover:border-primary/40 transition-colors">
            <div className="h-10 w-10 rounded-lg bg-primary/10 flex items-center justify-center shrink-0">
                <TypeIcon className="h-5 w-5 text-primary" weight="fill"/>
            </div>

            <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                    <h3 className="font-medium truncate">{humanizeGameRule(rule.key)}</h3>
                    {isModified && (
                        <span className="text-[10px] uppercase tracking-wide text-primary bg-primary/10 px-1.5 py-0.5 rounded">
                            {t("gamerules.modified")}
                        </span>
                    )}
                </div>
                <p className="text-xs text-muted-foreground font-mono truncate">{rule.key}</p>
            </div>

            <div className="flex items-center gap-2 shrink-0">
                <a
                    href={gameRuleWikiUrl(rule.key)}
                    target="_blank"
                    rel="noopener noreferrer"
                    title={t("gamerules.wiki")}
                    className="h-9 w-9 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
                >
                    <BookOpenTextIcon className="h-4 w-4"/>
                </a>

                {hasDefault && isModified && (
                    <Button
                        variant="ghost"
                        size="icon"
                        className="h-9 w-9 rounded-lg text-muted-foreground"
                        title={t("gamerules.reset_default")}
                        onClick={() => onChange(rule.key, rule.defaultValue as string)}
                    >
                        <ArrowCounterClockwiseIcon className="h-4 w-4"/>
                    </Button>
                )}

                {isBoolean ? (
                    <div className="flex items-center gap-3">
                        <Switch
                            checked={rule.value === "true"}
                            onCheckedChange={(checked) => onChange(rule.key, checked ? "true" : "false")}
                            className="data-[state=checked]:bg-primary"
                        />
                        <span className={`text-sm font-medium w-16 ${rule.value === "true" ? "text-primary" : "text-muted-foreground"}`}>
                            {rule.value === "true" ? t("gamerules.enabled") : t("gamerules.disabled")}
                        </span>
                    </div>
                ) : (
                    <Input
                        type={rule.type === "INTEGER" ? "number" : "text"}
                        value={draft}
                        onChange={(e) => setDraft(e.target.value)}
                        onBlur={commitDraft}
                        onKeyDown={(e) => {
                            if (e.key === "Enter") (e.target as HTMLInputElement).blur();
                        }}
                        className="w-32 h-10 rounded-xl"
                    />
                )}
            </div>
        </div>
    );
}
