import {useEffect, useState} from "react";
import {useRegisterSW} from "virtual:pwa-register/react";
import {AnimatePresence, motion} from "motion/react";
import {ArrowsClockwiseIcon, DownloadSimpleIcon, RocketLaunchIcon, XIcon} from "@phosphor-icons/react";
import {Button} from "@/components/ui/button.tsx";
import {t} from "i18next";

interface BeforeInstallPromptEvent extends Event {
    prompt: () => Promise<void>;
    userChoice: Promise<{outcome: "accepted" | "dismissed"}>;
}

const INSTALL_DISMISS_KEY = "pwa-install-dismissed";

const Sheet = ({children}: { children: React.ReactNode }) => (
    <motion.div
        initial={{opacity: 0, y: 24}}
        animate={{opacity: 1, y: 0}}
        exit={{opacity: 0, y: 24}}
        transition={{duration: 0.25, ease: [0.22, 1, 0.36, 1]}}
        className="pointer-events-auto fixed inset-x-0 bottom-0 z-[60] mx-auto flex w-full max-w-md items-center gap-3
                   rounded-t-2xl border border-border/60 bg-card/95 px-4 py-3 shadow-2xl backdrop-blur
                   sm:bottom-4 sm:rounded-2xl"
        style={{paddingBottom: "max(0.75rem, env(safe-area-inset-bottom))"}}
    >
        {children}
    </motion.div>
);

export const PWAManager = () => {
    const {needRefresh: [needRefresh, setNeedRefresh], updateServiceWorker} = useRegisterSW();
    const [installEvent, setInstallEvent] = useState<BeforeInstallPromptEvent | null>(null);

    useEffect(() => {
        const handler = (e: Event) => {
            e.preventDefault();
            if (localStorage.getItem(INSTALL_DISMISS_KEY) === "1") return;
            setInstallEvent(e as BeforeInstallPromptEvent);
        };
        window.addEventListener("beforeinstallprompt", handler);
        const installed = () => setInstallEvent(null);
        window.addEventListener("appinstalled", installed);
        return () => {
            window.removeEventListener("beforeinstallprompt", handler);
            window.removeEventListener("appinstalled", installed);
        };
    }, []);

    const install = async () => {
        if (!installEvent) return;
        await installEvent.prompt();
        await installEvent.userChoice;
        setInstallEvent(null);
    };

    const dismissInstall = () => {
        localStorage.setItem(INSTALL_DISMISS_KEY, "1");
        setInstallEvent(null);
    };

    return (
        <AnimatePresence mode="wait">
            {needRefresh ? (
                <Sheet key="update">
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                        <ArrowsClockwiseIcon className="size-5 text-primary"/>
                    </div>
                    <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium">{t("pwa.update_available")}</p>
                        <p className="truncate text-xs text-muted-foreground">{t("pwa.update_desc")}</p>
                    </div>
                    <div className="flex shrink-0 items-center gap-1.5">
                        <Button size="sm" variant="ghost" onClick={() => setNeedRefresh(false)}>
                            {t("pwa.dismiss")}
                        </Button>
                        <Button size="sm" className="vd-press" onClick={() => updateServiceWorker(true)}>
                            <ArrowsClockwiseIcon className="mr-1.5 size-4"/> {t("pwa.reload")}
                        </Button>
                    </div>
                </Sheet>
            ) : installEvent ? (
                <Sheet key="install">
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                        <RocketLaunchIcon className="size-5 text-primary"/>
                    </div>
                    <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium">{t("pwa.install_title")}</p>
                        <p className="truncate text-xs text-muted-foreground">{t("pwa.install_desc")}</p>
                    </div>
                    <div className="flex shrink-0 items-center gap-1.5">
                        <Button size="icon" variant="ghost" className="size-8" onClick={dismissInstall}
                                aria-label={t("pwa.not_now")}>
                            <XIcon className="size-4"/>
                        </Button>
                        <Button size="sm" className="vd-press" onClick={install}>
                            <DownloadSimpleIcon className="mr-1.5 size-4"/> {t("pwa.install")}
                        </Button>
                    </div>
                </Sheet>
            ) : null}
        </AnimatePresence>
    );
};
