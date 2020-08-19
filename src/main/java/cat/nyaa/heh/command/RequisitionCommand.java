package cat.nyaa.heh.command;

import cat.nyaa.heh.I18n;
import cat.nyaa.heh.business.auction.Requisition;
import cat.nyaa.heh.item.ShopItem;
import cat.nyaa.heh.item.ShopItemManager;
import cat.nyaa.heh.item.ShopItemType;
import cat.nyaa.heh.utils.SystemAccountUtils;
import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static cat.nyaa.heh.command.CommandUtils.filtered;

public class RequisitionCommand extends CommandReceiver {
    /**
     * @param plugin for logging purpose only
     * @param _i18n
     */
    public RequisitionCommand(Plugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
    }
    private static final String PERMISSION_REQ = "heh.business.req";
    private static final String PERMISSION_ADMIN = "heh.admin.run";

    @SubCommand(isDefaultCommand = true, permission = PERMISSION_REQ, tabCompleter = "reqCompleter")
    public void onReq(CommandSender sender, Arguments arguments){
        Player player = asPlayer(sender);
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        if (itemInMainHand.getType().isAir()){
            new Message(I18n.format("command.requisition.no_item")).send(sender);
            return;
        }
        int amount = arguments.nextInt();
        double unitPrice = arguments.nextDouble();
        boolean isSystemAuc = false;
        if (sender.hasPermission(PERMISSION_ADMIN)){
            isSystemAuc = arguments.top() != null && arguments.nextBoolean();
        }

        UUID from = isSystemAuc? SystemAccountUtils.getSystemUuid() : player.getUniqueId();

        ShopItem shopItem = ShopItemManager.newShopItem(from, ShopItemType.REQUISITION, itemInMainHand, unitPrice);
        Requisition.startRequisition(shopItem);
    }

    public List<String> auctionCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.add("<amount>");
                break;
            case 2:
                completeStr.add("<unit price>");
                break;
            case 3:
                if (sender.hasPermission(PERMISSION_ADMIN)){
                    completeStr.add("<is system req?>");
                }
        }
        return filtered(arguments, completeStr);
    }

    @Override
    public String getHelpPrefix() {
        return "requisition";
    }
}
