package server.custom.bossrank6;

import client.MapleCharacter;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ZEV1
 */
public class BossRankManager6 {

    public void setLog(MapleCharacter player, String ҡҡ��1, byte b, byte b0) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    private static class InstanceHolder {

        public static final BossRankManager6 instance = new BossRankManager6();
    }

    public static BossRankManager6 getInstance() {
        return InstanceHolder.instance;
    }

    private BossRankManager6() {
    }

    public Map<String, BossRankInfo6> getInfoMap(int cid) {
        Map<String, BossRankInfo6> info_map = new HashMap<>();
        Connection con1 = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con1.prepareStatement("select * from bossrank6 where cid = ?");
            ps.setInt(1, cid);
            rs = ps.executeQuery();
            while (rs.next()) {
                BossRankInfo6 info = new BossRankInfo6();
                info.setCid(rs.getInt("cid"));
                info.setCname(rs.getString("cname"));
                info.setBossname(rs.getString("bossname"));
                info.setPoints(rs.getInt("points"));
                info.setCount(rs.getInt("count"));
                info_map.put(info.getBossname(), info);
            }
        } catch (Exception Ex) {
            Ex.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(BossRankManager6.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return info_map;
    }

    public BossRankInfo6 getInfo(int cid, String bossname) {
        BossRankInfo6 info = null;
        Connection con1 = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con1.prepareStatement("select * from bossrank6 where cid = ? and bossname = ?");
            ps.setInt(1, cid);
            ps.setString(2, bossname);
            rs = ps.executeQuery();
            if (rs.next()) {
                info = new BossRankInfo6();
                info.setCid(rs.getInt("cid"));
                info.setCname(rs.getString("cname"));
                info.setBossname(rs.getString("bossname"));
                info.setPoints(rs.getInt("points"));
                info.setCount(rs.getInt("count"));
            }
        } catch (Exception Ex) {
            Ex.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(BossRankManager6.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return info;
    }

    public int setLog(int cid, String cname, String bossname, byte type, int update) {
        int ret=-1;
        BossRankInfo6 info = getInfo(cid, bossname);
        boolean add = false;
        boolean doUpdate = true;
        if (info == null) {
            doUpdate = false;
            add = true;
            info = new BossRankInfo6();
            info.setCid(cid);
            info.setCname(cname);
            info.setBossname(bossname);
        }
        switch (type) {
            case 1://�e��
                ret=info.getPoints() + update;
                info.setPoints(ret);
                break;
            case 2://�Δ�
                ret=info.getCount() + update;
                info.setCount(ret);
                break;
            default:
                doUpdate = false;
                break;
        }
        if (!doUpdate) {
            if (add) {
                add(info);
            }
            return ret;
        }
        update(info);
        return ret;
    }//zev

    public void update(BossRankInfo6 info) {
        if (info == null) {
            return;
        }
        Connection con1 = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con1.prepareStatement("update bossrank6 set points = ?,count = ?  where cid = ? and bossname = ?");
            ps.setInt(1, info.getPoints());
            ps.setInt(2, info.getCount());
            ps.setInt(3, info.getCid());
            ps.setString(4, info.getBossname());
            ps.executeUpdate();
        } catch (Exception Ex) {
            Ex.printStackTrace();
        } finally {
            if (ps != null) {

                try {
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(BossRankManager6.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }//zev

    public void add(BossRankInfo6 info) {
        if (info == null) {
            return;
        }
        Connection con1 = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con1.prepareStatement("insert into bossrank6 (cid,cname,bossname,points,count) values (?,?,?,?,?)");
            ps.setInt(1, info.getCid());
            ps.setString(2, info.getCname());
            ps.setString(3, info.getBossname());
            ps.setInt(4, info.getPoints());
            ps.setInt(5, info.getCount());
            ps.executeUpdate();
        } catch (Exception Ex) {
            Ex.printStackTrace();

        } finally {
            if (ps != null) {

                try {
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(BossRankManager6.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }//zev

    public List<BossRankInfo6> getRank(String bossname, int type) {
        List<BossRankInfo6> list = new LinkedList<>();
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        ResultSet rs=null;
        try {
            switch (type) {
                case 1://�e��
                    ps = con.prepareStatement("SELECT * FROM bossrank6 WHERE bossname = ?  ORDER BY points DESC LIMIT 100");
                    break;
                case 2://�Δ�
                    ps = con.prepareStatement("SELECT * FROM bossrank6 WHERE bossname = ?  ORDER BY count DESC LIMIT 100");
                    break;
                default:
                    ps = con.prepareStatement("SELECT * FROM bossrank6 WHERE bossname = ?  ORDER BY points DESC LIMIT 100");
                    break;
            }
            ps.setString(1, bossname);
             rs = ps.executeQuery();
            while (rs.next()) {
               BossRankInfo6 info = new BossRankInfo6();
                info.setCid(rs.getInt("cid"));
                info.setCname(rs.getString("cname"));
                info.setBossname(rs.getString("bossname"));
                info.setPoints(rs.getInt("points"));
                info.setCount(rs.getInt("count"));
                list.add(info);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(BossRankManager6.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return list;
    }  
}
