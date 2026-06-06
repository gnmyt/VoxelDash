import { useEffect, useState } from "react";
import { UsersIcon } from "@phosphor-icons/react";
import { t } from "i18next";
import { jsonRequest } from "@/lib/RequestUtil";
import { OnlinePlayer, BannedPlayer, WhitelistData, PlayerCapabilities } from "@/types/player";
import { World } from "@/types/world";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import OnlinePlayersTab from "./components/OnlinePlayersTab";
import WhitelistTab from "./components/WhitelistTab";
import BannedPlayersTab from "./components/BannedPlayersTab";
import AllPlayersTab from "./components/AllPlayersTab";

const Players = () => {
    const [onlinePlayers, setOnlinePlayers] = useState<OnlinePlayer[]>([]);
    const [whitelistData, setWhitelistData] = useState<WhitelistData>({ players: [], enabled: false });
    const [bannedPlayers, setBannedPlayers] = useState<BannedPlayer[]>([]);
    const [capabilities, setCapabilities] = useState<PlayerCapabilities | null>(null);
    const [worlds, setWorlds] = useState<World[]>([]);

    const fetchOnlinePlayers = async () => {
        const data = await jsonRequest("players/online");
        setOnlinePlayers(data.players || []);
    };

    const fetchWhitelist = async () => {
        const data = await jsonRequest("players/whitelist");
        setWhitelistData({ players: data.players || [], enabled: data.enabled });
    };

    const fetchBannedPlayers = async () => {
        const data = await jsonRequest("players/banned");
        setBannedPlayers(data.players || []);
    };

    const fetchAllData = async () => {
        await Promise.all([fetchOnlinePlayers(), fetchWhitelist(), fetchBannedPlayers()]);
    };

    useEffect(() => {
        fetchAllData();
        jsonRequest("players/capabilities").then((d) => setCapabilities({
            inventory: d.inventory, teleport: d.teleport, mute: d.mute,
        })).catch(() => {});
        jsonRequest("worlds").then((d) => setWorlds(d.worlds || [])).catch(() => {});

        const interval = setInterval(fetchAllData, 10000);
        return () => clearInterval(interval);
    }, []);

    return (
        <div className="flex flex-col p-4 md:p-6 pt-0 gap-6" style={{ height: 'calc(var(--app-vh) - 5.5rem)' }}>
            <div className="flex items-center justify-between p-4 rounded-xl border bg-card shrink-0">
                <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center">
                        <UsersIcon className="h-6 w-6 text-primary" weight="fill" />
                    </div>
                    <div>
                        <h1 className="text-lg font-semibold">{t("players.title")}</h1>
                        <p className="text-sm text-muted-foreground">{t("players.subtitle")}</p>
                    </div>
                </div>
            </div>

            <div className="flex-1 min-h-0">
                <Tabs defaultValue="online" className="h-full flex flex-col">
                    <TabsList className="w-full sm:w-fit mb-4 overflow-x-auto">
                        <TabsTrigger value="online" className="gap-2 flex-1 sm:flex-none whitespace-nowrap">
                            {t("players.tabs.online")}
                            <span className="bg-primary/20 text-primary px-2 py-0.5 rounded-md text-xs font-medium">
                                {onlinePlayers.length}
                            </span>
                        </TabsTrigger>
                        <TabsTrigger value="whitelist" className="gap-2 flex-1 sm:flex-none whitespace-nowrap">
                            {t("players.tabs.whitelist")}
                            <span className="bg-muted px-2 py-0.5 rounded-md text-xs font-medium">
                                {whitelistData.players.length}
                            </span>
                        </TabsTrigger>
                        <TabsTrigger value="banned" className="gap-2 flex-1 sm:flex-none whitespace-nowrap">
                            {t("players.tabs.banned")}
                            <span className="bg-destructive/20 text-destructive px-2 py-0.5 rounded-md text-xs font-medium">
                                {bannedPlayers.length}
                            </span>
                        </TabsTrigger>
                        <TabsTrigger value="all" className="gap-2 flex-1 sm:flex-none whitespace-nowrap">
                            {t("players.tabs.all")}
                        </TabsTrigger>
                    </TabsList>

                    <ScrollArea className="flex-1">
                        <TabsContent value="online" className="mt-0 h-full">
                            <OnlinePlayersTab
                                players={onlinePlayers}
                                onRefresh={fetchOnlinePlayers}
                                onBanComplete={fetchBannedPlayers}
                                capabilities={capabilities}
                            />
                        </TabsContent>
                        <TabsContent value="whitelist" className="mt-0 h-full">
                            <WhitelistTab 
                                data={whitelistData} 
                                onRefresh={fetchWhitelist}
                            />
                        </TabsContent>
                        <TabsContent value="banned" className="mt-0 h-full">
                            <BannedPlayersTab
                                players={bannedPlayers}
                                onRefresh={fetchBannedPlayers}
                            />
                        </TabsContent>
                        <TabsContent value="all" className="mt-0 h-full">
                            <AllPlayersTab
                                onlinePlayers={onlinePlayers}
                                capabilities={capabilities}
                                worlds={worlds}
                            />
                        </TabsContent>
                    </ScrollArea>
                </Tabs>
            </div>
        </div>
    );
};

export default Players;
