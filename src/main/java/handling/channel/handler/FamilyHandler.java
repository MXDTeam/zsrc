/*
学院系统
 */
package handling.channel.handler;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import handling.world.family.MapleFamily;
import handling.world.family.MapleFamilyBuff;
import handling.world.family.MapleFamilyBuff.MapleFamilyBuffEntry;
import handling.world.family.MapleFamilyCharacter;
import server.maps.FieldLimitType;
import tools.MaplePacketCreator;
import tools.data.LittleEndianAccessor;
import tools.packet.FamilyPacket;

import java.util.List;

public class FamilyHandler {

    public static final void RequestFamily(final LittleEndianAccessor slea, MapleClient c) {
        /**
         * c.getPlayer().dropMessage(1, "<" + 开服名字 + "学院师徒系统>\r\n" + "v1.0\r\n"
         * + "1.一个玩家最多只能收一个徒弟。\r\n" + "2.只有等级达到30级才可以收徒。\r\n" +
         * "3.只能收等级达到10级的玩家。");
         */

        //95 00 
        //0C 00 D7 FE D7 FE D7 FE D7 FE B3 D0 C5 B5
        MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
        if (chr != null) {
            c.sendPacket(FamilyPacket.getFamilyPedigree(chr));
        }
    }

    public static final void OpenFamily(final LittleEndianAccessor slea, MapleClient c) {
        /*c.getPlayer().dropMessage(1, "<" + 开服名字 + "学院师徒系统>\r\n"
                + "v1.0\r\n"
                + "1.一个玩家最多只能收一个徒弟。\r\n"
                + "2.只有等级达到30级才可以收徒。\r\n"
                + "3.只能收等级达到10级的玩家。");*/
        c.sendPacket(FamilyPacket.getFamilyInfo(c.getPlayer()));
    }

    public static final void UseFamily(final LittleEndianAccessor slea, MapleClient c) {
        int type = slea.readInt();
        MapleFamilyBuffEntry entry = MapleFamilyBuff.getBuffEntry(type);
        if (entry == null) {
            return;
        }
        boolean success = c.getPlayer().getFamilyId() > 0 && c.getPlayer().canUseFamilyBuff(entry) && c.getPlayer().getCurrentRep() > entry.rep;
        if (!success) {
            return;
        }
        MapleCharacter victim = null;
        switch (type) {
            case 0: //teleport: need add check for if not a safe place
                victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
                if (FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) || !c.getPlayer().isAlive()) {
                    c.getPlayer().dropMessage(5, "传唤失败。您当前的位置或状态不允许传唤。");
                    success = false;
                } else if (victim == null || (victim.isGM() && !c.getPlayer().isGM())) {
                    c.getPlayer().dropMessage(1, "无效名称或您不在同一频道。");
                    success = false;
                } else if (victim.getFamilyId() == c.getPlayer().getFamilyId() && !FieldLimitType.VipRock.check(victim.getMap().getFieldLimit()) && victim.getId() != c.getPlayer().getId()) {
                    c.getPlayer().changeMap(victim.getMap(), victim.getMap().getPortal(0));
                } else {
                    c.getPlayer().dropMessage(5, "传唤失败。您当前的位置或状态不允许传唤。");
                    success = false;
                }
                break;
            case 1: // TODO give a check to the player being forced somewhere else..
                victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
                if (FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) || !c.getPlayer().isAlive()) {
                    c.getPlayer().dropMessage(5, "传唤失败。您当前的位置或状态不允许传唤。");
                } else if (victim == null || (victim.isGM() && !c.getPlayer().isGM())) {
                    c.getPlayer().dropMessage(1, "无效名称或您不在同一频道.");
                } else if (victim.getTeleportName().length() > 0) {
                    c.getPlayer().dropMessage(1, "另一个角色要求传唤这个角色。请稍后再试。");
                } else if (victim.getFamilyId() == c.getPlayer().getFamilyId() && !FieldLimitType.VipRock.check(victim.getMap().getFieldLimit()) && victim.getId() != c.getPlayer().getId()) {
                    victim.getClient().sendPacket(FamilyPacket.familySummonRequest(c.getPlayer().getName(), c.getPlayer().getMap().getMapName()));
                    victim.setTeleportName(c.getPlayer().getName());
                } else {
                    c.getPlayer().dropMessage(5, "传唤失败。您当前的位置或状态不允许传唤。");
                }
                return; //RETURN not break
            case 4: // 6 family members in pedigree online Drop Rate & Exp Rate + 100% 30 minutes
                final MapleFamily fam = World.Family.getFamily(c.getPlayer().getFamilyId());
                List<MapleFamilyCharacter> chrs = fam.getMFC(c.getPlayer().getId()).getOnlineJuniors(fam);
                if (chrs.size() < 7) {
                    success = false;
                } else {
                    for (MapleFamilyCharacter chrz : chrs) {
                        int chr = World.Find.findChannel(chrz.getId());
                        if (chr == -1) {
                            continue; //STOP WTF?! take reps though..
                        }
                        MapleCharacter chrr = World.getStorage(chr).getCharacterById(chrz.getId());
                        entry.applyTo(chrr);
                        //chrr.getClient().sendPacket(FamilyPacket.familyBuff(entry.type, type, entry.effect, entry.duration*60000));
                    }
                }
                break;

