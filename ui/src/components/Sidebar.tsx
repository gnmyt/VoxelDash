import {
    CaretRightIcon,
    CaretUpDownIcon,
    SquaresFourIcon,
    StorefrontIcon
} from "@phosphor-icons/react";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu.tsx";
import {useServerSelection} from "@/contexts/ServerSelectionContext.tsx";
import {isMasterMode} from "@/lib/RequestUtil.ts";
import {softwareMeta, statusMeta} from "@/lib/servers.ts";

import {
    Sidebar as ShadSidebar,
    SidebarContent, SidebarFooter,
    SidebarGroup,
    SidebarHeader,
    SidebarMenu,
    SidebarMenuAction,
    SidebarMenuButton,
    SidebarMenuItem,
    SidebarMenuSub,
    SidebarMenuSubButton,
    SidebarMenuSubItem,
} from "@/components/ui/sidebar";
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from "@/components/ui/collapsible.tsx";
import {sidebar, getResourceIcon} from "@/states/Root/routes.tsx";
import {useContext, useState, useMemo} from "react";
import {Link, useLocation, useNavigate} from "react-router-dom";
import {ServerInfoContext} from "@/contexts/ServerInfoContext.tsx";
import {ResourcesContext} from "@/contexts/ResourcesContext.tsx";
import {UserProfile} from "@/components/UserProfile.tsx";
import logo from "@/assets/images/logo.png";
import {t} from "i18next";

function ServerSwitcher() {
    return isMasterMode() ? <MasterServerSwitcher/> : <StandaloneServerHeader/>;
}

function StandaloneServerHeader() {
    return (
        <SidebarMenu>
            <SidebarMenuItem>
                <SidebarMenuButton size="lg" asChild>
                    <Link to="/" className="flex items-center gap-2 cursor-pointer">
                        <img src={logo} alt="VoxelDash" className="size-9 shrink-0 rounded-lg"/>
                        <div className="grid flex-1 text-left leading-tight">
                            <span className="truncate font-display font-semibold">VoxelDash</span>
                            <span className="truncate text-xs text-muted-foreground">{__APP_VERSION__}</span>
                        </div>
                    </Link>
                </SidebarMenuButton>
            </SidebarMenuItem>
        </SidebarMenu>
    );
}

function MasterServerSwitcher() {
    const {servers, activeServer, selectServer} = useServerSelection();
    const navigate = useNavigate();
    const meta = softwareMeta(activeServer?.software || "");

    return (
        <SidebarMenu>
            <SidebarMenuItem>
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <SidebarMenuButton size="lg"
                            className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground">
                            <div className="flex size-9 shrink-0 items-center justify-center rounded-lg text-xs font-bold text-white"
                                 style={{backgroundColor: meta.accent}}>
                                {meta.short}
                            </div>
                            <div className="grid flex-1 text-left leading-tight">
                                <span className="truncate font-display font-semibold">{activeServer?.name || t("nav.select_server")}</span>
                                <span className="truncate text-xs text-muted-foreground">
                                    {meta.name}{activeServer?.mcVersion ? ` · ${activeServer.mcVersion}` : ""}
                                </span>
                            </div>
                            <CaretUpDownIcon className="ml-auto size-4"/>
                        </SidebarMenuButton>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent className="w-[--radix-dropdown-menu-trigger-width] min-w-60 rounded-lg"
                        align="start" side="bottom" sideOffset={4}>
                        {servers.map((server) => {
                            const sm = softwareMeta(server.software);
                            const st = statusMeta(server.status);
                            return (
                                <DropdownMenuItem key={server.id} className="gap-2"
                                    onClick={() => { selectServer(server.id); navigate("/"); }}>
                                    <div className="flex size-6 items-center justify-center rounded text-[10px] font-bold text-white"
                                         style={{backgroundColor: sm.accent}}>
                                        {sm.short}
                                    </div>
                                    <span className="flex-1 truncate">{server.name}</span>
                                    <span className={`size-2 rounded-full ${st.dot}`}/>
                                </DropdownMenuItem>
                            );
                        })}
                        <DropdownMenuSeparator/>
                        <DropdownMenuItem onClick={() => navigate("/servers")}>
                            <SquaresFourIcon className="mr-2 size-4"/> {t("nav.manage_servers")}
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </SidebarMenuItem>
        </SidebarMenu>
    );
}

