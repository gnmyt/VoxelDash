import { useEffect, useState } from "react";
import { UsersThreeIcon, PlusIcon, TrashIcon, PencilIcon, KeyIcon, ShieldCheckIcon, LockKeyIcon } from "@phosphor-icons/react";
import { t } from "i18next";
import { jsonRequest, deleteRequest, postRequest, putRequest } from "@/lib/RequestUtil";
import { User, PermissionLevel } from "@/types/user";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { toast } from "@/hooks/use-toast";
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
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import CreateUserDialog from "./components/CreateUserDialog";
import ChangePasswordDialog from "./components/ChangePasswordDialog";
import EditUsernameDialog from "./components/EditUsernameDialog";
import PermissionToggle from "./components/PermissionToggle";

const Users = () => {
    const [users, setUsers] = useState<User[]>([]);
    const [availableFeatures, setAvailableFeatures] = useState<string[]>([]);
    const [loading, setLoading] = useState(true);

    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [userToDelete, setUserToDelete] = useState<User | null>(null);
    const [createDialogOpen, setCreateDialogOpen] = useState(false);
    const [passwordDialogOpen, setPasswordDialogOpen] = useState(false);
    const [usernameDialogOpen, setUsernameDialogOpen] = useState(false);
    const [permissionsDialogOpen, setPermissionsDialogOpen] = useState(false);
    const [selectedUser, setSelectedUser] = useState<User | null>(null);
    const [updating, setUpdating] = useState<string | null>(null);

    const editableFeatures = availableFeatures.filter(f => f !== "UserManagement");

    const fetchUsers = async () => {
        try {
            const data = await jsonRequest("users");
            setUsers(data.users || []);
        } catch (error) {
            console.error("Failed to fetch users:", error);
        }
    };

    const fetchFeatures = async () => {
        try {
            const data = await jsonRequest("users/features");
            setAvailableFeatures(data.features || []);
        } catch (error) {
            console.error("Failed to fetch features:", error);
        }
    };

    const fetchAllData = async () => {
        setLoading(true);
        await Promise.all([fetchUsers(), fetchFeatures()]);
        setLoading(false);
    };

    useEffect(() => {
        fetchAllData();
    }, []);

    const handleDeleteUser = async () => {
        if (!userToDelete) return;
        try {
            const result = await deleteRequest(`users/${userToDelete.id}`);
            if (result.error) {
                toast({ variant: "destructive", description: result.error });
                return;
            }
            toast({ description: t("users.deleted") });
            await fetchUsers();
        } catch (error) {
            toast({ variant: "destructive", description: t("users.delete_failed") });
        } finally {
            setDeleteDialogOpen(false);
            setUserToDelete(null);
        }
    };

    const handleCreateUser = async (username: string, password: string) => {
        try {
            const result = await postRequest("users", { username, password });
            if (result.error) {
                toast({ variant: "destructive", description: result.error });
                return false;
            }
            toast({ description: t("users.created") });
            await fetchUsers();
            return true;
        } catch (error) {
            toast({ variant: "destructive", description: t("users.create_failed") });
            return false;
        }
    };

    const handleChangePassword = async (userId: number, password: string) => {
        try {
            const result = await putRequest(`users/${userId}/password`, { password });
            if (result.error) {
                toast({ variant: "destructive", description: result.error });
                return false;
            }
            toast({ description: t("users.password_changed") });
            return true;
        } catch (error) {
            toast({ variant: "destructive", description: t("users.password_change_failed") });
            return false;
        }
    };

    const handleChangeUsername = async (userId: number, username: string) => {
        try {
            const result = await putRequest(`users/${userId}/username`, { username });
            if (result.error) {
                toast({ variant: "destructive", description: result.error });
                return false;
            }
            toast({ description: t("users.username_changed") });
            await fetchUsers();
            return true;
        } catch (error) {
            toast({ variant: "destructive", description: t("users.username_change_failed") });
            return false;
        }
    };

    const handlePermissionChange = async (userId: number, feature: string, level: PermissionLevel) => {
        setUpdating(`${userId}-${feature}`);
        try {
            const result = await putRequest(`users/${userId}/permissions`, {
                permissions: { [feature]: level }
            });
            if (result.error) {
                toast({ variant: "destructive", description: result.error });
            } else {
                await fetchUsers();
                if (selectedUser?.id === userId) {
                    setSelectedUser(prev => prev ? {
                        ...prev,
                        permissions: { ...prev.permissions, [feature]: level }
                    } : null);
                }
            }
        } catch (error) {
            toast({ variant: "destructive", description: t("users.permission_update_failed") });
        } finally {
            setUpdating(null);
        }
    };

    const getPermissionSummary = (user: User) => {
        if (user.isAdmin) return t("users.full_access");
        const fullCount = editableFeatures.filter(f => (user.permissions[f] || 0) === 2).length;
        const readCount = editableFeatures.filter(f => (user.permissions[f] || 0) === 1).length;
        if (fullCount === 0 && readCount === 0) return t("users.no_access");
        const parts = [];
        if (fullCount > 0) parts.push(`${fullCount} ${t("users.level.full").toLowerCase()}`);
        if (readCount > 0) parts.push(`${readCount} ${t("users.level.read").toLowerCase()}`);
        return parts.join(", ");
    };

    const openPermissionsDialog = (user: User) => {
        setSelectedUser(user);
        setPermissionsDialogOpen(true);
    };

    return (
        <div className="flex flex-col p-6 pt-0 gap-6" style={{ height: 'calc(var(--app-vh) - 5.5rem)' }}>
            <div className="flex items-center justify-between p-4 rounded-xl border bg-card shrink-0">
                <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center">
                        <UsersThreeIcon className="h-6 w-6 text-primary" weight="fill" />
                    </div>
                    <div>
                        <h1 className="text-lg font-semibold">{t("users.title")}</h1>
                        <p className="text-sm text-muted-foreground">{t("users.subtitle")}</p>
                    </div>
                </div>
                <Button onClick={() => setCreateDialogOpen(true)} className="rounded-lg">
                    <PlusIcon className="h-4 w-4 mr-2" />
                    {t("users.create")}
                </Button>
            </div>

            <ScrollArea className="flex-1">
                {loading ? (
                    <div className="flex items-center justify-center py-16">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
                    </div>
                ) : users.length === 0 ? (
                    <div className="flex flex-col items-center justify-center py-16 text-center">
                        <div className="h-16 w-16 rounded-2xl bg-muted flex items-center justify-center mb-4">
                            <UsersThreeIcon className="h-8 w-8 text-muted-foreground" />
                        </div>
                        <p className="text-lg font-medium text-muted-foreground">{t("users.no_users")}</p>
                        <p className="text-sm text-muted-foreground mt-1">{t("users.no_users_description")}</p>
                    </div>
                ) : (
                    <div className="rounded-xl border bg-card">
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead>{t("users.table.user")}</TableHead>
                                    <TableHead>{t("users.permissions_summary")}</TableHead>
                                    <TableHead className="w-[140px]">{t("users.table.actions")}</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {users.map((user) => (
                                    <TableRow key={user.id}>
                                        <TableCell>
                                            <div className="flex items-center gap-3">
                                                <div className="h-9 w-9 rounded-lg bg-primary/10 flex items-center justify-center">
                                                    <span className="text-sm font-semibold text-primary">
                                                        {user.username.charAt(0).toUpperCase()}
                                                    </span>
                                                </div>
                                                <div>
                                                    <div className="flex items-center gap-2">
                                                        <span className="font-medium">{user.username}</span>
                                                        {user.isAdmin && (
                                                            <Badge className="bg-primary/20 text-primary hover:bg-primary/30 gap-1 text-xs py-0">
                                                                <ShieldCheckIcon className="h-3 w-3" weight="fill" />
                                                                {t("users.admin")}
                                                            </Badge>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                        </TableCell>
                                        <TableCell>
                                            <span className="text-sm text-muted-foreground">
                                                {getPermissionSummary(user)}
                                            </span>
                                        </TableCell>
                                        <TableCell>
                                            <div className="flex items-center gap-0.5">
                                                <Button
                                                    variant="ghost"
                                                    size="icon"
                                                    className="h-8 w-8"
                                                    onClick={() => openPermissionsDialog(user)}
                                                    disabled={user.isAdmin}
                                                    title={user.isAdmin ? t("users.admin_note") : t("users.edit_permissions")}
                                                >
                                                    <LockKeyIcon className="h-4 w-4" />
                                                </Button>
                                                <Button
                                                    variant="ghost"
                                                    size="icon"
                                                    className="h-8 w-8"
                                                    onClick={() => { setSelectedUser(user); setUsernameDialogOpen(true); }}
                                                    title={t("users.edit_username")}
                                                >
                                                    <PencilIcon className="h-4 w-4" />
                                                </Button>
                                                <Button
                                                    variant="ghost"
                                                    size="icon"
                                                    className="h-8 w-8"
                                                    onClick={() => { setSelectedUser(user); setPasswordDialogOpen(true); }}
                                                    title={t("users.change_password")}
                                                >
                                                    <KeyIcon className="h-4 w-4" />
                                                </Button>
                                                <Button
                                                    variant="ghost"
                                                    size="icon"
                                                    className="h-8 w-8 text-destructive hover:text-destructive"
                                                    onClick={() => { setUserToDelete(user); setDeleteDialogOpen(true); }}
                                                    title={t("users.delete")}
                                                >
                                                    <TrashIcon className="h-4 w-4" />
                                                </Button>
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </div>
                )}
            </ScrollArea>

            <CreateUserDialog
                open={createDialogOpen}
                onOpenChange={setCreateDialogOpen}
                onSubmit={handleCreateUser}
            />

            <ChangePasswordDialog
                open={passwordDialogOpen}
                onOpenChange={setPasswordDialogOpen}
                user={selectedUser}
                onSubmit={handleChangePassword}
            />

            <EditUsernameDialog
                open={usernameDialogOpen}
                onOpenChange={setUsernameDialogOpen}
                user={selectedUser}
                onSubmit={handleChangeUsername}
            />

            <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
                <AlertDialogContent className="rounded-xl">
                    <AlertDialogHeader>
                        <AlertDialogTitle>{t("users.delete_title")}</AlertDialogTitle>
                        <AlertDialogDescription>
                            {t("users.delete_description", { username: userToDelete?.username })}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>{t("action.cancel")}</AlertDialogCancel>
                        <AlertDialogAction
                            onClick={handleDeleteUser}
                            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                        >
                            {t("action.remove")}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            <Dialog open={permissionsDialogOpen} onOpenChange={setPermissionsDialogOpen}>
                <DialogContent className="max-w-md max-h-[80vh] overflow-hidden flex flex-col">
                    <DialogHeader>
                        <DialogTitle>{t("users.edit_permissions_for", { username: selectedUser?.username })}</DialogTitle>
                        <DialogDescription>{t("users.edit_permissions_description")}</DialogDescription>
                    </DialogHeader>
                    
                    <div className="flex items-center gap-4 py-3 border-b">
                        <div className="flex items-center gap-2">
                            <div className="w-2.5 h-2.5 rounded-full bg-muted border"></div>
                            <span className="text-xs text-muted-foreground">{t("users.level.none")}</span>
                        </div>
                        <div className="flex items-center gap-2">
                            <div className="w-2.5 h-2.5 rounded-full bg-yellow-500"></div>
                            <span className="text-xs text-muted-foreground">{t("users.level.read")}</span>
                        </div>
                        <div className="flex items-center gap-2">
                            <div className="w-2.5 h-2.5 rounded-full bg-green-500"></div>
                            <span className="text-xs text-muted-foreground">{t("users.level.full")}</span>
                        </div>
                    </div>

                    <div className="flex-1 overflow-y-auto -mx-6 px-6">
                        <div className="space-y-1 py-2">
                            {editableFeatures.map((feature) => (
                                <div key={feature} className="flex items-center justify-between py-2.5 px-3 rounded-lg hover:bg-muted/50">
                                    <span className="font-medium text-sm">{t(`users.features.${feature}`, feature)}</span>
                                    <PermissionToggle
                                        value={(selectedUser?.permissions[feature] || 0) as PermissionLevel}
                                        onChange={(level) => {
                                            if (selectedUser) {
                                                handlePermissionChange(selectedUser.id, feature, level);
                                                setSelectedUser(prev => prev ? {
                                                    ...prev,
                                                    permissions: { ...prev.permissions, [feature]: level }
                                                } : null);
                                            }
                                        }}
                                        disabled={updating === `${selectedUser?.id}-${feature}`}
                                    />
                                </div>
                            ))}
                        </div>
                    </div>
                </DialogContent>
            </Dialog>
        </div>
    );
};

export default Users;
