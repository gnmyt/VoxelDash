import {useEffect, useState} from "react";
import {GlobeHemisphereWestIcon} from "@phosphor-icons/react";
import {t} from "i18next";
import {jsonRequest, postRequest} from "@/lib/RequestUtil";
import {World, CreateWorldRequest} from "@/types/world";
import {ScrollArea} from "@/components/ui/scroll-area";
import {toast} from "@/hooks/use-toast";
import WorldCard from "./components/WorldCard";
import CreateWorldDialog from "./components/CreateWorldDialog";

const Worlds = () => {
    const [worlds, setWorlds] = useState<World[]>([]);
    const [isCreating, setIsCreating] = useState(false);

    const fetchWorlds = async () => {
        const data = await jsonRequest("worlds");
        setWorlds(data.worlds || []);
    };

    const handleSetTime = async (worldName: string, time: string) => {
        await postRequest("worlds/time", {worldName, time});
        await fetchWorlds();
        toast({description: t("worlds.time_updated")});
    };

    const handleSetWeather = async (worldName: string, weather: string) => {
        await postRequest("worlds/weather", {worldName, weather});
        await fetchWorlds();
        toast({description: t("worlds.weather_updated")});
    };

    const handleSetDifficulty = async (worldName: string, difficulty: string) => {
        await postRequest("worlds/difficulty", {worldName, difficulty});
        await fetchWorlds();
        toast({description: t("worlds.difficulty_updated")});
    };

    const handleSaveWorld = async (worldName: string) => {
        await postRequest("worlds/save", {worldName});
        toast({description: t("worlds.saved")});
    };

    const handleDeleteWorld = async (worldName: string) => {
        const result = await postRequest("worlds/delete", {worldName});
        if (result.error) {
            toast({
                description: t("worlds.delete_failed"),
                variant: "destructive"
            });
        } else {
            await fetchWorlds();
            toast({description: t("worlds.deleted")});
        }
    };

    const handleCreateWorld = async (data: CreateWorldRequest) => {
        setIsCreating(true);
        try {
            const result = await postRequest("worlds/create", data);
            if (result.error) {
                toast({
                    description: t("worlds.create_failed"),
                    variant: "destructive"
                });
            } else {
                await fetchWorlds();
                toast({description: t("worlds.created")});
            }
        } finally {
            setIsCreating(false);
        }
    };

    useEffect(() => {
        fetchWorlds();

        const interval = setInterval(fetchWorlds, 10000);
        return () => clearInterval(interval);
    }, []);

    const mainWorldName = worlds.length > 0 ? worlds[0].name : null;

    return (
        <div className="flex flex-col p-6 pt-0 gap-6" style={{height: 'calc(var(--app-vh) - 5.5rem)'}}>
            <div className="flex items-center justify-between p-4 rounded-xl border bg-card shrink-0">
                <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center">
                        <GlobeHemisphereWestIcon className="h-6 w-6 text-primary" weight="fill"/>
                    </div>
                    <div>
                        <h1 className="text-lg font-semibold">{t("worlds.title")}</h1>
                        <p className="text-sm text-muted-foreground">{t("worlds.subtitle")}</p>
                    </div>
                </div>
                <CreateWorldDialog onCreate={handleCreateWorld} disabled={isCreating}/>
            </div>

            <div className="flex-1 min-h-0">
                <ScrollArea className="h-full">
                    {worlds.length === 0 ? (
                        <div className="flex flex-col items-center justify-center py-16 text-center">
                            <div className="h-16 w-16 rounded-2xl bg-muted flex items-center justify-center mb-4">
                                <GlobeHemisphereWestIcon className="h-8 w-8 text-muted-foreground"/>
                            </div>
                            <p className="text-lg font-medium text-muted-foreground">{t("worlds.none_found")}</p>
                            <p className="text-sm text-muted-foreground mt-1">{t("worlds.none_found_hint")}</p>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4 gap-3">
                            {worlds.map((world) => (
                                <WorldCard
                                    key={world.name}
                                    world={world}
                                    onSetTime={handleSetTime}
                                    onSetWeather={handleSetWeather}
                                    onSetDifficulty={handleSetDifficulty}
                                    onSave={handleSaveWorld}
                                    onDelete={handleDeleteWorld}
                                    isMainWorld={world.name === mainWorldName}
                                />
                            ))}
                        </div>
                    )}
                </ScrollArea>
            </div>
        </div>
    );
};

export default Worlds;
