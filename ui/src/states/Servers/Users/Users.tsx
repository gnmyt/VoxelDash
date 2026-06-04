import {useEffect, useState} from "react";
import {Navigate} from "react-router-dom";
import {
    KeyIcon, LockKeyIcon, PencilIcon, PlusIcon, ShieldCheckIcon, TrashIcon, UsersThreeIcon,
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
    Servers: "Servers",
    Forwardings: "Forwardings",
    UserManagement: "User management",
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
        if (username.length < 3) return err(new Error("Username must be at least 3 characters"));
        if (password.length < 4) return err(new Error("Password must be at least 4 characters"));
        if (password !== confirm) return err(new Error("Passwords don't match"));
        setBusy(true);
        try {
            const res = await masterRequest("users", "POST", {username, password});
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || "Failed to create user");
            toast({description: "User created"});
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
                    <DialogTitle className="font-display">Create user</DialogTitle>
                    <DialogDescription>New users start with no access until you grant permissions.</DialogDescription>
                </DialogHeader>
                <div className="space-y-3">
                    <div className="space-y-1.5">
                        <Label>Username</Label>
                        <Input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus/>
                    </div>
                    <div className="space-y-1.5">
                        <Label>Password</Label>
                        <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)}/>
                    </div>
                    <div className="space-y-1.5">
                        <Label>Confirm password</Label>
                        <Input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)}/>
                    </div>
                </div>
                <DialogFooter>
                    <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
                    <Button disabled={busy} onClick={submit}>Create</Button>
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
                    <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
                    <Button disabled={busy} onClick={submit}>Save</Button>
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
            if (!res.ok) throw new Error(data.error || "Failed to update permissions");
            toast({description: "Permissions updated"});
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
                    <DialogTitle className="font-display">Permissions · {user?.username}</DialogTitle>
                    <DialogDescription>Control what this user can do across the servers.</DialogDescription>
                </DialogHeader>

                <div className="-mx-6 flex-1 overflow-y-auto px-6">
                    <div className="space-y-1 py-1">
                        {features.map((feature) => (
                            <div key={feature} className="flex items-center justify-between rounded-lg px-3 py-2.5 hover:bg-muted/50">
                                <span className="text-sm font-medium">{FEATURE_LABELS[feature] || feature}</span>
                                <PermissionToggle value={(perms[feature] || 0) as Level}
                                                  onChange={(level) => setPerms((p) => ({...p, [feature]: level as Level}))}/>
                            </div>
                        ))}
                    </div>

                    <div className="mt-4 border-t pt-4">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm font-medium">Access all servers</p>
                                <p className="text-xs text-muted-foreground">Off lets you pick specific servers.</p>
                            </div>
                            <Switch checked={allServers} onCheckedChange={setAllServers}/>
                        </div>
                        {!allServers && (
                            <div className="mt-3 space-y-1">
                                {servers.length === 0 && <p className="text-xs text-muted-foreground">No servers exist yet.</p>}
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
                    <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
                    <Button disabled={busy} onClick={save}>Save</Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

const accessSummary = (user: MasterUser) => {
    if (user.isAdmin || user.allServers) return "All servers";
    const n = user.serverIds?.length || 0;
    return n === 0 ? "No servers" : `${n} server${n === 1 ? "" : "s"}`;
};

const permSummary = (user: MasterUser, features: string[]) => {
    if (user.isAdmin) return "Full access";
    const full = features.filter((f) => (user.permissions[f] || 0) === 2).length;
    const read = features.filter((f) => (user.permissions[f] || 0) === 1).length;
    if (!full && !read) return "No access";
    return [full && `${full} full`, read && `${read} read`].filter(Boolean).join(", ");
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
            if (!res.ok) throw new Error(data.error || "Failed to delete user");
            toast({description: "User deleted"});
            load();
        } catch (e) {
            err(e);
        }
    };

    if (!authLoading && !authenticated) return <Navigate to="/login" replace/>;
    if (!authLoading && authenticated && !can("UserManagement", 2)) return <Navigate to="/servers" replace/>;

    return (
        <MasterLayout active="users" title="Users" subtitle={`${users.length}`}
                      actions={<Button onClick={() => setCreateOpen(true)}>
                          <PlusIcon weight="bold" className="mr-1.5 size-4"/> New user
                      </Button>}>
            {loading ? (
                <div className="space-y-2.5">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-14 rounded-xl"/>)}</div>
            ) : (
                <div className="rounded-2xl border border-border/60 bg-card/40">
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>User</TableHead>
                                <TableHead>Permissions</TableHead>
                                <TableHead>Access</TableHead>
                                <TableHead className="w-[150px] text-right">Actions</TableHead>
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
                                                        <ShieldCheckIcon className="size-3" weight="fill"/> Admin
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
                                                    title={user.isAdmin ? "The admin always has full access" : "Edit permissions"}
                                                    onClick={() => setPermsUser(user)}>
                                                <LockKeyIcon className="size-4"/>
                                            </Button>
                                            <Button variant="ghost" size="icon" className="size-8" title="Rename"
                                                    onClick={() => setUsernameUser(user)}>
                                                <PencilIcon className="size-4"/>
                                            </Button>
                                            <Button variant="ghost" size="icon" className="size-8" title="Change password"
                                                    onClick={() => setPasswordUser(user)}>
                                                <KeyIcon className="size-4"/>
                                            </Button>
                                            <Button variant="ghost" size="icon"
                                                    className="size-8 text-destructive hover:text-destructive"
                                                    disabled={user.isAdmin || user.id === me?.id} title="Delete"
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
                         title="Rename user" label="Username" initial={usernameUser?.username}
                         onSave={async (value) => {
                             const res = await masterRequest(`users/${usernameUser!.id}/username`, "PUT", {username: value});
                             const data = await res.json();
                             if (!res.ok) throw new Error(data.error || "Failed to rename");
                             toast({description: "Username updated"});
                             load();
                         }}/>
            <FieldDialog open={!!passwordUser} onOpenChange={(o) => !o && setPasswordUser(null)}
                         title="Change password" label="New password" type="password"
                         onSave={async (value) => {
                             const res = await masterRequest(`users/${passwordUser!.id}/password`, "PUT", {password: value});
                             const data = await res.json();
                             if (!res.ok) throw new Error(data.error || "Failed to change password");
                             toast({description: "Password updated"});
                         }}/>

            <AlertDialog open={!!deleteUser} onOpenChange={(o) => !o && setDeleteUser(null)}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle className="font-display">Delete {deleteUser?.username}?</AlertDialogTitle>
                        <AlertDialogDescription>This removes the user and their access. This can't be undone.</AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                                           onClick={confirmDelete}>Delete</AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </MasterLayout>
    );
};

export default Users;
