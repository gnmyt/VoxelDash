import {StrictMode} from "react"
import {createRoot} from "react-dom/client"
import "./index.css";
import App from "./App.tsx"
import {isDesktop, setupDesktopLinks} from "@/lib/desktop.ts";

if (isDesktop()) document.documentElement.classList.add("desktop");
setupDesktopLinks();

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <App/>
    </StrictMode>,
)
