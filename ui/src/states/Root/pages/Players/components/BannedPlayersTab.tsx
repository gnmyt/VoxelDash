import { useState } from "react";
import { t } from "i18next";
import { BannedPlayer } from "@/types/player";
import { postRequest } from "@/lib/RequestUtil";
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
    ProhibitIcon,
    CalendarIcon,
} from "@phosphor-icons/react";

interface BannedPlayersTabProps {
    players: BannedPlayer[];
    onRefresh: () => Promise<void>;
}

const formatDate = (timestamp: number | null): string => {
    if (!timestamp) return t("players.permanent");
    return new Date(timestamp).toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
};

const BannedPlayersTab = ({ players, onRefresh }: BannedPlayersTabProps) => {
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
    
    const [unbanDialogOpen, setUnbanDialogOpen] = useState(false);

    const handleUnban = async (playerNames: string[]) => {
        for (const name of playerNames) {
            await postRequest("players/unban", { playerName: name });
        }
        toast({ description: t("players.unbanned", { count: playerNames.length }) });
        setUnbanDialogOpen(false);
        clearSelection();
        await onRefresh();
    };

    const openUnbanDialog = (playerName?: string) => {
        setTargetPlayer(playerName || null);
        setUnbanDialogOpen(true);
    };

    if (players.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center py-16 text-center">
                <div className="h-16 w-16 rounded-2xl bg-muted flex items-center justify-center mb-4">
                    <ProhibitIcon className="h-8 w-8 text-muted-foreground" />
                </div>
                <p className="text-lg font-medium text-muted-foreground">{t("players.no_banned")}</p>
                <p className="text-sm text-muted-foreground mt-1">{t("players.no_banned_description")}</p>
            </div>
        );
    }

    return (
        <div className="space-y-4">
            {hasSelection && (
                <div className="flex flex-wrap items-center gap-2 p-3 rounded-xl bg-muted/50 border">
                    <span className="text-sm text-muted-foreground">
                        {t("players.selected", { count: selectedPlayers.length })}
                    </span>
                    <div className="flex-1" />
                    <Button onClick={() => openUnbanDialog()} className="rounded-lg">
                        {t("players.unban")}
                    </Button>
                </div>
            )}

            <div className="rounded-xl border bg-card overflow-x-auto">
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
                            <TableHead>{t("players.table.reason")}</TableHead>
                            <TableHead>{t("players.table.banned_on")}</TableHead>
                            <TableHead>{t("players.table.expires")}</TableHead>
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
                                            <code className="text-xs text-muted-foreground">
                                                {player.uuid.substring(0, 8)}...
                                            </code>
                                        </div>
                                    </div>
                                </TableCell>
                                <TableCell>
                                    <span className="text-sm">
                                        {player.reason || t("players.no_reason")}
                                    </span>
                                </TableCell>
                                <TableCell>
                                    <div className="flex items-center gap-2 text-sm">
                                        <CalendarIcon className="h-4 w-4 text-muted-foreground" />
                                        <span>{formatDate(player.banDate)}</span>
                                    </div>
                                </TableCell>
                                <TableCell>
                                    <span className={`text-sm ${!player.expiry ? 'text-destructive font-medium' : ''}`}>
                                        {formatDate(player.expiry)}
                                    </span>
                                </TableCell>
                                <TableCell>
                                    <Button 
                                        variant="outline" 
                                        size="sm" 
                                        onClick={() => openUnbanDialog(player.name)}
                                        className="rounded-lg"
                                    >
                                        {t("players.unban")}
                                    </Button>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </div>

            <AlertDialog open={unbanDialogOpen} onOpenChange={setUnbanDialogOpen}>
                <AlertDialogContent className="rounded-xl">
                    <AlertDialogHeader>
                        <AlertDialogTitle>{t("players.unban_title")}</AlertDialogTitle>
                        <AlertDialogDescription>
                            {t("players.unban_description", { count: getPlayersToAction().length })}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>{t("action.cancel")}</AlertDialogCancel>
                        <AlertDialogAction onClick={() => handleUnban(getPlayersToAction())}>
                            {t("players.unban")}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    );
};

export default BannedPlayersTab;
