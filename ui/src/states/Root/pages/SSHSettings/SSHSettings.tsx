import {FormEvent, useContext, useEffect, useState} from "react";
import {Input} from "@/components/ui/input";
import {Button} from "@/components/ui/button";
import {Switch} from "@/components/ui/switch";
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from "@/components/ui/table";
import {Avatar, AvatarFallback, AvatarImage} from "@/components/ui/avatar";
import {CheckIcon, GlobeIcon, LockKeyIcon, PowerIcon, HardDrivesIcon, XIcon, PlugIcon, TerminalWindowIcon, FolderIcon} from "@phosphor-icons/react";
import {jsonRequest, patchRequest, postRequest} from "@/lib/RequestUtil.ts";
import {SSHSession} from "@/types/ssh.ts";
import {ServerInfoContext} from "@/contexts/ServerInfoContext.tsx";
import {t} from "i18next";
import {ScrollArea} from "@/components/ui/scroll-area.tsx";

export default function SSHSettings() {
    const [sshPort, setSshPort] = useState("");
    const [serverEnabled, setServerEnabled] = useState(true);
    const [sftpEnabled, setSftpEnabled] = useState(true);
    const [consoleEnabled, setConsoleEnabled] = useState(true);
    const [activeSessions, setActiveSessions] = useState<SSHSession[]>([]);

    const {serverInfo} = useContext(ServerInfoContext)!;

    const handleConnect = () => {
        window.open(`sftp://${serverInfo?.accountName}@${window.location.hostname}:${sshPort}`, "_blank");
    }

    const disconnectSession = (sessionId: string) =>
        postRequest("/service/ssh/disconnect", {sessionId}).then(() => fetchData());

    const fetchData = async () => {
        jsonRequest("service/ssh").then((data) => {
            setActiveSessions(data.activeClients || []);
            setServerEnabled(data.enabled);
            setSftpEnabled(data.sftpEnabled);
            setConsoleEnabled(data.consoleEnabled);
            setSshPort(data.port);
        });
    }

    const changeEnabled = (enabled: boolean) =>
        patchRequest("service/ssh/enabled", {value: enabled}).then(() => fetchData());

    const changePort = (e: FormEvent) => {
        e.preventDefault();
        patchRequest("service/ssh/port", {value: sshPort}).then(() => fetchData());
    }

    const changeSftp = (enabled: boolean) =>
        patchRequest("service/ssh/sftpEnabled", {value: enabled}).then(() => fetchData());

    const changeConsole = (enabled: boolean) =>
        patchRequest("service/ssh/consoleEnabled", {value: enabled}).then(() => fetchData());

    useEffect(() => {
        fetchData();
    }, []);

    return (
        <div className="flex flex-col p-4 md:p-6 pt-0 gap-6" style={{ height: 'calc(var(--app-vh) - 5.5rem)' }}>
            <ScrollArea className="flex-1">
                <div className="space-y-4 pr-4">
                    <div className="flex flex-wrap items-center justify-between gap-3 p-4 rounded-xl border bg-card">
                        <div className="flex items-center gap-4">
                            <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center">
                                <PowerIcon className="h-6 w-6 text-primary" weight="fill"/>
                            </div>
                            <div>
                                <h3 className="text-base font-semibold">{t("ssh.status.title")}</h3>
                                <p className="text-sm text-muted-foreground">{t("ssh.status.description")}</p>
                            </div>
                        </div>
                        <div className="flex items-center gap-3 flex-wrap">
                            <div className="flex items-center gap-3">
                                <Switch id="server-status" checked={serverEnabled} onCheckedChange={changeEnabled}/>
                                <span className={`text-sm font-medium ${serverEnabled ? 'text-primary' : 'text-muted-foreground'}`}>
                                    {serverEnabled ? t("ssh.status.enabled") : t("ssh.status.disabled")}
                                </span>
                            </div>
                            <Button onClick={handleConnect} disabled={!serverEnabled} size="lg" className="h-12 px-6 rounded-xl text-base">
                                <PlugIcon className="h-5 w-5 mr-2" weight="fill" />
                                {t("ssh.status.connect")}
                            </Button>
                        </div>
                    </div>

                    <div className="flex flex-wrap items-center justify-between gap-3 p-4 rounded-xl border bg-card">
                        <div className="flex items-center gap-4">
                            <div className="h-12 w-12 rounded-xl bg-muted flex items-center justify-center">
                                <GlobeIcon className="h-6 w-6 text-muted-foreground"/>
                            </div>
                            <div>
                                <h3 className="text-base font-semibold">{t("ssh.port.title")}</h3>
                                <p className="text-sm text-muted-foreground">{t("ssh.port.description")}</p>
                            </div>
                        </div>
                        <form className="flex items-center gap-3" onSubmit={changePort}>
                            <Input 
                                onChange={(e) => setSshPort(e.target.value)}
                                min="1" 
                                value={sshPort || ""} 
                                max="65535" 
                                className="w-28 h-12 text-base rounded-xl"
                                disabled={!serverEnabled}
                            />
                            <Button type="submit" disabled={!serverEnabled} size="lg" className="h-12 px-6 rounded-xl text-base">
                                {t("ssh.port.change")}
                            </Button>
                        </form>
                    </div>

                    <div className="p-4 rounded-xl border bg-card space-y-4">
                        <div className="flex items-center gap-4 pb-4 border-b">
                            <div className="h-12 w-12 rounded-xl bg-muted flex items-center justify-center">
                                <LockKeyIcon className="h-6 w-6 text-muted-foreground"/>
                            </div>
                            <div>
                                <h3 className="text-base font-semibold">{t("ssh.access.title")}</h3>
                                <p className="text-sm text-muted-foreground">{t("ssh.access.description")}</p>
                            </div>
                        </div>
                        
                        <div className="flex items-center justify-between p-4 rounded-xl bg-background">
                            <div className="flex items-center gap-4">
                                <div className="h-10 w-10 rounded-xl bg-muted flex items-center justify-center">
                                    <FolderIcon className="h-5 w-5 text-muted-foreground"/>
                                </div>
                                <div>
                                    <h4 className="text-base font-medium">{t("ssh.access.sftp_label")}</h4>
                                    <p className="text-sm text-muted-foreground">{t("ssh.access.sftp_hint")}</p>
                                </div>
                            </div>
                            <div className="flex items-center gap-3">
                                <Switch id="sftp-access" checked={sftpEnabled} onCheckedChange={changeSftp} disabled={!serverEnabled}/>
                                <span className={`text-sm font-medium w-16 ${sftpEnabled ? 'text-primary' : 'text-muted-foreground'}`}>
                                    {sftpEnabled ? t("ssh.access.state_enabled") : t("ssh.access.state_disabled")}
                                </span>
                            </div>
                        </div>

                        <div className="flex items-center justify-between p-4 rounded-xl bg-background">
                            <div className="flex items-center gap-4">
                                <div className="h-10 w-10 rounded-xl bg-muted flex items-center justify-center">
                                    <TerminalWindowIcon className="h-5 w-5 text-muted-foreground"/>
                                </div>
                                <div>
                                    <h4 className="text-base font-medium">{t("ssh.access.console_label")}</h4>
                                    <p className="text-sm text-muted-foreground">{t("ssh.access.console_hint")}</p>
                                </div>
                            </div>
                            <div className="flex items-center gap-3">
                                <Switch id="console-access" checked={consoleEnabled} onCheckedChange={changeConsole} disabled={!serverEnabled}/>
                                <span className={`text-sm font-medium w-16 ${consoleEnabled ? 'text-primary' : 'text-muted-foreground'}`}>
                                    {consoleEnabled ? t("ssh.access.state_enabled") : t("ssh.access.state_disabled")}
                                </span>
                            </div>
                        </div>
                    </div>

                    <div className="rounded-xl border bg-card overflow-hidden">
                        <div className="flex items-center gap-4 p-4 border-b">
                            <div className="h-12 w-12 rounded-xl bg-muted flex items-center justify-center">
                                <HardDrivesIcon className="h-6 w-6 text-muted-foreground"/>
                            </div>
                            <div>
                                <h3 className="text-base font-semibold">{t("ssh.sessions.title")}</h3>
                                <p className="text-sm text-muted-foreground">{t("ssh.sessions.description")}</p>
                            </div>
                        </div>
                        
                        {activeSessions.length === 0 ? (
                            <div className="p-8 text-center text-muted-foreground">
                                {t("ssh.sessions.none")}
                            </div>
                        ) : (
                            <div className="overflow-x-auto">
                            <Table className="text-base">
                                <TableHeader>
                                    <TableRow className="hover:bg-transparent">
                                        <TableHead className="w-[50%] h-14 text-base font-semibold">{t("ssh.sessions.username")}</TableHead>
                                        <TableHead className="w-[20%] h-14 text-base font-semibold">{t("ssh.sessions.ip")}</TableHead>
                                        <TableHead className="w-[10%] h-14 text-base font-semibold">{t("ssh.sessions.sftp")}</TableHead>
                                        <TableHead className="w-[20%] h-14 text-base font-semibold text-right">{t("ssh.sessions.actions")}</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {activeSessions.map((session: SSHSession) => (
                                        <TableRow key={session.username} className="h-16">
                                            <TableCell className="py-4">
                                                <div className="flex items-center gap-3">
                                                    <Avatar className="h-10 w-10">
                                                        <AvatarImage src={`https://minotar.net/avatar/${session.username}.png`} alt={session.username}/>
                                                        <AvatarFallback>{session.username.charAt(0).toUpperCase()}</AvatarFallback>
                                                    </Avatar>
                                                    <span className="font-medium">{session.username}</span>
                                                </div>
                                            </TableCell>
                                            <TableCell className="py-4 text-muted-foreground">{session.address}</TableCell>
                                            <TableCell className="py-4">
                                                {session.isSFTP ? (
                                                    <div className="h-8 w-8 rounded-lg bg-primary/10 flex items-center justify-center">
                                                        <CheckIcon className="h-4 w-4 text-primary" weight="bold" />
                                                    </div>
                                                ) : (
                                                    <div className="h-8 w-8 rounded-lg bg-muted flex items-center justify-center">
                                                        <XIcon className="h-4 w-4 text-muted-foreground" weight="bold" />
                                                    </div>
                                                )}
                                            </TableCell>
                                            <TableCell className="py-4 text-right">
                                                <Button 
                                                    onClick={() => disconnectSession(session.sessionId)} 
                                                    variant="destructive"
                                                    size="lg"
                                                    className="h-10 px-4 rounded-xl"
                                                >
                                                    {t("ssh.sessions.disconnect")}
                                                </Button>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                            </div>
                        )}
                    </div>
                </div>
            </ScrollArea>
        </div>
    );
}
