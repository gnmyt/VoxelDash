import { useEffect, useState, useCallback } from "react";
import { t } from "i18next";
import { jsonRequest } from "@/lib/RequestUtil";
import { PlayerProfile } from "@/types/player";
import { Button } from "@/components/ui/button";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogFooter,
    DialogTitle,
} from "@/components/ui/dialog";

interface Target {
    name: string;
    uuid: string;
    online: boolean;
}

interface PlayerProfileDialogProps {
    player: Target | null;
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

const fmtDate = (ms: number): string => (ms > 0 ? new Date(ms).toLocaleString() : "-");
const fmtPlaytime = (ms: number): string => {
    const h = Math.floor(ms / 3600000);
    const m = Math.floor((ms % 3600000) / 60000);
    return h > 0 ? `${h}h ${m}m` : `${m}m`;
};

const PlayerProfileDialog = ({ player, open, onOpenChange }: PlayerProfileDialogProps) => {
    const [profile, setProfile] = useState<PlayerProfile | null>(null);
    const [loading, setLoading] = useState(false);

    const uuid = player?.uuid;
    const online = player?.online ?? false;

    const fetchProfile = useCallback(async () => {
        if (!uuid) return;
        setLoading(true);
        try {
            const data = await jsonRequest(`players/profile?uuid=${uuid}&online=${online}`);
            setProfile(data.profile ?? null);
        } finally {
            setLoading(false);
        }
    }, [uuid, online]);

    useEffect(() => {
        if (open && uuid) fetchProfile();
        else if (!open) setProfile(null);
    }, [open, uuid, online, fetchProfile]);

    if (!player) return null;

    const Stat = ({ label, value }: { label: string; value: string }) => (
        <div className="rounded-lg border bg-muted/30 p-2">
            <div className="text-[11px] text-muted-foreground">{label}</div>
            <div className="text-sm font-medium truncate">{value}</div>
        </div>
    );

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="rounded-xl max-w-2xl max-h-[85vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-3">
                        <img src={`https://mc-heads.net/avatar/${player.uuid}/32`} alt={player.name} className="h-8 w-8 rounded" />
                        <span>{player.name}</span>
                        <span className={`text-xs px-2 py-0.5 rounded-md ${online ? "bg-emerald-500/20 text-emerald-500" : "bg-muted text-muted-foreground"}`}>
                            {online ? t("players.online_badge") : t("players.offline_badge")}
                        </span>
                        {profile?.muted && (
                            <span className="text-xs px-2 py-0.5 rounded-md bg-amber-500/20 text-amber-500">{t("players.actions.muted_badge")}</span>
                        )}
                    </DialogTitle>
                </DialogHeader>

                {loading && <p className="text-sm text-muted-foreground py-6 text-center">{t("action.loading")}</p>}

                {profile && (
                    <div className="pt-2">
                        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                            <Stat label={t("players.profile.first_join")} value={fmtDate(profile.firstJoin)} />
                            <Stat label={t("players.profile.last_seen")} value={online ? t("players.online_badge") : fmtDate(profile.lastSeen)} />
                            <Stat label={t("players.table.playtime")} value={fmtPlaytime(profile.playtimeMillis)} />
                            <Stat label={t("players.table.gamemode")} value={profile.gamemode ?? "-"} />
                            <Stat label={t("players.profile.dimension")} value={profile.dimension ?? "-"} />
                            <Stat label={t("players.profile.health")} value={online ? `${Math.round(profile.health)}/20` : "-"} />
                            <Stat label={t("players.table.op")} value={profile.op ? t("action.yes") : t("action.no")} />
                            <Stat label={t("players.profile.banned")} value={profile.banned ? t("action.yes") : t("action.no")} />
                            <Stat label={t("players.profile.uuid")} value={player.uuid} />
                        </div>
                        <div className="mt-4">
                            <div className="text-xs text-muted-foreground mb-1">{t("players.profile.ip_history")}</div>
                            {profile.ipHistory.length === 0 ? (
                                <p className="text-sm text-muted-foreground">{t("players.profile.no_ips")}</p>
                            ) : (
                                <div className="space-y-1">
                                    {profile.ipHistory.map((ip) => (
                                        <div key={ip.ip} className="flex justify-between text-sm border rounded-md px-2 py-1">
                                            <span className="font-mono">{ip.ip}</span>
                                            <span className="text-muted-foreground">{fmtDate(ip.lastSeen)} · {ip.count}×</span>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                )}

                <DialogFooter>
                    <Button variant="outline" onClick={() => onOpenChange(false)}>{t("action.close")}</Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default PlayerProfileDialog;
