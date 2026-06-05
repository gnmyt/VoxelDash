import {useState, useEffect, useCallback, useContext, useMemo} from "react";
import {useParams, useNavigate} from "react-router-dom";
import {
    StorefrontIcon,
    MagnifyingGlassIcon,
    DownloadSimpleIcon,
    CaretLeftIcon,
    CaretRightIcon,
    CheckCircleIcon,
    CircleNotchIcon,
    ArrowLeftIcon,
    KeyIcon,
    GearIcon
} from "@phosphor-icons/react";
import {t} from "i18next";

import {Card, CardContent} from "@/components/ui/card";
import {Input} from "@/components/ui/input";
import {Button} from "@/components/ui/button";
import {Badge} from "@/components/ui/badge";
import {ScrollArea} from "@/components/ui/scroll-area";
import {Skeleton} from "@/components/ui/skeleton";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import {toast} from "@/hooks/use-toast";
import {ResourcesContext} from "@/contexts/ResourcesContext";
import {ApiKeyDialog} from "@/components/store/ApiKeyDialog";

import {
    StoreProvider,
    StoreProject,
    StoreSearchResult,
    StoreVersion,
    InstalledStoreResource
} from "@/types/store";
import {
    getStoreProviders,
    searchStore,
    getProjectVersions,
    installStoreResource,
    getInstalledStoreResources,
    formatDownloads,
    formatFileSize,
    formatRelativeTime
} from "@/lib/StoreApi";

