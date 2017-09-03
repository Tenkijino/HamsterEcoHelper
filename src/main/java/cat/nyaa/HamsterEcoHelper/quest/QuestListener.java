package cat.nyaa.HamsterEcoHelper.quest;

import cat.nyaa.HamsterEcoHelper.HamsterEcoHelper;
import cat.nyaa.HamsterEcoHelper.I18n;
import cat.nyaa.HamsterEcoHelper.utils.database.tables.quest.QuestStation;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class QuestListener implements Listener {
    private final HamsterEcoHelper plugin;

    public QuestListener(HamsterEcoHelper plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignChangeEvent(SignChangeEvent event) {
        if ("[Quest]".equalsIgnoreCase(event.getLine(0))) {
            Player player = event.getPlayer();
            if (!player.hasPermission("heh.quest.admin")) return;
            Double fee;
            try {
                fee = Double.parseDouble(event.getLine(3));
            } catch (NumberFormatException ex) {
                fee = null;
            }
            if (fee == null) {
                event.getPlayer().sendMessage(I18n.format("user.quest.invalid_fee"));
                return;
            }
            plugin.database.query(QuestStation.class).insert(new QuestStation(event.getBlock().getLocation(), fee));
            event.getPlayer().sendMessage(I18n.format("user.quest.station_created"));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            QuestStation station = QuestCommon.toQuestStation(block.getLocation());
            if (station == null) return;
            event.getPlayer().sendMessage("Station clicked");
        }
    }
}
