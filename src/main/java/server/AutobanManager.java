/*
封号
 */
package server;

import client.MapleClient;
import handling.world.World;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static gui.QQMsgServer.sendMsgToQQGroup;

public class AutobanManager implements Runnable {

    private static class ExpirationEntry implements Comparable<ExpirationEntry> {

        public long time;
        public int acc;
        public int points;

        public ExpirationEntry(long time, int acc, int points) {
            this.time = time;
            this.acc = acc;
            this.points = points;
        }

        public int compareTo(AutobanManager.ExpirationEntry o) {
            return (int) (time - o.time);
        }

        @Override
        public boolean equals(Object oth) {
            if (!(oth instanceof AutobanManager.ExpirationEntry)) {
                return false;
            }
            final AutobanManager.ExpirationEntry ee = (AutobanManager.ExpirationEntry) oth;
            return (time == ee.time && points == ee.points && acc == ee.acc);
        }
    }
    private final Map<Integer, Integer> points = new HashMap<Integer, Integer>();
    private final Map<Integer, List<String>> reasons = new HashMap<Integer, List<String>>();
    private final Set<ExpirationEntry> expirations = new TreeSet<ExpirationEntry>();
    private static final int AUTOBAN_POINTS = 5000;
    private static final AutobanManager instance = new AutobanManager();
    private final ReentrantLock lock = new ReentrantLock(true);

    public static final AutobanManager getInstance() {
        return instance;
    }

    public final void autoban(final MapleClient c, final String reason) {
        if (c.getPlayer().isGM() || c.getPlayer().isClone()) {
            c.getPlayer().dropMessage(5, "[WARNING] A/b triggled : " + reason);
            return;
        }
        addPoints(c, AUTOBAN_POINTS, 0, reason);
    }

    //系统自动封号
    public final void 系统封号(final MapleClient c, final int points, final long expiration, final String reason) {
        lock.lock();
        try {
            List<String> reasonList;
            final int acc = c.getPlayer().getAccountID();

            if (this.points.containsKey(acc)) {
                final int SavedPoints = this.points.get(acc);
                if (SavedPoints >= AUTOBAN_POINTS) { // Already auto ban'd.
                    return;
                }
                this.points.put(acc, SavedPoints + points); // Add
                reasonList = this.reasons.get(acc);
                reasonList.add(reason);
            } else {
                this.points.put(acc, points);
                reasonList = new LinkedList<String>();
                reasonList.add(reason);
                this.reasons.put(acc, reasonList);
            }

            if (this.points.get(acc) >= AUTOBAN_POINTS) { // See if it's sufficient to auto ban
                if (c.getPlayer().isGM() || c.getPlayer().isClone()) {
                    c.getPlayer().dropMessage(5, "[WARNING] A/b triggled : " + reason);
                    return;
                }
                final StringBuilder sb = new StringBuilder("a/b ");
                sb.append(c.getPlayer().getName());
                sb.append(" (IP ");
                sb.append(c.getSession().remoteAddress().toString());
                sb.append("): ");
                sb.append(" (MAC ");
                sb.append(c.getMac());
                sb.append("): ");
                for (final String s : reasons.get(acc)) {
                    sb.append(s);
                    sb.append(", ");
                }
                sendMsgToQQGroup("[封号信息]:玩家 " + c.getPlayer().getName() + " 使用非法插件/破坏游戏平衡，现已被系统封号处理。");

                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(0, "'" + c.getPlayer().getName() + "'自动封号系统非法使用外挂程序！永久封停处理！"));

                FileoutputUtil.logToFile_chr(c.getPlayer(), FileoutputUtil.ban_log, sb.toString());
                c.getPlayer().ban(sb.toString(), false, true, false);
                c.disconnect(true, false);
            }
        } finally {
            lock.unlock();
        }
    }
    //系统自动封号
    public final void addPoints(final MapleClient c, final int points, final long expiration, final String reason) {
        lock.lock();
        try {
            List<String> reasonList;
            final int acc = c.getPlayer().getAccountID();

            if (this.points.containsKey(acc)) {
                final int SavedPoints = this.points.get(acc);
                if (SavedPoints >= AUTOBAN_POINTS) { // Already auto ban'd.
                    return;
                }
                this.points.put(acc, SavedPoints + points); // Add
                reasonList = this.reasons.get(acc);
                reasonList.add(reason);
            } else {
                this.points.put(acc, points);
                reasonList = new LinkedList<String>();
                reasonList.add(reason);
                this.reasons.put(acc, reasonList);
            }

            if (this.points.get(acc) >= AUTOBAN_POINTS) { // See if it's sufficient to auto ban
                if (c.getPlayer().isGM() || c.getPlayer().isClone()) {
                    c.getPlayer().dropMessage(5, "[WARNING] A/b triggled : " + reason);
                    return;
                }
                final StringBuilder sb = new StringBuilder("a/b ");
                sb.append(c.getPlayer().getName());
                sb.append(" (IP ");
                sb.append(c.getSession().remoteAddress().toString());
                sb.append("): ");
                sb.append(" (MAC ");
                sb.append(c.getMac());
                sb.append("): ");
                for (final String s : reasons.get(acc)) {
                    sb.append(s);
                    sb.append(", ");
                }
                sendMsgToQQGroup("[封号信息]:玩家 " + c.getPlayer().getName() + " 使用非法插件/破坏游戏平衡，现已被系统封号处理。");

                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(0, "'" + c.getPlayer().getName() + "'自动封号系统非法使用外挂程序！永久封停处理！"));
//		Calendar cal = Calendar.getInstance();
//		cal.add(Calendar.DATE, 60);
//		c.getPlayer().tempban(sb.toString(), cal, 1, false);
                FileoutputUtil.logToFile_chr(c.getPlayer(), FileoutputUtil.ban_log, sb.toString());
                c.getPlayer().ban(sb.toString(), false, true, false);
                c.disconnect(true, false);
            } else if (expiration > 0) {
                expirations.add(new ExpirationEntry(System.currentTimeMillis() + expiration, acc, points));
            }
        } finally {
            lock.unlock();
        }
    }

    public final void run() {
        final long now = System.currentTimeMillis();
        for (final ExpirationEntry e : expirations) {
            if (e.time <= now) {
                this.points.put(e.acc, this.points.get(e.acc) - e.points);
            } else {
                return;
            }
        }
    }
}
