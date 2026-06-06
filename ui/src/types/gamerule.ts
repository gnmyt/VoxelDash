export type GameRuleType = "BOOLEAN" | "INTEGER" | "STRING";

export interface GameRule {
    key: string;
    type: GameRuleType;
    value: string;
    defaultValue?: string | null;
}

export interface GameRuleCapabilities {
    canList: boolean;
    liveApply: boolean;
    requiresRestart: boolean;
    knownDefaults: boolean;
}
