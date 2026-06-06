import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle
} from "@/components/ui/dialog.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Input} from "@/components/ui/input.tsx";
import {useEffect, useState} from "react";
import {FolderIcon, HouseIcon} from "@phosphor-icons/react";
import {jsonRequest, postRequest} from "@/lib/RequestUtil.ts";
import {toast} from "@/hooks/use-toast.ts";
import {t} from "i18next";
import {File} from "@/states/Root/pages/FileManager/FileManager.tsx";

interface MoveDialogProps {
    isOpen: boolean;
    setOpen: (open: boolean) => void;
    sourcePath: string;
    fileName: string;
    isFolder: boolean;
    isCopy: boolean;
    updateFiles: () => void;
}

const MoveDialog = ({isOpen, setOpen, sourcePath, fileName, isFolder, isCopy, updateFiles}: MoveDialogProps) => {
    const [currentPath, setCurrentPath] = useState("/");
    const [folders, setFolders] = useState<File[]>([]);
    const [loading, setLoading] = useState(false);
    const [newName, setNewName] = useState(fileName);

    useEffect(() => {
        if (isOpen) {
            setCurrentPath("/");
            setNewName(fileName);
            loadFolders("/");
        }
    }, [isOpen, fileName]);

    const loadFolders = (path: string) => {
        setLoading(true);
        jsonRequest("files/list?path=." + path)
            .then((data) => {
                const folderList = data.files.filter((f: File) => f.is_folder);
                setFolders(folderList);
            })
            .catch(() => {
                toast({description: t("files.error_loading_folders"), variant: "destructive"});
            })
            .finally(() => setLoading(false));
    };

    const navigateToFolder = (folderName: string) => {
        const newPath = currentPath + folderName + "/";
        setCurrentPath(newPath);
        loadFolders(newPath);
    };

    const navigateUp = () => {
        if (currentPath === "/") return;
        const newPath = currentPath.substring(0, currentPath.length - 1).split("/").slice(0, -1).join("/") + "/";
        setCurrentPath(newPath);
        loadFolders(newPath);
    };

    const navigateToRoot = () => {
        setCurrentPath("/");
        loadFolders("/");
    };

    const handleConfirm = () => {
        const operation = isCopy ? "copy" : "move";
        const endpoint = isFolder ? `folder/${operation}` : `files/${operation}`;
        const destinationPath = currentPath + newName;

        postRequest(endpoint, {
            sourcePath: sourcePath,
            destinationPath: destinationPath
        }).then(() => {
            toast({description: isCopy ? t("files.file_copied") : t("files.file_moved")});
            setOpen(false);
            updateFiles();
        }).catch(() => {
            toast({description: t("files.operation_error"), variant: "destructive"});
        });
    };

    return (
        <Dialog open={isOpen} onOpenChange={setOpen}>
            <DialogContent className="sm:max-w-[500px] rounded-2xl">
                <DialogHeader>
                    <DialogTitle>
                        {isCopy ? t("files.copy_to") : t("files.move_to")}
                    </DialogTitle>
                    <DialogDescription>
                        {isCopy ? t("files.copy_description") : t("files.move_description")}
                    </DialogDescription>
                </DialogHeader>

                <div className="space-y-4 py-4">
                    <div>
                        <label className="text-sm font-medium mb-2 block">{t("files.name")}</label>
                        <Input
                            value={newName}
                            onChange={(e) => setNewName(e.target.value)}
                            className="rounded-lg"
                        />
                    </div>

                    <div>
                        <label className="text-sm font-medium mb-2 block">{t("files.destination")}</label>

                        <div className="border rounded-lg p-2 mb-2 bg-muted/30">
                            <div className="flex items-center gap-2 text-sm">
                                <HouseIcon className="h-4 w-4 text-primary cursor-pointer" onClick={navigateToRoot}/>
                                <span className="text-muted-foreground">/</span>
                                <span>{currentPath === "/" ? t("files.root") : currentPath}</span>
                            </div>
                        </div>

                        <div className="border rounded-lg max-h-[200px] overflow-y-auto">
                            {currentPath !== "/" && (
                                <div
                                    className="flex items-center gap-2 px-3 py-2 hover:bg-muted/50 cursor-pointer border-b"
                                    onClick={navigateUp}
                                >
                                    <FolderIcon className="h-4 w-4 text-primary" weight="fill"/>
                                    <span className="text-sm">..</span>
                                </div>
                            )}

                            {loading && (
                                <div className="px-3 py-6 text-center text-sm text-muted-foreground">
                                    {t("files.loading")}
                                </div>
                            )}

                            {!loading && folders.length === 0 && (
                                <div className="px-3 py-6 text-center text-sm text-muted-foreground">
                                    {t("files.no_folders")}
                                </div>
                            )}

                            {!loading && folders.map((folder) => (
                                <div
                                    key={folder.name}
                                    className="flex items-center gap-2 px-3 py-2 hover:bg-muted/50 cursor-pointer"
                                    onClick={() => navigateToFolder(folder.name)}
                                >
                                    <FolderIcon className="h-4 w-4 text-primary" weight="fill"/>
                                    <span className="text-sm">{folder.name}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                <DialogFooter className="flex-col-reverse sm:flex-row gap-2">
                    <Button variant="outline" onClick={() => setOpen(false)} className="rounded-lg">
                        {t("action.cancel")}
                    </Button>
                    <Button onClick={handleConfirm} className="rounded-lg" disabled={!newName.trim()}>
                        {isCopy ? t("action.copy") : t("action.move")}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default MoveDialog;
