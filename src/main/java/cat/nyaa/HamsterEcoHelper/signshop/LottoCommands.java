package cat.nyaa.HamsterEcoHelper.signshop;


import cat.nyaa.HamsterEcoHelper.HamsterEcoHelper;
import cat.nyaa.HamsterEcoHelper.I18n;
import cat.nyaa.HamsterEcoHelper.utils.database.tables.signshop.LottoStorageLocation;
import cat.nyaa.utils.CommandReceiver;
import cat.nyaa.utils.Internationalization;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LottoCommands extends CommandReceiver<HamsterEcoHelper> {

    private final HamsterEcoHelper plugin;

    public LottoCommands(Object plugin, Internationalization i18n) {
        super((HamsterEcoHelper) plugin, i18n);
        this.plugin = (HamsterEcoHelper) plugin;
    }

    public String getHelpPrefix() {
        return "lotto";
    }

    @SubCommand(value = "info", permission = "heh.signshop.lotto")
    public void info(CommandSender sender, Arguments args) {
        Player player = asPlayer(sender);
        LottoStorageLocation loc = plugin.database.getLottoStorageLocation(player.getUniqueId());
        if (loc == null || loc.getLocation() == null ||
                !SignShopManager.isChest(loc.getLocation().getBlock())) {
            player.sendMessage(I18n.format("user.signshop.lotto.set_storage"));
            return;
        }
        player.sendMessage(I18n.format("user.signshop.lotto.info", loc.getWorld(),
                loc.getLocation().getBlockX(), loc.getLocation().getBlockY(), loc.getLocation().getBlockZ()));
    }

    @SubCommand(value = "set", permission = "heh.signshop.lotto")
    public void setStorage(CommandSender sender, Arguments args) {
        Player player = asPlayer(sender);
        player.sendMessage(I18n.format("user.signshop.lotto.select_chest"));
        plugin.signShopListener.selectChest.put(player.getUniqueId(), ShopMode.LOTTO);
    }
}
