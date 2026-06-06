import {useEffect, useMemo, useState} from "react";
import {BookOpenTextIcon, GameControllerIcon, MagnifyingGlassIcon} from "@phosphor-icons/react";
import {t} from "i18next";
import {jsonRequest, postRequest} from "@/lib/RequestUtil";
import {GameRule} from "@/types/gamerule";
import {GAME_RULE_WIKI_URL, humanizeGameRule} from "@/lib/GameRuleUtil";
import {ScrollArea} from "@/components/ui/scroll-area";
import {Input} from "@/components/ui/input";
import {toast} from "@/hooks/use-toast";
import {GameRuleCard} from "./components/GameRuleCard";

const Gamerules = () => {
    const [rules, setRules] = useState<GameRule[]>([]);
    const [loaded, setLoaded] = useState(false);
    const [search, setSearch] = useState("");

    const fetchRules = async () => {
        const data = await jsonRequest("gamerules");
        const list: GameRule[] = data.gamerules || [];
        list.sort((a, b) => humanizeGameRule(a.key).localeCompare(humanizeGameRule(b.key)));
        setRules(list);
        setLoaded(true);
    };

    const handleChange = async (key: string, value: string) => {
        setRules((prev) => prev.map((rule) => rule.key === key ? {...rule, value} : rule));
        const result = await postRequest("gamerules", {key, value});
        if (result?.error) {
            toast({description: result.error, variant: "destructive"});
            await fetchRules();
        } else {
            toast({description: t("gamerules.updated", {rule: humanizeGameRule(key)})});
        }
    };

    useEffect(() => {
        fetchRules();
    }, []);

    const filtered = useMemo(() => {
        const query = search.trim().toLowerCase();
        if (!query) return rules;
        return rules.filter((rule) =>
            rule.key.toLowerCase().includes(query) ||
            humanizeGameRule(rule.key).toLowerCase().includes(query));
    }, [rules, search]);

    return (
        <div className="flex flex-col p-4 md:p-6 pt-0 gap-6" style={{height: 'calc(var(--app-vh) - 5.5rem)'}}>
            <div className="flex flex-wrap items-center justify-between gap-3 p-4 rounded-xl border bg-card shrink-0">
                <div className="flex items-center gap-4 min-w-0">
                    <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
                        <GameControllerIcon className="h-6 w-6 text-primary" weight="fill"/>
                    </div>
                    <div className="min-w-0">
                        <h1 className="text-lg font-semibold">{t("gamerules.title")}</h1>
                        <p className="text-sm text-muted-foreground truncate">{t("gamerules.subtitle")}</p>
                    </div>
                </div>
                <div className="flex items-center gap-3 w-full sm:w-auto shrink-0">
                    <a
                        href={GAME_RULE_WIKI_URL}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="hidden sm:flex items-center gap-2 h-11 px-4 rounded-xl border text-sm text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
                    >
                        <BookOpenTextIcon className="h-4 w-4"/>
                        {t("gamerules.wiki_learn")}
                    </a>
                    <div className="relative w-full sm:w-64">
                        <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground"/>
                        <Input
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            placeholder={t("gamerules.search")}
                            className="pl-9 h-11 rounded-xl"
                        />
                    </div>
                </div>
            </div>

            <div className="flex-1 min-h-0">
                <ScrollArea className="h-full">
                    {!loaded ? null : filtered.length === 0 ? (
                        <div className="flex flex-col items-center justify-center py-16 text-center">
                            <div className="h-16 w-16 rounded-2xl bg-muted flex items-center justify-center mb-4">
                                <GameControllerIcon className="h-8 w-8 text-muted-foreground"/>
                            </div>
                            <p className="text-lg font-medium text-muted-foreground">
                                {rules.length === 0 ? t("gamerules.none_found") : t("gamerules.no_matches")}
                            </p>
                            <p className="text-sm text-muted-foreground mt-1">
                                {rules.length === 0 ? t("gamerules.none_found_hint") : t("gamerules.no_matches_hint")}
                            </p>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 xl:grid-cols-2 gap-3 pr-1">
                            {filtered.map((rule) => (
                                <GameRuleCard key={rule.key} rule={rule} onChange={handleChange}/>
                            ))}
                        </div>
                    )}
                </ScrollArea>
            </div>
        </div>
    );
};

export default Gamerules;
