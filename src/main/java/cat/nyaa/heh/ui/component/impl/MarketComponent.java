package cat.nyaa.heh.ui.component.impl;

import cat.nyaa.heh.HamsterEcoHelper;
import cat.nyaa.heh.business.item.ShopItem;
import cat.nyaa.heh.business.market.Market;
import cat.nyaa.heh.business.transaction.TaxReason;
import cat.nyaa.heh.ui.MarketGUI;
import cat.nyaa.heh.ui.UiManager;
import cat.nyaa.heh.ui.component.ShopComponent;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MarketComponent extends ShopComponent {
    public MarketComponent(Inventory inventory) {
        super(inventory);
    }

    @Override
    protected String getReason() {
        return TaxReason.REASON_MARKET;
    }

    @Override
    protected double getFee() {
        return HamsterEcoHelper.plugin.config.marketFeeBase;
    }

    @Override
    protected void onPostTransaction() {
        UiManager.getInstance().getMarketUis().forEach(MarketGUI::refreshGUI);
    }

    private List<ShopItem> loadItems() {
        List<ShopItem> marketItems;
        Market.getInstance().loadItems();
        if (ownerFilter == null){
            marketItems = Market.getInstance().getMarketItems();
        }else {
            marketItems = Market.getInstance().getMarketItems(ownerFilter);
        }
        return marketItems;
    }

    @Override
    public void loadData() {
        items = loadItems();
    }

    @Override
    public void loadData(List<ShopItem> data) {
        this.items = data;
    }

    @Override
    public Map<String, String> getInfo() {
        Map<String, String> info = super.getInfo();
        return info;
    }

    private UUID ownerFilter = null;

    public void setOwnerFilter(UUID whoClicked) {
        ownerFilter = whoClicked;
    }

    public void removeOwnerFilter(){
        ownerFilter = null;
    }

    public boolean isFiltered() {
        return ownerFilter != null;
    }
}
