export const isDesktop = (): boolean =>
    typeof window !== "undefined" &&
    ("__TAURI_INTERNALS__" in window || "__TAURI__" in window);

export const setupDesktopLinks = (): void => {
    if (!isDesktop()) return;

    const openExternally = (url: string) => {
        void import("@tauri-apps/plugin-opener")
            .then(({openUrl}) => openUrl(url))
            .catch((err) => console.error("[desktop] failed to open external url", url, err));
    };

    const nativeOpen = window.open.bind(window);
    window.open = ((url?: string | URL, ...rest: unknown[]) => {
        if (url) {
            openExternally(url.toString());
            return null;
        }
        return (nativeOpen as (...a: unknown[]) => Window | null)(url, ...rest);
    }) as typeof window.open;

    document.addEventListener("click", (event) => {
        const anchor = (event.target as HTMLElement | null)?.closest?.(
            "a[target='_blank']"
        ) as HTMLAnchorElement | null;
        if (anchor?.href) {
            event.preventDefault();
            openExternally(anchor.href);
        }
    }, true);
};
