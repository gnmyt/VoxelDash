package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.World;
import de.gnm.voxeldash.api.pipes.worlds.WorldPipe;
import de.gnm.voxeldash.util.ForgeCompat;
import de.gnm.voxeldash.util.ForgeUtil;

import java.util.ArrayList;

public class WorldPipeImpl implements WorldPipe {

    @Override
    public ArrayList<World> getWorlds() {
        return ForgeUtil.compat().worlds();
    }

    @Override
    public void setTime(String worldName, String time) {
        long ticks;
        switch (time.toLowerCase()) {
            case "day": ticks = 1000; break;
            case "noon": ticks = 6000; break;
            case "sunset": ticks = 12000; break;
            case "night": ticks = 13000; break;
            case "midnight": ticks = 18000; break;
            case "sunrise": ticks = 23000; break;
            default:
                try {
                    ticks = Long.parseLong(time);
                } catch (NumberFormatException e) {
                    return;
                }
        }
        ForgeUtil.compat().setTime(worldName, ticks);
    }

    @Override
    public void setWeather(String worldName, String weather) {
        ForgeCompat.Weather kind;
        switch (weather.toLowerCase()) {
            case "clear": kind = ForgeCompat.Weather.CLEAR; break;
            case "rain": kind = ForgeCompat.Weather.RAIN; break;
            case "thunder": kind = ForgeCompat.Weather.THUNDER; break;
            default: return;
        }
        ForgeUtil.compat().setWeather(worldName, kind);
    }

    @Override
    public void setDifficulty(String worldName, String difficulty) {
        ForgeUtil.compat().setDifficulty(worldName, difficulty);
    }

    @Override
    public boolean createWorld(String worldName, String environment, String worldType, String seed) {
        return false;
    }

    @Override
    public boolean deleteWorld(String worldName) {
        return false;
    }

    @Override
    public void teleportPlayers(String fromWorld, String toWorld) {
        ForgeUtil.compat().teleportPlayers(fromWorld, toWorld);
    }

    @Override
    public void saveWorld(String worldName) {
        ForgeUtil.compat().saveWorld(worldName);
    }
}
