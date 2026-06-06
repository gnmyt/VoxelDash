import {Label} from "@/components/ui/label.js";
import {Input} from "@/components/ui/input.js";
import {Button} from "@/components/ui/button.js";

import PasswordDialog from "@/states/Login/components/PasswordDialog.tsx";
import React from "react";
import {SpinnerGapIcon, ArrowRightIcon} from "@phosphor-icons/react";
import {t} from "i18next";

interface LoginFormProps {
    username: string;
    setUsername: (value: string) => void;
    password: string;
    setPassword: (value: string) => void;
    login: (event: React.FormEvent<HTMLFormElement>) => void;
    busy?: boolean;
}

const LoginForm: React.FC<LoginFormProps> = ({username, setUsername, password, setPassword, login, busy}) => {
    return (
        <form className="space-y-4" onSubmit={login}>
            <div className="space-y-2">
                <Label htmlFor="username">{t("login.name")}</Label>
                <Input
                    id="username"
                    type="text"
                    placeholder={t("login.name")}
                    required
                    autoFocus
                    autoComplete="username"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                />
            </div>
            <div className="space-y-2">
                <div className="flex items-center justify-between">
                    <Label htmlFor="password">{t("login.password")}</Label>
                    <PasswordDialog/>
                </div>
                <Input
                    id="password"
                    type="password"
                    placeholder="••••••••"
                    required
                    autoComplete="current-password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                />
            </div>
            <Button type="submit" size="lg" className="mt-2 w-full" disabled={busy || !username || !password}>
                {busy ? <SpinnerGapIcon className="size-4 animate-spin"/> : (
                    <>
                        {t("login.sign_in")}
                        <ArrowRightIcon weight="bold" className="ml-1.5 size-4"/>
                    </>
                )}
            </Button>
        </form>
    );
};

export default LoginForm;
