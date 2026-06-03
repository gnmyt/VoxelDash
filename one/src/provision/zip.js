import {readFileSync} from "node:fs";
import {inflateRawSync} from "node:zlib";

const EOCD_SIG = 0x06054b50;
const CD_SIG = 0x02014b50;
const LFH_SIG = 0x04034b50;

const findEocd = (buf) => {
    const min = Math.max(0, buf.length - 0x10000 - 22);
    for (let i = buf.length - 22; i >= min; i--) {
        if (buf.readUInt32LE(i) === EOCD_SIG) return i;
    }
    return -1;
};

const readLocalEntry = (buf, offset, method, compSize) => {
    if (buf.readUInt32LE(offset) !== LFH_SIG) return null;
    const nameLen = buf.readUInt16LE(offset + 26);
    const extraLen = buf.readUInt16LE(offset + 28);
    const dataStart = offset + 30 + nameLen + extraLen;
    const data = buf.subarray(dataStart, dataStart + compSize);
    if (method === 0) return Buffer.from(data);
    if (method === 8) return inflateRawSync(data);
    return null;
};

export const readZipEntry = (zipPath, entryName) => {
    const buf = readFileSync(zipPath);
    const eocd = findEocd(buf);
    if (eocd < 0) return null;

    const count = buf.readUInt16LE(eocd + 10);
    const cdOffset = buf.readUInt32LE(eocd + 16);
    if (count === 0xffff || cdOffset === 0xffffffff) return null;

    let p = cdOffset;
    for (let i = 0; i < count; i++) {
        if (buf.readUInt32LE(p) !== CD_SIG) break;
        const method = buf.readUInt16LE(p + 10);
        const compSize = buf.readUInt32LE(p + 20);
        const nameLen = buf.readUInt16LE(p + 28);
        const extraLen = buf.readUInt16LE(p + 30);
        const commentLen = buf.readUInt16LE(p + 32);
        const localOffset = buf.readUInt32LE(p + 42);
        const name = buf.toString("utf8", p + 46, p + 46 + nameLen);
        if (name === entryName) return readLocalEntry(buf, localOffset, method, compSize);
        p += 46 + nameLen + extraLen + commentLen;
    }
    return null;
};
