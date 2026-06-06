import { useState } from "react";
import { t } from "i18next";
import { postRequest, jsonRequest } from "@/lib/RequestUtil";
import { PlayerCapabilities } from "@/types/player";
import { World } from "@/types/world";
import { toast } from "@/hooks/use-toast";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import {
    DotsThreeIcon,
    BackpackIcon,
    IdentificationCardIcon,
    ChatCircleIcon,
    NavigationArrowIcon,
    SignOutIcon,
    ProhibitIcon,
    HourglassIcon,
    MicrophoneSlashIcon,
    MicrophoneIcon,
} from "@phosphor-icons/react";
import PlayerInventoryDialog from "./PlayerInventoryDialog";
import PlayerProfileDialog from "./PlayerProfileDialog";

export interface PlayerTarget {
    name: string;
    uuid: string;
    online: boolean;
}

interface PlayerActionsMenuProps {
    player: PlayerTarget;
    capabilities: PlayerCapabilities | null;
    worlds: World[];
    onActionComplete?: () => void;
    align?: "start" | "end";
}

type DialogKind = null | "whisper" | "teleport" | "kick" | "ban" | "tempban" | "mute";

const DURATIONS: { label: string; seconds: number }[] = [
    { label: "1h", seconds: 3600 },
    { label: "6h", seconds: 21600 },
    { label: "1d", seconds: 86400 },
    { label: "7d", seconds: 604800 },
    { label: "30d", seconds: 2592000 },
];

