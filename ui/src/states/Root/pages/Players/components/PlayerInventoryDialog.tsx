import { useEffect, useState, useCallback } from "react";
import { t } from "i18next";
import { jsonRequest, postRequest } from "@/lib/RequestUtil";
import { InventoryItem, InventoryView, InventoryCapabilities } from "@/types/player";
import { toast } from "@/hooks/use-toast";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import ItemIcon from "@/components/ItemIcon";

interface Target {
    name: string;
    uuid: string;
    online: boolean;
}

interface PlayerInventoryDialogProps {
    player: Target | null;
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

const ARMOR_SLOTS = [103, 102, 101, 100];
const OFFHAND_SLOT = -106;

const SlotCell = ({
    item,
    editable,
    onClick,
    onDragStartSlot,
    onDropSlot,
}: {
    item?: InventoryItem;
    editable: boolean;
    onClick: () => void;
    onDragStartSlot?: () => void;
    onDropSlot?: () => void;
}) => {
    const durability =
        item && item.maxDamage > 0 ? Math.max(0, 1 - item.damage / item.maxDamage) : null;
    return (
        <button
            type="button"
            disabled={!editable}
            onClick={onClick}
            draggable={editable && !!item}
            onDragStart={onDragStartSlot}
            onDragOver={editable ? (e) => e.preventDefault() : undefined}
            onDrop={editable ? (e) => { e.preventDefault(); onDropSlot?.(); } : undefined}
            className={`relative aspect-square w-full rounded-md border bg-muted/40 flex items-center justify-center ${editable ? "hover:bg-accent cursor-pointer" : "cursor-default"} ${item?.enchanted ? "ring-1 ring-fuchsia-400/60" : ""}`}
            title={item ? (item.name ? `${item.name} (${item.id})` : item.id) : undefined}
        >
            {item && (
                <>
                    <ItemIcon id={item.id} alt={item.name ?? item.id} className="h-3/4 w-3/4" />
                    {item.count > 1 && (
                        <span className="absolute bottom-0 right-0.5 text-[11px] font-semibold drop-shadow">
                            {item.count}
                        </span>
                    )}
                    {durability !== null && durability < 1 && (
                        <span className="absolute bottom-0.5 left-1 right-1 h-1 rounded bg-black/40">
                            <span
                                className="block h-full rounded"
                                style={{
                                    width: `${durability * 100}%`,
                                    backgroundColor: `hsl(${durability * 120}, 80%, 45%)`,
                                }}
                            />
                        </span>
                    )}
                </>
            )}
        </button>
    );
};

const PlayerInventoryDialog = ({ player, open, onOpenChange }: PlayerInventoryDialogProps) => {
    const [inventory, setInventory] = useState<InventoryView | null>(null);
    const [enderChest, setEnderChest] = useState<InventoryView | null>(null);
    const [caps, setCaps] = useState<InventoryCapabilities | null>(null);
    const [loading, setLoading] = useState(false);
    const [tab, setTab] = useState<string>("inventory");

    const [drag, setDrag] = useState<{ slot: number; ender: boolean } | null>(null);
    const [editSlot, setEditSlot] = useState<{ slot: number; ender: boolean } | null>(null);
    const [editId, setEditId] = useState("");
    const [editCount, setEditCount] = useState("1");
    const [giveId, setGiveId] = useState("");
    const [giveCount, setGiveCount] = useState("1");

    const uuid = player?.uuid;
    const online = player?.online ?? false;

    const fetchInventory = useCallback(async () => {
        if (!uuid) return;
        setLoading(true);
        try {
            const data = await jsonRequest(`players/inventory?uuid=${uuid}&online=${online}`);
            setCaps(data.capabilities ?? null);
            setInventory(data.inventory ?? null);
            setEnderChest(data.enderchest ?? null);
        } finally {
            setLoading(false);
        }
    }, [uuid, online]);

    useEffect(() => {
        if (open && uuid) {
            setTab("inventory");
            fetchInventory();
        } else if (!open) {
            setInventory(null);
            setEnderChest(null);
            setCaps(null);
        }
    }, [open, uuid, online, fetchInventory]);

    const canEdit = !!(player?.online && caps?.editOnline);

    const bySlot = (view: InventoryView | null): Map<number, InventoryItem> => {
        const map = new Map<number, InventoryItem>();
        view?.items.forEach((it) => map.set(it.slot, it));
        return map;
    };

    const openSlotEditor = (slot: number, ender: boolean, current?: InventoryItem) => {
        if (!canEdit) return;
        setEditSlot({ slot, ender });
        setEditId(current?.id ?? "");
        setEditCount(String(current?.count ?? 1));
    };

    const applySlot = async (clear: boolean) => {
        if (!player || !editSlot) return;
        const body: Record<string, unknown> = {
            uuid: player.uuid,
            enderChest: editSlot.ender,
            slot: editSlot.slot,
        };
        if (!clear && editId.trim()) {
            body.item = { id: editId.trim(), count: Math.max(1, parseInt(editCount) || 1) };
        }
        await postRequest("players/inventory/set", body);
        setEditSlot(null);
        await fetchInventory();
    };

    const handleGive = async () => {
        if (!player || !giveId.trim()) return;
        await postRequest("players/inventory/give", {
            uuid: player.uuid,
            id: giveId.trim(),
            count: Math.max(1, parseInt(giveCount) || 1),
        });
        setGiveId("");
        setGiveCount("1");
        toast({ description: t("players.inventory.given") });
        await fetchInventory();
    };

    const handleClear = async (ender: boolean) => {
        if (!player) return;
        await postRequest("players/inventory/clear", { uuid: player.uuid, enderChest: ender });
        await fetchInventory();
    };

    const handleDrop = async (toSlot: number, ender: boolean) => {
        const src = drag;
        setDrag(null);
        if (!player || !canEdit || !src) return;
        if (src.ender !== ender || src.slot === toSlot) return;
        await postRequest("players/inventory/move", {
            uuid: player.uuid,
            enderChest: ender,
            fromSlot: src.slot,
            toSlot,
        });
        await fetchInventory();
    };

    const renderMainGrid = () => {
        const map = bySlot(inventory);
        const main = [];
        for (let s = 9; s <= 35; s++) main.push(s);
        const hotbar = [];
        for (let s = 0; s <= 8; s++) hotbar.push(s);

        return (
            <div className="rounded-lg border bg-muted/40 p-3">
                <div className="flex flex-col sm:flex-row gap-4">
                    <div className="flex flex-col gap-1 shrink-0 w-11">
                        <span className="text-xs text-muted-foreground">{t("players.inventory.armor")}</span>
                        {ARMOR_SLOTS.map((s) => (
                            <SlotCell key={s} item={map.get(s)} editable={canEdit} onClick={() => openSlotEditor(s, false, map.get(s))} onDragStartSlot={() => setDrag({ slot: s, ender: false })} onDropSlot={() => handleDrop(s, false)} />
                        ))}
                        <span className="mt-2 text-xs text-muted-foreground">{t("players.inventory.offhand")}</span>
                        <SlotCell item={map.get(OFFHAND_SLOT)} editable={canEdit} onClick={() => openSlotEditor(OFFHAND_SLOT, false, map.get(OFFHAND_SLOT))} onDragStartSlot={() => setDrag({ slot: OFFHAND_SLOT, ender: false })} onDropSlot={() => handleDrop(OFFHAND_SLOT, false)} />
                    </div>
                    <div className="flex-1 space-y-2 min-w-0">
                        <div className="grid gap-1" style={{ gridTemplateColumns: "repeat(9, minmax(0, 2.75rem))" }}>
                            {main.map((s) => (
                                <SlotCell key={s} item={map.get(s)} editable={canEdit} onClick={() => openSlotEditor(s, false, map.get(s))} onDragStartSlot={() => setDrag({ slot: s, ender: false })} onDropSlot={() => handleDrop(s, false)} />
                            ))}
                        </div>
                        <div className="grid gap-1 pt-2 border-t border-dashed" style={{ gridTemplateColumns: "repeat(9, minmax(0, 2.75rem))" }}>
                            {hotbar.map((s) => (
                                <SlotCell key={s} item={map.get(s)} editable={canEdit} onClick={() => openSlotEditor(s, false, map.get(s))} onDragStartSlot={() => setDrag({ slot: s, ender: false })} onDropSlot={() => handleDrop(s, false)} />
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        );
    };

    const renderEnder = () => {
        const map = bySlot(enderChest);
        const slots = [];
        for (let s = 0; s < 27; s++) slots.push(s);
        return (
            <div className="rounded-lg border bg-muted/40 p-3">
                <div className="flex flex-col sm:flex-row gap-4">
                    <div className="flex-1 space-y-2 min-w-0">
                        <div className="grid gap-1" style={{ gridTemplateColumns: "repeat(9, minmax(0, 2.75rem))" }}>
                            {slots.map((s) => (
                                <SlotCell key={s} item={map.get(s)} editable={canEdit} onClick={() => openSlotEditor(s, true, map.get(s))} onDragStartSlot={() => setDrag({ slot: s, ender: true })} onDropSlot={() => handleDrop(s, true)} />
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        );
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="rounded-xl max-w-3xl">
                <DialogHeader>
                    <DialogTitle>{t("players.inventory.title", { player: player?.name ?? "" })}</DialogTitle>
                    <DialogDescription>
                        {player?.online
                            ? canEdit
                                ? t("players.inventory.editable")
                                : t("players.inventory.online_readonly")
                            : t("players.inventory.offline_readonly")}
                    </DialogDescription>
                </DialogHeader>

                {loading && !inventory && (
                    <p className="text-sm text-muted-foreground py-8 text-center">{t("action.loading")}</p>
                )}

                {(inventory || !loading) && (
                    <Tabs value={tab} onValueChange={setTab}>
                        <TabsList>
                            <TabsTrigger value="inventory">{t("players.inventory.tab_inventory")}</TabsTrigger>
                            {caps?.viewEnderChest && (
                                <TabsTrigger value="ender">{t("players.inventory.tab_enderchest")}</TabsTrigger>
                            )}
                        </TabsList>
                        <TabsContent value="inventory" className="pt-4 space-y-4">
                            {renderMainGrid()}
                            {canEdit && (
                                <div className="flex flex-wrap items-end gap-2 pt-2 border-t">
                                    <div className="flex-1 min-w-[180px]">
                                        <Label className="text-xs">{t("players.inventory.give_item")}</Label>
                                        <Input value={giveId} onChange={(e) => setGiveId(e.target.value)} placeholder="minecraft:diamond" className="mt-1" />
                                    </div>
                                    <Input type="number" min={1} value={giveCount} onChange={(e) => setGiveCount(e.target.value)} className="w-20" />
                                    <Button onClick={handleGive} disabled={!giveId.trim()}>{t("players.inventory.give")}</Button>
                                    <Button variant="outline" onClick={() => handleClear(false)}>{t("players.inventory.clear")}</Button>
                                </div>
                            )}
                        </TabsContent>
                        {caps?.viewEnderChest && (
                            <TabsContent value="ender" className="pt-4 space-y-4">
                                {renderEnder()}
                                {canEdit && (
                                    <div className="pt-2 border-t">
                                        <Button variant="outline" onClick={() => handleClear(true)}>{t("players.inventory.clear")}</Button>
                                    </div>
                                )}
                            </TabsContent>
                        )}
                    </Tabs>
                )}

                <DialogFooter>
                    <Button variant="outline" onClick={() => onOpenChange(false)}>{t("action.close")}</Button>
                </DialogFooter>
            </DialogContent>

            {/* Slot editor */}
            <Dialog open={!!editSlot} onOpenChange={(o) => !o && setEditSlot(null)}>
                <DialogContent className="rounded-xl max-w-sm">
                    <DialogHeader>
                        <DialogTitle>{t("players.inventory.edit_slot")}</DialogTitle>
                    </DialogHeader>
                    <div className="space-y-3 py-2">
                        <div>
                            <Label className="text-xs">{t("players.inventory.item_id")}</Label>
                            <Input value={editId} onChange={(e) => setEditId(e.target.value)} placeholder="minecraft:diamond_sword" className="mt-1" />
                        </div>
                        <div>
                            <Label className="text-xs">{t("players.inventory.count")}</Label>
                            <Input type="number" min={1} value={editCount} onChange={(e) => setEditCount(e.target.value)} className="mt-1 w-24" />
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => applySlot(true)}>{t("players.inventory.clear_slot")}</Button>
                        <Button onClick={() => applySlot(false)} disabled={!editId.trim()}>{t("action.save")}</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </Dialog>
    );
};

export default PlayerInventoryDialog;
