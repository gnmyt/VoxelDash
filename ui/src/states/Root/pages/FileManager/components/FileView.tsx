import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from "@/components/ui/table.tsx";
import {
    FileIcon,
    FolderIcon,
    DotsThreeVerticalIcon,
    CaretUpIcon,
    CaretDownIcon,
    FolderOpenIcon,
    MagnifyingGlassIcon
} from "@phosphor-icons/react";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger
} from "@/components/ui/dropdown-menu.tsx";
import {
    ContextMenu,
    ContextMenuContent,
    ContextMenuItem,
    ContextMenuTrigger
} from "@/components/ui/context-menu.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Input} from "@/components/ui/input.tsx";
import {File} from "@/states/Root/pages/FileManager/FileManager.tsx";
import {convertSize} from "@/lib/FileUtil.ts";
import {useState, useRef, useEffect, KeyboardEvent} from "react";
import {downloadRequest, patchRequest, putRequest} from "@/lib/RequestUtil.ts";
import DeleteDialog from "@/states/Root/pages/FileManager/components/DeleteDialog.tsx";
import MoveDialog from "@/states/Root/pages/FileManager/components/MoveDialog.tsx";
import {toast} from "@/hooks/use-toast.ts";
import {t} from "i18next";

interface FileViewProps {
    files: File[];
    click: (file: File) => void;
    directory: string;
    updateFiles: () => void;
    creatingFolder?: boolean;
    setCreatingFolder?: (creating: boolean) => void;
    setFiles?: (files: File[]) => void;
}

type SortField = "name" | "last_modified" | "size";
type SortDirection = "asc" | "desc";

