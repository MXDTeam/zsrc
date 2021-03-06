/*
豆豆机
 */
package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import tools.MaplePacketCreator;
import tools.data.LittleEndianAccessor;

import java.util.ArrayList;
import java.util.List;

public class BeanGame {

    public static void BeanGame1(LittleEndianAccessor slea, MapleClient c) {
      
        MapleCharacter chr = c.getPlayer();
        List<Beans> beansInfo = new ArrayList<Beans>();

        int type = slea.readByte();
        int 力度 = 0;
        int 豆豆序號 = 0;
        if (type == 1) {
            //01 E8 03
            力度 = slea.readShort();
            chr.setBeansRange(力度);
            c.sendPacket(MaplePacketCreator.enableActions());
        } else if (type == 0) {
            力度 = slea.readShort();
            豆豆序號 = slea.readInt() + 1;
            chr.setBeansRange(力度);
            chr.setBeansNum(豆豆序號);
            if (豆豆序號 == 1) {
                chr.setCanSetBeansNum(false);
            }
        } else if (type == 2) {
            if ((type == 11) || (type == 0)) {
                力度 = slea.readShort();
                豆豆序號 = slea.readInt() + 1;
                chr.setBeansRange(力度);
                chr.setBeansNum(豆豆序號);
                if (豆豆序號 == 1) {
                    chr.setCanSetBeansNum(false);
                }
            }
        } else if (type == 6) {
            slea.skip(1);
            int 循環次數 = slea.readByte();
            if (循環次數 == 0) {
                return;
            }
            if (循環次數 != 1) {
                slea.skip((循環次數 - 1) * 8);
            }
            if (chr.isCanSetBeansNum()) {
                chr.setBeansNum(chr.getBeansNum() + 循環次數);
            }
            chr.gainBeans(-循環次數);
            chr.setCanSetBeansNum(true);
        } else if ((type == 11) || (type == 6)) {
            力度 = slea.readShort();
            chr.setBeansRange(力度);
            byte size = (byte) (slea.readByte() + 1);
            short Pos = slea.readShort();
            byte Type = (byte) (slea.readByte() + 1);
            // for (int i = 0; i < 5; i++) {
            // beansInfo.add(new Beans(chr.getBeansRange() + rand(-100, 100), getBeanType(), chr.getBeansNum() + i));
            // }
            c.sendPacket(MaplePacketCreator.showBeans(力度, size, Pos, Type));
        } else {
            System.out.println("未處理的類型【" + type + "】\n包" + slea);
        }
    }

    private static int getBeanType() {
        int random = rand(1, 100);
        int beanType = 0;
        //3 - ??, 2 - ???, 1 - ??, 0 - ??????
        switch (random) {
            case 2:
                beanType = 1;
                break;
            case 49:
                beanType = 2;
                break;
            case 99:
                beanType = 3;
                break;
        }
        return beanType;
    }

    private static int rand(int lbound, int ubound) {
        return (int) ((Math.random() * (ubound - lbound + 1)) + lbound);
    }

    public class Beans {

        private final int number;
        private final int type;
        private final int pos;

        public Beans(int pos, int type, int number) {
            this.pos = pos;
            this.number = number;
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public int getNumber() {
            return number;
        }

        public int getPos() {
            return pos;
        }
    }

    public static final void BeanGame2(LittleEndianAccessor slea, MapleClient c) {
        c.sendPacket(MaplePacketCreator.updateBeans(c.getPlayer().getId(), c.getPlayer().getBeans()));
        c.sendPacket(MaplePacketCreator.enableActions());
    }
}
