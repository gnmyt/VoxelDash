import {useEffect, useState} from "react";
import {Navigate} from "react-router-dom";
import {t} from "i18next";
import {
    KeyIcon, LockKeyIcon, PencilIcon, PlusIcon, ShieldCheckIcon, TrashIcon
} from "@phosphor-icons/react";
import {useMasterAuth} from "@/contexts/MasterAuthContext.tsx";
import {masterDelete, masterJson, masterRequest} from "@/lib/RequestUtil.ts";
import {MasterLayout} from "@/states/Servers/MasterLayout.tsx";
import {ManagedServer} from "@/contexts/ServerSelectionContext.tsx";
import PermissionToggle from "@/states/Root/pages/Users/components/PermissionToggle.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Badge} from "@/components/ui/badge.tsx";
import {Input} from "@/components/ui/input.tsx";
import {Label} from "@/components/ui/label.tsx";
import {Switch} from "@/components/ui/switch.tsx";
import {Checkbox} from "@/components/ui/checkbox.tsx";
import {Skeleton} from "@/components/ui/skeleton.tsx";
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from "@/components/ui/table.tsx";
import {
    Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,
} from "@/components/ui/dialog.tsx";
import {
    AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription,
    AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from "@/components/ui/alert-dialog.tsx";
import {toast} from "@/hooks/use-toast.ts";

type Level = 0 | 1 | 2;

interface MasterUser {
    id: number;
    username: string;
    isAdmin: boolean;
    permissions: Record<string, Level>;
    allServers: boolean;
    serverIds: string[] | null;
}

const FEATURE_LABELS: Record<string, string> = {
    Servers: "master_users.feature.servers",
    Forwardings: "master_users.feature.forwardings",
    UserManagement: "master_users.feature.user_management",
};

const err = (e: unknown) => toast({variant: "destructive", description: (e as Error).message});

const CreateUserDialog = ({open, onOpenChange, onCreated}: {
    open: boolean; onOpenChange: (o: boolean) => void; onCreated: () => void;
}) => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [confirm, setConfirm] = useState("");
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        if (open) { setUsername(""); setPassword(""); setConfirm(""); }
    }, [open]);

    const submit = async () => {
        if (username.length < 3) return err(new Error(t("master_users.error.username_length")));
        if (password.length < 4) return err(new Error(t("master_users.error.password_length")));
        if (password !== confirm) return err(new Error(t("master_users.error.passwords_mismatch")));
        setBusy(true);
        try {
            const res = await masterRequest("users", "POST", {username, password});
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || t("master_users.create_failed"));
            toast({description: t("master_users.created")});
            onCreated();
            onOpenChange(false);
        } catch (e) {
            err(e);
        } finally {
            setBusy(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="max-w-md">
                <DialogHeader>
                    <DialogTitle className="font-display">{t("master_users.create_title")}</DialogTitle>
                    <DialogDescription>{t("master_users.create_description")}</DialogDescription>
                </DialogHeader>
                <div className="space-y-3">
                    <div className="space-y-1.5">
                        <Label>{t("master_users.username")}</Label>
                        <Input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus/>
                    </div>
                    <div className="space-y-1.5">
                        <Label>{t("master_users.password")}</Label>
                        <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)}/>
                    </div>
                    <div className="space-y-1.5">
                        <Label>{t("master_users.confirm_password")}</Label>
                        <Input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)}/>
                    </div>
                </div>
                <DialogFooter>
                    <Button variant="outline" onClick={() => onOpenChange(false)}>{t("action.cancel")}</Button>
                    <Button disabled={busy} onClick={submit}>{t("action.create")}</Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

