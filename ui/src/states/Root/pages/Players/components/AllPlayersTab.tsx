import { useDeferredValue, useEffect, useMemo, useRef, useState } from "react";
import { t } from "i18next";
import { jsonRequest } from "@/lib/RequestUtil";
import { OfflinePlayer, OnlinePlayer, PlayerCapabilities } from "@/types/player";
import { World } from "@/types/world";
import { Input } from "@/components/ui/input";
import { MagnifyingGlassIcon, UsersThreeIcon } from "@phosphor-icons/react";
import PlayerActionsMenu from "./PlayerActionsMenu";

interface AllPlayersTabProps {
    onlinePlayers: OnlinePlayer[];
    capabilities: PlayerCapabilities | null;
    worlds: World[];
}

const PAGE_SIZE = 60;
const collator = new Intl.Collator(undefined, { sensitivity: "base" });

const AllPlayersTab = ({ onlinePlayers, capabilities, worlds }: AllPlayersTabProps) => {
    const [players, setPlayers] = useState<OfflinePlayer[]>([]);
    const [query, setQuery] = useState("");
    const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
    const sentinelRef = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        jsonRequest("players/all").then((d) => setPlayers(d.players || [])).catch(() => {});
    }, []);

    const deferredQuery = useDeferredValue(query);
    const onlineUuids = useMemo(() => new Set(onlinePlayers.map((p) => p.uuid)), [onlinePlayers]);
    const filtered = useMemo(() => {
        const q = deferredQuery.toLowerCase();
        return players
            .filter((p) => p.name.toLowerCase().includes(q))
            .sort((a, b) => collator.compare(a.name, b.name));
    }, [players, deferredQuery]);

    useEffect(() => {
        setVisibleCount(PAGE_SIZE);
    }, [deferredQuery, players]);

    const visible = filtered.slice(0, visibleCount);
    const hasMore = visibleCount < filtered.length;

    useEffect(() => {
        if (!hasMore) return;
        const node = sentinelRef.current;
        if (!node) return;
        const observer = new IntersectionObserver((entries) => {
            if (entries.some((e) => e.isIntersecting)) {
                setVisibleCount((c) => c + PAGE_SIZE);
            }
        });
        observer.observe(node);
        return () => observer.disconnect();
    }, [hasMore, filtered.length]);

    if (players.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center py-16 text-center">
                <div className="h-16 w-16 rounded-2xl bg-muted flex items-center justify-center mb-4">
                    <UsersThreeIcon className="h-8 w-8 text-muted-foreground" />
                </div>
                <p className="text-lg font-medium text-muted-foreground">{t("players.all.empty")}</p>
                <p className="text-sm text-muted-foreground mt-1">{t("players.all.empty_description")}</p>
            </div>
        );
    }

    return (
        <div className="space-y-4">
            <div className="relative max-w-sm">
                <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder={t("players.all.search")} className="pl-9" />
            </div>

            <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                {visible.map((p) => {
                    const isOnline = onlineUuids.has(p.uuid);
                    return (
                        <div key={p.uuid} className="flex items-center gap-3 rounded-xl border bg-card p-3">
                            <img src={`https://mc-heads.net/avatar/${p.uuid}/32`} alt={p.name} loading="lazy" decoding="async" className="h-9 w-9 rounded" />
                            <div className="min-w-0 flex-1">
                                <div className="font-medium truncate flex items-center gap-2">
                                    {p.name}
                                    {isOnline && <span className="h-2 w-2 rounded-full bg-emerald-500" title={t("players.online_badge")} />}
                                </div>
                                <div className="text-[11px] text-muted-foreground truncate">{p.uuid}</div>
                            </div>
                            <PlayerActionsMenu
                                player={{ name: p.name, uuid: p.uuid, online: isOnline }}
                                capabilities={capabilities}
                                worlds={worlds}
                            />
                        </div>
                    );
                })}
            </div>

            {hasMore && <div ref={sentinelRef} className="h-8" aria-hidden="true" />}
        </div>
    );
};

export default AllPlayersTab;
