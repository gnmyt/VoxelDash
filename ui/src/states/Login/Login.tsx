import {FormEvent, useContext, useState} from "react";
import {Navigate, useNavigate} from "react-router-dom";
import {useMasterAuth} from "@/contexts/MasterAuthContext.tsx";
import {ServerInfoContext} from "@/contexts/ServerInfoContext.tsx";
import {isMasterMode, postRequest} from "@/lib/RequestUtil.ts";
import LoginForm from "@/states/Login/components/LoginForm.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Input} from "@/components/ui/input.tsx";
import {Label} from "@/components/ui/label.tsx";
import {toast} from "@/hooks/use-toast.ts";
import logo from "@/assets/images/logo.png";
import {SpinnerGapIcon, ArrowRightIcon} from "@phosphor-icons/react";
import {t} from "i18next";

const Login = () => isMasterMode() ? <MasterLogin/> : <StandaloneLogin/>;

const StandaloneLogin = () => {
    const {checkToken, tokenValid} = useContext(ServerInfoContext)!;
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");

    if (tokenValid) return <Navigate to="/" replace/>;

    const login = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        postRequest("session/create", {username, password}).then(async (r) => {
            if (!r.session) throw new Error("Invalid credentials");
            localStorage.setItem("sessionToken", r.session);
            await checkToken();
        }).catch(() => {
            toast({description: t("login.failed"), variant: "destructive"});
        });
    };

    return (
        <div className="grid min-h-screen lg:grid-cols-2">
            <div className="flex flex-col gap-4 p-6 md:p-10">
                <div className="flex flex-1 items-center justify-center">
                    <div className="w-full max-w-xs">
                        <LoginForm username={username} setUsername={setUsername} password={password}
                                   setPassword={setPassword} login={login}/>
                    </div>
                </div>
            </div>
            <div className="relative hidden items-center justify-center p-8 lg:flex">
                <img src={logo} alt="VoxelDash" className="max-h-lg max-w-lg h-4/5 w-4/5 object-contain"/>
            </div>
        </div>
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
            toast({description: (err as Error).message || "Authentication failed", variant: "destructive"});
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="relative grid min-h-screen bg-background text-foreground lg:grid-cols-[1.05fr_minmax(440px,0.95fr)]">
            <div aria-hidden className="pointer-events-none absolute inset-y-0 left-[52.5%] hidden w-px bg-border lg:block"/>

            <div className="relative hidden flex-col justify-between overflow-hidden p-12 xl:p-16 lg:flex">
                <div className="relative flex items-center gap-3.5">
                    <img src={logo} alt="VoxelDash" className="size-12 rounded-2xl shadow-sm"/>
                    <div className="font-display text-xl font-bold tracking-tight">VoxelDash One</div>
                </div>

                <div className="relative max-w-lg space-y-6">
                    <h1 className="font-display text-[2.9rem] font-bold leading-[1.04] tracking-tight">
                        All your Minecraft<br/>servers in one place.
                    </h1>
                    <p className="max-w-md text-[15px] leading-relaxed text-muted-foreground">
                        Create Paper, Fabric, Vanilla or proxy servers from here. VoxelDash One downloads
                        the server jar and a matching Java version, then starts it for you. No manual
                        setup, no SSH.
                    </p>
                </div>

                <div className="relative font-mono text-xs text-muted-foreground">v1.2.0</div>
            </div>

            <div className="relative flex items-center justify-center p-6 md:p-10">
                <div className="vd-rise w-full max-w-sm">
                    <div className="mb-10 flex items-center gap-3 lg:hidden">
                        <img src={logo} alt="VoxelDash" className="size-10 rounded-xl"/>
                        <span className="font-display text-lg font-bold">VoxelDash One</span>
                    </div>

                    <h2 className="font-display text-[1.75rem] font-bold leading-tight">
                        {setupRequired ? "Set up your admin account" : "Welcome back"}
                    </h2>
                    <p className="mb-8 mt-2 text-sm leading-relaxed text-muted-foreground">
                        {setupRequired
                            ? "This is the first run. Create the admin account for this instance."
                            : "Sign in to manage your servers."}
                    </p>

                    <form onSubmit={submit} className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="username">Username</Label>
                            <Input id="username" value={username} autoFocus autoComplete="username"
                                   onChange={(e) => setUsername(e.target.value)} placeholder="admin"/>
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="password">Password</Label>
                            <Input id="password" type="password" value={password}
                                   autoComplete={setupRequired ? "new-password" : "current-password"}
                                   onChange={(e) => setPassword(e.target.value)} placeholder="••••••••"/>
                        </div>
                        <Button type="submit" size="lg" className="mt-2 w-full"
                                disabled={busy || loading || !username || !password}>
                            {busy ? <SpinnerGapIcon className="size-4 animate-spin"/> : (
                                <>
                                    {setupRequired ? "Create admin account" : "Sign in"}
                                    <ArrowRightIcon weight="bold" className="ml-1.5 size-4"/>
                                </>
                            )}
                        </Button>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default Login;
