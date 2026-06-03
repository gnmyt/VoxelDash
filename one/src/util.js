import net from "node:net";

export const randomToken = (bytes = 32) => {
    const arr = new Uint8Array(bytes);
    crypto.getRandomValues(arr);
    return Array.from(arr, (b) => b.toString(16).padStart(2, "0")).join("");
};

export const randomId = () => {
    return randomToken(8);
};

const reservePort = () => {
    return new Promise((resolveFn, reject) => {
        const srv = net.createServer();
        srv.unref();
        srv.on("error", reject);
        srv.listen(0, "127.0.0.1", () => resolveFn(srv));
    });
};

export const findFreePorts = async (count) => {
    const servers = await Promise.all(Array.from({length: count}, reservePort));
    const ports = servers.map((srv) => srv.address().port);
    await Promise.all(servers.map((srv) => new Promise((res) => srv.close(res))));
    return ports;
};
