import { useState } from "react";


const FLAT_VERSION = "1.21.1";

const RENDERS = "https://cdn.jsdelivr.net/gh/Owen1212055/mc-assets@main/item-assets";

const stripNamespace = (id: string): string => {
    const colon = id.indexOf(":");
    return colon >= 0 ? id.substring(colon + 1) : id;
};

export const itemIconUrls = (id: string): string[] => {
    const name = stripNamespace(id);
    const flat = `https://assets.mcasset.cloud/${FLAT_VERSION}/assets/minecraft/textures`;
    return [
        `${RENDERS}/${name.toUpperCase()}.png`,
        `${flat}/item/${name.toLowerCase()}.png`,
        `${flat}/block/${name.toLowerCase()}.png`,
    ];
};

interface ItemIconProps {
    id: string;
    className?: string;
    alt?: string;
}

export const ItemIcon = ({ id, className, alt }: ItemIconProps) => {
    const urls = itemIconUrls(id);
    const [index, setIndex] = useState(0);

    if (index >= urls.length) {
        const letter = stripNamespace(id).charAt(0).toUpperCase();
        return (
            <div
                className={`flex items-center justify-center bg-muted text-[10px] text-muted-foreground ${className ?? ""}`}
                title={alt ?? id}
            >
                {letter}
            </div>
        );
    }

    const pixelated = index > 0;

    return (
        <img
            src={urls[index]}
            alt={alt ?? id}
            title={alt ?? id}
            className={className}
            style={pixelated ? { imageRendering: "pixelated" } : undefined}
            onError={() => setIndex((i) => i + 1)}
        />
    );
};

export default ItemIcon;
