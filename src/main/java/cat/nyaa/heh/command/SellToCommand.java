package cat.nyaa.heh.command;

import cat.nyaa.heh.I18n;
import cat.nyaa.heh.business.direct.DirectInvoice;
import cat.nyaa.heh.business.item.ShopItem;
import cat.nyaa.heh.business.item.ShopItemManager;
import cat.nyaa.heh.business.item.ShopItemType;
import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

import static cat.nyaa.heh.command.CommandUtils.filtered;
import static cat.nyaa.heh.command.CommandUtils.getOnlinePlayers;

public class SellToCommand extends CommandReceiver implements ShortcutCommand{

    /**
     * @param plugin for logging purpose only
     * @param _i18n
     */
    public SellToCommand(Plugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
    }

    @Override
    public String getHelpPrefix() {
        return "shop";
    }

    private static final String PERMISSION_SELLTO = "heh.business.sellto";

    @SubCommand(isDefaultCommand = true, permission = PERMISSION_SELLTO, tabCompleter = "sellToCompleter")
    public void onSellTo(CommandSender sender, Arguments arguments){
        Player player = asPlayer(sender);
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand().clone()  ;
        if (itemInMainHand.getType().isAir()){
            new Message(I18n.format("command.sellto.no_item")).send(sender);
            return;
        }

        OfflinePlayer sellToPlayer = arguments.nextOfflinePlayer();
        double price = arguments.nextDouble();

        int amount = itemInMainHand.getAmount();
        double unitPrice = price / amount;
        ShopItem shopItem = ShopItemManager.newShopItem(player.getUniqueId(), ShopItemType.DIRECT, itemInMainHand, unitPrice);
        ShopItemManager.insertShopItem(shopItem);
        DirectInvoice.getInstance().newInvoice(player, shopItem, sellToPlayer.getUniqueId());
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        double realPrice = shopItem.getUnitPrice() * amount;
        new Message("").append(I18n.format("command.sellto.incoming_invoice", player.getName(), realPrice, shopItem.getUid()), shopItem.getItemStack())
                .send(sellToPlayer);
        new Message("").append(I18n.format("command.sellto.invoice_created", sellToPlayer.getName(), realPrice, shopItem.getUid()), shopItem.getItemStack())
                .send(sender);
    }

    public List<String> sellToCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(getOnlinePlayers());
                break;
            case 2:
                completeStr.add("<price>");
        }
        return filtered(arguments, completeStr);
    }

    public List<String> sampleCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                break;
        }
        return filtered(arguments, completeStr);
    }

    @Override
    public String getShortcutName() {
        return "hsellto";
    }
}
