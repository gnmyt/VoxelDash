export interface MotdSegment {
    text: string;
    color?: string | null;
    bold?: boolean;
    italic?: boolean;
    underlined?: boolean;
    strikethrough?: boolean;
    obfuscated?: boolean;
}

export interface MotdLine {
    segments: MotdSegment[];
}

export interface Motd {
    lines: MotdLine[];
}

export interface MotdCapabilities {
    maxLines: number;
    hex: boolean;
    styles: boolean;
    favicon: boolean;
    liveApply: boolean;
    requiresRestart: boolean;
}

export interface CharStyle {
    color: string | null;
    bold: boolean;
    italic: boolean;
    underlined: boolean;
    strikethrough: boolean;
    obfuscated: boolean;
}

export interface EditorLine {
    text: string;
    runs: CharStyle[];
}

export type StyleKey = "bold" | "italic" | "underlined" | "strikethrough" | "obfuscated";
