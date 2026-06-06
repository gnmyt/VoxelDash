import {
    ArchiveIcon,
    CalendarIcon,
    ChatCircleTextIcon,
    FolderOpenIcon,
    GearSixIcon,
    GlobeHemisphereWestIcon,
    HardDrivesIcon,
    Icon,
    PackageIcon,
    PuzzlePieceIcon,
    SquaresFourIcon,
    TerminalIcon,
    UsersIcon,
    UsersThreeIcon,
    WifiHighIcon
} from "@phosphor-icons/react";
import {t} from "i18next";
import FileManager from "@/states/Root/pages/FileManager/FileManager.tsx";
import Overview from "@/states/Root/pages/Overview/Overview.tsx";
import ServerSettings from "@/states/Root/pages/ServerSettings/ServerSettings.tsx";
import SSHSettings from "@/states/Root/pages/SSHSettings/SSHSettings.tsx";
import Backups from "@/states/Root/pages/Backups/Backups.tsx";
import Console from "@/states/Root/pages/Console/Console.tsx";
import Players from "@/states/Root/pages/Players/Players.tsx";
import Users from "@/states/Root/pages/Users/Users.tsx";
import Schedules from "@/states/Root/pages/Schedules/Schedules.tsx";
import Worlds from "@/states/Root/pages/Worlds/Worlds.tsx";
import Motd from "@/states/Root/pages/Motd/Motd.tsx";

export const getResourceIcon = (typeIdentifier: string): Icon => {
    switch (typeIdentifier) {
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

export const sidebar = [
    {
        path: "/",
        icon: SquaresFourIcon,
        element: <Overview />,
        name: () => t("nav.overview")
    },
    {
        path: "/players",
        icon: UsersIcon,
        requiredFeatures: ["Players"],
        element: <Players />,
        name: () => t("nav.players")
    },
    {
        routerPath: "/files/*",
        path: "/files/",
        icon: FolderOpenIcon,
        requiredFeatures: ["FileManager"],
        element: <FileManager/>,
        name: () => t("nav.files")
    },
    {
        path: "/console",
        icon: TerminalIcon,
        requiredFeatures: ["Console"],
        element: <Console />,
        name: () => t("nav.console")
    },
    {
        path: "/motd",
        icon: ChatCircleTextIcon,
        requiredFeatures: ["Motd"],
        element: <Motd/>,
        name: () => t("nav.motd")
    },
    {
        path: "/worlds",
        icon: GlobeHemisphereWestIcon,
        requiredFeatures: ["Worlds"],
        element: <Worlds />,
        name: () => t("nav.worlds")
    },
    {
        path: "/backups",
        name: () => t("nav.backups"),
        icon: ArchiveIcon,
        requiredFeatures: ["Backups"],
        element: <Backups />
    },
    {
        path: "/schedules",
        name: () => t("nav.schedules"),
        icon: CalendarIcon,
        requiredFeatures: ["Schedules"],
        element: <Schedules />
    },
    {
        path: "/settings",
        name: () => t("nav.settings.base"),
        icon: GearSixIcon,
        items: [
            {
                path: "/settings/server",
                name: () => t("nav.settings.server"),
                icon: HardDrivesIcon,
                requiredFeatures: ["Properties"],
                element: <ServerSettings />
            },
            {
                path: "/settings/ssh",
                name: () => t("nav.settings.ssh"),
                icon: WifiHighIcon,
                requiredFeatures: ["SSH"],
                element: <SSHSettings />
            },
            {
                path: "/settings/users",
                name: () => t("nav.settings.users"),
                icon: UsersThreeIcon,
                requiredFeatures: ["UserManagement"],
                element: <Users />
            }
        ]
    }
];

export const getLocationByPath = (path: string) => {
    for (const item of sidebar) {
        if (item.path !== "/" && path.startsWith(item.path)) return item;
        if (item.path === path) return item;
        if (item.items) {
            for (const subitem of item.items) {
                if (subitem.path === path) return subitem;
            }
        }
    }
    return null;
}

export const getRoutes = () => {
    const routes = [];
    for (const item of sidebar) {
        if (item.items) {
            for (const subitem of item.items) {
                routes.push(subitem);
            }
        } else {
            const newItem = {...item};
            newItem.path = item.routerPath || item.path;
            routes.push(newItem);
        }
    }
    return routes;
}