const FileView = ({
                      files,
                      click,
                      directory,
                      updateFiles,
                      creatingFolder,
                      setCreatingFolder,
                      setFiles
                  }: FileViewProps) => {
    const [isDeleteOpen, setIsDeleteOpen] = useState(false);
    const [isMoveOpen, setIsMoveOpen] = useState(false);
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [moveOperation, setMoveOperation] = useState<{isCopy: boolean}>({isCopy: false});
    const [editingFile, setEditingFile] = useState<File | null>(null);
    const [editValue, setEditValue] = useState("");
    const [newFolderName, setNewFolderName] = useState("");
    const [searchQuery, setSearchQuery] = useState("");
    const [sortField, setSortField] = useState<SortField>("name");
    const [sortDirection, setSortDirection] = useState<SortDirection>("asc");
    const inputRef = useRef<HTMLInputElement>(null);
    const newFolderInputRef = useRef<HTMLInputElement>(null);
    const hasSelectedRef = useRef(false);
    const isEditingRef = useRef(false);

    useEffect(() => {
        if (creatingFolder && newFolderInputRef.current) {
            newFolderInputRef.current.focus();
        }
    }, [creatingFolder]);

    useEffect(() => {
        if (editingFile && editValue && inputRef.current && !hasSelectedRef.current) {
            hasSelectedRef.current = true;
            setTimeout(() => {
                if (inputRef.current) {
                    inputRef.current.focus();
                    if (!editingFile.is_folder) {
                        const dotIndex = editValue.lastIndexOf(".");
                        if (dotIndex > 0) {
                            inputRef.current.setSelectionRange(0, dotIndex);
                        } else {
                            inputRef.current.select();
                        }
                    } else {
                        inputRef.current.select();
                    }
                }
            }, 0);
        }

        if (!editingFile) {
            hasSelectedRef.current = false;
        }
    }, [editingFile, editValue]);

    const handleRename = (file: File) => {
        hasSelectedRef.current = false;
        isEditingRef.current = true;
        setEditingFile(file);
        setEditValue(file.name);
        setTimeout(() => {
            isEditingRef.current = false;
        }, 100);
    }

    const handleDownload = (file: File) => {
        if (file.is_folder) {
            downloadRequest("folder/download?path=" + directory + file.name);
        } else {
            downloadRequest("files/download?path=" + directory + file.name);
        }
    }

    const handleDelete = (file: File) => {
        setSelectedFile(file);
        requestAnimationFrame(() => {
            requestAnimationFrame(() => setIsDeleteOpen(true));
        });
    }

    const handleCopy = (file: File) => {
        setSelectedFile(file);
        setMoveOperation({isCopy: true});
        requestAnimationFrame(() => {
            requestAnimationFrame(() => setIsMoveOpen(true));
        });
    }

    const handleMove = (file: File) => {
        setSelectedFile(file);
        setMoveOperation({isCopy: false});
        requestAnimationFrame(() => {
            requestAnimationFrame(() => setIsMoveOpen(true));
        });
    }

    const submitRename = () => {
        if (isEditingRef.current) return;

        if (!editingFile || editValue === editingFile.name || !editValue.trim()) {
            setEditingFile(null);
            return;
        }

        patchRequest((editingFile.is_folder ? "folder" : "files") + "/rename", {
            path: directory + editingFile.name,
            newName: editValue,
            newPath: directory + editValue
        }).then(() => {
            updateFiles();
            toast({description: t("files.file_renamed")});
        });
        setEditingFile(null);
    }

    const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Enter") {
            submitRename();
        } else if (e.key === "Escape") {
            setEditingFile(null);
        }
    }

    const submitNewFolder = () => {
        if (!newFolderName.trim()) {
            cancelNewFolder();
            return;
        }

        putRequest("folder", {path: directory + newFolderName}).then(() => {
            updateFiles();
            toast({description: t("files.create_folder.feedback")});
        });
        setNewFolderName("");
        setCreatingFolder?.(false);
    }

    const cancelNewFolder = () => {
        setNewFolderName("");
        setCreatingFolder?.(false);
        if (setFiles) {
            setFiles(files.filter(f => f.name !== ""));
        }
    }

    const handleNewFolderKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Enter") {
            submitNewFolder();
        } else if (e.key === "Escape") {
            cancelNewFolder();
        }
    }

    const handleSort = (field: SortField) => {
        if (sortField === field) {
            setSortDirection(sortDirection === "asc" ? "desc" : "asc");
        } else {
            setSortField(field);
            setSortDirection("asc");
        }
    }

    const getSortedAndFilteredFiles = () => {
        let filtered = files;

        if (searchQuery.trim()) {
            filtered = files.filter(file =>
                file.name.toLowerCase().includes(searchQuery.toLowerCase())
            );
        }

        const folders = filtered.filter(f => f.is_folder);
        const regularFiles = filtered.filter(f => !f.is_folder);

        const sortFn = (a: File, b: File) => {
            let comparison = 0;
            switch (sortField) {
                case "name":
                    comparison = a.name.localeCompare(b.name);
                    break;
                case "last_modified":
                    comparison = a.last_modified - b.last_modified;
                    break;
                case "size":
                    comparison = parseInt(a.size) - parseInt(b.size);
                    break;
            }
            return sortDirection === "asc" ? comparison : -comparison;
        };

        return [...folders.sort(sortFn), ...regularFiles.sort(sortFn)];
    }

    const SortIcon = ({field}: { field: SortField }) => {
        if (sortField !== field) return null;
        return sortDirection === "asc"
            ? <CaretUpIcon className="h-3 w-3 ml-1"/>
            : <CaretDownIcon className="h-3 w-3 ml-1"/>;
    }

    const renderMenuItems = (item: File) => (
        <>
            <ContextMenuItem onSelect={() => handleRename(item)} className="rounded-lg h-9 text-sm cursor-pointer">
                {t("files.rename")}
            </ContextMenuItem>
            <ContextMenuItem onSelect={() => handleCopy(item)} className="rounded-lg h-9 text-sm cursor-pointer">
                {t("files.copy")}
            </ContextMenuItem>
            <ContextMenuItem onSelect={() => handleMove(item)} className="rounded-lg h-9 text-sm cursor-pointer">
                {t("files.move")}
            </ContextMenuItem>
            <ContextMenuItem onSelect={() => handleDownload(item)}
                             className="rounded-lg h-9 text-sm cursor-pointer">
                {item.is_folder ? t("files.download_folder") : t("files.download")}
            </ContextMenuItem>
            <ContextMenuItem onSelect={() => handleDelete(item)}
                             className="rounded-lg h-9 text-sm cursor-pointer text-red-600">
                {t("files.delete")}
            </ContextMenuItem>
        </>
    );

    const sortedFiles = getSortedAndFilteredFiles();

    if (files.length === 0 || (files.length === 1 && files[0].name === "")) {
        if (!creatingFolder) {
            return (
                <div
                    className="rounded-xl border flex-grow overflow-hidden bg-card flex flex-col items-center justify-center py-16 gap-4">
                    <FolderOpenIcon className="h-16 w-16 text-muted-foreground"/>
                    <div className="text-center">
                        <h3 className="text-lg font-medium">{t("files.empty.title")}</h3>
                        <p className="text-sm text-muted-foreground">{t("files.empty.description")}</p>
                    </div>
                </div>
            );
        }
    }

    return (
        <div className="rounded-xl border flex-grow min-h-0 overflow-hidden bg-card flex flex-col">
            <DeleteDialog path={directory + selectedFile?.name} isOpen={isDeleteOpen} setOpen={setIsDeleteOpen}
                          updateFiles={updateFiles} isFolder={selectedFile?.is_folder ?? false}/>
            <MoveDialog
                isOpen={isMoveOpen}
                setOpen={setIsMoveOpen}
                sourcePath={directory + (selectedFile?.name ?? "")}
                fileName={selectedFile?.name ?? ""}
                isFolder={selectedFile?.is_folder ?? false}
                isCopy={moveOperation.isCopy}
                updateFiles={updateFiles}
            />

            <div className="p-3 border-b">
                <div className="relative">
                    <MagnifyingGlassIcon
                        className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground"/>
                    <Input
                        placeholder={t("files.search_placeholder")}
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="pl-9 h-9 rounded-lg"
                    />
                </div>
            </div>

            <Table className="text-sm" containerClassName="flex-1 min-h-0">
                <TableHeader className="sticky top-0 z-10 bg-card">
                    <TableRow className="hover:bg-transparent">
                        <TableHead
                            className="w-[50%] h-10 text-sm font-semibold cursor-pointer select-none hover:bg-muted/50"
                            onClick={() => handleSort("name")}
                        >
                            <div className="flex items-center">
                                {t("files.table.name")}
                                <SortIcon field="name"/>
                            </div>
                        </TableHead>
                        <TableHead
                            className="w-[12%] h-10 text-sm font-semibold cursor-pointer select-none hover:bg-muted/50"
                            onClick={() => handleSort("last_modified")}
                        >
                            <div className="flex items-center">
                                {t("files.table.last_modified")}
                                <SortIcon field="last_modified"/>
                            </div>
                        </TableHead>
                        <TableHead
                            className="w-[5%] h-10 text-sm font-semibold cursor-pointer select-none hover:bg-muted/50"
                            onClick={() => handleSort("size")}
                        >
                            <div className="flex items-center">
                                {t("files.table.size")}
                                <SortIcon field="size"/>
                            </div>
                        </TableHead>
                        <TableHead
                            className="w-[4%] h-10 text-sm font-semibold text-right">{t("files.table.actions")}</TableHead>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {creatingFolder && (
                        <TableRow className="h-10">
                            <TableCell className="font-medium py-2">
                                <div className="flex items-center gap-2">
                                    <FolderIcon className="h-4 w-4 text-primary flex-shrink-0" weight="fill"/>
                                    <Input
                                        ref={newFolderInputRef}
                                        value={newFolderName}
                                        onChange={(e) => setNewFolderName(e.target.value)}
                                        onBlur={submitNewFolder}
                                        onKeyDown={handleNewFolderKeyDown}
                                        placeholder={t("files.create_folder.placeholder")}
                                        className="h-7 text-sm py-0 px-2"
                                    />
                                </div>
                            </TableCell>
                            <TableCell className="py-2 text-muted-foreground text-sm">-</TableCell>
                            <TableCell className="py-2 text-muted-foreground text-sm">-</TableCell>
                            <TableCell className="text-right py-2"></TableCell>
                        </TableRow>
                    )}
                    {sortedFiles.length === 0 && searchQuery && (
                        <TableRow>
                            <TableCell colSpan={4} className="text-center py-8 text-muted-foreground">
                                {t("files.no_results")}
                            </TableCell>
                        </TableRow>
                    )}
                    {
                        sortedFiles.filter(item => item.name !== "").map((item) => (
                            <ContextMenu key={item.name}>
                                <ContextMenuTrigger asChild>
                                    <TableRow
                                        onClick={() => editingFile?.name !== item.name && click(item)}
                                        className="cursor-pointer h-10 transition-colors"
                                    >
                                        <TableCell className="font-medium py-2">
                                            <div className="flex items-center gap-2">
                                                {item.is_folder ? (
                                                    <FolderIcon className="h-4 w-4 text-primary flex-shrink-0"
                                                                weight="fill"/>
                                                ) : (
                                                    <FileIcon className="h-4 w-4 text-muted-foreground flex-shrink-0"/>
                                                )}
                                                {editingFile?.name === item.name ? (
                                                    <Input
                                                        ref={inputRef}
                                                        value={editValue}
                                                        onChange={(e) => setEditValue(e.target.value)}
                                                        onBlur={submitRename}
                                                        onKeyDown={handleKeyDown}
                                                        onClick={(e) => e.stopPropagation()}
                                                        className="h-7 text-sm py-0 px-2"
                                                    />
                                                ) : (
                                                    <span className="text-sm">{item.name}</span>
                                                )}
                                            </div>
                                        </TableCell>
                                        <TableCell
                                            className="py-2 text-muted-foreground text-sm">{new Date(item.last_modified).toLocaleString()}</TableCell>
                                        <TableCell
                                            className="py-2 text-muted-foreground text-sm">{!item.is_folder && convertSize(parseInt(item.size))}</TableCell>
                                        <TableCell className="text-right py-2">
                                            <DropdownMenu>
                                                <DropdownMenuTrigger asChild>
                                                    <Button
                                                        variant="ghost"
                                                        className="h-8 w-8 p-0 rounded-lg"
                                                        onClick={(event) => event.stopPropagation()}>
                                                        <span className="sr-only">{t("files.open_menu")}</span>
                                                        <DotsThreeVerticalIcon className="h-4 w-4"/>
                                                    </Button>
                                                </DropdownMenuTrigger>
                                                <DropdownMenuContent
                                                    align="end"
                                                    className="w-[160px] rounded-xl p-1.5"
                                                    onClick={(event) => event.stopPropagation()}>
                                                    <DropdownMenuItem onClick={() => handleRename(item)}
                                                                      className="rounded-lg h-9 text-sm cursor-pointer">{t("files.rename")}</DropdownMenuItem>
                                                    <DropdownMenuItem onClick={() => handleCopy(item)}
                                                                      className="rounded-lg h-9 text-sm cursor-pointer">{t("files.copy")}</DropdownMenuItem>
                                                    <DropdownMenuItem onClick={() => handleMove(item)}
                                                                      className="rounded-lg h-9 text-sm cursor-pointer">{t("files.move")}</DropdownMenuItem>
                                                    <DropdownMenuItem onClick={() => handleDownload(item)}
                                                                      className="rounded-lg h-9 text-sm cursor-pointer">{item.is_folder ? t("files.download_folder") : t("files.download")}</DropdownMenuItem>
                                                    <DropdownMenuItem onClick={() => handleDelete(item)}
                                                                      className="rounded-lg h-9 text-sm cursor-pointer text-red-600">{t("files.delete")}</DropdownMenuItem>
                                                </DropdownMenuContent>
                                            </DropdownMenu>
                                        </TableCell>
                                    </TableRow>
                                </ContextMenuTrigger>
                                <ContextMenuContent
                                    className="w-[160px] rounded-xl p-1.5"
                                    onCloseAutoFocus={(e) => e.preventDefault()}
                                >
                                    {renderMenuItems(item)}
                                </ContextMenuContent>
                            </ContextMenu>
                        ))}
                </TableBody>
            </Table>
        </div>
    )
}

export default FileView;