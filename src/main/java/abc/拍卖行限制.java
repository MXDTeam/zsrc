/*
�����˺���װ��
 */
package abc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Properties;

public class ���������� {

    private static ���������� instance = null;
    private static boolean CANLOG;
    private final Properties itempb_cfg;
    private String PM;

    private static final Logger log = LoggerFactory.getLogger(����������.class);

    public ����������() {
        itempb_cfg = new Properties();
        try {
            InputStreamReader is = new FileReader("�����ļ�\\����������.ini");
            itempb_cfg.load(is);
            is.close();
            PM = itempb_cfg.getProperty("PM");
        } catch (Exception e) {
            log.error("Could not configuration", e);
        }
    }

    public String getPM() {
        return PM;
    }

    public boolean isCANLOG() {
        return CANLOG;
    }

    public void setCANLOG(boolean CANLOG) {
        ����������.CANLOG = CANLOG;
    }

    public static ���������� getInstance() {
        if (instance == null) {
            instance = new ����������();
        }
        return instance;
    }
}
