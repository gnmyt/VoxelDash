import {Button} from "@/components/ui/button.tsx";
import {Dialog, DialogContent, DialogHeader, DialogTitle} from "@/components/ui/dialog.tsx";
import {useEffect, useState} from "react";
import {Input} from "@/components/ui/input.tsx";
import {patchRequest} from "@/lib/RequestUtil.ts";
import {toast} from "@/hooks/use-toast.ts";
import {File} from "@/states/Root/pages/FileManager/FileManager.tsx";
import {t} from "i18next";

interface RenameFileDialogProps {
    directory: string;
    selectedFile: File | null;
    updateFiles: () => void;
    isOpen: boolean;
    setOpen: (open: boolean) => void;
}

const RenameDialog = ({directory, selectedFile, updateFiles, isOpen, setOpen}: RenameFileDialogProps) => {
    const [newItemName, setNewItemName] = useState(selectedFile?.name ?? "");

    const handleRename = () => {
        patchRequest((selectedFile?.is_folder ? "folder" : "files") + "/rename", {path: directory + selectedFile?.name, newName: newItemName,
            newPath: directory + newItemName}).then(() => {
            setOpen(false);
            updateFiles();

            toast({description: t("files.file_renamed")});
        });
    }

    useEffect(() => {
        setNewItemName(selectedFile?.name ?? "");
    }, [selectedFile]);

    return (
        <Dialog open={isOpen} onOpenChange={setOpen}>
            <DialogContent className="sm:max-w-[425px] rounded-xl" aria-label={t("files.rename")}>
                <DialogHeader>
                    <DialogTitle className="text-lg">{t("files.rename")}</DialogTitle>
                </DialogHeader>
                <div className="grid gap-4 py-4">
                    <Input
                        value={newItemName}
                        onChange={(e) => setNewItemName(e.target.value)}
                        placeholder={t("files.enter_name")}
                        className="col-span-3 h-12 text-base rounded-xl"
                    />
                </div>
                <Button onClick={handleRename} size="lg" className="w-full h-12 text-base rounded-xl">{t("files.rename")}</Button>
            </DialogContent>
        </Dialog>
    )
}

export default RenameDialog;