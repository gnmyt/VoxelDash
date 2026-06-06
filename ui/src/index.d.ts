declare const __APP_VERSION__: string

declare module '*.css'
declare module '*.png'
declare module '*.svg' {
    const src: string
    export default src
}