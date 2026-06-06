import { useEffect, useState, useContext } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import { PuzzlePieceIcon, PackageIcon, StorefrontIcon } from "@phosphor-icons/react";
import { t } from "i18next";
import { jsonRequest, postRequest, deleteRequest } from "@/lib/RequestUtil";
import { Resource } from "@/types/resource";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { toast } from "@/hooks/use-toast";
import { ResourcesContext } from "@/contexts/ResourcesContext";

export const ResourceList = () => {
    const { type } = useParams<{ type: string }>();
    const navigate = useNavigate();
    const location = useLocation();
    const resourcesContext = useContext(ResourcesContext);
    const [resources, setResources] = useState<Resource[]>([]);
    const [loading, setLoading] = useState(true);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [resourceToDelete, setResourceToDelete] = useState<Resource | null>(null);
    const [togglingResource, setTogglingResource] = useState<string | null>(null);

    const fetchResources = async () => {
        if (!type) return;
        try {
            const data = await jsonRequest(`resources/list?type=${type}`);
            setResources(data.resources || []);
        } catch (error) {
            console.error("Failed to fetch resources:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        setLoading(true);
        fetchResources();
    }, [type, location.pathname]);

    const getIcon = () => {
        switch (type) {
            case "plugin":
                return PuzzlePieceIcon;
            case "datapack":
                return PackageIcon;
            case "mod":
                return PuzzlePieceIcon;
            case "extension":
                return PuzzlePieceIcon;
            default:
                return PackageIcon;
        }
    };

    const getTypeLabel = () => {
        if (!type) return "Resources";
        return t(`resources.types.${type}`, type.charAt(0).toUpperCase() + type.slice(1));
    };

    const handleToggleResource = async (resource: Resource) => {
        setTogglingResource(resource.fileName);
        try {
            const endpoint = resource.enabled ? "resources/disable" : "resources/enable";
            const result = await postRequest(endpoint, {
                type: type,
                fileName: resource.fileName,
            });

            if (result.error) {
                toast({ description: result.error, variant: "destructive" });
            } else {
                toast({ 
                    description: resource.enabled 
                        ? t("resources.disabled_success") 
                        : t("resources.enabled_success")
                });
                await fetchResources();
                resourcesContext?.refreshResources(type);
            }
        } catch (error) {
            toast({ description: t("resources.toggle_failed"), variant: "destructive" });
        } finally {
            setTogglingResource(null);
        }
    };

    const handleDeleteResource = async () => {
        if (!resourceToDelete) return;
        
        try {
            const result = await deleteRequest("resources/delete", {
                type: type,
                fileName: resourceToDelete.fileName,
            });

            if (result.error) {
                toast({ description: result.error, variant: "destructive" });
            } else {
                toast({ description: t("resources.deleted_success") });
                await fetchResources();
                resourcesContext?.refreshResources(type);
            }
        } catch (error) {
            toast({ description: t("resources.delete_failed"), variant: "destructive" });
        } finally {
            setDeleteDialogOpen(false);
            setResourceToDelete(null);
        }
    };

    const formatFileSize = (bytes: number): string => {
        if (bytes < 1024) return `${bytes} B`;
        if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
        return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    };

    const Icon = getIcon();

    return (
        <div className="flex flex-col p-4 md:p-6 pt-0 gap-6" style={{ height: 'calc(var(--app-vh) - 5.5rem)' }}>
            <div className="flex flex-wrap items-center justify-between gap-3 p-4 rounded-xl border bg-card shrink-0">
                <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center">
                        <Icon className="h-6 w-6 text-primary" weight="fill" />
                    </div>
                    <div>
                        <h1 className="text-lg font-semibold">{getTypeLabel()}</h1>
                        <p className="text-sm text-muted-foreground">
                            {t("resources.subtitle", { count: resources.length })}
                        </p>
                    </div>
                </div>
                <Button onClick={() => navigate(`/resources/${type}/store`)} className="gap-2">
                    <StorefrontIcon className="h-4 w-4" weight="fill" />
                    {t("resources.store")}
                </Button>
            </div>

            <ScrollArea className="flex-1">
                <div className="grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
                    {loading ? (
                        Array.from({ length: 6 }).map((_, i) => (
                            <Card key={i}>
                                <CardHeader>
                                    <Skeleton className="h-6 w-3/4" />
                                    <Skeleton className="h-4 w-1/2 mt-2" />
                                </CardHeader>
                                <CardContent>
                                    <Skeleton className="h-4 w-full" />
                                    <Skeleton className="h-4 w-2/3 mt-2" />
                                </CardContent>
                            </Card>
                        ))
                    ) : resources.length === 0 ? (
                        <div className="col-span-full flex flex-col items-center justify-center py-16 text-center">
                            <div className="h-16 w-16 rounded-xl bg-muted flex items-center justify-center mb-4">
                                <Icon className="h-8 w-8 text-muted-foreground" />
                            </div>
                            <h3 className="text-lg font-medium">{t("resources.no_resources")}</h3>
                            <p className="text-sm text-muted-foreground mt-1">
                                {t("resources.no_resources_description", { type: getTypeLabel().toLowerCase() })}
                            </p>
                        </div>
                    ) : (
                        resources.map((resource) => (
                            <Card key={resource.fileName} className={!resource.enabled ? "opacity-60" : ""}>
                                <CardHeader className="pb-3">
                                    <div className="flex items-start justify-between gap-2">
                                        <div className="flex-1 min-w-0">
                                            <CardTitle className="text-base truncate">
                                                {resource.name}
                                            </CardTitle>
                                            {resource.version && (
                                                <Badge variant="secondary" className="mt-1 text-xs">
                                                    v{resource.version}
                                                </Badge>
                                            )}
                                        </div>
                                        <Switch
                                            checked={resource.enabled}
                                            disabled={togglingResource === resource.fileName}
                                            onCheckedChange={() => handleToggleResource(resource)}
                                        />
                                    </div>
                                    {resource.authors.length > 0 && (
                                        <CardDescription className="text-xs mt-1">
                                            {t("resources.by")} {resource.authors.join(", ")}
                                        </CardDescription>
                                    )}
                                </CardHeader>
                                <CardContent className="pt-0">
                                    {resource.description ? (
                                        <p className="text-sm text-muted-foreground line-clamp-2">
                                            {resource.description}
                                        </p>
                                    ) : (
                                        <p className="text-sm text-muted-foreground italic">
                                            {t("resources.no_description")}
                                        </p>
                                    )}
                                    <div className="flex items-center justify-between mt-4">
                                        <span className="text-xs text-muted-foreground">
                                            {formatFileSize(resource.fileSize)}
                                        </span>
                                        <Button
                                            variant="destructive"
                                            size="sm"
                                            onClick={() => {
                                                setResourceToDelete(resource);
                                                setDeleteDialogOpen(true);
                                            }}
                                        >
                                            {t("action.delete")}
                                        </Button>
                                    </div>
                                </CardContent>
                            </Card>
                        ))
                    )}
                </div>
            </ScrollArea>

            <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>{t("resources.delete_title")}</AlertDialogTitle>
                        <AlertDialogDescription>
                            {t("resources.delete_description", { name: resourceToDelete?.name })}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>{t("action.cancel")}</AlertDialogCancel>
                        <AlertDialogAction onClick={handleDeleteResource}>
                            {t("action.delete")}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    );
};