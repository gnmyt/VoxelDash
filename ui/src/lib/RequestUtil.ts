const MASTER_TOKEN_KEY = "masterToken";

let masterMode = true;

export const isMasterMode = () => masterMode;

export const detectInstanceMode = async (): Promise<boolean> => {
    try {
        const res = await fetch("/master/status", {headers: {Accept: "application/json"}});
        const contentType = res.headers.get("content-type") || "";
        masterMode = res.ok && contentType.includes("application/json");
    } catch {
        masterMode = false;
    }
    return masterMode;
};

let activeServerId: string | null = localStorage.getItem("activeServerId");

export const setActiveServerId = (serverId: string | null) => {
    activeServerId = serverId;
    if (serverId) localStorage.setItem("activeServerId", serverId);
    else localStorage.removeItem("activeServerId");
};

export const getActiveServerId = () => activeServerId;

export const getMasterToken = () => localStorage.getItem(MASTER_TOKEN_KEY);
export const setMasterToken = (token: string | null) => {
    if (token) localStorage.setItem(MASTER_TOKEN_KEY, token);
    else localStorage.removeItem(MASTER_TOKEN_KEY);
};

const getHeaders = (): Record<string, string> => {
    const token = masterMode ? getMasterToken() : localStorage.getItem("sessionToken");
    return token ? {Authorization: "Bearer " + token} : {};
};

const proxyBase = () => masterMode ? `/api/proxy/${activeServerId}/` : "/api/";

export const request = async (path: string, method = "GET", body = {}, headers = {}, abort = true) => {
    const controller = new AbortController();
    if (abort) setTimeout(() => {controller.abort()}, 10000);

    return await fetch(proxyBase() + path, {
        headers: {...getHeaders(), ...headers}, method,
        body: Object.keys(body).length ? JSON.stringify(body) : undefined,
        signal: controller.signal
    });
}

export const jsonRequest = async (path: string, headers = {}) => {
    return (await request(path, "GET", undefined, headers)).json();
}

export const postRequest = async (path: string, body = {}, headers = {}) => {
    return (await request(path, "POST", body, headers)).json();
}

export const putRequest = async (path: string, body = {}, headers = {}) => {
    return (await request(path, "PUT", body, headers)).json();
}

export const deleteRequest = async (path: string, body = {}, headers = {}) => {
    return (await request(path, "DELETE", body, headers)).json();
}

export const patchRequest = async (path: string, body = {}, headers = {}) => {
    return (await request(path, "PATCH", body, headers)).json();
}

export const downloadRequest = async (path: string, body = {}, headers = {}) => {
    const file = await request(path, "GET", body, headers);
    const element = document.createElement('a');
    const url = file.headers.get('Content-Disposition')?.split('filename=')[1] || "file";
    element.setAttribute("download", url.replaceAll("\"", ""));

    const blob = await file.blob();
    element.href = window.URL.createObjectURL(blob);
    document.body.appendChild(element);
    element.click();
    element.remove();
}

export const masterRequest = async (path: string, method = "GET", body?: unknown) => {
    return await fetch("/master/" + path, {
        method,
        headers: {"Content-Type": "application/json", ...getHeaders()},
        body: body !== undefined ? JSON.stringify(body) : undefined,
    });
};

export const masterJson = async (path: string) => (await masterRequest(path)).json();
export const masterPost = async (path: string, body?: unknown) => masterRequest(path, "POST", body);
export const masterDelete = async (path: string) => masterRequest(path, "DELETE");


export const uploadChunks = async (
    file: File,
    directory: string,
    onProgress: (percent: number) => void
) => {
    const CHUNK_SIZE = 4 * 1024 * 1024;
    const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
    let uploadedChunks = 0;

    const handleBeforeUnload = (event: BeforeUnloadEvent) =>event.preventDefault();

    window.addEventListener("beforeunload", handleBeforeUnload);

    try {
        const initResponse = await request("files/upload/init", "POST", {});
        const initResult = await initResponse.json();
        const uuid = initResult.uuid;

        for (let chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
            const chunkStart = chunkIndex * CHUNK_SIZE;
            const chunkEnd = Math.min(chunkStart + CHUNK_SIZE, file.size);
            const chunk = file.slice(chunkStart, chunkEnd);

            const uploadResponse = await fetch(`${proxyBase()}files/upload/chunk/${uuid}/${chunkIndex}`, {
                method: "PUT",
                headers: getHeaders(),
                body: chunk,
            });

            if (!uploadResponse.ok) {
                throw new Error("Failed to upload chunk " + chunkIndex);
            }

            uploadedChunks++;
            onProgress(Math.round((uploadedChunks / totalChunks) * 100));
        }

        await request("files/upload/stop", "POST", {uuid, destinationPath: directory + file.name}, {}, false);
    } catch (error) {
        console.error("Upload failed:", error);
        throw error;
    } finally {
        window.removeEventListener("beforeunload", handleBeforeUnload);
        onProgress(0);
    }
};

export const uploadFolder = async (files: FileList, directory: string, onProgress: (percent: number) => void) => {
    const handleBeforeUnload = (event: BeforeUnloadEvent) => event.preventDefault();
    window.addEventListener("beforeunload", handleBeforeUnload);
    const totalFiles = files.length;
    let uploadedFiles = 0;

    try {
        for (let i = 0; i < files.length; i++) {
            const file = files[i];
            const relativePath = (file as File & { webkitRelativePath?: string }).webkitRelativePath || file.name;
            const targetPath = directory + relativePath;
            const dirPath = targetPath.substring(0, targetPath.lastIndexOf('/'));

            if (dirPath && dirPath !== directory.slice(0, -1)) {
                await request("folder", "PUT", { path: dirPath }, {}, false).catch(() => {});
            }

            const CHUNK_SIZE = 4 * 1024 * 1024;
            const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
            const initResponse = await request("files/upload/init", "POST", {});
            const initResult = await initResponse.json();
            const uuid = initResult.uuid;

            for (let chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                const chunkStart = chunkIndex * CHUNK_SIZE;
                const chunkEnd = Math.min(chunkStart + CHUNK_SIZE, file.size);
                const chunk = file.slice(chunkStart, chunkEnd);
                const uploadResponse = await fetch(`${proxyBase()}files/upload/chunk/${uuid}/${chunkIndex}`, {
                    method: "PUT",
                    headers: getHeaders(),
                    body: chunk,
                });
                if (!uploadResponse.ok) throw new Error("Failed to upload chunk " + chunkIndex);
            }

            await request("files/upload/stop", "POST", { uuid, destinationPath: targetPath }, {}, false);
            uploadedFiles++;
            onProgress(Math.round((uploadedFiles / totalFiles) * 100));
        }
    } catch (error) {
        console.error("Folder upload failed:", error);
        throw error;
    } finally {
        window.removeEventListener("beforeunload", handleBeforeUnload);
        onProgress(0);
    }
};
