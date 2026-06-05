import {useCallback, useEffect, useState} from "react";
import FileView from "@/states/Root/pages/FileManager/components/FileView.tsx";
import {downloadRequest, jsonRequest, patchRequest} from "@/lib/RequestUtil.ts";
import { useLocation, useNavigate } from "react-router-dom";
import FileHeader from "@/states/Root/pages/FileManager/components/FileHeader.tsx";
import FileEditor from "@/states/Root/pages/FileManager/components/FileEditor.tsx";
import {Skeleton} from "@/components/ui/skeleton.tsx";
import {toast} from "@/hooks/use-toast.ts";
import {t} from "i18next";

export interface File {
    name: string;
    size: string;
    last_modified: number;
    is_folder: boolean;
}

const FileManager = () => {
    const [files, setFiles] = useState<File[]>([]);
    const [loading, setLoading] = useState(true);
    const location = useLocation();
    
    const getInitialState = () => {
        const fullPath = location.pathname.substring(6);
        const fileExtensions = ['.yml', '.yaml', '.json', '.txt', '.properties', '.conf', '.cfg', '.log', '.md', '.sh', '.bat'];
        const hasFileExtension = fileExtensions.some(ext => fullPath.toLowerCase().endsWith(ext));
        
        if (hasFileExtension) {
            const lastSlash = fullPath.lastIndexOf('/');
            const dir = lastSlash > 0 ? fullPath.substring(0, lastSlash + 1) : '/';
            const file = fullPath.substring(lastSlash + 1);
            return { dir, file };
        }
        
        return { dir: fullPath || '/', file: null };
    };
    
    const initialState = getInitialState();
    const [directory, setDirectory] = useState(initialState.dir);
    const [currentFile, setCurrentFile] = useState<string | null>(initialState.file);
    const [fileContent, setFileContent] = useState<string | undefined>("");
    const [creatingFolder, setCreatingFolder] = useState(false);

    const navigate = useNavigate();

    const changeDirectory = useCallback((newDirectory: string) => {
        if (currentFile !== null) setCurrentFile(null);

        if (newDirectory === "..") {
            if (directory === "/") return;
            setDirectory(directory.substring(0, directory.length - 1).split("/").slice(0, -1).join("/") + "/");
        } else {
            setDirectory(directory + newDirectory + "/");
        }
    }, [currentFile, directory]);

    const onClick = (file: File) => {
        if (file.is_folder) return changeDirectory(file.name);
        if (parseInt(file.size) > 1000000) return downloadRequest("files/download?path=" + directory + file.name);

        setCurrentFile(file.name);
    };

    const updateFiles = () => {
        setLoading(true);
        jsonRequest("files/list?path=." + directory)
            .then((data) => setFiles(data.files.sort((a: File, b: File) => (b.is_folder ? 1 : 0) - (a.is_folder ? 1 : 0))))
            .catch(() => changeDirectory(".."))
            .finally(() => setLoading(false));
    }

    const handleCreateFolder = () => {
        const tempFolder: File = {
            name: "",
            size: "0",
            last_modified: Date.now(),
            is_folder: true
        };
        setFiles([tempFolder, ...files]);
        setCreatingFolder(true);
    }

    const saveFile = useCallback(() => {
        if (!currentFile) return;
        patchRequest("files/content", {path: directory + currentFile, content: fileContent}).then(() => {
            toast({description: t("files.file_saved")});
        });
    }, [currentFile, directory, fileContent]);

    useEffect(() => {
        navigate("/files" + directory);
        updateFiles();
    }, [directory, location.pathname]);

    const LoadingSkeleton = () => (
        <div className="rounded-xl border flex-grow overflow-hidden bg-card">
            <div className="p-4 space-y-3">
                <div className="flex items-center gap-4">
                    <Skeleton className="h-4 w-[50%]" />
                    <Skeleton className="h-4 w-[15%]" />
                    <Skeleton className="h-4 w-[10%]" />
                    <Skeleton className="h-4 w-[5%] ml-auto" />
                </div>
                {[...Array(8)].map((_, i) => (
                    <div key={i} className="flex items-center gap-4">
                        <Skeleton className="h-4 w-4 rounded" />
                        <Skeleton className="h-4 w-[45%]" />
                        <Skeleton className="h-4 w-[15%]" />
                        <Skeleton className="h-4 w-[10%]" />
                        <Skeleton className="h-8 w-8 ml-auto rounded-lg" />
                    </div>
                ))}
            </div>
        </div>
    );

    return (
        <div className="flex flex-1 flex-col gap-6 p-6 pt-0 min-h-0">
            <FileHeader currentFile={currentFile} directory={directory} setDirectory={setDirectory} setCurrentFile={setCurrentFile}
                        updateFiles={updateFiles} fileContent={fileContent} onCreateFolder={handleCreateFolder} saveFile={saveFile} />
            {loading && !currentFile && <LoadingSkeleton />}
            {!loading && !currentFile && <FileView files={files} click={onClick} directory={directory} updateFiles={updateFiles} 
                        creatingFolder={creatingFolder} setCreatingFolder={setCreatingFolder} setFiles={setFiles} />}
            {currentFile && <FileEditor currentFile={currentFile} directory={directory} fileContent={fileContent} setFileContent={setFileContent} onSave={saveFile} />}
        </div>
    );
};

export default FileManager;
