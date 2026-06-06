import {
    Breadcrumb,
    BreadcrumbItem,
    BreadcrumbLink,
    BreadcrumbList,
    BreadcrumbSeparator
} from "@/components/ui/breadcrumb.tsx";
import {HouseIcon, FloppyDiskIcon, XIcon, PlusIcon, FileIcon, FolderIcon, FolderOpenIcon} from "@phosphor-icons/react";
import {Fragment, useState} from "react";
import {Button} from "@/components/ui/button.tsx";
import {Progress} from "@/components/ui/progress.tsx";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger
} from "@/components/ui/dropdown-menu.tsx";
import {uploadChunks, uploadFolder} from "@/lib/RequestUtil.ts";
import {toast} from "@/hooks/use-toast.ts";
import {t} from "i18next";

interface FileHeaderProps {
    currentFile: string | null;
    directory: string;
    setDirectory: (directory: string) => void;
    setCurrentFile: (file: string | null) => void;
    updateFiles: () => void;
    fileContent: string | undefined;
    onCreateFolder: () => void;
    saveFile: () => void;
}

const FileHeader = ({currentFile, directory, setDirectory, setCurrentFile, updateFiles, fileContent: _fileContent, onCreateFolder, saveFile}: FileHeaderProps) => {
    const [uploading, setUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [uploadLabel, setUploadLabel] = useState("");

    const uploadFile = () => {
        const input = document.createElement("input");
        input.type = "file";
        input.accept = "*/*";
        input.multiple = true;
        input.style.display = "none";

        input.onchange = async (e: Event) => {
            const target = e.target as HTMLInputElement;
            if (target.files && target.files.length > 0) {
                const files = target.files;
                setUploading(true);
                setUploadLabel(files.length === 1 ? files[0].name : `${files.length} ${t("files.files")}`);
                
                try {
                    for (let i = 0; i < files.length; i++) {
                        await uploadChunks(files[i], directory, (p) => {
                            const fileProgress = (i / files.length) * 100 + (p / files.length);
                            setUploadProgress(Math.round(fileProgress));
                        });
                    }
                    toast({description: files.length === 1 ? t("files.file_uploaded") : t("files.files_uploaded")});
                } catch {
                    toast({description: t("files.upload_error"), variant: "destructive"});
                } finally {
                    setUploading(false);
                    setUploadProgress(0);
                    updateFiles();
                }
            }
        };

        input.click();
    };

    const uploadFolderHandler = () => {
        const input = document.createElement("input");
        input.type = "file";
        input.style.display = "none";
        input.setAttribute("webkitdirectory", "");
        input.setAttribute("directory", "");

        input.onchange = async (e: Event) => {
            const target = e.target as HTMLInputElement;
            if (target.files && target.files.length > 0) {
                const files = target.files;
                const firstPath = (files[0] as File & { webkitRelativePath?: string }).webkitRelativePath || "";
                const folderName = firstPath.split("/")[0] || t("files.folder");
                
                setUploading(true);
                setUploadLabel(folderName);
                
                try {
                    await uploadFolder(files, directory, setUploadProgress);
                    toast({description: t("files.folder_uploaded")});
                } catch {
                    toast({description: t("files.folder_upload_error"), variant: "destructive"});
                } finally {
                    setUploading(false);
                    setUploadProgress(0);
                    updateFiles();
                }
            }
        };

        input.click();
    };

    const getPathArray = () => {
        const strings = directory.split("/").splice(0, directory.split("/").length - 1);
        if (strings.length === 0) return ["/"];
        if (strings[0] === "") strings.splice(0, 1);
        if (currentFile) strings.push(currentFile);
        return strings;
    }

    return (
        <>
            <div className="flex flex-wrap items-center justify-between gap-3 py-2">
                <Breadcrumb className="min-w-0 flex-1 overflow-x-auto">
                    <BreadcrumbList className="text-base gap-2 flex-nowrap">
                        <HouseIcon className="h-5 w-5 text-primary cursor-pointer transition-colors hover:text-primary/80" onClick={() => !currentFile && setDirectory("/")}/>
                        <BreadcrumbSeparator className="hidden md:block"/>

                        {getPathArray().map((dir, index) => (
                            <Fragment key={index}>
                                {dir !== currentFile && <>
                                    <BreadcrumbItem key={index} className="cursor-pointer">
                                        <BreadcrumbLink
                                            onClick={() => !currentFile && setDirectory(directory.split("/").splice(0, index + 2).join("/") + "/")}>{dir}</BreadcrumbLink>
                                    </BreadcrumbItem>
                                    <BreadcrumbSeparator className="hidden md:block"/>
                                </>}
                                {dir === currentFile && <BreadcrumbItem key={index}>
                                    <BreadcrumbLink>{dir}</BreadcrumbLink>
                                </BreadcrumbItem>}
                            </Fragment>
                        ))}
                    </BreadcrumbList>
                </Breadcrumb>
                
                {currentFile === null && !uploading && (
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button size="lg" className="rounded-xl h-12 px-5 text-base">
                                <PlusIcon className="mr-2 h-5 w-5"/> {t("files.new")}
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="w-[200px] rounded-xl p-1.5">
                            <DropdownMenuItem onClick={uploadFile} className="rounded-lg h-10 text-sm cursor-pointer">
                                <FileIcon className="mr-2 h-4 w-4"/> {t("files.upload_file")}
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={uploadFolderHandler} className="rounded-lg h-10 text-sm cursor-pointer">
                                <FolderOpenIcon className="mr-2 h-4 w-4"/> {t("files.upload_folder")}
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={onCreateFolder} className="rounded-lg h-10 text-sm cursor-pointer">
                                <FolderIcon className="mr-2 h-4 w-4"/> {t("files.new_folder")}
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                )}
                
                {currentFile !== null && (
                    <div className="flex gap-2 shrink-0">
                        <Button variant="outline" size="lg" className="rounded-xl h-12 px-5 text-base" onClick={saveFile}>
                            <FloppyDiskIcon className="mr-2 h-5 w-5"/> {t("files.save_file")}
                        </Button>
                        <Button variant="destructive" size="lg" className="rounded-xl h-12 px-5 text-base" onClick={() => setCurrentFile(null)}>
                            <XIcon className="mr-2 h-5 w-5"/> {t("action.exit")}
                        </Button>
                    </div>
                )}
            </div>
            
            {uploading && (
                <div className="flex items-center gap-3 bg-muted/50 rounded-lg p-3">
                    <span className="text-sm text-muted-foreground truncate max-w-[200px]">
                        {t("files.uploading")}: {uploadLabel}
                    </span>
                    <Progress value={uploadProgress} className="flex-1"/>
                    <span className="text-sm font-medium min-w-[3rem] text-right">
                        {uploadProgress}%
                    </span>
                </div>
            )}
        </>
    )
}

export default FileHeader;