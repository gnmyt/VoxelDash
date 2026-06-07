<h1 align="center"><img src=".github/logo.png" alt="" width="32" height="32" style="vertical-align: middle;"> VoxelDash</h1>
<p align="center">A modern, beautiful web dashboard for managing your Minecraft server <em>(formerly MCDash)</em></p>
<p align="center">
  <a href="https://github.com/gnmyt/VoxelDash/stargazers"><img src="https://img.shields.io/github/stars/gnmyt/VoxelDash?style=flat-square&logo=github&color=f97316" alt="GitHub Stars"></a>
  <a href="https://github.com/gnmyt/VoxelDash/network/members"><img src="https://img.shields.io/github/forks/gnmyt/VoxelDash?style=flat-square&logo=github&color=f97316" alt="GitHub Forks"></a>
  <a href="https://github.com/gnmyt/VoxelDash/issues"><img src="https://img.shields.io/github/issues/gnmyt/VoxelDash?style=flat-square&logo=github" alt="GitHub Issues"></a>
  <a href="https://github.com/gnmyt/VoxelDash/releases/latest"><img src="https://img.shields.io/github/downloads/gnmyt/VoxelDash/total?style=flat-square&logo=github&color=f97316" alt="Downloads"></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/gnmyt/VoxelDash?style=flat-square" alt="License"></a>
</p>
<p align="center">
  <a href="https://voxeldash.dev">📖 Documentation</a> · 
  <a href="https://github.com/gnmyt/VoxelDash/issues/new?template=bug_report.md">🐛 Report Bug</a> · 
  <a href="https://github.com/gnmyt/VoxelDash/issues/new?template=feature_request.md">✨ Request Feature</a>
</p>

---

## Features

VoxelDash provides everything you need to manage your Minecraft server from a sleek, modern web interface:

| Feature | Description |
|---------|-------------|
| **Dashboard** | Real-time status with customizable widgets for TPS, memory, CPU, players, and more |
| **Players** | See who's online, manage the whitelist, bans, and operators, and edit inventories or profiles |
| **File Manager** | Browse, edit, upload, and download server files right in your browser |
| **Console** | Live console output and command execution, with optional SSH access |
| **Worlds** | Manage every world's time, weather, and difficulty, or create and delete worlds |
| **Plugins & Mods** | Install and update plugins and mods from Modrinth, SpigotMC, and CurseForge |
| **Backups** | Create, restore, and download backups, manually or on a schedule |
| **Schedules** | Automate commands, broadcasts, backups, and restarts |
| **MOTD Editor** | Design the message and icon shown in the multiplayer server list |
| **Profiling** | Track live performance metrics and find out what's making your server lag |
| **Game Rules** | Tweak the game's rules from a searchable list, applied to the running server |
| **Server Settings** | Edit `server.properties` through a clean, categorized editor |

VoxelDash runs as a plugin or mod on **Spigot, Paper, Fabric, and Forge**, or as a standalone app for **vanilla** servers. You can also run it in front of multiple servers at once with [VoxelDash One](https://voxeldash.dev/voxeldash-one/introduction).

## Screenshots

<div align="center">
  <table>
    <tr>
      <td align="center">
        <img src=".github/screenshots/overview.png" alt="Dashboard" width="400">
        <br><strong>Dashboard</strong>
      </td>
      <td align="center">
        <img src=".github/screenshots/players.png" alt="Players" width="400">
        <br><strong>Players</strong>
      </td>
    </tr>
    <tr>
      <td align="center">
        <img src=".github/screenshots/file_manager.png" alt="File Manager" width="400">
        <br><strong>File Manager</strong>
      </td>
      <td align="center">
        <img src=".github/screenshots/console.png" alt="Console" width="400">
        <br><strong>Console</strong>
      </td>
    </tr>
    <tr>
      <td align="center">
        <img src=".github/screenshots/worlds.png" alt="Worlds" width="400">
        <br><strong>Worlds</strong>
      </td>
      <td align="center">
        <img src=".github/screenshots/plugins.png" alt="Plugins & Mods" width="400">
        <br><strong>Plugins &amp; Mods</strong>
      </td>
    </tr>
    <tr>
      <td align="center">
        <img src=".github/screenshots/motd.png" alt="MOTD Editor" width="400">
        <br><strong>MOTD Editor</strong>
      </td>
      <td align="center">
        <img src=".github/screenshots/gamerules.png" alt="Game Rules" width="400">
        <br><strong>Game Rules</strong>
      </td>
    </tr>
  </table>
</div>

## Quick Start

### Requirements

- Java 17 or higher
- A Minecraft server (Spigot, Paper, Fabric, or Vanilla)

### Installation

1. **Download** the latest release from the [releases page](https://github.com/gnmyt/VoxelDash/releases/latest)

2. **Install** the plugin/mod on your server:
   - **Spigot/Paper**: Place the `.jar` file in the `plugins` folder
   - **Fabric/Forge**: Place the `.jar` file in the `mods` folder
   - **Vanilla**: Run the standalone `.jar` file

3. **Start** your server and access the dashboard at `http://localhost:7867`

4. **Login** with the credentials shown in the console on first start

For detailed installation instructions, check out our [documentation](https://voxeldash.dev/getting-started/introduction).

## Tech Stack

- **Frontend**: React, TypeScript, Vite, Tailwind CSS, shadcn/ui
- **Backend**: Java, integrated with Minecraft server APIs
- **Supported Platforms**: Spigot, Paper, Fabric, Forge, Vanilla

## Contributing

Contributions are welcome! Feel free to:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">
  <sub>Built with ❤️ by <a href="https://gnm.dev">GNM</a> and contributors</sub>
</div>
