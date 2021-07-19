/*
事件
*/
package scripting;

import client.MapleCharacter;
import client.MapleQuestStatus;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import server.MapleCarnivalParty;
import server.MapleItemInformationProvider;
import server.MapleSquad;
import server.MapleSquad.MapleSquadType;
import server.Timer.EventTimer;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.packet.UIPacket;

import javax.script.ScriptException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EventInstanceManager {

    private List<MapleCharacter> chars = new LinkedList<MapleCharacter>(); //this is messy
    private List<Integer> dced = new LinkedList<Integer>();
    private List<MapleMonster> mobs = new LinkedList<MapleMonster>();
    private Map<Integer, Integer> killCount = new HashMap<Integer, Integer>();
    private final EventManager em;
    private final int channel;
    private final String name;
    private Properties props = new Properties();
    private long timeStarted = 0;
    private long eventTime = 0;
    private List<Integer> mapIds = new LinkedList<Integer>();
    private List<Boolean> isInstanced = new LinkedList<Boolean>();
    private ScheduledFuture<?> eventTimer;
    private final ReentrantReadWriteLock mutex = new ReentrantReadWriteLock();
    private final Lock rL = mutex.readLock(), wL = mutex.writeLock();
    private boolean disposed = false;

    public EventInstanceManager(EventManager em, String name, int channel) {
        this.em = em;
        this.name = name;
        this.channel = channel;
    }

    public void registerPlayer(MapleCharacter chr) {
        if (disposed || chr == null) {
            return;
        }
        try {
            wL.lock();
            try {
                chars.add(chr);
            } finally {
                wL.unlock();
            }
            chr.setEventInstance(this);
            em.getIv().invokeFunction("playerEntry", this, chr);
        } catch (NullPointerException ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
            ex.printStackTrace();
        } catch (Exception ex) {
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + em.getName() + ", Instance name : " + name + ", method Name : playerEntry:\n" + ex);
            System.out.println("Event name" + em.getName() + ", Instance name : " + name + ", method Name : playerEntry:\n" + ex);
        }
    }

    public void changedMap(final MapleCharacter chr, final int mapid) {
        if (disposed) {
            return;
        }
        try {
            em.getIv().invokeFunction("changedMap", this, chr, mapid);
        } catch (NullPointerException npe) {
        } catch (Exception ex) {
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "副本名称" + em.getName() + ", 实例名称 : " + name + ", 方法名称 : changedMap:\n" + ex);
            System.out.println("Event name" + em.getName() + ", Instance name : " + name + ", method Name : changedMap:\n" + ex);
        }
    }

    public void timeOut(final long delay, final EventInstanceManager eim) {
        if (disposed || eim == null) {
            return;
        }
        eventTimer = EventTimer.getInstance().schedule(new Runnable() {

            public void run() {
                if (disposed || eim == null || em == null) {
                    return;
                }
                try {
                    em.getIv().invokeFunction("scheduledTimeout", eim);
                } catch (Exception ex) {
                    FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + em.getName() + ", Instance name : " + name + ", method Name : scheduledTimeout:\n" + ex);
                    System.out.println("Event name" + em.getName() + ", Instance name : " + name + ", method Name : scheduledTimeout:\n" + ex);
                }
            }
        }, delay);
    }

    public final void forceRemovePlayerByCharName(String name) {
        ChannelServer.forceRemovePlayerByCharName(name);
    }

    public void stopEventTimer() {
        eventTime = 0;
        timeStarted = 0;
        if (eventTimer != null) {
            eventTimer.cancel(false);
        }
    }

    public void restartEventTimer(long time) {
        try {
            if (disposed) {
                return;
            }
            timeStarted = System.currentTimeMillis();
            eventTime = time;
            if (eventTimer != null) {
                eventTimer.cancel(false);
            }
            eventTimer = null;
            final int timesend = (int) time / 1000;

            for (MapleCharacter chr : getPlayers()) {
                chr.getClient().sendPacket(MaplePacketCreator.getClock(timesend));
            }
            timeOut(time, this);

        } catch (Exception ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
            System.out.println("Event name" + em.getName() + ", Instance name : " + name + ", method Name : restartEventTimer:\n");
            ex.printStackTrace();
        }
    }

    public boolean isSquadLeader(MapleCharacter tt, MapleSquadType ttt) {
        return (tt.getClient().getChannelServer().getMapleSquad(ttt).getLeader().equals(tt));
    }

    public void startEventTimer(long time) {
        restartEventTimer(time); //just incase
    }

    public int getInstanceId() {
        return ChannelServer.getInstance(1).getInstanceId();
    }

    public void addInstanceId() {
        ChannelServer.getInstance(1).addInstanceId();
    }

    public boolean isTimerStarted() {
        return eventTime > 0 && timeStarted > 0;
    }

    public long getTimeLeft() {
        return eventTime - (System.currentTimeMillis() - timeStarted);
    }

    public void registerParty(MapleParty party, MapleMap map) {
        if (disposed) {
            return;
        }
        for (MaplePartyCharacter pc : party.getMembers()) {
            MapleCharacter c = map.getCharacterById(pc.getId());
            registerPlayer(c);
        }
    }

    public void unregisterPlayer(final MapleCharacter chr) {
        if (disposed) {
            chr.setEventInstance(null);
            return;
        }
        wL.lock();
        try {
            unregisterPlayer_NoLock(chr);
        } finally {
            wL.unlock();
        }
    }

    private boolean unregisterPlayer_NoLock(final MapleCharacter chr) {
        if (name.equals("CWKPQ")) { //hard code it because i said so
            final MapleSquad squad = ChannelServer.getInstance(channel).getMapleSquad("CWKPQ");//so fkin hacky
            if (squad != null) {
                squad.removeMember(chr.getName());
                if (squad.getLeaderName().equals(chr.getName())) {
                    em.setProperty("leader", "false");
                }
            }
        }
        chr.setEventInstance(null);
        if (disposed) {
            return false;
        }
        if (chars.contains(chr)) {
            chars.remove(chr);
            return true;
        }
        return false;
    }

    public final boolean disposeIfPlayerBelow(final byte size, final int towarp) {
        if (disposed) {
            return true;
        }
        MapleMap map = null;
        if (towarp > 0) {
            map = this.getMapFactory().getMap(towarp);
        }

        wL.lock();
        try {
            if (chars.size() <= size) {
                final List<MapleCharacter> chrs = new LinkedList<MapleCharacter>(chars);
                for (MapleCharacter chr : chrs) {
                    unregisterPlayer_NoLock(chr);
                    if (towarp > 0) {
                        chr.changeMap(map, map.getPortal(0));
                    }
                }
                dispose_NoLock();
                return true;
            }
        } finally {
            wL.unlock();
        }
        return false;
    }

    public final void saveBossQuest(final int points) {
        if (disposed) {
            return;
        }
        for (MapleCharacter chr : getPlayers()) {
            final MapleQuestStatus record = chr.getQuestNAdd(MapleQuest.getInstance(150001));

            if (record.getCustomData() != null) {
                record.setCustomData(String.valueOf(points + Integer.parseInt(record.getCustomData())));
            } else {
                record.setCustomData(String.valueOf(points)); // First time
            }
        }
    }

    public List<MapleCharacter> getPlayers() {
        if (disposed) {
            return Collections.emptyList();
        }
        rL.lock();
        try {
            return new LinkedList<MapleCharacter>(chars);
        } finally {
            rL.unlock();
        }
    }

    public List<Integer> getDisconnected() {
        return dced;
    }

    public final int getPlayerCount() {
        if (disposed) {
            return 0;
        }
        return chars.size();
    }

    public void registerMonster(MapleMonster mob) {
        if (disposed) {
            return;
        }
        mobs.add(mob);
        mob.setEventInstance(this);
    }

    public void unregisterMonster(MapleMonster mob) {
        mob.setEventInstance(null);
        if (disposed) {
            return;
        }
        mobs.remove(mob);
        if (mobs.size() == 0) {
            try {
                em.getIv().invokeFunction("allMonstersDead", this);
            } catch (Exception ex) {
                FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + em.getName() + ", Instance name : " + name + ", method Name : allMonstersDead:\n" + ex);
                System.out.println("Event name" + em.getName() + ", Instance name : " + name + ", method Name : allMonstersDead:\n" + ex);
            }
        }
    }

    public void playerKilled(MapleCharacter chr) {
        if (disposed) {
            return;
        }
        try {
            em.getIv().invokeFunction("playerDead", this, chr);
        } catch (Exception ex) {
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + em.getName() + ", Instance name : " + name + ", method Name : playerDead:\n" + ex);
            System.out.println("Event name" + em.getName() + ", Instance name : " + name + ", method Name : playerDead:\n" + ex);
        }
    }

    public boolean revivePlayer(MapleCharacter chr) {
        if (disposed) {
            return false;
        }
        try {
            Object b = em.getIv().invokeFunction("playerRevive", this, chr);
            if (b instanceof Boolean) {
                return (Boolean) b;
            }
        } catch (Exception ex) {
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + em.getName() + ", Instance name : " + name + ", method Name : playerRevive:\n" + ex);
            System.out.println("Event name" + em.getName() + ", Instance name : " + name + ", method Name : playerRevive:\n" + ex);
        }
        return true;
    }

    public void playerDisconnected(final MapleCharacter chr, int idz) {
        if (disposed) {
            return;
        }
        byte ret;
        try {
            ret = ((Double) em.getIv().invokeFunction("playerDisconnected", this, chr)).byteValue();
        } catch (Exception e) {
            ret = 0;
        }

        wL.lock();
        try {
            if (disposed) {
                return;
            }
            dced.add(idz);
            if (chr != null) {
                unregisterPlayer_NoLock(chr);
            }
            if (ret == 0) {
                if (getPlayerCount() <= 0) {
                    dispose_NoLock();
                }
            } else if ((ret > 0 && getPlayerCount() < ret) || (ret < 0 && (isLeader(chr) || getPlayerCount() < (ret * -1)))) {
                final List<MapleCharacter> chrs = new LinkedList<MapleCharacter>(chars);
                for (MapleCharacter player : chrs) {
                    if (player.getId() != idz) {
                        removePlayer(player);
                    }
                }
                dispose_NoLock();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
        } finally {
            wL.unlock();
        }

    }

    /**
     *
     * @param chr
     * @param mob
     */
    public void monsterKilled(final MapleCharacter chr, final MapleMonster mob) {
        if (disposed) {
            return;
        }
        try {
            Integer kc = killCount.get(chr.getId());
            int inc = ((Double) em.getIv().invokeFunction("monsterValue", this, mob.getId())).intValue();
            if (disposed) {
                return;
            }
            if (kc == null) {
                kc = inc;
            } else {
                kc += inc;
            }
            killCount.put(chr.getId(), kc);
            if (chr.getCarnivalParty() != null && (mob.getStats().getPoint() > 0 || mob.getStats().getCP() > 0)) {
                em.getIv().invokeFunction("monsterKilled", this, chr, mob.getStats().getCP() > 0 ? mob.getStats().getCP() : mob.getStats().getPoint());
            }
        } catch (ScriptException ex) {
            System.out.println("Event name" + (em == null ? "null" : em.getName()) + ", Instance name : " + name + ", method Name : monsterValue:\n" + ex);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + (em == null ? "null" : em.getName()) + ", Instance name : " + name + ", method Name : monsterValue:\n" + ex);
        } catch (NoSuchMethodException ex) {
            System.out.println("Event name" + (em == null ? "null" : em.getName()) + ", Instance name : " + name + ", method Name : monsterValue:\n" + ex);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + (em == null ? "null" : em.getName()) + ", Instance name : " + name + ", method Name : monsterValue:\n" + ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
        }
    }

    public void monsterDamaged(final MapleCharacter chr, final MapleMonster mob, final int damage) {
        if (disposed || mob.getId() != 9700037) { //ghost PQ boss only.
            return;
        }
        try {
            em.getIv().invokeFunction("monsterDamaged", this, chr, mob.getId(), damage);
        } catch (ScriptException ex) {
            System.out.println("Event name" + (em == null ? "null" : em.getName()) + ", Instance name : " + name + ", method Name : monsterValue:\n" + ex);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + (em == null ? "null" : em.getName()) + ", Instance name : " + name + ", method Name : monsterValue:\n" + ex);
        } catch (NoSuchMethodException ex) {
            System.out.println("Event name" + (em == null ? "null" : em.getName()) + ", Instance name : " + name + ", method Name : monsterValue:\n" + ex);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + (em == null ? "null" : em.getName()) + ", Instance name : " + name + ", method Name : monsterValue:\n" + ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
        }
    }

    public int getKillCount(MapleCharacter chr) {
        if (disposed) {
            return 0;
        }
        Integer kc = killCount.get(chr.getId());
        if (kc == null) {
            return 0;
        } else {
            return kc;
        }
    }

    public void dispose_NoLock() {
        if (disposed || em == null) {
            return;
        }
        final String emN = em.getName();
        try {

            disposed = true;
            for (MapleCharacter chr : chars) {
                chr.setEventInstance(null);
            }
            chars.clear();
            chars = null;
            for (MapleMonster mob : mobs) {
                mob.setEventInstance(null);
            }
            mobs.clear();
            mobs = null;
            killCount.clear();
            killCount = null;
            dced.clear();
            dced = null;
            timeStarted = 0;
            eventTime = 0;
            props.clear();
            props = null;
            for (int i = 0; i < mapIds.size(); i++) {
                if (isInstanced.get(i)) {
                    this.getMapFactory().removeInstanceMap(mapIds.get(i));
                }
            }
            mapIds.clear();
            mapIds = null;
            isInstanced.clear();
            isInstanced = null;
            em.disposeInstance(name);
        } catch (Exception e) {
            System.out.println("Caused by : " + emN + " instance name: " + name + " method: dispose: " + e);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + emN + ", Instance name : " + name + ", method Name : dispose:\n" + e);
        }
    }

    public void dispose() {
        wL.lock();
        try {
            dispose_NoLock();
        } finally {
            wL.unlock();
        }

    }

    public ChannelServer getChannelServer() {
        return ChannelServer.getInstance(channel);
    }

    public List<MapleMonster> getMobs() {
        return mobs;
    }

    /*
     * public final void giveAchievement(final int type) { if (disposed) {
     * return; } for (MapleCharacter chr : getPlayers()) {
     * chr.finishAchievement(type); }
     }
     */
    public final void broadcastPlayerMsg(final int type, final String msg) {
        if (disposed) {
            return;
        }
        for (MapleCharacter chr : getPlayers()) {
            chr.getClient().sendPacket(MaplePacketCreator.serverNotice(type, msg));
        }
    }

    public final MapleMap createInstanceMap(final int mapid) {
        if (disposed) {
            return null;
        }
        final int assignedid = getChannelServer().getEventSM().getNewInstanceMapId();
        mapIds.add(assignedid);
        isInstanced.add(true);
        return this.getMapFactory().CreateInstanceMap(mapid, true, true, true, assignedid);
    }

    public final MapleMap createInstanceMapS(final int mapid) {
        if (disposed) {
            return null;
        }
        final int assignedid = getChannelServer().getEventSM().getNewInstanceMapId();
        mapIds.add(assignedid);
        isInstanced.add(true);
        return this.getMapFactory().CreateInstanceMap(mapid, false, false, false, assignedid);
    }

    public final MapleMap setInstanceMap(final int mapid) { //gets instance map from the channelserv
        if (disposed) {
            return this.getMapFactory().getMap(mapid);
        }

        mapIds.add(mapid);
        isInstanced.add(false);
        return this.getMapFactory().getMap(mapid);
    }

    public final MapleMapFactory getMapFactory() {
        return getChannelServer().getMapFactory();
    }

    public final MapleMap getMapInstance(int args) {
        if (disposed) {
            return null;
        }
        try {
            boolean instanced = false;
            int trueMapID = -1;
            if (args >= mapIds.size()) {
                //assume real map
                trueMapID = args;
            } else {
                trueMapID = mapIds.get(args);
                instanced = isInstanced.get(args);
            }
            MapleMap map = null;
            if (!instanced) {
                map = this.getMapFactory().getMap(trueMapID);
                if (map == null) {
                    return null;
                }
                // in case reactors need shuffling and we are actually loading the map
                if (map.getCharactersSize() == 0) {
                    if (em.getProperty("shuffleReactors") != null && em.getProperty("shuffleReactors").equals("true")) {
                        map.shuffleReactors();
                    }
                }
            } else {
                map = this.getMapFactory().getInstanceMap(trueMapID);
                if (map == null) {
                    return null;
                }
                // in case reactors need shuffling and we are actually loading the map
                if (map.getCharactersSize() == 0) {
                    if (em.getProperty("shuffleReactors") != null && em.getProperty("shuffleReactors").equals("true")) {
                        map.shuffleReactors();
                    }
                }
            }
            return map;
        } catch (NullPointerException ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
            ex.printStackTrace();
            return null;
        }
    }

    public final void schedule(final String methodName, final long delay) {
        if (disposed) {
            return;
        }
        EventTimer.getInstance().schedule(new Runnable() {

            public void run() {
                if (disposed || EventInstanceManager.this == null || em == null) {
                    return;
                }
                try {
                    em.getIv().invokeFunction(methodName, EventInstanceManager.this);
                } catch (NullPointerException npe) {
                } catch (Exception ex) {
                    System.out.println("Event name" + em.getName() + ", Instance name : " + name + ", method Name : " + methodName + ":\n" + ex);
                    FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + em.getName() + ", Instance name : " + name + ", method Name(schedule) : " + methodName + " :\n" + ex);
                }
            }
        }, delay);
    }

    public final String getName() {
        return name;
    }

    public final void setProperty(final String key, final String value) {
        if (disposed) {
            return;
        }
        props.setProperty(key, value);
    }

    public final Object setProperty(final String key, final String value, final boolean prev) {
        if (disposed) {
            return null;
        }
        return props.setProperty(key, value);
    }

    public final String getProperty(final String key) {
        if (disposed) {
            return "";
        }
        return props.getProperty(key);
    }

    public final Properties getProperties() {
        return props;
    }

    public final void leftParty(final MapleCharacter chr) {
        if (disposed) {
            return;
        }
        try {
            em.getIv().invokeFunction("leftParty", this, chr);
        } catch (Exception ex) {
            System.out.println("Event name" + em.getName() + ", Instance name : " + name + ", method Name : leftParty:\n" + ex);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + em.getName() + ", Instance name : " + name + ", method Name : leftParty:\n" + ex);
        }
    }

    public final void disbandParty() {
        if (disposed) {
            return;
        }
        try {
            em.getIv().invokeFunction("disbandParty", this);
        } catch (Exception ex) {
            System.out.println("Event name" + em.getName() + ", Instance name : " + name + ", method Name : disbandParty:\n" + ex);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + em.getName() + ", Instance name : " + name + ", method Name : disbandParty:\n" + ex);
        }
    }

    //Separate function to warp players to a "finish" map, if applicable
    public final void finishPQ() {
        if (disposed) {
            return;
        }
        try {
            em.getIv().invokeFunction("clearPQ", this);
        } catch (Exception ex) {
            System.out.println("Event name" + em.getName() + ", Instance name : " + name + ", method Name : clearPQ:\n" + ex);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + em.getName() + ", Instance name : " + name + ", method Name : clearPQ:\n" + ex);
        }
    }

    public final void removePlayer(final MapleCharacter chr) {
        if (disposed) {
            return;
        }
        try {
            em.getIv().invokeFunction("playerExit", this, chr);
        } catch (Exception ex) {
            System.out.println("Event name" + em.getName() + ", Instance name : " + name + ", method Name : playerExit:\n" + ex);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + em.getName() + ", Instance name : " + name + ", method Name : playerExit:\n" + ex);
        }
    }

    public final void registerCarnivalParty(final MapleCharacter leader, final MapleMap map, final byte team) {
        if (disposed) {
            return;
        }
        leader.clearCarnivalRequests();
        List<MapleCharacter> characters = new LinkedList<MapleCharacter>();
        final MapleParty party = leader.getParty();

        if (party == null) {
            return;
        }
        for (MaplePartyCharacter pc : party.getMembers()) {
            final MapleCharacter c = map.getCharacterById(pc.getId());
            if (c != null) {
                characters.add(c);
                registerPlayer(c);
                c.resetCP();
            }
        }
        final MapleCarnivalParty carnivalParty = new MapleCarnivalParty(leader, characters, team);
        try {
            em.getIv().invokeFunction("registerCarnivalParty", this, carnivalParty);
        } catch (ScriptException ex) {
            System.out.println("Event name" + em.getName() + ", Instance name : " + name + ", method Name : registerCarnivalParty:\n" + ex);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + em.getName() + ", Instance name : " + name + ", method Name : registerCarnivalParty:\n" + ex);
        } catch (NoSuchMethodException ex) {
            //ignore
        }
    }

    public void onMapLoad(final MapleCharacter chr) {
        if (disposed) {
            return;
        }
        try {
            em.getIv().invokeFunction("onMapLoad", this, chr);
        } catch (ScriptException ex) {
            System.out.println("Event name" + em.getName() + ", Instance name : " + name + ", method Name : onMapLoad:\n" + ex);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Event name" + em.getName() + ", Instance name : " + name + ", method Name : onMapLoad:\n" + ex);
        } catch (NoSuchMethodException ex) {
            // Ignore, we don't want to update this for all events.
        }
    }

    public boolean isLeader(final MapleCharacter chr) {
        return (chr != null && chr.getParty() != null && chr.getParty().getLeader().getId() == chr.getId());
    }

    public void registerSquad(MapleSquad squad, MapleMap map, int questID) {
        if (disposed) {
            return;
        }
        final int mapid = map.getId();

        for (String chr : squad.getMembers()) {
            MapleCharacter player = squad.getChar(chr);
            if (player != null && player.getMapId() == mapid) {
                if (questID > 0) {
                    player.getQuestNAdd(MapleQuest.getInstance(questID)).setCustomData(String.valueOf(System.currentTimeMillis()));
                }
                registerPlayer(player);
                /*
                 * if (player.getParty() != null) { PartySearch ps =
                 * World.Party.getSearch(player.getParty()); if (ps != null) {
                 * World.Party.removeSearch(ps, "The Party Listing has been
                 * removed because the Party Quest has started."); }
                 }
                 */
            }
        }
        squad.setStatus((byte) 2);
        //squad.getBeginMap().broadcastMessage(MaplePacketCreator.stopClock());
    }

    public boolean isDisconnected(final MapleCharacter chr) {
        if (disposed) {
            return false;
        }
        return (dced.contains(chr.getId()));
    }

    public void removeDisconnected(final int id) {
        if (disposed) {
            return;
        }
        dced.remove(id);
    }

    public EventManager getEventManager() {
        return em;
    }

    public void applyBuff(final MapleCharacter chr, final int id) {
        MapleItemInformationProvider.getInstance().getItemEffect(id).applyTo(chr);
        chr.getClient().sendPacket(UIPacket.getStatusMsg(id));
    }

    public boolean check() {
        for (MapleCharacter chr : getPlayers()) {
            if (!(chr.getLevel() >= 30 && chr.getLevel() <= 200)) {
                return false;
            }
        }
        return true;
    }

    public boolean check1() {//嘉年华等级限制
        for (MapleCharacter chr : getPlayers()) {
            if (!(chr.getLevel() >= 30 && chr.getLevel() <= 200)) {
                return false;
            }
        }
        return true;
    }
    
     public int 获取当前星期() {

        return Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

    }
    public int GetZfuben(String Name, int Channale) {
        int ret = -1;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM Zfuben WHERE channel = ? and Name = ?");
            ps.setInt(1, Channale);
            ps.setString(2, Name);
            ResultSet rs = ps.executeQuery();
            rs.next();
            ret = rs.getInt("Point");
            rs.close();
            ps.close();
        } catch (SQLException ex) {
        }
        return ret;
    }

    
    /***
     * 增加点数
     * @param Name
     * @param Channale
     * @param Piot 
     */
    public void GainZfuben(String Name, int Channale, int Piot) {
        try {
            int ret = GetZfuben(Name, Channale);
            if (ret == -1) {
                ret = 0;
                PreparedStatement ps = null;
                try {
                    ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO Zfuben (channel, Name,Point) VALUES (?, ?, ?)");
                    ps.setInt(1, Channale);
                    ps.setString(2, Name);
                    ps.setInt(3, ret);

                    ps.execute();
                } catch (SQLException e) {
                    System.out.println("xxxxxxxx:"+e);
                } finally {
                    try {
                        if (ps != null) {
                            ps.close();
                        }
                    } catch (SQLException e) {
                        System.out.println("xxxxxxxxzzzzzzz:"+e);
                    }
                }
            }
            ret+=Piot;
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE Zfuben SET `Point` = ? WHERE Name = ? and channel = ?");
            ps.setInt(1, ret);
            ps.setString(2, Name);
            ps.setInt(3, Channale);
            ps.execute();
            ps.close();
        } catch (SQLException sql) {
            System.err.println("獲取錯誤!!55" + sql);
        }
    }  
     public int Get房屋租赁(String Name, int Channale) {
        int ret = -1;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM 房屋租赁 WHERE channel = ? and Name = ?");
            ps.setInt(1, Channale);
            ps.setString(2, Name);
            ResultSet rs = ps.executeQuery();
            rs.next();
            ret = rs.getInt("Point");
            rs.close();
            ps.close();
        } catch (SQLException ex) {
        }
        return ret;
    }

    
    /***
     * 增加点数
     * @param Name
     * @param Channale
     * @param Piot 
     */
    public void Gain房屋租赁(String Name, int Channale, int Piot) {
        try {
            int ret = Get房屋租赁(Name, Channale);
            if (ret == -1) {
                ret = 0;
                PreparedStatement ps = null;
                try {
                    ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO 房屋租赁 (channel, Name,Point) VALUES (?, ?, ?)");
                    ps.setInt(1, Channale);
                    ps.setString(2, Name);
                    ps.setInt(3, ret);

                    ps.execute();
                } catch (SQLException e) {
                    System.out.println("xxxxxxxx:"+e);
                } finally {
                    try {
                        if (ps != null) {
                            ps.close();
                        }
                    } catch (SQLException e) {
                        System.out.println("xxxxxxxxzzzzzzz:"+e);
                    }
                }
            }
            ret+=Piot;
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE 房屋租赁 SET `Point` = ? WHERE Name = ? and channel = ?");
            ps.setInt(1, ret);
            ps.setString(2, Name);
            ps.setInt(3, Channale);
            ps.execute();
            ps.close();
        } catch (SQLException sql) {
            System.err.println("獲取錯誤!!55" + sql);
        }
    }  
    /**
     * 获取点数
     *
     * @return
     */
    public int GetPiot(String Name, int Channale) {
        int ret = -1;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM FullPoint WHERE channel = ? and Name = ?");
            ps.setInt(1, Channale);
            ps.setString(2, Name);
            ResultSet rs = ps.executeQuery();
            rs.next();
            ret = rs.getInt("Point");
            rs.close();
            ps.close();
        } catch (SQLException ex) {
        }
        return ret;
    }
    public String getServerName() {
        return MapleParty.开服名字;//var MC = cm.etServerName();
    }
    
    /***
     * 增加点数
     * @param Name
     * @param Channale
     * @param Piot 
     */
    public void GainPiot(String Name, int Channale, int Piot) {
        try {
            int ret = GetPiot(Name, Channale);
            if (ret == -1) {
                ret = 0;
                PreparedStatement ps = null;
                try {
                    ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO FullPoint (channel, Name,Point) VALUES (?, ?, ?)");
                    ps.setInt(1, Channale);
                    ps.setString(2, Name);
                    ps.setInt(3, ret);

                    ps.execute();
                } catch (SQLException e) {
                    System.out.println("xxxxxxxx:"+e);
                } finally {
                    try {
                        if (ps != null) {
                            ps.close();
                        }
                    } catch (SQLException e) {
                        System.out.println("xxxxxxxxzzzzzzz:"+e);
                    }
                }
            }
            ret+=Piot;
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE FullPoint SET `Point` = ? WHERE Name = ? and channel = ?");
            ps.setInt(1, ret);
            ps.setString(2, Name);
            ps.setInt(3, Channale);
            ps.execute();
            ps.close();
        } catch (SQLException sql) {
            System.err.println("獲取錯誤!!55" + sql);
        }
    }
     public static int 获取最高玩家等级() {
        int data = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT MAX(level) as DATA FROM characters WHERE gm = 0");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getInt("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("获取最高玩家等级出错 - 数据库查询失败：" + Ex);
        }

        return data;
    }
    
    public static String 获取最高等级玩家名字() {
        String name = "";
        String level = "";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT `name`, `level` FROM characters WHERE gm = 0 ORDER BY `level` DESC LIMIT 1");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    name = rs.getString("name");
                    level = rs.getString("level");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("获取家族名称出错 - 数据库查询失败：" + Ex);
        }

        return String.format("%s", name);
    }
     public static int 获取最高玩家人气() {
        int data = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT MAX(fame) as DATA FROM characters WHERE gm = 0");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getInt("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("获取最高玩家等级出错 - 数据库查询失败：" + Ex);
        }

        return data;
    }
     
     
         public static String 获取最高人气玩家名字() {
        String name = "";
        String level = "";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT `name`, `fame` FROM characters WHERE gm = 0 ORDER BY `fame` DESC LIMIT 1");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    name = rs.getString("name");
                    level = rs.getString("fame");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("获取家族名称出错 - 数据库查询失败：" + Ex);
        }

        return String.format("%s", name);
    }
          public static int 获取最高玩家金币() {
        int data = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT MAX(meso) as DATA FROM characters WHERE gm = 0");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getInt("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("获取最高玩家等级出错 - 数据库查询失败：" + Ex);
        }

        return data;
    }
             public static String 获取最高金币玩家名字() {
        String name = "";
        String level = "";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT `name`, `meso` FROM characters WHERE gm = 0 ORDER BY `meso` DESC LIMIT 1");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    name = rs.getString("name");
                    level = rs.getString("meso");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("获取家族名称出错 - 数据库查询失败：" + Ex);
        }

        return String.format("%s", name);
    }      
          
          public static int 获取最高玩家在线() {
        int data = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT MAX(totalOnlineTime) as DATA FROM characters WHERE gm = 0");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getInt("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("获取最高玩家等级出错 - 数据库查询失败：" + Ex);
        }

        return data;
    }          
            public static String 获取最高在线玩家名字() {
        String name = "";
        String level = "";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT `name`, `totalOnlineTime` FROM characters WHERE gm = 0 ORDER BY `totalOnlineTime` DESC LIMIT 1");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    name = rs.getString("name");
                    level = rs.getString("totalOnlineTime");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("获取家族名称出错 - 数据库查询失败：" + Ex);
        }

        return String.format("%s", name);
    }         

}
