package cat.nyaa.heh.business.item;

import cat.nyaa.heh.db.DatabaseManager;
import cat.nyaa.heh.db.MarketConnection;
import cat.nyaa.heh.utils.UidUtils;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class ShopItemManager {
    private static ShopItemManager INSTANCE;
    private static final String TABLE_NAME = "items";
    private UidUtils uidManager = UidUtils.create(TABLE_NAME);

    private ShopItemManager() {
        uidManager.loadUid();
    }

    public static ShopItemManager getInstance(){
        if (INSTANCE == null){
            synchronized (ShopItemManager.class){
                if (INSTANCE == null) {
                    INSTANCE = new ShopItemManager();
                }
            }
        }
        return INSTANCE;
    }

    public Map<Long, ShopItem> shopItemMap = new WeakHashMap<>();

    public ShopItem getShopItem(long itemID) {
        MarketConnection instance = MarketConnection.getInstance();
        return shopItemMap.computeIfAbsent(itemID, (id) -> instance.loadItemFromDb(itemID));
    }

    public UidUtils getUidManager() {
        return uidManager;
    }

    public long getNextUid() {
        return this.uidManager.getNextUid();
    }

    public void updateShopItem(ShopItem shopItem) {
        DatabaseManager.getInstance().updateShopItem(shopItem);
    }

    public static ShopItem newShopItem(UUID from, ShopItemType type, ItemStack itemInMainHand, double unitPrice) {
        ShopItem item = new ShopItem(from, type, itemInMainHand, unitPrice);
        item.setUid(getInstance().getNextUid());
        return item;
    }

    public static void insertShopItem(ShopItem item){
        MarketConnection.getInstance().addItem(item);
    }

    public int getShopItemCount() {
        return DatabaseManager.getInstance().getShopItemCount();
    }

    public List<ShopItem> getShopItems(int current, int batchSize) {
        return DatabaseManager.getInstance().getShopItems(current, batchSize);
    }

    public List<ShopItem> searchShopItems(String keywords) {
        return DatabaseManager.getInstance().getShopItems(keywords);
    }

    public void invalidateItem(ShopItem item) {
        DatabaseManager.getInstance().invalidateItem(item);
    }

    public int getShopItemCount(UUID owner, ShopItemType shopItemType) {
        return DatabaseManager.getInstance().getShopItemCount(owner, shopItemType);
    }
}
