package gui;

import handling.world.MapleParty;
import server.ServerProperties;

import static gui.QQMsgServer.sendMsg;
import static gui.QQMsgServer.sendMsgToQQGroup;
import static tools.FileoutputUtil.CurrentReadable_Time;

public class QQͨ�� {

    /**
     * <ʵ����Ϣ������QQ����������>
     * ר�õ�ͨ���̣߳�����˳��������һʱ��֪ͨ���˺�����
     */
    public static String Sammy = "513251863";
    public static String ����1 = ServerProperties.getProperty("QQ1");
    public static String ����2 = ServerProperties.getProperty("QQ2");
    public static String ð�յ� = MapleParty.��������;

    public static void ͨ��(String a) {
        /**
         * <֪ͨСZ>
         */
        sendMsg("[" + ð�յ� + "]\r\n" + "Time:" + CurrentReadable_Time() + "\r\n" + a, Sammy);
        /**
         * <֪ͨ1�����ˣ�����1�����˲���Сz>
         */
        if (!"71447500".equals(ServerProperties.getProperty("ZEV.QQ1"))) {
            sendMsg("[" + ð�յ� + "]\r\n" + "Time:" + CurrentReadable_Time() + "\r\n" + a, ����1);
        }
        /**
         * <֪ͨ2�����ˣ�����2�����˲���Сz������2�ź�1�����˲�һ��������֪ͨ2��>
         */
        if (����1 == null ? ����2 != null : !����1.equals(����2)) {
            sendMsg("[" + ð�յ� + "]\r\n" + "Time:" + CurrentReadable_Time() + "\r\n" + a, ����2);
        }
    }

    public static void ͨ��(String a, String b) {
        sendMsg(a, b);
    }

    public static void OX����(String a) {
        sendMsgToQQGroup(a);
    }
    
     public static void Ⱥ֪ͨ(String a) {
        sendMsgToQQGroup(a);
    }

}
