package cat.nyaa.heh.ui.component;

import cat.nyaa.heh.business.item.ModelableItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseComponent<T extends ModelableItem> extends MatrixComponent implements InventoryHolder, InfoHolder, RefreshableUi<T>, ClickEventHandler{
    protected Inventory uiInventory;

    protected List<T> items = new ArrayList<>();

    public BaseComponent(int startRow, int startCol, int rows, int columns) {
        super(startRow, startCol, rows, columns);
    }

    @Override
    public Inventory getInventory() {
        return uiInventory;
    }

    @Override
    public Map<String, String> getInfo() {
        HashMap<String, String> info = new HashMap<>();
        return info;
    }

    protected void setItemAt(int index, ItemStack itemStack){
        int row = index / columns();
        int col = index % columns();
        setItemAt(row, col, itemStack);
    }

    protected void setItemAt(int row, int col, ItemStack itemStack){
        uiInventory.setItem(access(row, col), itemStack);
    }
}
