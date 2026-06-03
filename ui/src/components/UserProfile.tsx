"use client"
import {CaretUpDownIcon, HeartIcon, TranslateIcon, SignOutIcon, MoonIcon, SunIcon} from "@phosphor-icons/react"

import {Avatar, AvatarFallback, AvatarImage,} from "@/components/ui/avatar"
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem, DropdownMenuPortal,
    DropdownMenuSeparator, DropdownMenuSub, DropdownMenuSubContent, DropdownMenuSubTrigger,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    useSidebar,
} from "@/components/ui/sidebar"
import {useMasterAuth} from "@/contexts/MasterAuthContext.tsx";
import {ServerInfoContext} from "@/contexts/ServerInfoContext.tsx";
import {isMasterMode, postRequest} from "@/lib/RequestUtil.ts";
import {useContext} from "react";
import {useNavigate} from "react-router-dom";
import {SUPPORT_URL} from "@/App.tsx";
import { useTheme } from "@/components/theme-provider"
import {languages} from "@/i18n.ts";
import Flag from 'react-world-flags';
import i18n, {t} from "i18next";

export function AccountMenuItems({onLogout}: { onLogout: () => void }) {
    const {setTheme, theme} = useTheme();

    const changeLanguage = (language: string) => {
        localStorage.setItem("language", language);
        i18n.changeLanguage(language);
    }

    return (
        <>
            <DropdownMenuGroup>
                <DropdownMenuItem className="text-red-600" onClick={() => window.open(SUPPORT_URL, "_blank")}>
                    <HeartIcon/>
                    {t("header.support_me")}
                </DropdownMenuItem>
            </DropdownMenuGroup>
            <DropdownMenuSeparator/>
            <DropdownMenuGroup>
                <DropdownMenuItem onClick={() => setTheme(theme === "dark" ? "light" : "dark")}>
                    {theme === "dark" ? <SunIcon/> : <MoonIcon/>}
                    {theme === "dark" ? t("header.light_theme") : t("header.dark_theme")}
                </DropdownMenuItem>
                <DropdownMenuSub>
                    <DropdownMenuSubTrigger>
                        <TranslateIcon/>
                        {t("header.update_language")}
                    </DropdownMenuSubTrigger>
                    <DropdownMenuPortal>
                        <DropdownMenuSubContent>
                            {languages.map((lang) => (
                                <DropdownMenuItem key={lang.code} onClick={() => changeLanguage(lang.code)}>
                                    <Flag code={lang.imageCode} className="h-4 w-4 mr-2"/>
                                    {lang.name}
                                </DropdownMenuItem>
                            ))}
                        </DropdownMenuSubContent>
                    </DropdownMenuPortal>
                </DropdownMenuSub>
            </DropdownMenuGroup>
            <DropdownMenuSeparator/>
            <DropdownMenuItem onClick={onLogout}>
                <SignOutIcon/>
                {t("header.logout")}
            </DropdownMenuItem>
        </>
    );
}

function AccountMenu({accountName, onLogout}: { accountName: string; onLogout: () => void }) {
    const {isMobile} = useSidebar();

    return (
        <SidebarMenu>
            <SidebarMenuItem>
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <SidebarMenuButton
                            className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground h-14">
                            <Avatar className="h-10 w-10">
                                <AvatarImage src={"https://minotar.net/avatar/" + accountName + ".png"} alt={accountName}/>
                                <AvatarFallback>{accountName?.charAt(0).toUpperCase()}</AvatarFallback>
                            </Avatar>
                            <div className="grid flex-1 text-left leading-tight">
                                <span className="truncate font-semibold text-base">{accountName}</span>
                                <span className="truncate text-xs text-muted-foreground">{t("header.account")}</span>
                            </div>
                            <CaretUpDownIcon className="ml-auto size-5"/>
                        </SidebarMenuButton>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent className="w-[--radix-dropdown-menu-trigger-width] min-w-56 rounded-lg"
                        side={isMobile ? "bottom" : "right"} align="end" sideOffset={4}>
                        <AccountMenuItems onLogout={onLogout}/>
                    </DropdownMenuContent>
                </DropdownMenu>
            </SidebarMenuItem>
        </SidebarMenu>
    );
}

function MasterUserProfile() {
    const {user, logout} = useMasterAuth();
    const navigate = useNavigate();

    if (!user) return null;

    return <AccountMenu accountName={user.username} onLogout={() => logout().then(() => navigate("/login"))}/>;
}

function StandaloneUserProfile() {
    const {serverInfo, checkToken} = useContext(ServerInfoContext)!;

    if (!serverInfo.accountName) return null;

    const logout = () => {
        postRequest("session/destroy", {session: localStorage.getItem("sessionToken")}).then(async () => {
            localStorage.removeItem("sessionToken");
            await checkToken();
        });
    };

    return <AccountMenu accountName={serverInfo.accountName} onLogout={logout}/>;
}

export function UserProfile() {
    return isMasterMode() ? <MasterUserProfile/> : <StandaloneUserProfile/>;
}
