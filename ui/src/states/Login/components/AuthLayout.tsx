import {ReactNode} from "react";
import {AuroraBackground} from "@/components/AuroraBackground.tsx";
import {BookOpenIcon, GithubLogoIcon} from "@phosphor-icons/react";
import logo from "@/assets/images/logo.png";
import {t} from "i18next";

interface AuthLayoutProps {
    brand: string;
    tagline: string;
    description: string;
    title: string;
    subtitle: string;
    children: ReactNode;
}

const AuthLayout = ({brand, tagline, description, title, subtitle, children}: AuthLayoutProps) => (
    <div className="relative isolate grid min-h-screen overflow-hidden bg-background text-foreground lg:grid-cols-[1.05fr_minmax(440px,0.95fr)]">
        <AuroraBackground/>

        <div className="pointer-events-none absolute inset-x-0 bottom-0 z-10 flex items-center justify-between p-6 text-muted-foreground md:p-8">
            <span className="pointer-events-auto font-mono text-xs">v{__APP_VERSION__}</span>
            <div className="pointer-events-auto flex items-center gap-1">
                <a href="https://voxeldash.dev" target="_blank" rel="noreferrer" aria-label={t("login.docs")}
                   title={t("login.docs")}
                   className="flex size-9 items-center justify-center rounded-lg transition-colors hover:bg-accent hover:text-foreground">
                    <BookOpenIcon className="size-[18px]"/>
                </a>
                <a href="https://github.com/gnmyt/VoxelDash" target="_blank" rel="noreferrer" aria-label="GitHub"
                   title="GitHub"
                   className="flex size-9 items-center justify-center rounded-lg transition-colors hover:bg-accent hover:text-foreground">
                    <GithubLogoIcon className="size-[18px]"/>
                </a>
            </div>
        </div>

        <div className="relative hidden flex-col justify-between overflow-hidden p-12 xl:p-16 lg:flex">
            <div className="flex items-center gap-3.5">
                <img src={logo} alt={brand} className="size-12 rounded-2xl shadow-sm"/>
                <div className="font-display text-xl font-bold tracking-tight">{brand}</div>
            </div>

            <div className="max-w-lg space-y-6">
                <h1 className="font-display text-[2.9rem] font-bold leading-[1.04] tracking-tight">
                    {tagline}
                </h1>
                <p className="max-w-md text-[15px] leading-relaxed text-muted-foreground">
                    {description}
                </p>
            </div>

            <div aria-hidden/>
        </div>

        <div className="relative flex items-center justify-center p-6 md:p-10">
            <div className="vd-rise w-full max-w-sm">
                <div className="mb-10 flex items-center gap-3 lg:hidden">
                    <img src={logo} alt={brand} className="size-10 rounded-xl"/>
                    <span className="font-display text-lg font-bold">{brand}</span>
                </div>

                <h2 className="font-display text-[1.75rem] font-bold leading-tight">{title}</h2>
                <p className="mb-8 mt-2 text-sm leading-relaxed text-muted-foreground">{subtitle}</p>

                {children}
            </div>
        </div>
    </div>
);

export default AuthLayout;
