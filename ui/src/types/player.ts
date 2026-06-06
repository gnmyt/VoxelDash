export interface OnlinePlayer {
  name: string;
  uuid: string;
  world: string;
  ipAddress: string;
  health: number;
  hunger: number;
  op: boolean;
  gamemode: string;
  playtime: number;
}

export interface OfflinePlayer {
  name: string;
  uuid: string;
}

export interface BannedPlayer {
  name: string;
  uuid: string;
  reason: string | null;
  banDate: number | null;
  expiry: number | null;
  source: string | null;
}

export interface WhitelistData {
  players: OfflinePlayer[];
  enabled: boolean;
}


export interface InventoryItem {
  slot: number;
  id: string;
  count: number;
  damage: number;
  maxDamage: number;
  name: string | null;
  enchanted: boolean;
  headOwner: string | null;
  lore: string[] | null;
}

export interface InventoryView {
  type: string;
  items: InventoryItem[];
  live: boolean;
}

export interface InventoryCapabilities {
  viewOnline: boolean;
  viewOffline: boolean;
  viewEnderChest: boolean;
  editOnline: boolean;
}

export interface TeleportCapabilities {
  coords: boolean;
  toPlayer: boolean;
  toSpawn: boolean;
  toServer: boolean;
}

export interface MuteCapabilities {
  supported: boolean;
}

export interface PlayerCapabilities {
  inventory: InventoryCapabilities;
  teleport: TeleportCapabilities;
  mute: MuteCapabilities;
}

export interface IpHistoryEntry {
  ip: string;
  firstSeen: number;
  lastSeen: number;
  count: number;
}

export interface PlayerProfile {
  name: string;
  uuid: string;
  online: boolean;
  firstJoin: number;
  lastSeen: number;
  playtimeMillis: number;
  health: number;
  foodLevel: number;
  gamemode: string | null;
  dimension: string | null;
  op: boolean;
  banned: boolean;
  whitelisted: boolean;
  muted: boolean;
  muteExpiry: number;
  ipHistory: IpHistoryEntry[];
}