const PlayerActionsMenu = ({ player, capabilities, worlds, onActionComplete, align = "end" }: PlayerActionsMenuProps) => {
    const [dialog, setDialog] = useState<DialogKind>(null);
    const [inventoryOpen, setInventoryOpen] = useState(false);
    const [profileOpen, setProfileOpen] = useState(false);
    const [muted, setMuted] = useState<boolean | null>(null);

    const [message, setMessage] = useState("");
    const [reason, setReason] = useState("");
    const [duration, setDuration] = useState("86400");
    const [muteDuration, setMuteDuration] = useState("0");
    const [tpTarget, setTpTarget] = useState("");
    const [tpCoords, setTpCoords] = useState({ x: "", y: "", z: "", world: "" });

    const online = player.online;
    const inv = capabilities?.inventory;
    const tp = capabilities?.teleport;
    const muteSupported = capabilities?.mute?.supported;
    const canViewInventory = !!inv && (online ? inv.viewOnline : inv.viewOffline);

    const done = async (msg: string) => {
        toast({ description: msg });
        setDialog(null);
        setMessage("");
        setReason("");
        onActionComplete?.();
    };

    const sendWhisper = async () => {
        await postRequest("players/whisper", { playerName: player.name, message });
        await done(t("players.actions.message_sent", { player: player.name }));
    };
    const doKick = async () => {
        await postRequest("players/kick", { playerName: player.name, uuid: player.uuid, reason: reason || t("players.default_kick_reason") });
        await done(t("players.kicked", { count: 1 }));
    };
    const doBan = async () => {
        await postRequest("players/ban", { playerName: player.name, uuid: player.uuid, reason: reason || t("players.default_ban_reason") });
        await done(t("players.banned", { count: 1 }));
    };
    const doTempBan = async () => {
        const expiry = Date.now() + parseInt(duration) * 1000;
        await postRequest("players/tempban", { playerName: player.name, uuid: player.uuid, expiry, reason: reason || t("players.default_ban_reason") });
        await done(t("players.actions.tempbanned"));
    };
    const doMute = async () => {
        const expiry = muteDuration === "0" ? 0 : Date.now() + parseInt(muteDuration) * 1000;
        await postRequest("players/mute", { playerName: player.name, uuid: player.uuid, expiry, reason: reason || t("players.actions.default_mute_reason") });
        setMuted(true);
        await done(t("players.actions.muted"));
    };
    const doUnmute = async () => {
        await postRequest("players/unmute", { playerName: player.name, uuid: player.uuid });
        setMuted(false);
        await done(t("players.actions.unmuted"));
    };
    const refreshMuteStatus = () => {
        if (!muteSupported) return;
        jsonRequest(`players/mute/status?uuid=${player.uuid}`)
            .then((d) => setMuted(!!d.muted))
            .catch(() => {});
    };
    const doTpCoords = async () => {
        await postRequest("players/teleport/coords", {
            playerName: player.name,
            x: parseFloat(tpCoords.x) || 0,
            y: parseFloat(tpCoords.y) || 0,
            z: parseFloat(tpCoords.z) || 0,
            world: tpCoords.world || undefined,
        });
        await done(t("players.actions.teleported"));
    };
    const doTpPlayer = async () => {
        await postRequest("players/teleport/player", { playerName: player.name, targetName: tpTarget.trim() });
        await done(t("players.actions.teleported"));
    };
    const doTpSpawn = async () => {
        await postRequest("players/teleport/spawn", { playerName: player.name });
        await done(t("players.actions.teleported"));
    };
    const doTpWorld = async (worldName: string) => {
        await postRequest("players/teleport", { playerName: player.name, worldName });
        await done(t("players.actions.teleported"));
    };

    return (
        <>
            <DropdownMenu onOpenChange={(o) => { if (o) refreshMuteStatus(); }}>
                <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                        <DotsThreeIcon className="h-4 w-4" weight="bold" />
                    </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align={align}>
                    {canViewInventory && (
                        <DropdownMenuItem onClick={() => setInventoryOpen(true)}>
                            <BackpackIcon className="h-4 w-4 mr-2" />
                            {t("players.inventory.menu")}
                        </DropdownMenuItem>
                    )}
                    <DropdownMenuItem onClick={() => setProfileOpen(true)}>
                        <IdentificationCardIcon className="h-4 w-4 mr-2" />
                        {t("players.profile.menu")}
                    </DropdownMenuItem>

                    {online && (
                        <>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem onClick={() => setDialog("whisper")}>
                                <ChatCircleIcon className="h-4 w-4 mr-2" />
                                {t("players.actions.whisper")}
                            </DropdownMenuItem>
                            {tp && (tp.coords || tp.toPlayer || tp.toSpawn || worlds.length > 0) && (
                                <DropdownMenuItem onClick={() => setDialog("teleport")}>
                                    <NavigationArrowIcon className="h-4 w-4 mr-2" />
                                    {t("players.actions.teleport")}
                                </DropdownMenuItem>
                            )}
                            <DropdownMenuItem onClick={() => setDialog("kick")}>
                                <SignOutIcon className="h-4 w-4 mr-2" />
                                {t("players.kick")}
                            </DropdownMenuItem>
                        </>
                    )}

                    <DropdownMenuSeparator />
                    <DropdownMenuItem onClick={() => setDialog("ban")} className="text-destructive">
                        <ProhibitIcon className="h-4 w-4 mr-2" />
                        {t("players.ban")}
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => setDialog("tempban")} className="text-destructive">
                        <HourglassIcon className="h-4 w-4 mr-2" />
                        {t("players.actions.tempban")}
                    </DropdownMenuItem>
                    {muteSupported && (
                        muted ? (
                            <DropdownMenuItem onClick={doUnmute}>
                                <MicrophoneIcon className="h-4 w-4 mr-2" />
                                {t("players.actions.unmute")}
                            </DropdownMenuItem>
                        ) : (
                            <DropdownMenuItem onClick={() => setDialog("mute")}>
                                <MicrophoneSlashIcon className="h-4 w-4 mr-2" />
                                {t("players.actions.mute")}
                            </DropdownMenuItem>
                        )
                    )}
                </DropdownMenuContent>
            </DropdownMenu>

            <PlayerInventoryDialog player={player} open={inventoryOpen} onOpenChange={setInventoryOpen} />
            <PlayerProfileDialog player={player} open={profileOpen} onOpenChange={setProfileOpen} />

            <Dialog open={dialog === "whisper"} onOpenChange={(o) => !o && setDialog(null)}>
                <DialogContent className="rounded-xl">
                    <DialogHeader>
                        <DialogTitle>{t("players.actions.whisper")} ({player.name})</DialogTitle>
                    </DialogHeader>
                    <div className="py-2">
                        <Label>{t("players.actions.message_placeholder")}</Label>
                        <Input value={message} onChange={(e) => setMessage(e.target.value)} className="mt-2" autoFocus />
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setDialog(null)}>{t("action.cancel")}</Button>
                        <Button onClick={sendWhisper} disabled={!message.trim()}>{t("players.actions.send")}</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            <Dialog open={dialog === "teleport"} onOpenChange={(o) => !o && setDialog(null)}>
                <DialogContent className="rounded-xl">
                    <DialogHeader>
                        <DialogTitle>{t("players.actions.teleport")} ({player.name})</DialogTitle>
                    </DialogHeader>
                    <div className="space-y-4 py-2">
                        {tp?.coords && (
                            <div className="space-y-2">
                                <Label className="text-xs">{t("players.actions.coordinates")}</Label>
                                <div className="flex flex-wrap items-center gap-2">
                                    <Input className="w-16" placeholder="x" value={tpCoords.x} onChange={(e) => setTpCoords({ ...tpCoords, x: e.target.value })} />
                                    <Input className="w-16" placeholder="y" value={tpCoords.y} onChange={(e) => setTpCoords({ ...tpCoords, y: e.target.value })} />
                                    <Input className="w-16" placeholder="z" value={tpCoords.z} onChange={(e) => setTpCoords({ ...tpCoords, z: e.target.value })} />
                                    <Select value={tpCoords.world} onValueChange={(v) => setTpCoords({ ...tpCoords, world: v })}>
                                        <SelectTrigger className="w-[140px]"><SelectValue placeholder={t("players.current_world")} /></SelectTrigger>
                                        <SelectContent>
                                            {worlds.map((w) => <SelectItem key={w.name} value={w.name}>{w.name}</SelectItem>)}
                                        </SelectContent>
                                    </Select>
                                    <Button variant="outline" onClick={doTpCoords}>{t("players.actions.go")}</Button>
                                </div>
                            </div>
                        )}
                        {tp?.toPlayer && (
                            <div className="space-y-2">
                                <Label className="text-xs">{t("players.actions.to_player")}</Label>
                                <div className="flex items-center gap-2">
                                    <Input placeholder={t("players.actions.target_player")} value={tpTarget} onChange={(e) => setTpTarget(e.target.value)} />
                                    <Button variant="outline" onClick={doTpPlayer} disabled={!tpTarget.trim()}>{t("players.actions.go")}</Button>
                                </div>
                            </div>
                        )}
                        {worlds.length > 0 && (
                            <div className="space-y-2">
                                <Label className="text-xs">{t("players.actions.to_world")}</Label>
                                <div className="flex flex-wrap gap-2">
                                    {worlds.map((w) => (
                                        <Button key={w.name} variant="outline" size="sm" onClick={() => doTpWorld(w.name)}>{w.name}</Button>
                                    ))}
                                </div>
                            </div>
                        )}
                        {tp?.toSpawn && (
                            <Button variant="outline" onClick={doTpSpawn}>{t("players.actions.to_spawn")}</Button>
                        )}
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setDialog(null)}>{t("action.close")}</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            <Dialog open={dialog === "kick"} onOpenChange={(o) => !o && setDialog(null)}>
                <DialogContent className="rounded-xl">
                    <DialogHeader>
                        <DialogTitle>{t("players.kick_title")} ({player.name})</DialogTitle>
                    </DialogHeader>
                    <div className="py-2">
                        <Label>{t("players.reason")}</Label>
                        <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder={t("players.default_kick_reason")} className="mt-2" />
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setDialog(null)}>{t("action.cancel")}</Button>
                        <Button onClick={doKick}>{t("players.kick")}</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            <Dialog open={dialog === "ban"} onOpenChange={(o) => !o && setDialog(null)}>
                <DialogContent className="rounded-xl">
                    <DialogHeader>
                        <DialogTitle>{t("players.ban_title")} ({player.name})</DialogTitle>
                        <DialogDescription>{t("players.ban_description", { count: 1 })}</DialogDescription>
                    </DialogHeader>
                    <div className="py-2">
                        <Label>{t("players.reason")}</Label>
                        <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder={t("players.default_ban_reason")} className="mt-2" />
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setDialog(null)}>{t("action.cancel")}</Button>
                        <Button variant="destructive" onClick={doBan}>{t("players.ban")}</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            <Dialog open={dialog === "tempban"} onOpenChange={(o) => !o && setDialog(null)}>
                <DialogContent className="rounded-xl">
                    <DialogHeader>
                        <DialogTitle>{t("players.actions.tempban")} ({player.name})</DialogTitle>
                    </DialogHeader>
                    <div className="space-y-3 py-2">
                        <div>
                            <Label className="text-xs">{t("players.actions.duration")}</Label>
                            <Select value={duration} onValueChange={setDuration}>
                                <SelectTrigger className="mt-1 w-28"><SelectValue /></SelectTrigger>
                                <SelectContent>
                                    {DURATIONS.map((d) => <SelectItem key={d.seconds} value={String(d.seconds)}>{d.label}</SelectItem>)}
                                </SelectContent>
                            </Select>
                        </div>
                        <div>
                            <Label className="text-xs">{t("players.reason")}</Label>
                            <Input value={reason} onChange={(e) => setReason(e.target.value)} className="mt-1" />
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setDialog(null)}>{t("action.cancel")}</Button>
                        <Button variant="destructive" onClick={doTempBan}>{t("players.actions.tempban")}</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            <Dialog open={dialog === "mute"} onOpenChange={(o) => !o && setDialog(null)}>
                <DialogContent className="rounded-xl">
                    <DialogHeader>
                        <DialogTitle>{t("players.actions.mute")} ({player.name})</DialogTitle>
                    </DialogHeader>
                    <div className="space-y-3 py-2">
                        <div>
                            <Label className="text-xs">{t("players.actions.duration")}</Label>
                            <Select value={muteDuration} onValueChange={setMuteDuration}>
                                <SelectTrigger className="mt-1 w-32"><SelectValue /></SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="0">{t("players.actions.permanent")}</SelectItem>
                                    {DURATIONS.map((d) => <SelectItem key={d.seconds} value={String(d.seconds)}>{d.label}</SelectItem>)}
                                </SelectContent>
                            </Select>
                        </div>
                        <div>
                            <Label className="text-xs">{t("players.reason")}</Label>
                            <Input value={reason} onChange={(e) => setReason(e.target.value)} className="mt-1" />
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setDialog(null)}>{t("action.cancel")}</Button>
                        <Button onClick={doMute}>{t("players.actions.mute")}</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </>
    );
};

export default PlayerActionsMenu;
