package cat.nyaa.heh.updater;

import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import heh7_2.database.*;
import heh8_0.db.DatabaseManager;
import heh8_0.db.ShopItemType;
import heh8_0.db.model.*;
import heh8_0.db.utils.UidUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdaterMain extends JavaPlugin {
    I18n i18n;

    @Override
    public void onEnable() {
        super.onEnable();
        i18n = new I18n(this, "en_US");
        getCommand("hupdater").setExecutor(new UpdaterCommands(this, i18n));
    }

    public class UpdaterCommands extends CommandReceiver{
        /**
         * @param plugin for logging purpose only
         * @param _i18n
         */
        public UpdaterCommands(Plugin plugin, ILocalizer _i18n) {
            super(plugin, _i18n);
        }

        public void UpdaterCommands(){

        }

        @SubCommand(value = "update" , permission = "heh.update")
        public void onUpdate(CommandSender sender, Arguments arguments) throws SQLException, ClassNotFoundException, IOException {
            File dataFolder = UpdaterMain.this.getDataFolder().getParentFile();
            File dbFile = new File(dataFolder, "./HamsterEcoHelper/HamsterEcoHelper.db");
            Database database = new Database(dbFile);

            if (!dbFile.exists()){
                new Message("no old db file found, skipping").send(sender);
                return;
            }
            final Logger logger = Bukkit.getLogger();
            logger.log(Level.INFO, "backing up file...");
            try{
                int i = 1;
                String bakNameBase = "./HamsterEcoHelper/HamsterEcoHelper.db.bak";
                File backupFile = new File(dataFolder, bakNameBase);
                while(backupFile.exists()){
                    backupFile = new File(dataFolder, bakNameBase + i++);
                }
                Files.copy(dbFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }catch (Exception e){
                new Message("backup failed").send(sender);
                logger.log(Level.INFO, "backup failed", e);
                return;
            }

            List<SignShop> signShops = database.getSignShops();
            List<MarketItem> marketItems = database.getMarketItems(0, Integer.MAX_VALUE, null);
            List<Sign> shopSigns = database.getShopSigns();
            List<LottoStorageLocation> lottoStorageLocations = database.getLottoStorageLocations();
            List<Invoice> invoices = database.getInvoices();
            List<ShopStorageLocation> chestLocations = database.getChestLocations();


            File dbFileV8 = new File(dataFolder, "./HamsterEcoHelper/HamsterEcoHelper.db.new");
            if (dbFileV8.exists()){
                dbFileV8.delete();
            }else {
                dbFileV8.getParentFile().mkdirs();
            }

            UidUtils itemUid = UidUtils.create();
            UidUtils locationUid = UidUtils.create();
            DatabaseManager dbManagerV8 = DatabaseManager.getInstance(dbFileV8);

            new BukkitRunnable(){
                @Override
                public void run() {
                    new Message("update start").send(sender);
                    try{
                        logger.log(Level.INFO, "update start");
                        logger.log(Level.INFO, "clearing db");
                        dbManagerV8.clearAll();
                        logger.log(Level.INFO, "cleared db");
                        logger.log(Level.INFO, "update sign shop");
                        signShops.parallelStream().forEach(signShop -> Arrays.stream(ShopMode.values()).forEach(shopMode -> {
                            List<ShopItem> items = signShop.getItems(shopMode);
                            if (items.size() > 0){
                                items.stream().filter(Objects::nonNull).map(item -> {
                                    try {
                                        ShopItemType type = getShopItemType(shopMode);
                                        return createShopItemDbModel(itemUid, signShop.owner, item.getItemStack(1), item.amount, item.unitPrice, type);
                                    }catch (Exception e){
                                        return null;
                                    }
                                }).filter(Objects::nonNull).forEach(dbManagerV8::addShopItem);
                            }
                        }));
                        logger.log(Level.INFO, "update market");
                        marketItems.parallelStream().forEach(marketItem -> {
                            ShopItemDbModel shopItemDbModel = createShopItemDbModel(itemUid, marketItem.playerId, marketItem.getItemStack(), marketItem.amount, marketItem.unitPrice, ShopItemType.MARKET);
                            dbManagerV8.addShopItem(shopItemDbModel);
                        });
                        logger.log(Level.INFO, "update sign location");
                        shopSigns.parallelStream().forEach(sign -> {
                            LocationType locationType = getLocationType(sign.shopMode);
                            DataModel data = getData(sign.shopMode, sign.owner, sign.lotto_price);
                            LocationDbModel locationModel = createLocationModel(locationUid, sign.world, sign.x, sign.y, sign.z, locationType, null, sign.owner, data);
                            dbManagerV8.insertLocation(locationModel);
                        });
                        logger.log(Level.INFO, "update lotto storage");
                        lottoStorageLocations.parallelStream().forEach(lottoStorageLocation -> {
                            LocationType locationType = LocationType.CHEST_LOTTO;
                            LocationDbModel locationModel = createLocationModel(locationUid, lottoStorageLocation.world, lottoStorageLocation.x, lottoStorageLocation.y, lottoStorageLocation.z,
                                    locationType, null, lottoStorageLocation.owner, null);
                            dbManagerV8.insertLocation(locationModel);
                        });
                        logger.log(Level.INFO, "update invoice");
                        invoices.parallelStream().forEach(invoice -> {
                            ShopItemDbModel shopItemDbModel = createShopItemDbModel(
                                    itemUid,
                                    invoice.getSellerId(),
                                    invoice.getItemStack(),
                                    (int) invoice.getAmount(),
                                    invoice.getTotalPrice()/invoice.getAmount(),
                                    ShopItemType.DIRECT);
                            dbManagerV8.addShopItem(shopItemDbModel);

                            InvoiceDbModel model = createInvoiceModel(invoice.getSellerId(), invoice.getBuyerId(), invoice.getDraweeId(), shopItemDbModel, invoice.getCreatedTime());
                            dbManagerV8.insertInvoice(model);
                        });
                        logger.log(Level.INFO, "update chest location");
                        chestLocations.parallelStream().forEach(chestLocation -> {
                            LocationDbModel locationModel = createLocationModel(locationUid, chestLocation.world, chestLocation.x, chestLocation.y, chestLocation.z,
                                    LocationType.CHEST_BUY, null, chestLocation.owner, null);
                            dbManagerV8.insertLocation(locationModel);
                        });
                        logger.log(Level.INFO, "update complete");
                        database.close();
                        dbManagerV8.close();
                        System.gc();
                        for(int i = 0 ;;i++){
                            try{
                                Thread.sleep(5000);
                                new Message("copying file").send(sender);
                                Files.move(dbFileV8.toPath(), dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                new Message("update complete").send(sender);
                                break;
                            }catch (Exception e){
                                logger.log(Level.INFO, "copy failed, tried "+i+" times");
                            }
                        }

                    }catch (Exception e){
                        logger.log(Level.SEVERE, "update failed", e);
                        new Message("update failed").send(sender);
                    }finally {
                        try {
                            database.close();
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                        dbManagerV8.close();
                    }
                }
            }.runTaskAsynchronously(UpdaterMain.this);

        }

        private InvoiceDbModel createInvoiceModel(UUID sellerId, UUID buyerId, UUID payer, ShopItemDbModel item, long time) {
            InvoiceDbModel model = new InvoiceDbModel();
            model.setCustomer(buyerId);
            model.setPayer(payer);
            model.setFrom(sellerId);
            model.setUid(item.getUid());
            model.setTime(time);
            return model;
        }

        private DataModel getData(ShopMode shopMode, UUID owner, Double lottoPrice) {
            DataModel model = null;
            String ownerStr = Bukkit.getOfflinePlayer(owner).getName();
            switch (shopMode){
                case BUY:
                    ArrayList<String> lores = new ArrayList<>();
                    lores.add("");
                    lores.add("");
                    model = new SignShopData(lores);
                    break;
                case SELL:
                    ArrayList<String> lores1 = new ArrayList<>();
                    lores1.add("");
                    lores1.add("");
                    model = new SignShopData(lores1);
                    break;
                case LOTTO:
                    ArrayList<String> lores2 = new ArrayList<>();
                    lores2.add("");
                    LottoData model1 = new LottoData();
                    model1.setLottoPrice(lottoPrice);
                    model = model1;
                    break;
            }
            return model;
        }

        private LocationDbModel createLocationModel(UidUtils uidUtils, String world, Long x, Long y, Long z, LocationType locationType, UUID entityId, UUID owner, DataModel data) {
            LocationDbModel dbModel = new LocationDbModel();
            dbModel.setUid(uidUtils.getNextUid());
            dbModel.setWorld(world);
            dbModel.setX (x);
            dbModel.setY (y);
            dbModel.setZ (z);
            dbModel.setLocationType(locationType);
            dbModel.setEntityUUID(entityId);
            dbModel.setOwner(owner);
            dbModel.setData(data);
            return dbModel;
        }

        private LocationType getLocationType(ShopMode shopMode) {
            LocationType type = LocationType.SIGN_SHOP_SELL;
            switch (shopMode){
                case BUY:
                    type = (LocationType.SIGN_SHOP_BUY);
                    break;
                case SELL:
                    type = (LocationType.SIGN_SHOP_SELL);
                    break;
                case LOTTO:
                    type = (LocationType.SIGN_SHOP_LOTTO);
                    break;
            }
            return type;
        }

        private ShopItemType getShopItemType(ShopMode shopMode) {
            ShopItemType type = ShopItemType.SIGN_SHOP_SELL;
            switch (shopMode){
                case BUY:
                    type = (ShopItemType.SIGN_SHOP_BUY);
                    break;
                case SELL:
                    type = (ShopItemType.SIGN_SHOP_SELL);
                    break;
                case LOTTO:
                    type = (ShopItemType.LOTTO);
                    break;
            }
            return type;
        }

        private ShopItemDbModel createShopItemDbModel(final UidUtils uidUtils, UUID owner, ItemStack itemStack, int amount, Double unitPrice, ShopItemType type) {
            if (owner == null){
                return null;
            }
            ShopItemDbModel dbModel = new ShopItemDbModel();
            synchronized (uidUtils){

            }
            dbModel.setUid(uidUtils.getNextUid());
            dbModel.setOwner(owner);
            dbModel.setType(type);
            dbModel.setNbt(ItemStackUtils.itemToBase64(itemStack));
            dbModel.setPrice(unitPrice);
            dbModel.setAmount(amount);
            dbModel.setSold(0);
            dbModel.setAvailable(true);
            dbModel.setTime(System.currentTimeMillis());
            String ownerStr = Bukkit.getOfflinePlayer(owner).getName();
            dbModel.setMeta(itemStack.toString() + "owner:" + ownerStr);
            return dbModel;
        }

        @Override
        public String getHelpPrefix() {
            return "";
        }
    }

    public static void main(String[] args) throws Exception {


    }
}
