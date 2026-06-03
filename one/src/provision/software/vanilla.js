import {mojangJavaMajor, mojangReleases} from "../mojang.js";
import {writeMinecraftProperties} from "./common.js";

export const vanilla = {
    key: "vanilla",
    name: "Vanilla",
    kind: "server",
    voxeldashArtifact: "vanilla",
    minJava: 8,

    loaderJar: true,

    async listVersions() {
        return mojangReleases();
    },

    async javaMajor(mcVersion) {
        return mojangJavaMajor(mcVersion);
    },

    layout: writeMinecraftProperties,

    launchArgs(jarFileName) {
        return ["-jar", jarFileName];
    },
};
