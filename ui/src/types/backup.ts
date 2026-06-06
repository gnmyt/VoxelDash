export type BackupType = "ROOT" | "WORLDS" | "PLUGINS" | "CONFIGS" | "LOGS" | "MODS"

export interface Backup {
  id: number
  size: number
  name: string
  modes: BackupType[]
}

export interface BackupOption {
  id: string
  bit: number
}
