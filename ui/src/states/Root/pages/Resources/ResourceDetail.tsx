import { useEffect, useState, useContext } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { PuzzlePieceIcon, PackageIcon, ArrowLeftIcon, FileIcon, HardDrivesIcon, TagIcon, UsersIcon } from "@phosphor-icons/react";
import { t } from "i18next";
import { jsonRequest, postRequest, deleteRequest } from "@/lib/RequestUtil";
import { Resource } from "@/types/resource";
import { Button } from "@/components/ui/button";
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
import { ConfigEditor } from "./ConfigEditor";

export const ResourceDetail = () => {
    const { type, fileName } = useParams<{ type: string; fileName: string }>();
    const navigate = useNavigate();
    const resourcesContext = useContext(ResourcesContext);
    const [resource, setResource] = useState<Resource | null>(null);
    const [loading, setLoading] = useState(true);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [toggling, setToggling] = useState(false);

    const fetchResource = async () => {
        if (!type || !fileName) return;
        try {
            const data = await jsonRequest(`resources/get?type=${encodeURIComponent(type)}&fileName=${encodeURIComponent(fileName)}`);
            setResource(data.resource || null);
        } catch (error) {
            console.error("Failed to fetch resource:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchResource();
    }, [type, fileName]);

    const getIcon = () => {
        switch (type) {
            case "plugin":
            case "mod":
            case "extension":
                return PuzzlePieceIcon;
            case "datapack":
                return PackageIcon;
            default:
                return PackageIcon;
        }
    };

    const handleToggle = async () => {
        if (!resource) return;
        setToggling(true);
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
                await fetchResource();
                resourcesContext?.refreshResources(type);
            }
        } catch (error) {
            toast({ description: t("resources.toggle_failed"), variant: "destructive" });
        } finally {
            setToggling(false);
        }
    };

    const handleDelete = async () => {
        if (!resource) return;
        
        try {
            const result = await deleteRequest("resources/delete", {
                type: type,
                fileName: resource.fileName,
            });

            if (result.error) {
                toast({ description: result.error, variant: "destructive" });
            } else {
                toast({ description: t("resources.deleted_success") });
                resourcesContext?.refreshResources(type);
                navigate(`/resources/${type}`);
            }
        } catch (error) {
            toast({ description: t("resources.delete_failed"), variant: "destructive" });
        } finally {
            setDeleteDialogOpen(false);
        }
    };

    const formatFileSize = (bytes: number): string => {
        if (bytes < 1024) return `${bytes} B`;
        if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
        return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    };

    const Icon = getIcon();

    if (loading) {
        return (
            <div className="flex flex-col p-6 pt-0 gap-6">
                <div className="flex items-center justify-between p-4 rounded-xl border bg-card">
                    <div className="flex items-center gap-4">
                        <Skeleton className="h-12 w-12 rounded-xl" />
                        <div>
                            <Skeleton className="h-6 w-48" />
                            <Skeleton className="h-4 w-32 mt-2" />
                        </div>
                    </div>
                </div>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    {[...Array(4)].map((_, i) => (
                        <div key={i} className="flex items-center gap-3 p-4 rounded-xl border bg-card">
                            <Skeleton className="h-10 w-10 rounded-lg" />
                            <div className="flex-1">
                                <Skeleton className="h-3 w-12" />
                                <Skeleton className="h-4 w-20 mt-1" />
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        );
    }

    if (!resource) {
        return (
            <div className="flex flex-col p-6 pt-0 gap-6">
                <div className="flex items-center justify-center py-16">
                    <div className="text-center">
                        <Icon className="h-16 w-16 text-muted-foreground mx-auto mb-4" />
                        <h2 className="text-xl font-semibold">{t("resources.not_found")}</h2>
                        <p className="text-muted-foreground mt-2">{t("resources.not_found_description")}</p>
                        <Button 
                            variant="outline" 
                            className="mt-4"
                            onClick={() => navigate(`/resources/${type}`)}
                        >
                            <ArrowLeftIcon className="mr-2 h-4 w-4" />
                            {t("resources.go_back")}
                        </Button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="flex flex-col px-6 pb-6 gap-6 flex-1 min-h-0 overflow-hidden">
            <div className="flex items-center justify-between p-4 rounded-xl border bg-card shrink-0">
                <div className="flex items-center gap-4">
                    <div className={`h-12 w-12 rounded-xl flex items-center justify-center ${
                        resource.enabled ? "bg-primary/10" : "bg-muted"
                    }`}>
                        <Icon className={`h-6 w-6 ${
                            resource.enabled ? "text-primary" : "text-muted-foreground"
                        }`} weight="fill" />
                    </div>
                    <div>
                        <div className="flex items-center gap-2">
                            <h1 className="text-lg font-semibold">{resource.name}</h1>
                            {!resource.enabled && (
                                <Badge variant="outline" className="text-muted-foreground">{t("resources.disabled")}</Badge>
                            )}
                        </div>
                    </div>
                </div>
                <div className="flex items-center gap-3">
                    <div className="flex items-center gap-2">
                        <span className="text-sm text-muted-foreground">
                            {resource.enabled ? t("resources.enabled") : t("resources.disabled")}
                        </span>
                        <Switch
                            checked={resource.enabled}
                            disabled={toggling}
                            onCheckedChange={handleToggle}
                        />
                    </div>
                    <Button
                        variant="destructive"
                        onClick={() => setDeleteDialogOpen(true)}
                    >
                        {t("action.delete")}
                    </Button>
                </div>
            </div>

            {resource.description && (
                <p className="text-sm text-muted-foreground -mt-2 shrink-0">
                    {resource.description}
                </p>
            )}
            
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 shrink-0">
                <div className="flex items-center gap-3 p-4 rounded-xl border bg-card">
                    <div className="h-10 w-10 rounded-lg bg-muted flex items-center justify-center shrink-0">
                        <FileIcon className="h-5 w-5 text-muted-foreground" />
                    </div>
                    <div className="min-w-0">
                        <p className="text-xs text-muted-foreground">{t("resources.info.file")}</p>
                        <p className="text-sm font-medium truncate">{resource.fileName}</p>
                    </div>
                </div>
                
                <div className="flex items-center gap-3 p-4 rounded-xl border bg-card">
                    <div className="h-10 w-10 rounded-lg bg-muted flex items-center justify-center shrink-0">
                        <HardDrivesIcon className="h-5 w-5 text-muted-foreground" />
                    </div>
                    <div className="min-w-0">
                        <p className="text-xs text-muted-foreground">{t("resources.info.size")}</p>
                        <p className="text-sm font-medium">{formatFileSize(resource.fileSize)}</p>
                    </div>
                </div>
                
                <div className="flex items-center gap-3 p-4 rounded-xl border bg-card">
                    <div className="h-10 w-10 rounded-lg bg-muted flex items-center justify-center shrink-0">
                        <TagIcon className="h-5 w-5 text-muted-foreground" />
                    </div>
                    <div className="min-w-0">
                        <p className="text-xs text-muted-foreground">{t("resources.info.version")}</p>
                        <p className="text-sm font-medium">{resource.version || "—"}</p>
                    </div>
                </div>
                
                <div className="flex items-center gap-3 p-4 rounded-xl border bg-card">
                    <div className="h-10 w-10 rounded-lg bg-muted flex items-center justify-center shrink-0">
                        <UsersIcon className="h-5 w-5 text-muted-foreground" />
                    </div>
                    <div className="min-w-0">
                        <p className="text-xs text-muted-foreground">{t("resources.info.authors")}</p>
                        <p className="text-sm font-medium truncate">{resource.authors.length > 0 ? resource.authors.join(", ") : "—"}</p>
                    </div>
                </div>
            </div>

            <ConfigEditor resourceType={type!} resourceFileName={resource.fileName} resourceName={resource.name} />

            <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>{t("resources.delete_title")}</AlertDialogTitle>
                        <AlertDialogDescription>
                            {t("resources.delete_description", { name: resource.name })}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>{t("action.cancel")}</AlertDialogCancel>
                        <AlertDialogAction onClick={handleDelete}>
                            {t("action.delete")}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    );
};