import { useState, useEffect } from "react";
import { t } from "i18next";
import { OnlinePlayer } from "@/types/player";
import { World } from "@/types/world";
import { postRequest, jsonRequest } from "@/lib/RequestUtil";
import { toast } from "@/hooks/use-toast";
import { usePlayerSelection } from "@/hooks/use-player-selection";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSub,
    DropdownMenuSubContent,
    DropdownMenuSubTrigger,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import {
    DotsThreeIcon,
    UserIcon,
    GlobeIcon,
    GameControllerIcon,
    ClockIcon,
    ProhibitIcon,
    SignOutIcon,
    ShieldCheckIcon,
    ShieldIcon,
    NavigationArrowIcon
} from "@phosphor-icons/react";

interface OnlinePlayersTabProps {
    players: OnlinePlayer[];
    onRefresh: () => Promise<void>;
    onBanComplete: () => Promise<void>;
}

const formatPlaytime = (ms: number): string => {
    const hours = Math.floor(ms / 3600000);
    const minutes = Math.floor((ms % 3600000) / 60000);
    if (hours > 0) {
        return `${hours}h ${minutes}m`;
    }
    return `${minutes}m`;
};

const OnlinePlayersTab = ({ players, onRefresh, onBanComplete }: OnlinePlayersTabProps) => {
    const {
        selectedPlayers,
        setTargetPlayer,
        togglePlayer,
        toggleAll,
        clearSelection,
        getPlayersToAction,
        isAllSelected,
        hasSelection,
    } = usePlayerSelection({ players });
    
    const [kickDialogOpen, setKickDialogOpen] = useState(false);
    const [banDialogOpen, setBanDialogOpen] = useState(false);
    const [kickReason, setKickReason] = useState("");
    const [banReason, setBanReason] = useState("");
    const [worlds, setWorlds] = useState<World[]>([]);

    useEffect(() => {
        const fetchWorlds = async () => {
            try {
                const data = await jsonRequest("worlds");
                setWorlds(data.worlds || []);
            } catch {
            }
        };
        fetchWorlds();
    }, []);

    const handleKick = async (playerNames: string[]) => {
        for (const name of playerNames) {
            await postRequest("players/kick", { playerName: name, reason: kickReason || t("players.default_kick_reason") });
        }
        toast({ description: t("players.kicked", { count: playerNames.length }) });
        setKickDialogOpen(false);
        setKickReason("");
        clearSelection();
        await onRefresh();
    };

    const handleBan = async (playerNames: string[]) => {
        for (const name of playerNames) {
            await postRequest("players/ban", { playerName: name, reason: banReason || t("players.default_ban_reason") });
        }
        toast({ description: t("players.banned", { count: playerNames.length }) });
        setBanDialogOpen(false);
        setBanReason("");
        clearSelection();
        await onRefresh();
        await onBanComplete();
    };

    const handleOpToggle = async (playerName: string, currentOp: boolean) => {
        await postRequest("players/op", { playerName, op: !currentOp });
        toast({ description: currentOp ? t("players.deop_success") : t("players.op_success") });
        await onRefresh();
    };

    const handleGamemodeChange = async (playerName: string, gamemode: string) => {
        await postRequest("players/gamemode", { playerName, gamemode });
        toast({ description: t("players.gamemode_changed") });
        await onRefresh();
    };

    const handleTeleport = async (playerName: string, worldName: string) => {
        await postRequest("players/teleport", { playerName, worldName });
        toast({ description: t("players.teleported", { player: playerName, world: worldName }) });
        await onRefresh();
    };

    const openKickDialog = (playerName?: string) => {
        setTargetPlayer(playerName || null);
        setKickDialogOpen(true);
    };

    const openBanDialog = (playerName?: string) => {
        setTargetPlayer(playerName || null);
        setBanDialogOpen(true);
    };

    if (players.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center py-16 text-center">
                <div className="h-16 w-16 rounded-2xl bg-muted flex items-center justify-center mb-4">
                    <UserIcon className="h-8 w-8 text-muted-foreground" />
                </div>
                <p className="text-lg font-medium text-muted-foreground">{t("players.no_online")}</p>
                <p className="text-sm text-muted-foreground mt-1">{t("players.no_online_description")}</p>
            </div>
        );
    }

    return (
        <div className="space-y-4">
            {hasSelection && (
                <div className="flex items-center gap-2 p-3 rounded-xl bg-muted/50 border">
                    <span className="text-sm text-muted-foreground">
                        {t("players.selected", { count: selectedPlayers.length })}
                    </span>
                    <div className="flex-1" />
                    <Button variant="outline" size="sm" onClick={() => openKickDialog()} className="rounded-lg">
                        <SignOutIcon className="h-4 w-4 mr-2" />
                        {t("players.kick")}
                    </Button>
                    <Button variant="destructive" size="sm" onClick={() => openBanDialog()} className="rounded-lg">
                        <ProhibitIcon className="h-4 w-4 mr-2" />
                        {t("players.ban")}
                    </Button>
                </div>
            )}

            <div className="rounded-xl border bg-card">
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead className="w-12">
                                <Checkbox 
                                    checked={isAllSelected}
                                    onCheckedChange={toggleAll}
                                />
                            </TableHead>
                            <TableHead>{t("players.table.player")}</TableHead>
                            <TableHead>{t("players.table.world")}</TableHead>
                            <TableHead>{t("players.table.health")}</TableHead>
                            <TableHead>{t("players.table.gamemode")}</TableHead>
                            <TableHead>{t("players.table.playtime")}</TableHead>
                            <TableHead>{t("players.table.op")}</TableHead>
                            <TableHead className="w-12"></TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {players.map((player) => (
                            <TableRow key={player.uuid}>
                                <TableCell>
                                    <Checkbox 
                                        checked={selectedPlayers.includes(player.name)}
                                        onCheckedChange={() => togglePlayer(player.name)}
                                    />
                                </TableCell>
                                <TableCell>
                                    <div className="flex items-center gap-3">
                                        <img 
                                            src={`https://mc-heads.net/avatar/${player.uuid}/32`}
                                            alt={player.name}
                                            className="h-8 w-8 rounded"
                                        />
                                        <div>
                                            <div className="font-medium">{player.name}</div>
                                            <div className="text-xs text-muted-foreground">{player.ipAddress}</div>
                                        </div>
                                    </div>
                                </TableCell>
                                <TableCell>
                                    <div className="flex items-center gap-2">
                                        <GlobeIcon className="h-4 w-4 text-muted-foreground" />
                                        <span>{player.world}</span>
                                    </div>
                                </TableCell>
                                <TableCell>
                                    <div className="flex items-center gap-2">
                                        <img src="/assets/images/health.webp" alt={t("players.table.health")} className="h-4 w-4" />
                                        <span>{Math.round(player.health)}/20</span>
                                        <span className="text-muted-foreground">•</span>
                                        <img src="/assets/images/food.webp" alt={t("players.food")} className="h-4 w-4" />
                                        <span>{player.hunger}/20</span>
                                    </div>
                                </TableCell>
                                <TableCell>
                                    <Select
                                        value={player.gamemode}
                                        onValueChange={(value) => handleGamemodeChange(player.name, value)}
                                    >
                                        <SelectTrigger className="w-[130px] h-8">
                                            <div className="flex items-center gap-2">
                                                <GameControllerIcon className="h-4 w-4 text-muted-foreground" />
                                                <SelectValue />
                                            </div>
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="SURVIVAL">{t("players.gamemode.survival")}</SelectItem>
                                            <SelectItem value="CREATIVE">{t("players.gamemode.creative")}</SelectItem>
                                            <SelectItem value="ADVENTURE">{t("players.gamemode.adventure")}</SelectItem>
                                            <SelectItem value="SPECTATOR">{t("players.gamemode.spectator")}</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </TableCell>
                                <TableCell>
                                    <div className="flex items-center gap-2">
                                        <ClockIcon className="h-4 w-4 text-muted-foreground" />
                                        <span>{formatPlaytime(player.playtime)}</span>
                                    </div>
                                </TableCell>
                                <TableCell>
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() => handleOpToggle(player.name, player.op)}
                                        className={`rounded-lg ${player.op ? 'text-primary' : 'text-muted-foreground'}`}
                                    >
                                        {player.op ? (
                                            <ShieldCheckIcon className="h-5 w-5" weight="fill" />
                                        ) : (
                                            <ShieldIcon className="h-5 w-5" />
                                        )}
                                    </Button>
                                </TableCell>
                                <TableCell>
                                    <DropdownMenu>
                                        <DropdownMenuTrigger asChild>
                                            <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                                                <DotsThreeIcon className="h-4 w-4" weight="bold" />
                                            </Button>
                                        </DropdownMenuTrigger>
                                        <DropdownMenuContent align="end">
                                            {worlds.length > 0 && (
                                                <DropdownMenuSub>
                                                    <DropdownMenuSubTrigger>
                                                        <NavigationArrowIcon className="h-4 w-4 mr-2" />
                                                        {t("players.teleport")}
                                                    </DropdownMenuSubTrigger>
                                                    <DropdownMenuSubContent>
                                                        {worlds.map((world) => (
                                                            <DropdownMenuItem 
                                                                key={world.name}
                                                                onClick={() => handleTeleport(player.name, world.name)}
                                                                disabled={player.world === world.name}
                                                            >
                                                                <GlobeIcon className="h-4 w-4 mr-2" />
                                                                {world.name}
                                                                {player.world === world.name && (
                                                                    <span className="ml-2 text-xs text-muted-foreground">
                                                                        ({t("players.current_world")})
                                                                    </span>
                                                                )}
                                                            </DropdownMenuItem>
                                                        ))}
                                                    </DropdownMenuSubContent>
                                                </DropdownMenuSub>
                                            )}
                                            <DropdownMenuItem onClick={() => openKickDialog(player.name)}>
                                                <SignOutIcon className="h-4 w-4 mr-2" />
                                                {t("players.kick")}
                                            </DropdownMenuItem>
                                            <DropdownMenuItem 
                                                onClick={() => openBanDialog(player.name)}
                                                className="text-destructive"
                                            >
                                                <ProhibitIcon className="h-4 w-4 mr-2" />
                                                {t("players.ban")}
                                            </DropdownMenuItem>
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </div>

            <Dialog open={kickDialogOpen} onOpenChange={setKickDialogOpen}>
                <DialogContent className="rounded-xl">
                    <DialogHeader>
                        <DialogTitle>{t("players.kick_title")}</DialogTitle>
                        <DialogDescription>
                            {t("players.kick_description", { count: getPlayersToAction().length })}
                        </DialogDescription>
                    </DialogHeader>
                    <div className="py-4">
                        <Label htmlFor="kickReason">{t("players.reason")}</Label>
                        <Input
                            id="kickReason"
                            value={kickReason}
                            onChange={(e) => setKickReason(e.target.value)}
                            placeholder={t("players.default_kick_reason")}
                            className="mt-2"
                        />
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setKickDialogOpen(false)}>
                            {t("action.cancel")}
                        </Button>
                        <Button onClick={() => handleKick(getPlayersToAction())}>
                            {t("players.kick")}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            <AlertDialog open={banDialogOpen} onOpenChange={setBanDialogOpen}>
                <AlertDialogContent className="rounded-xl">
                    <AlertDialogHeader>
                        <AlertDialogTitle>{t("players.ban_title")}</AlertDialogTitle>
                        <AlertDialogDescription>
                            {t("players.ban_description", { count: getPlayersToAction().length })}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <div className="py-4">
                        <Label htmlFor="banReason">{t("players.reason")}</Label>
                        <Input
                            id="banReason"
                            value={banReason}
                            onChange={(e) => setBanReason(e.target.value)}
                            placeholder={t("players.default_ban_reason")}
                            className="mt-2"
                        />
                    </div>
                    <AlertDialogFooter>
                        <AlertDialogCancel>{t("action.cancel")}</AlertDialogCancel>
                        <AlertDialogAction 
                            onClick={() => handleBan(getPlayersToAction())}
                            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                        >
                            {t("players.ban")}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    );
};

export default OnlinePlayersTab;
