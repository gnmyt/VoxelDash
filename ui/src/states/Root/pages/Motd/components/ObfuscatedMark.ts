import {Mark, mergeAttributes} from "@tiptap/react";

const OBF_GLYPHS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789?!@#$%&/\\<>=+*";

export const Obfuscated = Mark.create({
    name: "obfuscated",

    parseHTML() {
        return [{tag: "span[data-motd-obfuscated]"}];
    },

    renderHTML({HTMLAttributes}) {
        return [
            "span",
            mergeAttributes(HTMLAttributes, {"data-motd-obfuscated": "true", class: "motd-obfuscated"}),
            0,
        ];
    },

    addMarkView() {
        return () => {
            const dom = document.createElement("span");
            dom.className = "motd-obfuscated";

            const contentDOM = document.createElement("span");
            contentDOM.className = "motd-obf-real";

            const overlay = document.createElement("span");
            overlay.className = "motd-obf-scramble";
            overlay.setAttribute("aria-hidden", "true");

            dom.appendChild(contentDOM);
            dom.appendChild(overlay);

            const scramble = () => {
                let out = "";
                for (const ch of contentDOM.textContent ?? "") {
                    out += ch === " " ? " " : OBF_GLYPHS.charAt(Math.floor(Math.random() * OBF_GLYPHS.length));
                }
                overlay.textContent = out;
            };
            scramble();
            const timer = window.setInterval(scramble, 70);

            return {
                dom,
                contentDOM,
                ignoreMutation: (mutation) => overlay === mutation.target || overlay.contains(mutation.target as Node),
                destroy: () => window.clearInterval(timer),
            };
        };
    },
});
