import { WarningCircleIcon, ArrowLeftIcon } from "@phosphor-icons/react";
import { useNavigate } from "react-router-dom";
import { t } from "i18next";

const NotFound = () => {
    const navigate = useNavigate();

    return (
        <div className="flex-1 flex items-center justify-center p-8">
            <div className="flex flex-col sm:flex-row items-center gap-8 text-center sm:text-left">
                <WarningCircleIcon className="w-24 h-24 sm:w-32 sm:h-32 text-muted-foreground shrink-0" weight="duotone" />
                <div className="flex flex-col gap-1">
                    <h1 className="text-4xl font-bold tracking-tight">404</h1>
                    <h2 className="text-xl font-semibold text-muted-foreground">
                        {t("errors.notFound.title", "Page Not Found")}
                    </h2>
                    <p className="text-muted-foreground text-sm mt-1">
                        {t("errors.notFound.description", "The page you're looking for doesn't exist or has been moved.")}
                    </p>
                    <a 
                        onClick={() => navigate("/")} 
                        className="flex items-center gap-1.5 text-sm text-primary hover:underline cursor-pointer mt-3"
                    >
                        <ArrowLeftIcon className="w-4 h-4" />
                        {t("errors.notFound.backHome", "Back to Home")}
                    </a>
                </div>
            </div>
        </div>
    );
};

export default NotFound;