const FieldDialog = ({open, onOpenChange, title, label, type, initial, onSave}: {
    open: boolean; onOpenChange: (o: boolean) => void; title: string; label: string;
    type?: string; initial?: string; onSave: (value: string) => Promise<void>;
}) => {
    const [value, setValue] = useState("");
    const [busy, setBusy] = useState(false);
    useEffect(() => { if (open) setValue(initial || ""); }, [open, initial]);

    const submit = async () => {
        setBusy(true);
        try {
            await onSave(value);
            onOpenChange(false);
        } catch (e) {
            err(e);
        } finally {
            setBusy(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="max-w-sm">
                <DialogHeader><DialogTitle className="font-display">{title}</DialogTitle></DialogHeader>
                <div className="space-y-1.5">
                    <Label>{label}</Label>
                    <Input type={type || "text"} value={value} onChange={(e) => setValue(e.target.value)} autoFocus/>
                </div>
                <DialogFooter>
                    <Button variant="outline" onClick={() => onOpenChange(false)}>{t("action.cancel")}</Button>
                    <Button disabled={busy} onClick={submit}>{t("action.save")}</Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

const PermissionsDialog = ({user, features, servers, onOpenChange, onSaved}: {
    user: MasterUser | null; features: string[]; servers: ManagedServer[];
    onOpenChange: (o: boolean) => void; onSaved: () => void;
}) => {
    const [perms, setPerms] = useState<Record<string, Level>>({});
    const [allServers, setAllServers] = useState(true);
    const [serverIds, setServerIds] = useState<string[]>([]);
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        if (user) {
            setPerms({...user.permissions});
            setAllServers(user.allServers);
            setServerIds(user.serverIds || []);
        }
    }, [user]);

    const toggleServer = (id: string) =>
        setServerIds((prev) => prev.includes(id) ? prev.filter((s) => s !== id) : [...prev, id]);

    const save = async () => {
        if (!user) return;
        setBusy(true);
        try {
            const res = await masterRequest(`users/${user.id}/permissions`, "PUT",
                {permissions: perms, allServers, serverIds});
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || t("master_users.permissions_update_failed"));
            toast({description: t("master_users.permissions_updated")});
            onSaved();
            onOpenChange(false);
        } catch (e) {
            err(e);
        } finally {
            setBusy(false);
        }
    };

    return (
        <Dialog open={!!user} onOpenChange={onOpenChange}>
            <DialogContent className="flex max-h-[85vh] max-w-md flex-col overflow-hidden">
                <DialogHeader>
                    <DialogTitle className="font-display">{t("master_users.permissions_title", {username: user?.username})}</DialogTitle>
                    <DialogDescription>{t("master_users.permissions_description")}</DialogDescription>
                </DialogHeader>

                <div className="-mx-6 flex-1 overflow-y-auto px-6">
                    <div className="space-y-1 py-1">
                        {features.map((feature) => (
                            <div key={feature} className="flex items-center justify-between rounded-lg px-3 py-2.5 hover:bg-muted/50">
                                <span className="text-sm font-medium">{FEATURE_LABELS[feature] ? t(FEATURE_LABELS[feature]) : feature}</span>
                                <PermissionToggle value={(perms[feature] || 0) as Level}
                                                  onChange={(level) => setPerms((p) => ({...p, [feature]: level as Level}))}/>
                            </div>
                        ))}
                    </div>

                    <div className="mt-4 border-t pt-4">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm font-medium">{t("master_users.access_all")}</p>
                                <p className="text-xs text-muted-foreground">{t("master_users.access_all_hint")}</p>
                            </div>
                            <Switch checked={allServers} onCheckedChange={setAllServers}/>
                        </div>
                        {!allServers && (
                            <div className="mt-3 space-y-1">
                                {servers.length === 0 && <p className="text-xs text-muted-foreground">{t("master_users.no_servers_exist")}</p>}
                                {servers.map((server) => (
                                    <label key={server.id}
                                           className="flex cursor-pointer items-center gap-2.5 rounded-lg px-3 py-2 hover:bg-muted/50">
                                        <Checkbox checked={serverIds.includes(server.id)}
                                                  onCheckedChange={() => toggleServer(server.id)}/>
                                        <span className="text-sm">{server.name}</span>
                                        <span className="text-xs text-muted-foreground">{server.mcVersion}</span>
                                    </label>
                                ))}
                            </div>
                        )}
                    </div>
                </div>

                <DialogFooter>
                    <Button variant="outline" onClick={() => onOpenChange(false)}>{t("action.cancel")}</Button>
                    <Button disabled={busy} onClick={save}>{t("action.save")}</Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

const accessSummary = (user: MasterUser) => {
    if (user.isAdmin || user.allServers) return t("master_users.access.all_servers");
    const n = user.serverIds?.length || 0;
    return n === 0 ? t("master_users.access.no_servers") : t("master_users.access.server_count", {count: n});
};

const permSummary = (user: MasterUser, features: string[]) => {
    if (user.isAdmin) return t("master_users.perm.full_access");
    const full = features.filter((f) => (user.permissions[f] || 0) === 2).length;
    const read = features.filter((f) => (user.permissions[f] || 0) === 1).length;
    if (!full && !read) return t("master_users.perm.no_access");
    return [full && t("master_users.perm.full", {count: full}), read && t("master_users.perm.read", {count: read})].filter(Boolean).join(", ");
};

const Users = () => {
    const {authenticated, loading: authLoading, can, user: me} = useMasterAuth();
    const [users, setUsers] = useState<MasterUser[]>([]);
    const [features, setFeatures] = useState<string[]>([]);
    const [servers, setServers] = useState<ManagedServer[]>([]);
    const [loading, setLoading] = useState(true);

    const [createOpen, setCreateOpen] = useState(false);
    const [permsUser, setPermsUser] = useState<MasterUser | null>(null);
    const [usernameUser, setUsernameUser] = useState<MasterUser | null>(null);
    const [passwordUser, setPasswordUser] = useState<MasterUser | null>(null);
    const [deleteUser, setDeleteUser] = useState<MasterUser | null>(null);

    const load = async () => {
        try {
            const [u, f, s] = await Promise.all([
                masterJson("users"), masterJson("users/features"), masterJson("servers"),
            ]);
            setUsers(u.users || []);
            setFeatures(f.features || []);
            setServers(s.servers || []);
        } catch (e) {
            err(e);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (authenticated) load();
    }, [authenticated]);

    const confirmDelete = async () => {
        if (!deleteUser) return;
        const target = deleteUser;
        setDeleteUser(null);
        try {
            const res = await masterDelete(`users/${target.id}`);
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || t("master_users.delete_failed"));
            toast({description: t("master_users.deleted")});
            load();
        } catch (e) {
            err(e);
        }
    };

    if (!authLoading && !authenticated) return <Navigate to="/login" replace/>;
    if (!authLoading && authenticated && !can("UserManagement", 2)) return <Navigate to="/servers" replace/>;

    return (
        <MasterLayout active="users" title={t("master_users.title")} subtitle={`${users.length}`}
                      actions={<Button onClick={() => setCreateOpen(true)}>
                          <PlusIcon weight="bold" className="mr-1.5 size-4"/> {t("master_users.new_user")}
                      </Button>}>
            {loading ? (
                <div className="space-y-2.5">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-14 rounded-xl"/>)}</div>
            ) : (
                <div className="overflow-x-auto rounded-2xl border border-border/60 bg-card/40">
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>{t("master_users.col.user")}</TableHead>
                                <TableHead>{t("master_users.col.permissions")}</TableHead>
                                <TableHead>{t("master_users.col.access")}</TableHead>
                                <TableHead className="w-[150px] text-right">{t("master_users.col.actions")}</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {users.map((user) => (
                                <TableRow key={user.id}>
                                    <TableCell>
                                        <div className="flex items-center gap-3">
                                            <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-sm font-semibold text-primary">
                                                {user.username.charAt(0).toUpperCase()}
                                            </div>
                                            <div className="flex items-center gap-2">
                                                <span className="font-medium">{user.username}</span>
                                                {user.isAdmin && (
                                                    <Badge className="gap-1 bg-primary/20 py-0 text-xs text-primary hover:bg-primary/30">
                                                        <ShieldCheckIcon className="size-3" weight="fill"/> {t("master_users.admin")}
                                                    </Badge>
                                                )}
                                            </div>
                                        </div>
                                    </TableCell>
                                    <TableCell><span className="text-sm text-muted-foreground">{permSummary(user, features)}</span></TableCell>
                                    <TableCell><span className="text-sm text-muted-foreground">{accessSummary(user)}</span></TableCell>
                                    <TableCell>
                                        <div className="flex items-center justify-end gap-0.5">
                                            <Button variant="ghost" size="icon" className="size-8" disabled={user.isAdmin}
                                                    title={user.isAdmin ? t("master_users.action.admin_full") : t("master_users.action.edit_permissions")}
                                                    onClick={() => setPermsUser(user)}>
                                                <LockKeyIcon className="size-4"/>
                                            </Button>
                                            <Button variant="ghost" size="icon" className="size-8" title={t("master_users.action.rename")}
                                                    onClick={() => setUsernameUser(user)}>
                                                <PencilIcon className="size-4"/>
                                            </Button>
                                            <Button variant="ghost" size="icon" className="size-8" title={t("master_users.action.change_password")}
                                                    onClick={() => setPasswordUser(user)}>
                                                <KeyIcon className="size-4"/>
                                            </Button>
                                            <Button variant="ghost" size="icon"
                                                    className="size-8 text-destructive hover:text-destructive"
                                                    disabled={user.isAdmin || user.id === me?.id} title={t("master_users.action.delete")}
                                                    onClick={() => setDeleteUser(user)}>
                                                <TrashIcon className="size-4"/>
                                            </Button>
                                        </div>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </div>
            )}

            <CreateUserDialog open={createOpen} onOpenChange={setCreateOpen} onCreated={load}/>
            <PermissionsDialog user={permsUser} features={features} servers={servers}
                               onOpenChange={(o) => !o && setPermsUser(null)} onSaved={load}/>
            <FieldDialog open={!!usernameUser} onOpenChange={(o) => !o && setUsernameUser(null)}
                         title={t("master_users.rename_title")} label={t("master_users.username")} initial={usernameUser?.username}
                         onSave={async (value) => {
                             const res = await masterRequest(`users/${usernameUser!.id}/username`, "PUT", {username: value});
                             const data = await res.json();
                             if (!res.ok) throw new Error(data.error || t("master_users.rename_failed"));
                             toast({description: t("master_users.username_updated")});
                             load();
                         }}/>
            <FieldDialog open={!!passwordUser} onOpenChange={(o) => !o && setPasswordUser(null)}
                         title={t("master_users.change_password_title")} label={t("master_users.new_password")} type="password"
                         onSave={async (value) => {
                             const res = await masterRequest(`users/${passwordUser!.id}/password`, "PUT", {password: value});
                             const data = await res.json();
                             if (!res.ok) throw new Error(data.error || t("master_users.change_password_failed"));
                             toast({description: t("master_users.password_updated")});
                         }}/>

            <AlertDialog open={!!deleteUser} onOpenChange={(o) => !o && setDeleteUser(null)}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle className="font-display">{t("master_users.delete.title", {username: deleteUser?.username})}</AlertDialogTitle>
                        <AlertDialogDescription>{t("master_users.delete.description")}</AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>{t("action.cancel")}</AlertDialogCancel>
                        <AlertDialogAction className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                                           onClick={confirmDelete}>{t("action.delete")}</AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </MasterLayout>
    );
};

export default Users;
