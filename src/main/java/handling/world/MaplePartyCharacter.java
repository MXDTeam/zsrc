package handling.world;

import client.MapleCharacter;
import server.maps.MapleDoor;

import java.awt.*;
import java.io.Serializable;
import java.util.List;

public class MaplePartyCharacter implements Serializable {

    private static final long serialVersionUID = 6215463252132450750L;
    private final String name;
    private int id;
    private int level;
    private int vip; 
    private int channel;
    private int jobid;
    private int mapid;
    private int doorTown = 999999999;
    private int doorTarget = 999999999;
    private int doorSkill = 0;
    private Point doorPosition = new Point(0, 0);
    private boolean online;

    public MaplePartyCharacter(MapleCharacter maplechar) {
        this.name = maplechar.getName();
        this.level = maplechar.getLevel();
        this.channel = maplechar.getClient().getChannel();
        this.id = maplechar.getId();
        this.jobid = maplechar.getJob();
        this.mapid = maplechar.getMapId();
        this.online = true;

        final List<MapleDoor> doors = maplechar.getDoors();
        if (doors.size() > 0) {
            final MapleDoor door = doors.get(0);

            this.doorTown = door.getTown().getId();
            this.doorTarget = door.getTarget().getId();
            this.doorSkill = door.getSkill();
            this.doorPosition = door.getTargetPosition();
        } else {
            this.doorPosition = new Point(maplechar.getPosition());
        }
    }

    public MaplePartyCharacter() {
        this.name = "";
        //default values for everything
    }

    public int getLevel() {
        return level;
    }

    public int getChannel() {
        return channel;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public int getMapid() {
        return mapid;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public int getJobId() {
        return jobid;
    }

    public int getDoorTown() {
        return doorTown;
    }

    public int getDoorTarget() {
        return doorTarget;
    }

    public int getDoorSkill() {
        return doorSkill;
    }

    public Point getDoorPosition() {
        return doorPosition;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MaplePartyCharacter other = (MaplePartyCharacter) obj;
        if (name == null) {
            return other.name == null;
        } else return name.equals(other.name);
    }
}