export function Sidebar() {
    const [openItems, setOpenItems] = useState<Record<string, boolean>>({});
    const {serverInfo} = useContext(ServerInfoContext)!;
    const resourcesContext = useContext(ResourcesContext);
    const navigate = useNavigate();
    const location = useLocation();

    const toggleCollapsible = (path: string) =>
        setOpenItems((prev) => ({...prev, [path]: !prev[path]}));

    const isFeatureAvailable = (requiredFeatures?: string[]) =>
        !requiredFeatures || requiredFeatures.every((feature) => serverInfo.availableFeatures?.includes(feature));

    const isCurrentRoute = (path: string) => {
        if (path !== "/" && location.pathname.startsWith(path)) return true;
        if (location.pathname === path) return true;
    }

    const handleNavigationClick = (item: { items?: { path: string; name: () => string }[]; path: string }) => {
        if (item.items?.length) {
            toggleCollapsible(item.path);
        }  else {
            navigate(item.path);
        }
    }

    const capitalize = (str: string) => str.charAt(0).toUpperCase() + str.slice(1);

    const resourceSidebarItems = useMemo(() => {
        if (!resourcesContext || !serverInfo.availableFeatures?.includes("Resources")) {
            return [];
        }

        return resourcesContext.resourceTypes.map((resourceType) => {
            const TypeIcon = getResourceIcon(resourceType.identifier);
            const resources = resourcesContext.resourcesByType[resourceType.identifier] || [];
            
            return {
                path: `/resources/${resourceType.identifier}`,
                icon: TypeIcon,
                requiredFeatures: ["Resources"],
                name: () => t(`resources.types.${resourceType.identifier}`, capitalize(resourceType.identifier) + "s"),
                items: [
                    {
                        path: `/resources/${resourceType.identifier}/store`,
                        name: () => t("resources.store"),
                        icon: StorefrontIcon,
                        requiredFeatures: ["Resources"]
                    },
                    ...resources.map((resource) => ({
                        path: `/resources/${resourceType.identifier}/${encodeURIComponent(resource.fileName)}`,
                        name: () => resource.name,
                        icon: TypeIcon,
                        requiredFeatures: ["Resources"],
                        enabled: resource.enabled
                    }))
                ]
            };
        });
    }, [resourcesContext?.resourceTypes, resourcesContext?.resourcesByType, serverInfo.availableFeatures]);

    const allSidebarItems = useMemo(() => {
        const schedulesIndex = sidebar.findIndex(item => item.path === "/schedules");
        const insertIndex = schedulesIndex !== -1 ? schedulesIndex + 1 : sidebar.length - 1;
        
        const result = [...sidebar] as unknown[];
        result.splice(insertIndex, 0, ...resourceSidebarItems);
        return result as typeof sidebar;
    }, [resourceSidebarItems]);

    return (
        <ShadSidebar variant="inset" className="select-none">
            <SidebarHeader>
                <ServerSwitcher/>
            </SidebarHeader>
            <SidebarContent>
                <SidebarGroup>
                    <SidebarMenu>
                        {allSidebarItems.map((item) => {
                            if (!isFeatureAvailable(item.requiredFeatures)) return null;

                            const visibleSubItems = item.items?.filter(subItem =>
                                isFeatureAvailable(subItem.requiredFeatures)
                                && !(isMasterMode() && subItem.path === "/settings/users"));

                            if (item.items && (!visibleSubItems || visibleSubItems.length === 0)) return null;

                            const isOpen = openItems[item.path] || false;

                            return (
                                <Collapsible key={item.path} asChild open={isOpen}>
                                    <SidebarMenuItem>
                                        <SidebarMenuButton
                                            asChild
                                            isActive={isCurrentRoute(item.path) && !visibleSubItems?.length}
                                            tooltip={item.name()}
                                            onClick={() => handleNavigationClick(item)}
                                            className="cursor-pointer">
                                            <a className="flex items-center gap-3">
                                                <item.icon weight={isCurrentRoute(item.path) ? "fill" : "regular"} />
                                                <span>{item.name()}</span>
                                            </a>
                                        </SidebarMenuButton>

                                        {visibleSubItems?.length ? (
                                            <>
                                                <CollapsibleTrigger asChild>
                                                    <SidebarMenuAction
                                                        className={`transition-transform duration-200 ${isOpen ? "rotate-90" : ""}`}
                                                        onClick={() => toggleCollapsible(item.path)}>
                                                        <CaretRightIcon/>
                                                        <span className="sr-only">{t("nav.toggle")}</span>
                                                    </SidebarMenuAction>
                                                </CollapsibleTrigger>
                                                <CollapsibleContent>
                                                    <SidebarMenuSub>
                                                        {visibleSubItems.map((subItem: any) => (
                                                            <SidebarMenuSubItem key={subItem.path}>
                                                                <SidebarMenuSubButton
                                                                    asChild
                                                                    isActive={isCurrentRoute(subItem.path)}
                                                                    onClick={() => handleNavigationClick(subItem)}
                                                                    className={`cursor-pointer ${subItem.enabled === false ? "opacity-50" : ""}`}>
                                                                    <a className="flex items-center gap-3">
                                                                        <subItem.icon weight={isCurrentRoute(subItem.path) ? "fill" : "regular"} />
                                                                        <span className="flex-1">{subItem.name()}</span>
                                                                    </a>
                                                                </SidebarMenuSubButton>
                                                            </SidebarMenuSubItem>
                                                        ))}
                                                    </SidebarMenuSub>
                                                </CollapsibleContent>
                                            </>
                                        ) : null}
                                    </SidebarMenuItem>
                                </Collapsible>
                            );
                        })}
                    </SidebarMenu>
                </SidebarGroup>
            </SidebarContent>
            <SidebarFooter>
                <UserProfile/>
            </SidebarFooter>
        </ShadSidebar>
    );
}