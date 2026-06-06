import {JSONContent} from "@tiptap/react";
import {Motd, MotdSegment} from "@/types/motd";

export const NAMED_COLORS: { name: string; hex: string }[] = [
    {name: "black", hex: "#000000"},
    {name: "dark_blue", hex: "#0000AA"},
    {name: "dark_green", hex: "#00AA00"},
    {name: "dark_aqua", hex: "#00AAAA"},
    {name: "dark_red", hex: "#AA0000"},
    {name: "dark_purple", hex: "#AA00AA"},
    {name: "gold", hex: "#FFAA00"},
    {name: "gray", hex: "#AAAAAA"},
    {name: "dark_gray", hex: "#555555"},
    {name: "blue", hex: "#5555FF"},
    {name: "green", hex: "#55FF55"},
    {name: "aqua", hex: "#55FFFF"},
    {name: "red", hex: "#FF5555"},
    {name: "light_purple", hex: "#FF55FF"},
    {name: "yellow", hex: "#FFFF55"},
    {name: "white", hex: "#FFFFFF"},
];

const NAME_TO_HEX = new Map(NAMED_COLORS.map((c) => [c.name, c.hex]));
const HEX_TO_NAME = new Map(NAMED_COLORS.map((c) => [c.hex.toUpperCase(), c.name]));

export const DEFAULT_COLOR = "#FFFFFF";

export const colorToHex = (color: string | null | undefined): string => {
    if (!color) return DEFAULT_COLOR;
    if (color.startsWith("#")) return color.toUpperCase();
    return NAME_TO_HEX.get(color) ?? DEFAULT_COLOR;
};

export const normalizeColor = (color: string | null | undefined): string | null => {
    if (!color) return null;
    const value = color.trim();
    if (value.startsWith("#")) {
        if (value.length === 4) {
            return ("#" + value.slice(1).split("").map((c) => c + c).join("")).toUpperCase();
        }
        return value.toUpperCase();
    }
    const rgb = value.match(/rgba?\(\s*(\d+)[,\s]+(\d+)[,\s]+(\d+)/i);
    if (rgb) {
        const hex = [rgb[1], rgb[2], rgb[3]]
            .map((n) => Math.max(0, Math.min(255, parseInt(n, 10))).toString(16).padStart(2, "0"))
            .join("");
        return ("#" + hex).toUpperCase();
    }
    return null;
};

const colorToModel = (cssColor: string | null | undefined): string | null => {
    const hex = normalizeColor(cssColor);
    if (!hex) return null;
    return HEX_TO_NAME.get(hex) ?? hex;
};

const MAX_LINES = 2;

export const motdToDoc = (motd: Motd | null | undefined): JSONContent => {
    const content: JSONContent[] = [];
    const lines = motd?.lines?.slice(0, MAX_LINES) ?? [];

    lines.forEach((line, index) => {
        if (index > 0) content.push({type: "hardBreak"});
        for (const segment of line.segments ?? []) {
            if (!segment.text) continue;
            const marks: JSONContent["marks"] = [];
            if (segment.bold) marks!.push({type: "bold"});
            if (segment.italic) marks!.push({type: "italic"});
            if (segment.underlined) marks!.push({type: "underline"});
            if (segment.strikethrough) marks!.push({type: "strike"});
            if (segment.obfuscated) marks!.push({type: "obfuscated"});
            if (segment.color) marks!.push({type: "textStyle", attrs: {color: colorToHex(segment.color)}});
            content.push({type: "text", text: segment.text, marks});
        }
    });

    return {
        type: "doc",
        content: [{type: "paragraph", content: content.length ? content : undefined}],
    };
};

export const docToMotd = (doc: JSONContent | null | undefined): Motd => {
    const paragraph = doc?.content?.find((node) => node.type === "paragraph") ?? doc?.content?.[0];
    const nodes = paragraph?.content ?? [];

    const lines: MotdSegment[][] = [[]];
    let lineIndex = 0;

    for (const node of nodes) {
        if (node.type === "hardBreak") {
            if (lineIndex < MAX_LINES - 1) {
                lineIndex++;
                lines.push([]);
            }
            continue;
        }
        if (node.type !== "text" || !node.text) continue;

        const segment: MotdSegment = {text: node.text};
        for (const mark of node.marks ?? []) {
            switch (mark.type) {
                case "bold":
                    segment.bold = true;
                    break;
                case "italic":
                    segment.italic = true;
                    break;
                case "underline":
                    segment.underlined = true;
                    break;
                case "strike":
                    segment.strikethrough = true;
                    break;
                case "obfuscated":
                    segment.obfuscated = true;
                    break;
                case "textStyle":
                    if (mark.attrs?.color) segment.color = colorToModel(mark.attrs.color as string);
                    break;
                default:
                    break;
            }
        }
        lines[lineIndex].push(segment);
    }

    return {
        lines: lines.map((segments) => ({segments: segments.length ? segments : [{text: ""}]})),
    };
};

export const countDocLines = (doc: JSONContent | null | undefined): number => {
    const paragraph = doc?.content?.find((node) => node.type === "paragraph") ?? doc?.content?.[0];
    const breaks = (paragraph?.content ?? []).filter((node) => node.type === "hardBreak").length;
    return breaks + 1;
};

export const MAX_MOTD_LINES = MAX_LINES;