export const ResourceStore = () => {
    const {type} = useParams<{ type: string }>();
    const navigate = useNavigate();
    const resourcesContext = useContext(ResourcesContext);

    const [providers, setProviders] = useState<StoreProvider[]>([]);
    const [selectedProvider, setSelectedProvider] = useState<string>("modrinth");
    const [searchQuery, setSearchQuery] = useState("");
    const [debouncedQuery, setDebouncedQuery] = useState("");
    const [searchResult, setSearchResult] = useState<StoreSearchResult | null>(null);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [installedResources, setInstalledResources] = useState<InstalledStoreResource[]>([]);

    const [installDialogOpen, setInstallDialogOpen] = useState(false);
    const [selectedProject, setSelectedProject] = useState<StoreProject | null>(null);
    const [projectVersions, setProjectVersions] = useState<StoreVersion[]>([]);
    const [selectedVersion, setSelectedVersion] = useState<string>("");
    const [loadingVersions, setLoadingVersions] = useState(false);
    const [installing, setInstalling] = useState(false);

    const [apiKeyDialogOpen, setApiKeyDialogOpen] = useState(false);

    const pageSize = 20;

    const currentProvider = useMemo(() => {
        return providers.find(p => p.id === selectedProvider);
    }, [providers, selectedProvider]);

    const needsApiKeyConfiguration = useMemo(() => {
        return currentProvider?.requiresApiKey && !currentProvider?.isConfigured;
    }, [currentProvider]);

    const loadProviders = useCallback(async () => {
        if (!type) return;
        try {
            const data = await getStoreProviders(type);
            setProviders(data);
            if (data.length > 0 && !data.find(p => p.id === selectedProvider)) {
                setSelectedProvider(data[0].id);
            }
        } catch (error) {
            console.error("Failed to load providers:", error);
        }
    }, [type, selectedProvider]);

    useEffect(() => {
        loadProviders();
    }, [type]);

    useEffect(() => {
        const loadInstalled = async () => {
            if (!type) return;
            try {
                const data = await getInstalledStoreResources(type);
                setInstalledResources(data);
            } catch (error) {
                console.error("Failed to load installed resources:", error);
            }
        };
        loadInstalled();
    }, [type]);

    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedQuery(searchQuery);
            setPage(0);
        }, 300);
        return () => clearTimeout(timer);
    }, [searchQuery]);

    const performSearch = useCallback(async () => {
        if (!type) return;

        if (needsApiKeyConfiguration) {
            setSearchResult(null);
            setLoading(false);
            return;
        }

        setLoading(true);
        try {
            const result = await searchStore(type, debouncedQuery, page, pageSize, selectedProvider);
            setSearchResult(result);
        } catch (error) {
            console.error("Search failed:", error);
            toast({description: t("store.search_failed"), variant: "destructive"});
        } finally {
            setLoading(false);
        }
    }, [type, debouncedQuery, page, selectedProvider, needsApiKeyConfiguration]);

    useEffect(() => {
        performSearch();
    }, [performSearch]);

    const handleApiKeySuccess = () => {
        loadProviders();
    };

    const isInstalled = (projectId: string): boolean => {
        return installedResources.some(r => r.projectId === projectId);
    };

    const getInstalledInfo = (projectId: string): InstalledStoreResource | undefined => {
        return installedResources.find(r => r.projectId === projectId);
    };

    const openInstallDialog = async (project: StoreProject) => {
        setSelectedProject(project);
        setInstallDialogOpen(true);
        setLoadingVersions(true);
        setSelectedVersion("");

        try {
            const {versions} = await getProjectVersions(project.id, selectedProvider, type);
            setProjectVersions(versions);
            if (versions.length > 0) {
                setSelectedVersion(versions[0].id);
            }
        } catch (error) {
            console.error("Failed to load versions:", error);
            toast({description: t("store.versions_failed"), variant: "destructive"});
        } finally {
            setLoadingVersions(false);
        }
    };

    const handleInstall = async () => {
        if (!type || !selectedProject || !selectedVersion) return;

        setInstalling(true);
        try {
            const result = await installStoreResource(type, selectedProject.id, selectedVersion, selectedProvider);

            if (result.success) {
                const successMessage = result.enabled
                    ? t("store.install_success_enabled")
                    : t("store.install_success_restart");
                toast({description: successMessage});
                setInstallDialogOpen(false);

                const data = await getInstalledStoreResources(type);
                setInstalledResources(data);

                if (resourcesContext?.refreshResources) {
                    await resourcesContext.refreshResources(type);
                }
            } else {
                toast({description: result.error || t("store.install_failed"), variant: "destructive"});
            }
        } catch (error) {
            console.error("Install failed:", error);
            toast({description: t("store.install_failed"), variant: "destructive"});
        } finally {
            setInstalling(false);
        }
    };

    const getTypeLabel = () => {
        return t(`resources.types.${type}`, type ? type.charAt(0).toUpperCase() + type.slice(1) : "Resource");
    };

    const renderProjectCard = (project: StoreProject) => {
        const installed = isInstalled(project.id);
        const installedInfo = getInstalledInfo(project.id);

        return (
            <Card
                key={project.id}
                className="overflow-hidden hover:shadow-lg transition-shadow cursor-pointer group"
                onClick={() => !installed && openInstallDialog(project)}
            >
                <CardContent className="p-4">
                    <div className="flex gap-4">
                        <div className="shrink-0">
                            {project.iconUrl ? (
                                <img
                                    src={project.iconUrl}
                                    alt={project.name}
                                    className="h-16 w-16 rounded-lg object-cover"
                                    onError={(e) => {
                                        e.currentTarget.style.display = 'none';
                                    }}
                                />
                            ) : (
                                <div className="h-16 w-16 rounded-lg bg-muted flex items-center justify-center">
                                    <StorefrontIcon className="h-8 w-8 text-muted-foreground"/>
                                </div>
                            )}
                        </div>

                        <div className="flex-1 min-w-0">
                            <div className="flex items-start justify-between gap-2">
                                <div className="min-w-0">
                                    <h3 className="font-semibold truncate group-hover:text-primary transition-colors">
                                        {project.name}
                                    </h3>
                                    <p className="text-sm text-muted-foreground">
                                        {t("resources.by")} {project.author}
                                    </p>
                                </div>

                                <div className="shrink-0">
                                    {installed ? (
                                        <Badge variant="secondary" className="gap-1">
                                            <CheckCircleIcon className="h-3 w-3"/>
                                            {installedInfo?.enabled ? t("store.installed") : t("store.installed_disabled")}
                                        </Badge>
                                    ) : (
                                        <Button
                                            size="sm"
                                            variant="secondary"
                                            className="gap-1 opacity-0 group-hover:opacity-100 transition-opacity"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                openInstallDialog(project);
                                            }}
                                        >
                                            <DownloadSimpleIcon className="h-4 w-4"/>
                                            {t("store.install")}
                                        </Button>
                                    )}
                                </div>
                            </div>

                            <p className="text-sm text-muted-foreground mt-2 line-clamp-2">
                                {project.description || t("resources.no_description")}
                            </p>

                            <div className="flex items-center gap-4 mt-3 text-xs text-muted-foreground">
                                <div className="flex items-center gap-1">
                                    <DownloadSimpleIcon className="h-3.5 w-3.5"/>
                                    {formatDownloads(project.downloads)}
                                </div>
                                <div>
                                    {t("store.updated")} {formatRelativeTime(project.dateModified)}
                                </div>
                            </div>
                        </div>
                    </div>
                </CardContent>
            </Card>
        );
    };

    return (
        <div className="flex flex-col p-6 pt-0 gap-6" style={{height: 'calc(var(--app-vh) - 5.5rem)'}}>
            <div className="flex items-center justify-between p-4 rounded-xl border bg-card shrink-0">
                <div className="flex items-center gap-4">
                    <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => navigate(`/resources/${type}`)}
                    >
                        <ArrowLeftIcon className="h-5 w-5"/>
                    </Button>
                    <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center">
                        <StorefrontIcon className="h-6 w-6 text-primary" weight="fill"/>
                    </div>
                    <div>
                        <h1 className="text-lg font-semibold">
                            {getTypeLabel()} {t("resources.store")}
                        </h1>
                        {searchResult && (
                            <p className="text-sm text-muted-foreground">
                                {t("store.results_count", {count: searchResult.totalHits})}
                                {searchResult.gameVersion && ` • ${searchResult.gameVersion}`}
                                {searchResult.loader && ` • ${searchResult.loader}`}
                            </p>
                        )}
                    </div>
                </div>

                <div className="flex items-center gap-2">
                    <Select value={selectedProvider} onValueChange={setSelectedProvider}>
                        <SelectTrigger className="w-[180px]">
                            <SelectValue/>
                        </SelectTrigger>
                        <SelectContent>
                            {providers.map(provider => (
                                <SelectItem key={provider.id} value={provider.id}>
                                    <div className="flex items-center gap-2">
                                        <img
                                            src={provider.logoPath}
                                            alt={provider.name}
                                            className="h-5 w-5 rounded"
                                        />
                                        {provider.name}
                                    </div>
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                    {currentProvider?.requiresApiKey && (
                        <Button
                            variant="outline"
                            size="icon"
                            onClick={() => setApiKeyDialogOpen(true)}
                            title={t("store.manage_api_key")}
                        >
                            <KeyIcon className="h-4 w-4"/>
                        </Button>
                    )}
                </div>
            </div>

            <div className="relative shrink-0">
                <MagnifyingGlassIcon
                    className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground"/>
                <Input
                    placeholder={t("store.search_placeholder", {type: getTypeLabel().toLowerCase()})}
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="pl-10"
                />
            </div>

            <ScrollArea className="flex-1">
                {needsApiKeyConfiguration ? (
                    <div className="flex flex-col items-center justify-center py-16 text-center">
                        <div className="h-20 w-20 rounded-xl bg-muted flex items-center justify-center mb-6">
                            <KeyIcon className="h-10 w-10 text-muted-foreground"/>
                        </div>
                        <h3 className="text-xl font-semibold mb-2">
                            {t("store.api_key_required_title")}
                        </h3>
                        <p className="text-muted-foreground max-w-md mb-6">
                            {t("store.api_key_required_description", {provider: currentProvider?.name})}
                        </p>
                        <Button onClick={() => setApiKeyDialogOpen(true)}>
                            <GearIcon className="h-4 w-4 mr-2"/>
                            {t("store.configure_api_key_button")}
                        </Button>
                    </div>
                ) : loading ? (
                    <div className="grid gap-4 md:grid-cols-1 lg:grid-cols-2">
                        {Array.from({length: 6}).map((_, i) => (
                            <Card key={i}>
                                <CardContent className="p-4">
                                    <div className="flex gap-4">
                                        <Skeleton className="h-16 w-16 rounded-lg"/>
                                        <div className="flex-1">
                                            <Skeleton className="h-5 w-3/4 mb-2"/>
                                            <Skeleton className="h-4 w-1/3 mb-3"/>
                                            <Skeleton className="h-4 w-full"/>
                                            <Skeleton className="h-4 w-2/3 mt-1"/>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                        ))}
                    </div>
                ) : searchResult && searchResult.projects.length > 0 ? (
                    <div className="grid gap-4 md:grid-cols-1 lg:grid-cols-2">
                        {searchResult.projects.map(renderProjectCard)}
                    </div>
                ) : (
                    <div className="flex flex-col items-center justify-center py-16 text-center">
                        <div className="h-16 w-16 rounded-xl bg-muted flex items-center justify-center mb-4">
                            <StorefrontIcon className="h-8 w-8 text-muted-foreground"/>
                        </div>
                        <h3 className="text-lg font-medium">{t("store.no_results")}</h3>
                        <p className="text-sm text-muted-foreground mt-1">
                            {t("store.no_results_description")}
                        </p>
                    </div>
                )}
            </ScrollArea>

            {searchResult && searchResult.totalPages > 1 && (
                <div className="flex items-center justify-center gap-2 shrink-0">
                    <Button
                        variant="outline"
                        size="sm"
                        disabled={page === 0}
                        onClick={() => setPage(p => Math.max(0, p - 1))}
                    >
                        <CaretLeftIcon className="h-4 w-4"/>
                    </Button>
                    <span className="text-sm text-muted-foreground px-4">
                        {t("store.page_info", {
                            current: page + 1,
                            total: searchResult.totalPages
                        })}
                    </span>
                    <Button
                        variant="outline"
                        size="sm"
                        disabled={page >= searchResult.totalPages - 1}
                        onClick={() => setPage(p => p + 1)}
                    >
                        <CaretRightIcon className="h-4 w-4"/>
                    </Button>
                </div>
            )}

            <Dialog open={installDialogOpen} onOpenChange={setInstallDialogOpen}>
                <DialogContent className="max-w-lg">
                    <DialogHeader>
                        <DialogTitle className="flex items-center gap-3">
                            {selectedProject?.iconUrl && (
                                <img
                                    src={selectedProject.iconUrl}
                                    alt={selectedProject.name}
                                    className="h-10 w-10 rounded-lg"
                                />
                            )}
                            {t("store.install_title", {name: selectedProject?.name})}
                        </DialogTitle>
                        <DialogDescription>
                            {t("store.install_description")}
                        </DialogDescription>
                    </DialogHeader>

                    <div className="space-y-4 py-4">
                        {loadingVersions ? (
                            <div className="space-y-2">
                                <Skeleton className="h-4 w-20"/>
                                <Skeleton className="h-10 w-full"/>
                            </div>
                        ) : projectVersions.length > 0 ? (
                            <div className="space-y-2">
                                <label className="text-sm font-medium">
                                    {t("store.select_version")}
                                </label>
                                <Select value={selectedVersion} onValueChange={setSelectedVersion}>
                                    <SelectTrigger>
                                        <SelectValue placeholder={t("store.select_version_placeholder")}/>
                                    </SelectTrigger>
                                    <SelectContent>
                                        {projectVersions.map(version => (
                                            <SelectItem key={version.id} value={version.id}>
                                                <div className="flex items-center gap-2">
                                                    <span>{version.versionNumber}</span>
                                                    <Badge
                                                        variant={version.versionType === "release" ? "default" : "secondary"}
                                                        className="text-xs"
                                                    >
                                                        {version.versionType}
                                                    </Badge>
                                                    <span className="text-xs text-muted-foreground">
                                                        {version.gameVersions[0]}
                                                    </span>
                                                </div>
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>

                                {selectedVersion && (
                                    <div className="text-xs text-muted-foreground mt-2">
                                        {(() => {
                                            const version = projectVersions.find(v => v.id === selectedVersion);
                                            if (!version) return null;
                                            const primaryFile = version.files.find(f => f.primary) || version.files[0];
                                            return (
                                                <div className="flex items-center gap-4">
                                                    <span>{formatDownloads(version.downloads)} {t("store.downloads")}</span>
                                                    {primaryFile && <span>{formatFileSize(primaryFile.size)}</span>}
                                                    <span>{formatRelativeTime(version.datePublished)}</span>
                                                </div>
                                            );
                                        })()}
                                    </div>
                                )}
                            </div>
                        ) : (
                            <p className="text-sm text-muted-foreground text-center py-4">
                                {t("store.no_compatible_versions")}
                            </p>
                        )}
                    </div>

                    <DialogFooter>
                        <Button variant="outline" onClick={() => setInstallDialogOpen(false)}>
                            {t("action.cancel")}
                        </Button>
                        <Button
                            onClick={handleInstall}
                            disabled={installing || !selectedVersion || projectVersions.length === 0}
                        >
                            {installing ? (
                                <>
                                    <CircleNotchIcon className="h-4 w-4 animate-spin mr-2"/>
                                    {t("store.installing")}
                                </>
                            ) : (
                                <>
                                    <DownloadSimpleIcon className="h-4 w-4 mr-2"/>
                                    {t("store.install")}
                                </>
                            )}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {currentProvider && (
                <ApiKeyDialog
                    open={apiKeyDialogOpen}
                    onOpenChange={setApiKeyDialogOpen}
                    providerId={currentProvider.id}
                    providerName={currentProvider.name}
                    isConfigured={currentProvider.isConfigured}
                    onSuccess={handleApiKeySuccess}
                />
            )}
        </div>
    );
};