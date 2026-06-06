export const GAME_RULE_WIKI_URL = "https://minecraft.wiki/w/Game_rule";

export const gameRuleWikiUrl = (key: string): string => {
    const local = key.includes(":") ? key.substring(key.lastIndexOf(":") + 1) : key;
    return `${GAME_RULE_WIKI_URL}#:~:text=${encodeURIComponent(local)}`;
};

export const humanizeGameRule = (key: string): string => {
    if (!key) return key;

    const local = key.includes(":") ? key.substring(key.lastIndexOf(":") + 1) : key;
    const spaced = local
        .replace(/_/g, " ")
        .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
        .replace(/([A-Z]+)([A-Z][a-z])/g, "$1 $2")
        .trim();
    return spaced.charAt(0).toUpperCase() + spaced.slice(1);
};