            case 2: // drop rate + 50% 15 min
            case 3: // exp rate + 50% 15 min
            case 5: // drop rate + 100% 15 min
            case 6: // exp rate + 100% 15 min
            case 7: // drop rate + 100% 30 min
            case 8: // exp rate + 100% 30 min
                //c.sendPacket(FamilyPacket.familyBuff(entry.type, type, entry.effect, entry.duration*60000));
                entry.applyTo(c.getPlayer());
                break;
            case 9: // drop rate + 100% party 30 min
            case 10: // exp rate + 100% party 30 min
                entry.applyTo(c.getPlayer());
                //c.sendPacket(FamilyPacket.familyBuff(entry.type, type, entry.effect, entry.duration*60000));
                if (c.getPlayer().getParty() != null) {
                    for (MaplePartyCharacter mpc : c.getPlayer().getParty().getMembers()) {
                        if (mpc.getId() != c.getPlayer().getId()) {
                            MapleCharacter chr = c.getPlayer().getMap().getCharacterById(mpc.getId());
                            if (chr != null) {
                                entry.applyTo(chr);
                                //chr.getClient().sendPacket(FamilyPacket.familyBuff(entry.type, type, entry.effect, entry.duration*60000));
                            }
                        }
                    }
                }
                break;
        }
        if (success) { //again
            //c.getPlayer().setCurrentRep(c.getPlayer().getCurrentRep() - entry.rep);
            //c.sendPacket(FamilyPacket.changeRep(-entry.rep));
            c.getPlayer().useFamilyBuff(entry);
        } else {
            c.getPlayer().dropMessage(5, "不正确的使用，地图不支持传送。");
        }
    }

    public static final void FamilyOperation(final LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer() == null) {
            return;
        }
        MapleCharacter addChr = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
        if (addChr == null) {
            c.getPlayer().dropMessage(1, "你邀请的玩家不存在，或者不在线。");
        } else if (addChr.getFamilyId() == c.getPlayer().getFamilyId() && addChr.getFamilyId() > 0) {
            c.getPlayer().dropMessage(1, "已经在相同的学院里。");
        } else if (addChr.getMapId() != c.getPlayer().getMapId()) {
            c.getPlayer().dropMessage(1, "你和邀请的玩家不在同一个地图。");
        } else if (addChr.getSeniorId() != 0) {
            c.getPlayer().dropMessage(1, "你邀请的玩家已经在別的学院里。");
        } else if (addChr.getLevel() >= c.getPlayer().getLevel()) {
            c.getPlayer().dropMessage(1, "你邀请的玩家比你的等级还要高。");
        } else if (c.getPlayer().getLevel() > 200) {
            c.getPlayer().dropMessage(1, "超过 200 级后无法收徒。");
        } else if (c.getPlayer().getLevel() < 30) {
            c.getPlayer().dropMessage(1, "你的等级不够 30 级，无法收徒。");
        } else if (addChr.getLevel() < c.getPlayer().getLevel() - 20) {
            c.getPlayer().dropMessage(1, "您邀请的玩家等级必須相差20等级以内。");
            //} else if (c.getPlayer().getFamilyId() != 0 && c.getPlayer().getFamily().getGens() >= 1000) {
            //	c.getPlayer().dropMessage(5, "Your family cannot extend more than 1000 generations from above and below.");*/
        } else if (addChr.getLevel() < 10) {
            c.getPlayer().dropMessage(1, "低于 10 级的玩家无法邀请。");
        } else if (c.getPlayer().getJunior1() > 0 && c.getPlayer().getJunior2() > 0) {
            c.getPlayer().dropMessage(1, "你已经收了2个徒弟，你的徒弟可以继续收徒弟。");
        } else /*if (c.getPlayer().isGM() || !addChr.isGM())*/ {
            addChr.getClient().sendPacket(FamilyPacket.sendFamilyInvite(c.getPlayer().getId(), c.getPlayer().getLevel(), c.getPlayer().getJob(), c.getPlayer().getName()));
        }
        c.sendPacket(MaplePacketCreator.enableActions());
    }

    public static final void FamilyPrecept(final LittleEndianAccessor slea, MapleClient c) {
        MapleFamily fam = World.Family.getFamily(c.getPlayer().getFamilyId());
        if (fam == null || fam.getLeaderId() != c.getPlayer().getId()) {
            return;
        }
        //fam.setNotice(slea.readMapleAsciiString());
        fam.setNotice(slea.readMapleAsciiString());
        c.getPlayer().dropMessage(1, "重新开启一次。");
    }

    public static final void FamilySummon(final LittleEndianAccessor slea, MapleClient c) {
        //召唤请求的类型。
        int TYPE = 1;
        MapleFamilyBuffEntry cost = MapleFamilyBuff.getBuffEntry(TYPE);
        MapleCharacter tt = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
        if (c.getPlayer().getFamilyId() > 0 && tt != null && tt.getFamilyId() == c.getPlayer().getFamilyId() && !FieldLimitType.VipRock.check(tt.getMap().getFieldLimit())
                && !FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) && c.getPlayer().isAlive() && tt.isAlive() && tt.canUseFamilyBuff(cost)
                && c.getPlayer().getTeleportName().equals(tt.getName()) && tt.getCurrentRep() > cost.rep && c.getPlayer().getEventInstance() == null && tt.getEventInstance() == null) {
            //大量支票
            boolean accepted = slea.readByte() > 0;
            if (accepted) {
                c.getPlayer().changeMap(tt.getMap(), tt.getMap().getPortal(0));
                tt.setCurrentRep(tt.getCurrentRep() - cost.rep);
                tt.getClient().sendPacket(FamilyPacket.changeRep(-cost.rep));
                tt.useFamilyBuff(cost);
            } else {
                tt.dropMessage(5, "传唤失败。您当前的位置或状态不允许传唤.");
            }
        } else {
            c.getPlayer().dropMessage(5, "传唤失败。您当前的位置或状态不允许传唤.");
        }
        c.getPlayer().setTeleportName("");
    }

    public static final void DeleteJunior(final LittleEndianAccessor slea, MapleClient c) {
        int juniorid = slea.readInt();
        if (c.getPlayer().getFamilyId() <= 0 || juniorid <= 0 || (c.getPlayer().getJunior1() != juniorid && c.getPlayer().getJunior2() != juniorid)) {
            return;
        }
        //junior is not required to be online.
        final MapleFamily fam = World.Family.getFamily(c.getPlayer().getFamilyId());
        final MapleFamilyCharacter other = fam.getMFC(juniorid);
        if (other == null) {
            return;
        }
        final MapleFamilyCharacter oth = c.getPlayer().getMFC();
        boolean junior2 = oth.getJunior2() == juniorid;
        if (junior2) {
            oth.setJunior2(0);
        } else {
            oth.setJunior1(0);
        }
        c.getPlayer().saveFamilyStatus();
        other.setSeniorId(0);
        //if (!other.isOnline()) {
        MapleFamily.setOfflineFamilyStatus(other.getFamilyId(), other.getSeniorId(), other.getJunior1(), other.getJunior2(), other.getCurrentRep(), other.getTotalRep(), other.getId());
        //}
        MapleCharacterUtil.sendNote(other.getName(), c.getPlayer().getName(), c.getPlayer().getName() + " 解散了学院", 0);
        if (!fam.splitFamily(juniorid, other)) { //juniorid splits to make their own family. function should handle the rest
            if (!junior2) {
                fam.resetDescendants();
            }
            fam.resetPedigree();
        }
        c.getPlayer().dropMessage(1, "踢出了 (" + other.getName() + ").");
        c.sendPacket(MaplePacketCreator.enableActions());
    }

    public static final void DeleteSenior(final LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getFamilyId() <= 0 || c.getPlayer().getSeniorId() <= 0) {
            return;
        }
        //not required to be online
        final MapleFamily fam = World.Family.getFamily(c.getPlayer().getFamilyId()); //this is old family
        final MapleFamilyCharacter mgc = fam.getMFC(c.getPlayer().getSeniorId());
        final MapleFamilyCharacter mgc_ = c.getPlayer().getMFC();
        mgc_.setSeniorId(0);
        boolean junior2 = mgc.getJunior2() == c.getPlayer().getId();
        if (junior2) {
            mgc.setJunior2(0);
        } else {
            mgc.setJunior1(0);
        }
        //if (!mgc.isOnline()) {
        MapleFamily.setOfflineFamilyStatus(mgc.getFamilyId(), mgc.getSeniorId(), mgc.getJunior1(), mgc.getJunior2(), mgc.getCurrentRep(), mgc.getTotalRep(), mgc.getId());
        //}
        c.getPlayer().saveFamilyStatus();
        MapleCharacterUtil.sendNote(mgc.getName(), c.getPlayer().getName(), c.getPlayer().getName() + "离开你的家族", 0);
        if (!fam.splitFamily(c.getPlayer().getId(), mgc_)) { //now, we're the family leader
            if (!junior2) {
                fam.resetDescendants();
            }
            fam.resetPedigree();
        }
        c.getPlayer().dropMessage(1, "退出了 (" + mgc.getName() + ") 的家族.");
        c.sendPacket(MaplePacketCreator.enableActions());
    }

    public static final void AcceptFamily(LittleEndianAccessor slea, MapleClient c) {
        MapleCharacter inviter = c.getPlayer().getMap().getCharacterById(slea.readInt());
        if (inviter != null && c.getPlayer().getSeniorId() == 0 && (c.getPlayer().isGM() || !inviter.isHidden()) && inviter.getLevel() - 20 <= c.getPlayer().getLevel() && inviter.getLevel() >= 10 && inviter.getName().equals(slea.readMapleAsciiString()) && inviter.getNoJuniors() < 2 /*&& inviter.getFamily().getGens() < 1000*/ && c.getPlayer().getLevel() >= 10) {
            boolean accepted = slea.readByte() > 0;
            inviter.getClient().sendPacket(FamilyPacket.sendFamilyJoinResponse(accepted, c.getPlayer().getName()));
            if (accepted) {
                //c.sendPacket(FamilyPacket.sendFamilyMessage(0));
                c.sendPacket(FamilyPacket.getSeniorMessage(inviter.getName()));
                int old = c.getPlayer().getMFC() == null ? 0 : c.getPlayer().getMFC().getFamilyId();
                int oldj1 = c.getPlayer().getMFC() == null ? 0 : c.getPlayer().getMFC().getJunior1();
                int oldj2 = c.getPlayer().getMFC() == null ? 0 : c.getPlayer().getMFC().getJunior2();
                if (inviter.getFamilyId() > 0 && World.Family.getFamily(inviter.getFamilyId()) != null) {
                    MapleFamily fam = World.Family.getFamily(inviter.getFamilyId());
                    //if old isn't null, don't set the familyid yet, mergeFamily will take care of it
                    c.getPlayer().setFamily(old <= 0 ? inviter.getFamilyId() : old, inviter.getId(), oldj1 <= 0 ? 0 : oldj1, oldj2 <= 0 ? 0 : oldj2);
                    MapleFamilyCharacter mf = inviter.getMFC();
                    if (mf.getJunior1() > 0) {
                        mf.setJunior2(c.getPlayer().getId());
                    } else {
                        mf.setJunior1(c.getPlayer().getId());
                    }
                    inviter.saveFamilyStatus();
                    if (old > 0 && World.Family.getFamily(old) != null) { //has junior
                        MapleFamily.mergeFamily(fam, World.Family.getFamily(old));
                    } else {
                        c.getPlayer().setFamily(inviter.getFamilyId(), inviter.getId(), oldj1 <= 0 ? 0 : oldj1, oldj2 <= 0 ? 0 : oldj2);
                        fam.setOnline(c.getPlayer().getId(), true, c.getChannel());
                        c.getPlayer().saveFamilyStatus();
                    }
                    if (fam != null) {
                        if (inviter.getNoJuniors() == 1 || old > 0) {//刚刚得到他们的第一次欢呼
                            fam.resetDescendants();
                        }
                        fam.resetPedigree(); //is this necessary?
                    }
                } else {
                    int id = MapleFamily.createFamily(inviter.getId());
                    if (id > 0) {
                        //before loading the family, set sql
                        MapleFamily.setOfflineFamilyStatus(id, 0, c.getPlayer().getId(), 0, inviter.getCurrentRep(), inviter.getTotalRep(), inviter.getId());
                        MapleFamily.setOfflineFamilyStatus(id, inviter.getId(), oldj1 <= 0 ? 0 : oldj1, oldj2 <= 0 ? 0 : oldj2, c.getPlayer().getCurrentRep(), c.getPlayer().getTotalRep(), c.getPlayer().getId());
                        inviter.setFamily(id, 0, c.getPlayer().getId(), 0); //load the family
                        c.getPlayer().setFamily(id, inviter.getId(), oldj1 <= 0 ? 0 : oldj1, oldj2 <= 0 ? 0 : oldj2);
                        MapleFamily fam = World.Family.getFamily(id);
                        fam.setOnline(inviter.getId(), true, inviter.getClient().getChannel());
                        if (old > 0 && World.Family.getFamily(old) != null) { //has junior
                            MapleFamily.mergeFamily(fam, World.Family.getFamily(old));
                        } else {
                            fam.setOnline(c.getPlayer().getId(), true, c.getChannel());
                        }
                        fam.resetDescendants();
                        fam.resetPedigree();

                    }
                }
                c.sendPacket(FamilyPacket.getFamilyInfo(c.getPlayer()));
            }
        }
    }
}
