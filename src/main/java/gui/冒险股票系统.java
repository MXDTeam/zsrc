package gui;

import server.Timer;

import java.util.concurrent.ScheduledFuture;

/**
 *
 * @author Administrator
 */
public class ð�չ�Ʊϵͳ {

    public static ScheduledFuture<?> ��Ʊ�߳� = null;
    public static Boolean �� = false;
    public static Boolean �� = false;
    public static Boolean һ = false;
    public static Boolean �� = false;

    public static void main(String[] args) {
        ��Ʊ�߳�();
    }

    /**
     * <23��>
     */
    public static void ��Ʊ�߳�() {
        if (��Ʊ�߳� == null) {
            ��Ʊ�߳� = Timer.BuffTimer.getInstance().register(new Runnable() {
                @Override
                public void run() {
                    System.err.println("1");
                }
            }, 1000 * 1);
        }
    }

}
