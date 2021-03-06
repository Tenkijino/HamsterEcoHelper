package cat.nyaa.heh.command;

import cat.nyaa.heh.HamsterEcoHelper;
import cat.nyaa.heh.I18n;
import cat.nyaa.heh.business.direct.DirectInvoice;
import cat.nyaa.heh.business.item.ShopItem;
import cat.nyaa.heh.business.item.ShopItemType;
import cat.nyaa.heh.business.transaction.Tax;
import cat.nyaa.heh.utils.SystemAccountUtils;
import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.util.*;

import static cat.nyaa.heh.command.CommandUtils.filtered;

public class PayCommand extends CommandReceiver implements ShortcutCommand{

    /**
     * @param plugin for logging purpose only
     * @param _i18n
     */
    public PayCommand(Plugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
    }

    @Override
    public String getHelpPrefix() {
        return "shop";
    }

    private static final String PERMISSION_PAY = "heh.business.pay";

    private static Map<UUID, ConfirmTask> confirmMap = new HashMap<>();

    @Override
    public String getShortcutName() {
        return "hpay";
    }

    class ConfirmTask extends BukkitRunnable{
        private UUID payer;
        private long uid;

        public ConfirmTask(UUID payer, long uid) {
            this.payer = payer;
            this.uid = uid;
        }

        @Override
        public void run() {
            confirmMap.remove(payer);
        }
    }

    @SubCommand(isDefaultCommand = true, permission = PERMISSION_PAY, tabCompleter = "payCompleter")
    public void onPay(CommandSender sender, Arguments arguments){
        Player player = asPlayer(sender);
        final UUID uniqueId = player.getUniqueId();
        long uid = arguments.nextLong();
        ShopItem item = DirectInvoice.getInstance().getInvoice(uid);
        if (item == null || !item.getShopItemType().equals(ShopItemType.DIRECT) || !item.isAvailable()){
            new Message("").append(I18n.format("command.pay.invalid_invoice", uid)).send(sender);
            return;
        }
        UUID customer = DirectInvoice.getInstance().getCustomer(item.getUid());
        OfflinePlayer customerOPlayer = Bukkit.getOfflinePlayer(customer);
        if (!customerOPlayer.isOnline()){
            new Message(I18n.format("command.pay.customer_offline", customerOPlayer.getName())).send(sender);
            return;
        }

        double realPrice = item.getUnitPrice() * (item.getAmount() - item.getSoldAmount());
        String sellerName = Bukkit.getOfflinePlayer(item.getOwner()).getName();
        if (item.isOwnedBySystem()){
            sellerName = SystemAccountUtils.getSystemName();
        }
        String customerName = Bukkit.getOfflinePlayer(customer).getName();

        if (item.getAmount() <= item.getSoldAmount()){
            new Message(I18n.format("command.cancel.payed_invoice", uid)).send(sender);
            return;
        }

        ConfirmTask cancelTask = confirmMap.getOrDefault(uniqueId, null);

        if (cancelTask == null){
            ConfirmTask runnable = new ConfirmTask(uniqueId, uid);
            confirmMap.put(uniqueId, runnable);
            runnable.runTaskLater(HamsterEcoHelper.plugin, 200);
            BigDecimal tax = Tax.calcTax(BigDecimal.valueOf(realPrice), Tax.getTaxRate(item));
            new Message("").append(I18n.format("command.pay.confirm_msg", sellerName, customerName, realPrice, tax.doubleValue()), item.getItemStack()).send(sender);
            return;
        }

        cancelTask.cancel();
        cancelTask.run();
        if (cancelTask.uid != uid) {
            new Message("").append(I18n.format("command.pay.uid_mismatch", cancelTask.uid, uid)).send(sender);
            return;
        }
        if (DirectInvoice.getInstance().payInvoice(player, item)){
            new Message("").append(I18n.format("command.pay.success", uid)).send(sender);
        }else {
            new Message("").append(I18n.format("command.pay.failed", uid)).send(sender);
        }
    }

    public List<String> payCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(DirectInvoice.getInstance().getDirectInvoiceIds());
                break;
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
}
