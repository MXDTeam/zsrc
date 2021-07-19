package server;

import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CashItemFactoryA {

    private static final Map<Integer, Integer> snLookup = new HashMap();
    private static final Map<Integer, Integer> idLookup = new HashMap();
    private static final Map<Integer, CashItemInfoA> itemStats = new HashMap();
    private static final MapleDataProvider data = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath","wz") + "/Etc.wz"));
    private static final MapleData commodities = data.getData(StringUtil.getLeftPaddedStr("Commodity.img", '0', 11));
    private static final Map<Integer, List<CashItemInfoA>> cashPackages = new HashMap();

    public static CashItemInfoA getItem(int sn) {
        CashItemInfoA stats = itemStats.get(sn);
        if (stats == null) {
            int cid = getCommodityFromSN(sn);

            int itemId = MapleDataTool.getIntConvert(cid + "/ItemId", commodities);
            int count = MapleDataTool.getIntConvert(cid + "/Count", commodities, 1);
            int price = MapleDataTool.getIntConvert(cid + "/Price", commodities, 0);
            int period = MapleDataTool.getIntConvert(cid + "/Period", commodities, 0);
            int gender = MapleDataTool.getIntConvert(cid + "/Gender", commodities, 2);
            boolean onSale = MapleDataTool.getIntConvert(cid + "/OnSale", commodities, 0) == 1;

            stats = new CashItemInfoA(sn, itemId, count, price, period, gender, onSale);

            itemStats.put(sn, stats);
        }

        return stats;
    }

    private static int getCommodityFromSN(int sn) {
        int cid;
        if (snLookup.get(Integer.valueOf(sn)) == null) {
            int curr = snLookup.size() - 1;
            int currSN = 0;
            if (curr == -1) {
                curr = 0;
                currSN = MapleDataTool.getIntConvert("0/SN", commodities);
                snLookup.put(currSN, curr);
            }

            for (int i = snLookup.size() - 1; currSN != sn; i++) {
                curr = i;
                currSN = MapleDataTool.getIntConvert(curr + "/SN", commodities);
                snLookup.put(currSN, curr);
            }
            cid = curr;
        } else {
            cid = snLookup.get(sn);
        }
        return cid;
    }

    public static List<CashItemInfoA> getPackageItems(int itemId) {
        if (cashPackages.containsKey(itemId)) {
            return cashPackages.get(itemId);
        }
        List<CashItemInfoA> packageItems = new ArrayList<CashItemInfoA>();
        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.cherry.wzpath") + "/" + "Etc.wz"));
        MapleData a = dataProvider.getData("CashPackage.img");
        for (MapleData b : a.getChildren()) {
            if (itemId == Integer.parseInt(b.getName())) {
                for (MapleData c : b.getChildren()) {
                    for (MapleData d : c.getChildren()) {
                        int SN = MapleDataTool.getIntConvert("" + Integer.parseInt(d.getName()), c);
                        packageItems.add(getItem(SN));
                    }
                }
                break;
            }
        }
        cashPackages.put(itemId, packageItems);
        return packageItems;
    }

    public static int getSnFromId(int id) {
        int cid;
        if (idLookup.get(Integer.valueOf(id)) == null) {
            int curr = idLookup.size() - 1;
            int currSN = 0;
            if (curr == -1) {
                curr = 0;
                currSN = MapleDataTool.getIntConvert("0/ItemId", commodities);
                idLookup.put(Integer.valueOf(currSN), Integer.valueOf(curr));
            }

            for (int i = idLookup.size() - 1; currSN != id; i++) {
                curr = i;
                currSN = MapleDataTool.getIntConvert(curr + "/ItemId", commodities);
                idLookup.put(Integer.valueOf(currSN), Integer.valueOf(curr));
            }
            cid = curr;
        } else {
            cid = idLookup.get(Integer.valueOf(id)).intValue();
        }
        return MapleDataTool.getIntConvert(cid + "/SN", commodities);
    }
}