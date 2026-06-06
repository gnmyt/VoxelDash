import {FormEvent, useContext, useState} from "react";
import {Navigate, useNavigate} from "react-router-dom";
import {useMasterAuth} from "@/contexts/MasterAuthContext.tsx";
import {ServerInfoContext} from "@/contexts/ServerInfoContext.tsx";
import {isMasterMode, postRequest} from "@/lib/RequestUtil.ts";
import LoginForm from "@/states/Login/components/LoginForm.tsx";
import AuthLayout from "@/states/Login/components/AuthLayout.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Input} from "@/components/ui/input.tsx";
import {Label} from "@/components/ui/label.tsx";
import {toast} from "@/hooks/use-toast.ts";
import {SpinnerGapIcon, ArrowRightIcon} from "@phosphor-icons/react";
import {t} from "i18next";

const Login = () => isMasterMode() ? <MasterLogin/> : <StandaloneLogin/>;

const StandaloneLogin = () => {
    const {checkToken, tokenValid} = useContext(ServerInfoContext)!;
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [busy, setBusy] = useState(false);

    if (tokenValid) return <Navigate to="/" replace/>;

    const login = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setBusy(true);
        try {
            const r = await postRequest("session/create", {username, password});
            if (!r.session) throw new Error("Invalid credentials");
            localStorage.setItem("sessionToken", r.session);
            await checkToken();
        } catch {
            toast({description: t("login.failed"), variant: "destructive"});
        } finally {
            setBusy(false);
        }
    };

    return (
        <AuthLayout
            brand="VoxelDash"
            tagline={t("login.tagline")}
            description={t("login.description")}
            title={t("login.welcome_title")}
            subtitle={t("login.welcome_subtitle")}
        >
            <LoginForm username={username} setUsername={setUsername} password={password}
                       setPassword={setPassword} login={login} busy={busy}/>
        </AuthLayout>
    );
};

const MasterLogin = () => {
    const {authenticated, loading, setupRequired, login, setup} = useMasterAuth();
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [busy, setBusy] = useState(false);
    const navigate = useNavigate();

    if (authenticated) return <Navigate to="/servers" replace/>;

    const submit = async (event: FormEvent) => {
        event.preventDefault();
        setBusy(true);
        try {
            if (setupRequired) await setup(username, password);
            else await login(username, password);
            navigate("/servers");
        } catch (err) {
            toast({description: (err as Error).message || t("login.auth_failed"), variant: "destructive"});
        } finally {
            setBusy(false);
        }
    };

    return (
        <AuthLayout
            brand="VoxelDash One"
            tagline={t("login.master.tagline")}
            description={t("login.master.description")}
            title={setupRequired ? t("login.master.setup_title") : t("login.master.welcome_title")}
            subtitle={setupRequired ? t("login.master.setup_subtitle") : t("login.master.welcome_subtitle")}
        >
            <form onSubmit={submit} className="space-y-4">
                <div className="space-y-2">
                    <Label htmlFor="username">{t("login.master.username")}</Label>
                    <Input id="username" value={username} autoFocus autoComplete="username"
                           onChange={(e) => setUsername(e.target.value)} placeholder="admin"/>
                </div>
                <div className="space-y-2">
                    <Label htmlFor="password">{t("login.master.password")}</Label>
                    <Input id="password" type="password" value={password}
                           autoComplete={setupRequired ? "new-password" : "current-password"}
                           onChange={(e) => setPassword(e.target.value)} placeholder="••••••••"/>
                </div>
                <Button type="submit" size="lg" className="mt-2 w-full"
                        disabled={busy || loading || !username || !password}>
                    {busy ? <SpinnerGapIcon className="size-4 animate-spin"/> : (
                        <>
                            {setupRequired ? t("login.master.create_account") : t("login.master.sign_in")}
                            <ArrowRightIcon weight="bold" className="ml-1.5 size-4"/>
                        </>
                    )}
                </Button>
            </form>
        </AuthLayout>
    );
};

export default Login;
