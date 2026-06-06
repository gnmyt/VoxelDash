import * as React from "react";
import { PlusIcon } from "@phosphor-icons/react";
import { Button } from "@/components/ui/button";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import { CreateWorldRequest, EnvironmentType, WorldTypeOption } from "@/types/world";
import { t } from "i18next";

interface CreateWorldDialogProps {
    onCreate: (data: CreateWorldRequest) => Promise<void>;
    disabled?: boolean;
}

const CreateWorldDialog = ({ onCreate, disabled }: CreateWorldDialogProps) => {
    const [open, setOpen] = React.useState(false);
    const [worldName, setWorldName] = React.useState("");
    const [environment, setEnvironment] = React.useState<EnvironmentType>("NORMAL");
    const [worldType, setWorldType] = React.useState<WorldTypeOption>("NORMAL");
    const [seed, setSeed] = React.useState("");
    const [isSubmitting, setIsSubmitting] = React.useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        
        if (!worldName.trim()) return;
        
        setIsSubmitting(true);
        try {
            await onCreate({
                worldName: worldName.trim(),
                environment,
                worldType,
                seed: seed.trim() || undefined,
            });
            
            setWorldName("");
            setEnvironment("NORMAL");
            setWorldType("NORMAL");
            setSeed("");
            setOpen(false);
        } finally {
            setIsSubmitting(false);
        }
    };

    const isValid = worldName.trim().length > 0 && /^[a-zA-Z0-9_-]+$/.test(worldName.trim());

    return (
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
                <Button disabled={disabled} size="lg" className="h-12 px-6 rounded-xl text-base w-full sm:w-auto">
                    <PlusIcon className="h-5 w-5 mr-2" weight="bold" />
                    {t("worlds.create")}
                </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-[480px] rounded-xl">
                <form onSubmit={handleSubmit}>
                    <DialogHeader>
                        <DialogTitle className="text-lg">{t("worlds.create")}</DialogTitle>
                        <DialogDescription>
                            {t("worlds.create_description")}
                        </DialogDescription>
                    </DialogHeader>
                    
                    <div className="grid gap-5 py-6">
                        <div className="grid gap-2">
                            <Label htmlFor="worldName">{t("worlds.form.name")}</Label>
                            <Input
                                id="worldName"
                                value={worldName}
                                onChange={(e) => setWorldName(e.target.value)}
                                placeholder={t("worlds.form.name_placeholder")}
                                className="rounded-xl h-11"
                            />
                            <p className="text-xs text-muted-foreground">
                                {t("worlds.form.name_hint")}
                            </p>
                        </div>

                        <div className="grid gap-2">
                            <Label htmlFor="environment">{t("worlds.form.environment")}</Label>
                            <Select value={environment} onValueChange={(v) => setEnvironment(v as EnvironmentType)}>
                                <SelectTrigger className="rounded-xl h-11">
                                    <SelectValue placeholder={t("worlds.form.environment_placeholder")} />
                                </SelectTrigger>
                                <SelectContent className="rounded-xl">
                                    <SelectItem value="NORMAL" className="rounded-lg">
                                        {t("worlds.environment.normal")}
                                    </SelectItem>
                                    <SelectItem value="NETHER" className="rounded-lg">
                                        {t("worlds.environment.nether")}
                                    </SelectItem>
                                    <SelectItem value="THE_END" className="rounded-lg">
                                        {t("worlds.environment.the_end")}
                                    </SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="grid gap-2">
                            <Label htmlFor="worldType">{t("worlds.form.type")}</Label>
                            <Select value={worldType} onValueChange={(v) => setWorldType(v as WorldTypeOption)}>
                                <SelectTrigger className="rounded-xl h-11">
                                    <SelectValue placeholder={t("worlds.form.type_placeholder")} />
                                </SelectTrigger>
                                <SelectContent className="rounded-xl">
                                    <SelectItem value="NORMAL" className="rounded-lg">
                                        {t("worlds.type.normal")}
                                    </SelectItem>
                                    <SelectItem value="FLAT" className="rounded-lg">
                                        {t("worlds.type.flat")}
                                    </SelectItem>
                                    <SelectItem value="AMPLIFIED" className="rounded-lg">
                                        {t("worlds.type.amplified")}
                                    </SelectItem>
                                    <SelectItem value="LARGE_BIOMES" className="rounded-lg">
                                        {t("worlds.type.large_biomes")}
                                    </SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="grid gap-2">
                            <Label htmlFor="seed">{t("worlds.form.seed")}</Label>
                            <Input
                                id="seed"
                                value={seed}
                                onChange={(e) => setSeed(e.target.value)}
                                placeholder={t("worlds.form.seed_placeholder")}
                                className="rounded-xl h-11"
                            />
                            <p className="text-xs text-muted-foreground">
                                {t("worlds.form.seed_hint")}
                            </p>
                        </div>
                    </div>

                    <DialogFooter className="flex-col-reverse sm:flex-row gap-2">
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => setOpen(false)}
                            className="rounded-xl w-full sm:w-auto"
                        >
                            {t("action.cancel")}
                        </Button>
                        <Button
                            type="submit"
                            disabled={!isValid || isSubmitting}
                            className="rounded-xl w-full sm:w-auto"
                        >
                            {t("action.create")}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
};

export default CreateWorldDialog;
