import {useEffect, useState} from "react"
import {WarningCircleIcon, GearSixIcon, GameControllerIcon, GlobeHemisphereWestIcon, UsersIcon, WifiHighIcon, GaugeIcon, ShieldCheckIcon, TerminalIcon, PackageIcon, InfoIcon, WrenchIcon, PawPrintIcon} from "@phosphor-icons/react"
import {parsePropertyValue} from "@/lib/PropertyUtil.ts"
import {propertyMappings} from "@/states/Root/pages/ServerSettings/property-mappings.ts"
import {ParsedProperty, PropertyCategory, ServerProperty} from "@/types/config"
import {PropertyCard} from "@/states/Root/pages/ServerSettings/components/PropertyCard.tsx";
import {jsonRequest, patchRequest} from "@/lib/RequestUtil.ts";
import {Alert, AlertDescription, AlertTitle} from "@/components/ui/alert.tsx";
import {Button} from "@/components/ui/button.tsx";
import {t} from "i18next";
import {toast} from "@/hooks/use-toast.ts";
import {ScrollArea} from "@/components/ui/scroll-area.tsx";

const categoryConfig: Record<PropertyCategory, { label: string; icon: typeof GameControllerIcon; description: string }> = {
    gameplay: { label: "properties.category.gameplay.label", icon: GameControllerIcon, description: "properties.category.gameplay.description" },
    world: { label: "properties.category.world.label", icon: GlobeHemisphereWestIcon, description: "properties.category.world.description" },
    spawning: { label: "properties.category.spawning.label", icon: PawPrintIcon, description: "properties.category.spawning.description" },
    players: { label: "properties.category.players.label", icon: UsersIcon, description: "properties.category.players.description" },
    network: { label: "properties.category.network.label", icon: WifiHighIcon, description: "properties.category.network.description" },
    performance: { label: "properties.category.performance.label", icon: GaugeIcon, description: "properties.category.performance.description" },
    security: { label: "properties.category.security.label", icon: ShieldCheckIcon, description: "properties.category.security.description" },
    rcon: { label: "properties.category.rcon.label", icon: TerminalIcon, description: "properties.category.rcon.description" },
    resourcepack: { label: "properties.category.resourcepack.label", icon: PackageIcon, description: "properties.category.resourcepack.description" },
    info: { label: "properties.category.info.label", icon: InfoIcon, description: "properties.category.info.description" },
    advanced: { label: "properties.category.advanced.label", icon: WrenchIcon, description: "properties.category.advanced.description" },
};

const ServerSettings = () => {
    const [properties, setProperties] = useState<ServerProperty[] | null>(null);
    const [activeCategory, setActiveCategory] = useState<PropertyCategory>("gameplay");

    const handleValueChange = (name: string, value: string) => {
        setProperties((prev) => {
            if (!prev) return null;
            return prev.map((prop) => prop.name === name ? {...prop, value} : prop);
        });

        patchRequest("properties/" + name, {value}).then(() => {
            toast({title: t("properties.updated_title"), description: t("properties.updated_description")});
        });
    }

    const parsedProperties = properties?.map((prop) => {
        const mapping = propertyMappings.find((m) => m.name === prop.name);
        if (!mapping) return parsePropertyValue(prop, {name: prop.name, icon: GearSixIcon, type: "string"});
        return parsePropertyValue(prop, mapping)
    });

    const groupedProperties = parsedProperties?.reduce((acc, prop) => {
        const category = prop.mapping.category || 'advanced';
        if (!acc[category]) acc[category] = [];
        acc[category].push(prop);
        return acc;
    }, {} as Record<PropertyCategory, ParsedProperty[]>);

    useEffect(() => {
        jsonRequest("properties").then((data) => {
            if (!data) return;

            setProperties(Object.keys(data.properties).map((key) => {
                return {name: key, value: data.properties[key]};
            }));
        });
    }, []);

    if (!parsedProperties || !groupedProperties) return null;

    const categories = Object.keys(categoryConfig) as PropertyCategory[];
    const currentCategoryProps = groupedProperties[activeCategory] || [];
    const CurrentCategoryIcon = categoryConfig[activeCategory].icon;

    return (
        <div className="flex flex-col p-6 pt-0 gap-6" style={{ height: 'calc(var(--app-vh) - 5.5rem)' }}>
            <Alert variant="destructive" className="rounded-xl shrink-0">
                <WarningCircleIcon className="h-5 w-5"/>
                <AlertTitle className="text-base font-semibold">{t("action.warn")}</AlertTitle>
                <AlertDescription className="text-sm">
                    {t("properties.warning")}
                </AlertDescription>
            </Alert>
            
            <div className="flex gap-6 flex-1 min-h-0">
                <div className="w-64 shrink-0">
                    <div className="bg-card border rounded-xl p-3 h-full">
                        <ScrollArea className="h-full">
                            <nav className="space-y-1">
                                {categories.map((category) => {
                                    const config = categoryConfig[category];
                                    const Icon = config.icon;
                                    const isActive = activeCategory === category;
                                    const count = groupedProperties[category]?.length || 0;
                                    
                                    if (count === 0) return null;
                                    
                                    return (
                                        <Button
                                            key={category}
                                            variant="ghost"
                                            onClick={() => setActiveCategory(category)}
                                            className={`w-full flex items-center justify-start gap-3 px-4 py-3 h-12 rounded-xl text-left ${isActive ? 'bg-accent font-semibold' : ''}`}
                                        >
                                            <Icon className="h-5 w-5 shrink-0" weight={isActive ? "fill" : "regular"} />
                                            <span className="text-base truncate">{t(config.label)}</span>
                                            <span className="ml-auto text-sm text-muted-foreground">
                                                {count}
                                            </span>
                                        </Button>
                                    );
                                })}
                            </nav>
                        </ScrollArea>
                    </div>
                </div>

                <div className="flex-1 min-w-0">
                    <div className="bg-card border rounded-xl h-full flex flex-col overflow-hidden">
                        <div className="p-6 border-b shrink-0">
                            <div className="flex items-center gap-3">
                                <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center">
                                    <CurrentCategoryIcon className="h-6 w-6 text-primary" weight="fill" />
                                </div>
                                <div>
                                    <h2 className="text-xl font-semibold">{t(categoryConfig[activeCategory].label)}</h2>
                                    <p className="text-sm text-muted-foreground">{t(categoryConfig[activeCategory].description)}</p>
                                </div>
                            </div>
                        </div>
                        <ScrollArea className="flex-1">
                            <div className="p-6 space-y-4">
                                {currentCategoryProps.map((property) => (
                                    <PropertyCard key={property.name} property={property} onValueChange={handleValueChange}/>
                                ))}
                            </div>
                        </ScrollArea>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default ServerSettings;