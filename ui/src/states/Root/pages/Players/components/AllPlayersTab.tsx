import { useEffect, useState } from "react";
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

const AllPlayersTab = ({ onlinePlayers, capabilities, worlds }: AllPlayersTabProps) => {
    const [players, setPlayers] = useState<OfflinePlayer[]>([]);
    const [query, setQuery] = useState("");

    useEffect(() => {
        jsonRequest("players/all").then((d) => setPlayers(d.players || [])).catch(() => {});
    }, []);

    const onlineUuids = new Set(onlinePlayers.map((p) => p.uuid));
    const filtered = players
        .filter((p) => p.name.toLowerCase().includes(query.toLowerCase()))
        .sort((a, b) => a.name.localeCompare(b.name));

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
                {filtered.map((p) => {
                    const isOnline = onlineUuids.has(p.uuid);
                    return (
                        <div key={p.uuid} className="flex items-center gap-3 rounded-xl border bg-card p-3">
                            <img src={`https://mc-heads.net/avatar/${p.uuid}/32`} alt={p.name} className="h-9 w-9 rounded" />
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
        </div>
    );
};

export default AllPlayersTab;
