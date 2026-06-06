const hashHue = (value: string): number => {
    let hash = 0;
    for (let i = 0; i < value.length; i++) {
        hash = (hash * 31 + value.charCodeAt(i)) >>> 0;
    }
    return hash % 360;
};

export const colorForResource = (resource: string | null, type: string | null): string => {
    switch (type) {
        case "minecraft":
            return "hsl(270 12% 38%)";
        case "jvm":
            return "hsl(270 8% 28%)";
        case "shared":
            return "hsl(38 45% 48%)";
        case "unknown":
            return "hsl(270 6% 34%)";
        default:
            return `hsl(${hashHue(resource || "")} 52% 52%)`;
    }
};

export const shortClass = (className: string | null): string => {
    if (!className) return "?";
    const idx = className.lastIndexOf(".");
    return idx >= 0 ? className.substring(idx + 1) : className;
};
