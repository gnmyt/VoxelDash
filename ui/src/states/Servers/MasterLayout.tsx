import {ReactNode} from "react";
import {useNavigate} from "react-router-dom";
import {t} from "i18next";
import {ArrowsClockwiseIcon, GlobeSimpleIcon, HardDrivesIcon, UsersThreeIcon} from "@phosphor-icons/react";
import {useMasterAuth} from "@/contexts/MasterAuthContext.tsx";
import {UserProfile} from "@/components/UserProfile.tsx";
import {Separator} from "@/components/ui/separator.tsx";
import {
    Sidebar as ShadSidebar, SidebarContent, SidebarFooter, SidebarGroup, SidebarHeader,
    SidebarInset, SidebarMenu, SidebarMenuButton, SidebarMenuItem, SidebarProvider, SidebarTrigger,
} from "@/components/ui/sidebar.tsx";
import {AuroraBackground} from "@/components/AuroraBackground.tsx";
import logo from "@/assets/images/logo.png";

export type MasterSection = "servers" | "forwardings" | "users" | "updates";

const MasterSidebar = ({active}: { active: MasterSection }) => {
    const {can, user} = useMasterAuth();
    const navigate = useNavigate();

    const items: { key: MasterSection; label: string; icon: typeof HardDrivesIcon; path: string; show: boolean }[] = [
        {key: "servers", label: t("master.nav.servers"), icon: HardDrivesIcon, path: "/servers", show: true},
        {key: "forwardings", label: t("master.nav.forwardings"), icon: GlobeSimpleIcon, path: "/forwardings", show: can("Forwardings", 1)},
        {key: "users", label: t("master.nav.users"), icon: UsersThreeIcon, path: "/users", show: can("UserManagement", 2)},
        {key: "updates", label: t("master.nav.updates"), icon: ArrowsClockwiseIcon, path: "/updates", show: !!user?.isAdmin},
    ];

    return (
        <ShadSidebar variant="inset" className="select-none">
            <SidebarHeader>
                <SidebarMenu>
                    <SidebarMenuItem>
                        <SidebarMenuButton size="lg" className="pointer-events-none">
                            <img src={logo} alt="VoxelDash" className="size-9 shrink-0 rounded-lg"/>
                            <div className="grid flex-1 text-left leading-tight">
                                <span className="truncate font-display font-semibold">VoxelDash One</span>
                                <span className="truncate text-xs text-muted-foreground">{t("master.server_manager")}</span>
                            </div>
                        </SidebarMenuButton>
                    </SidebarMenuItem>
                </SidebarMenu>
            </SidebarHeader>
            <SidebarContent>
                <SidebarGroup>
                    <SidebarMenu>
                        {items.filter((i) => i.show).map((item) => (
                            <SidebarMenuItem key={item.key}>
                                <SidebarMenuButton isActive={item.key === active}
                                                   onClick={() => navigate(item.path)}
                                                   className="cursor-pointer">
                                    <item.icon weight={item.key === active ? "fill" : "regular"}/>
                                    <span>{item.label}</span>
                                </SidebarMenuButton>
                            </SidebarMenuItem>
                        ))}
                    </SidebarMenu>
                </SidebarGroup>
            </SidebarContent>
            <SidebarFooter>
                <UserProfile/>
            </SidebarFooter>
        </ShadSidebar>
    );
};

export const MasterLayout = ({active, title, subtitle, actions, children}: {
    active: MasterSection;
    title: string;
    subtitle?: ReactNode;
    actions?: ReactNode;
    children: ReactNode;
}) => (
    <SidebarProvider>
        <MasterSidebar active={active}/>
        <SidebarInset className="isolate flex max-h-screen flex-col overflow-hidden md:max-h-[calc(var(--app-vh)_-_1rem)]">
            <AuroraBackground/>
            <header className="flex min-h-16 shrink-0 flex-wrap items-center gap-2 px-4 py-2">
                <SidebarTrigger className="-ml-1"/>
                <Separator orientation="vertical" className="mr-1 h-4"/>
                <h1 className="font-display text-base font-semibold">{title}</h1>
                {subtitle && <span className="text-sm text-muted-foreground">{subtitle}</span>}
                {actions && <div className="ml-auto flex flex-wrap items-center gap-2">{actions}</div>}
            </header>
            <main className="min-h-0 flex-1 overflow-auto px-4 py-6 sm:px-6">
                <div className="mx-auto w-full max-w-4xl">{children}</div>
            </main>
        </SidebarInset>
    </SidebarProvider>
);
