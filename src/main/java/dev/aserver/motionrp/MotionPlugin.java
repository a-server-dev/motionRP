package dev.aserver.motionrp;

import kr.toxicity.model.api.BetterModel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.logging.Level;

public final class MotionPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    private MotionCatalog catalog;
    private MotionItems items;
    private PlayerLoadouts loadouts;
    private LoadoutMenu menu;
    private MotionPlayback playback;
    private StudioProjects studio;
    StudioProjects studio(){return studio;}
    private AutomaticResourcePack resourcePack;
    private final dev.aserver.motionrp.core.PackApplicationState packApplications=new dev.aserver.motionrp.core.PackApplicationState();
    dev.aserver.motionrp.core.PackApplicationState packApplications(){return packApplications;}
    String currentPackHash(){return resourcePack==null?null:resourcePack.currentHash();}
    private EnergyPackService energyPack;
    private TerrainOverlayManager terrain;

    private FusionCoordinator fusion;
    FusionCoordinator fusion(){return fusion;}
    private MotionVisibility visibility;
    MotionVisibility visibility(){return visibility;}
    @Override public void onEnable() {
        saveDefaultConfig();
        // Add only the new built-in definition during upgrade; keep user entries.
        for (String builtIn : java.util.List.of("rasengan", "rasenshuriken", "gate_of_babylon", "susanoo", "senbonzakura","titan_transform","gate_of_babylon_100","gate_of_babylon_enuma","zangetsu_bankai","excalibur","gae_bolg","kamehameha","genki_dama","rho_aias","getsuga_tensho","road_roller","kokuryuha","kokujo_tengen_myoo","chibaku_tensei","fusion","broly","infinity_castle","death_sword","eternal_blizzard","death_spear","fire_tornado","emperor_penguin_1")) {
            if (!getConfig().contains("assets." + builtIn + "-definition-installed", true)) {
                if (!getConfig().contains("motions." + builtIn, true)) {
                    for (var entry : Objects.requireNonNull(getConfig().getDefaults())
                            .getConfigurationSection("motions." + builtIn).getValues(false).entrySet()) {
                        getConfig().set("motions." + builtIn + "." + entry.getKey(), entry.getValue());
                    }
                }
                getConfig().set("assets." + builtIn + "-definition-installed", true);
                saveConfig();
            }
        }
        if("motionrp_death_sword".equals(getConfig().getString("motions.death_sword.model"))
                && "death_sword".equals(getConfig().getString("motions.death_sword.effect"))
                && getConfig().getInt("motions.death_sword.max-ticks",0)==139) {
            getConfig().set("motions.death_sword.max-ticks",dev.aserver.motionrp.core.InazumaShot.DEATH_SWORD.end+10);saveConfig();
        }
        // Retimed anime reference: migrate only known stock timeouts.
        if("motionrp_eternal_blizzard".equals(getConfig().getString("motions.eternal_blizzard.model"))
                && "eternal_blizzard".equals(getConfig().getString("motions.eternal_blizzard.effect"))
                && (getConfig().getInt("motions.eternal_blizzard.max-ticks",0)==168||getConfig().getInt("motions.eternal_blizzard.max-ticks",0)==288)) {
            getConfig().set("motions.eternal_blizzard.max-ticks",dev.aserver.motionrp.core.InazumaShot.ETERNAL_BLIZZARD.end+10);saveConfig();
        }
        for(String football:java.util.List.of("fire_tornado","emperor_penguin_1")){
            String base="motions."+football+".";
            if(("motionrp_"+football).equals(getConfig().getString(base+"model"))&&(getConfig().getInt(base+"max-ticks")==174||(football.equals("fire_tornado")&&(getConfig().getInt(base+"max-ticks")==188||getConfig().getInt(base+"max-ticks")==122)))){
                getConfig().set(base+"max-ticks",dev.aserver.motionrp.core.FootballTimeline.end(football.equals("fire_tornado"))+10);saveConfig();
            }
        }
        if("刺し穿つ死棘の槍（ゲイ・ボルク）".equals(getConfig().getString("motions.gae_bolg.name"))) {
            getConfig().set("motions.gae_bolg.name","突き穿つ死翔の槍（ゲイ・ボルク）");saveConfig();
        }
        if("motionrp_gate_of_babylon_enuma".equals(getConfig().getString("motions.gate_of_babylon_enuma.model"))
                && "gate_of_babylon_enuma".equals(getConfig().getString("motions.gate_of_babylon_enuma.effect"))
                && (getConfig().getInt("motions.gate_of_babylon_enuma.max-ticks",0)==280
                || getConfig().getInt("motions.gate_of_babylon_enuma.max-ticks",0)<dev.aserver.motionrp.core.TreasuryTimeline.SERIOUS_END)) {
            getConfig().set("motions.gate_of_babylon_enuma.max-ticks",480);saveConfig();
        }
        if("motionrp_zangetsu_bankai".equals(getConfig().getString("motions.zangetsu_bankai.model"))
                && "zangetsu_bankai".equals(getConfig().getString("motions.zangetsu_bankai.effect"))
                && getConfig().getInt("motions.zangetsu_bankai.max-ticks",0)<dev.aserver.motionrp.core.BankaiTimeline.END) {
            getConfig().set("motions.zangetsu_bankai.max-ticks",280);saveConfig();
        }
        if("motionrp_susanoo".equals(getConfig().getString("motions.susanoo.model"))
                && "susanoo".equals(getConfig().getString("motions.susanoo.effect"))) {
            if(getConfig().getInt("motions.susanoo.max-ticks",0)<dev.aserver.motionrp.core.SusanooTimeline.END)
                getConfig().set("motions.susanoo.max-ticks",280);
            if("須佐能乎（上半身）".equals(getConfig().getString("motions.susanoo.name")))
                getConfig().set("motions.susanoo.name","完全体須佐能乎（マダラ）");
            saveConfig();
        }
        // Extend only this bundled scene's old timeout when upgrading its longer orbit.
        if("motionrp_senbonzakura".equals(getConfig().getString("motions.senbonzakura.model"))
                && "senbonzakura".equals(getConfig().getString("motions.senbonzakura.effect"))
                && getConfig().getInt("motions.senbonzakura.max-ticks",0)<dev.aserver.motionrp.core.SenbonzakuraTimeline.END) {
            getConfig().set("motions.senbonzakura.max-ticks",260);saveConfig();
        }
        if(!getConfig().contains("resource-pack",true)) {
            for(var entry:Objects.requireNonNull(getConfig().getDefaults()).getConfigurationSection("resource-pack").getValues(false).entrySet())
                getConfig().set("resource-pack."+entry.getKey(),entry.getValue());
            saveConfig();
        }
        if(!getConfig().contains("titan.use-bundled-skin",true)) {
            getConfig().set("titan.use-bundled-skin",true);saveConfig();
        }
        if(!getConfig().contains("visibility.range",true)){getConfig().set("visibility.range",48);saveConfig();}
        if("motionrp_getsuga_tensho".equals(getConfig().getString("motions.getsuga_tensho.model")) && "getsuga_tensho".equals(getConfig().getString("motions.getsuga_tensho.effect")) && getConfig().getInt("motions.getsuga_tensho.max-ticks",0)<dev.aserver.motionrp.core.BarrierSlashTimeline.GETSUGA_END){getConfig().set("motions.getsuga_tensho.max-ticks",186);saveConfig();}
        for(String id:java.util.List.of("road_roller","chibaku_tensei","kokuryuha"))if(("motionrp_"+id).equals(getConfig().getString("motions."+id+".model"))&&id.equals(getConfig().getString("motions."+id+".effect"))&&getConfig().getInt("motions."+id+".max-ticks",0)<dev.aserver.motionrp.core.EpicTimeline.end(id)){getConfig().set("motions."+id+".max-ticks",dev.aserver.motionrp.core.EpicTimeline.end(id)+10);saveConfig();}
        if(MotionNames.migrate(getConfig()))saveConfig();
        studio=new StudioProjects(this);
        catalog = studio.merge(MotionCatalog.read(getConfig()));
        items = new MotionItems(this);
        loadouts = new PlayerLoadouts(this, items);
        menu = new LoadoutMenu(this, loadouts, items);
        terrain=new TerrainOverlayManager();Bukkit.getPluginManager().registerEvents(terrain,this);
        visibility=new MotionVisibility(this);Bukkit.getPluginManager().registerEvents(visibility,this);
        playback = new MotionPlayback(this);fusion=new FusionCoordinator(this);
        Bukkit.getPluginManager().registerEvents(menu, this);
        Bukkit.getPluginManager().registerEvents(new ControllerListener(this), this);
        for(Player player:Bukkit.getOnlinePlayers()) {items.refreshNames(player);TitanState.recover(this,player);AerialAnchor.recover(this,player);}
        var command = Objects.requireNonNull(getCommand("motionrp"));
        command.setExecutor(this);
        command.setTabCompleter(this);
        var performances=new PerformanceCommands(this);
        getServer().getPluginManager().registerEvents(performances,this);
        for(String name:java.util.List.of("goalperformance","hrperformance")){var c=Objects.requireNonNull(getCommand(name));c.setExecutor(performances);c.setTabCompleter(performances);}
        getLogger().info("MotionRP enabled: " + catalog.motions().size() + " motion(s), 27 slots per player.");
        new BundledModels(this).install();
        energyPack=new EnergyPackService(this);
        configureResourcePack();
    }

    @Override public void onDisable() {
        if(fusion!=null)fusion.close();
        if(energyPack!=null)energyPack.close();
        if(resourcePack!=null) resourcePack.close();
        try { if (playback != null) playback.stopAll(); }
        finally { if(terrain!=null) terrain.restoreAll(); }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof LoadoutMenu.Holder) player.closeInventory();
        }
    }

    private void configureResourcePack() {
        if(resourcePack!=null) resourcePack.close();
        resourcePack=null;
        try { resourcePack=new AutomaticResourcePack(this); }
        catch(IllegalArgumentException invalid) { getLogger().warning("Resource pack settings are invalid; automatic delivery is disabled. Check resource-pack in config.yml."); }
    }
    private String packStatus() { return resourcePack==null ? "設定が不正です。resource-pack を確認してください。" : resourcePack.status(); }

    public MotionCatalog catalog() { return catalog; }
    public MotionItems items() { return items; }
    public PlayerLoadouts loadouts() { return loadouts; }
    public LoadoutMenu menu() { return menu; }
    TerrainOverlayManager terrain() { return terrain; }
    public MotionPlayback playback() { return playback; }

    public void tell(CommandSender sender, String message) {
        sender.sendMessage(Component.text("[MotionRP] ", NamedTextColor.GOLD).append(Component.text(message, NamedTextColor.WHITE)));
    }
    public void dataError(Player player, RuntimeException failure) {
        tell(player, "保存データを操作できません。アイテムを保護するため処理を中止しました。管理者に連絡してください。");
        getLogger().log(Level.SEVERE, "Loadout operation failed for " + player.getUniqueId() + "; existing data retained", failure);
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length>0&&args[0].equalsIgnoreCase("accept")){
            if(!(sender instanceof Player p)){tell(sender,"プレイヤーのみ実行できます。");return true;}if(args.length>2){tell(sender,"/motionrp accept [招待ID]");return true;}return fusion.acceptCurrent(p,args.length==2?args[1]:null);
        }
        if(args.length>0&&args[0].equalsIgnoreCase("emperor_penguin_2")){if(sender instanceof Player p)return fusion.penguin(p,args);tell(sender,"プレイヤーのみ実行できます。");return true;}
        if(args.length>0&&args[0].equalsIgnoreCase("broly")){if(sender instanceof Player p)return fusion.broly(p,args);tell(sender,"プレイヤーのみ実行できます。");return true;}
        if(args.length>0&&args[0].equalsIgnoreCase("fusion")){if(sender instanceof Player p)return fusion.command(p,args);tell(sender,"プレイヤーのみ実行できます。");return true;}
        if(args.length>0 && args[0].equalsIgnoreCase("view")){
            if(!(sender instanceof Player player)){tell(sender,"プレイヤーのみ実行できます。");return true;}
            if(!player.hasPermission("motionrp.use")){tell(sender,"権限がありません。");return true;}
            if(args.length>2 || (args.length==2 && !List.of("on","off").contains(args[1].toLowerCase(Locale.ROOT)))){tell(sender,"使い方: /motionrp view [on|off]");return true;}
            boolean value=args.length==1?!visibility.enabled(player.getUniqueId()):args[1].equalsIgnoreCase("on");
            visibility.set(player,value);if(!value)terrain.hideViewer(player);playback.refreshVisibility(player);
            tell(sender,"MotionRPの演出表示: "+(value?"ON":"OFF")+"（保存済み）");return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            if (sender instanceof Player player) menu.open(player);
            else tell(sender, "プレイヤーのみ実行できます。");
            return true;
        }
        if(args[0].equalsIgnoreCase("reload")){
            if(!(sender instanceof Player player)){tell(sender,"ゲーム内で /motionrp reload を実行してください。設定の再読込は /motionrp configreload です。");return true;}
            if(!player.hasPermission("motionrp.use")){tell(sender,"権限がありません。");return true;}
            if(args.length!=1){tell(sender,"/motionrp reload（自分のリソースパックのみ再適用）");return true;}
            if(resourcePack==null)tell(sender,packStatus());else resourcePack.reload(player);
            return true;
        }
        boolean giving=args[0].equalsIgnoreCase("give");
        if(giving?!canGive(sender):!sender.hasPermission("motionrp.admin")){
            tell(sender,giving?"give はOPまたはEmeraldREX91のみ利用できます。":"管理者権限が必要です。");return true;
        }
        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "configreload" -> {
                    reloadConfig();if(MotionNames.migrate(getConfig()))saveConfig();visibility.reload(this);
                    MotionCatalog updated = MotionCatalog.read(getConfig());
                    playback.stopAll();
                    catalog = studio.merge(updated);
                    configureResourcePack();
                    tell(sender, "設定を再読み込みしました。既存アイテムと登録内容は保持されます。");
                }
                case "status" -> {
                    tell(sender, "MotionRP " + getPluginMeta().getVersion() + " / BetterModel "
                            + Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("BetterModel")).getPluginMeta().getVersion());
                    tell(sender,"リソースパック: "+packStatus());
                    tell(sender, "BetterModel sight-trace=" + BetterModel.config().sightTrace()
                            + " min=" + BetterModel.config().minSight() + " max=" + BetterModel.config().maxSight());
                    tell(sender, "BetterModel プレイヤーモデル: " + String.join(", ", BetterModel.limbKeys()));
                    for (var definition : catalog.motions().values()) {
                        String problem = playback.unavailable(definition);
                        tell(sender, definition.id() + ": " + (problem == null ? "利用可能" : problem) + " [effect=" + definition.effect() + "]");
                    }
                }
                case "pack" -> {
                    if(args.length==2 && args[1].equalsIgnoreCase("publish")) {
                        tell(sender,resourcePack!=null && resourcePack.publishNow()?"パックの確認と公開を予約しました。/motionrp pack status で確認できます。":"resource-pack.enabled を有効にしてください。");
                    } else tell(sender,"リソースパック: "+packStatus());
                }
                case "give" -> give(sender, args);
                default -> usage(sender);
            }
        } catch (IllegalArgumentException invalid) {
            tell(sender, "設定または引数を確認してください: " + invalid.getMessage());
        }
        return true;
    }

    private boolean canGive(CommandSender sender){
        if(sender instanceof Player player)return dev.aserver.motionrp.core.GiveAccess.playerAllowed(player.getName(),player.isOp());
        return sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender;
    }
    private void give(CommandSender sender, String[] args) {
        if (args.length < 2) { usage(sender); return; }
        if(args[1].equalsIgnoreCase("all")&&args.length<=3){
            Player target=args.length==3?Bukkit.getPlayerExact(args[2]):(sender instanceof Player p?p:null);
            if(target==null){tell(sender,"オンラインの配布先プレイヤーを指定してください。");return;}
            if(target.getOpenInventory().getTopInventory().getHolder() instanceof LoadoutMenu.Holder){tell(sender,"配布先の登録GUIを閉じてから配布してください。");return;}
            var sets=items.completeControllers(catalog);long empty=java.util.Arrays.stream(target.getInventory().getStorageContents()).filter(i->i==null||i.getType().isAir()).count();
            if(empty<sets.size()){tell(sender,"配布先の所持品に空きがありません。");return;}
            target.getInventory().addItem(sets.toArray(ItemStack[]::new));target.saveData();tell(sender,target.getName()+" に全種セットを "+sets.size()+" 個配布しました。");return;
        }
        ItemStack item;
        String targetName;
        if (args[1].equalsIgnoreCase("controller") && args.length <= 3) {
            item = items.controller(catalog);
            targetName = args.length == 3 ? args[2] : null;

        } else if (args[1].equalsIgnoreCase("motion") && args.length >= 3 && args.length <= 5) {
            var definition = catalog.motions().get(args[2]);
            if (definition == null) throw new IllegalArgumentException("Unknown motion: " + args[2]);
            int amount = args.length >= 4 ? Integer.parseInt(args[3]) : 1;
            item = items.token(definition, amount);
            targetName = args.length == 5 ? args[4] : null;
        } else { usage(sender); return; }
        Player target = targetName == null ? (sender instanceof Player p ? p : null) : Bukkit.getPlayerExact(targetName);
        if (target == null) { tell(sender, "オンラインの配布先プレイヤーを指定してください。"); return; }
        if (target.getOpenInventory().getTopInventory().getHolder() instanceof LoadoutMenu.Holder) {
            tell(sender, "配布先の登録GUIを閉じてから配布してください。"); return;
        }
        if (!LoadoutMenu.fits(target.getInventory(), item)) { tell(sender, "配布先の所持品に空きがありません。"); return; }
        target.getInventory().addItem(item);
        target.saveData();
        tell(sender, target.getName() + " に " + item.getAmount() + " 個配布しました。");
    }

    private void usage(CommandSender sender) {
        tell(sender, "/motionrp gui | view [on|off] | reload | status | configreload | pack status | pack publish");
        tell(sender, "/motionrp broly [相手] | broly accept [招待者] | broly cancel");
        tell(sender,"/motionrp accept — 届いている招待に参加");
        tell(sender, "/motionrp fusion [相手] | fusion accept [招待者] | fusion cancel");
        tell(sender, "/motionrp give controller [プレイヤー] | give all [プレイヤー]");
        tell(sender, "/motionrp give motion <ID> [個数1〜64] [プレイヤー]");
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> choices = new ArrayList<>();
        if (args.length == 1) {
            choices.add("gui");choices.add("view");choices.add("fusion");choices.add("broly");choices.add("accept");choices.add("reload");
            if(canGive(sender))choices.add("give");
            if (sender.hasPermission("motionrp.admin")) choices.addAll(List.of("configreload", "status", "pack"));
        } else if((args.length==2||args.length==3)&&(args[0].equalsIgnoreCase("fusion")||args[0].equalsIgnoreCase("broly"))){if(args.length==2)choices.addAll(List.of("accept","cancel"));
            if(args.length==2||args[1].equalsIgnoreCase("accept"))Bukkit.getOnlinePlayers().stream().filter(p->p!=sender&&(!(sender instanceof Player viewer)||viewer.canSee(p))).forEach(p->choices.add(p.getName()));
        } else if(args.length==2 && args[0].equalsIgnoreCase("view")){choices.addAll(List.of("on","off"));
        } else if(sender.hasPermission("motionrp.admin") && args[0].equalsIgnoreCase("pack") && args.length==2) {
            choices.addAll(List.of("status","publish"));
        } else if (canGive(sender) && args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) choices.addAll(List.of("controller", "motion", "all"));
            else if (args.length == 3 && args[1].equalsIgnoreCase("motion")) choices.addAll(catalog.motions().keySet());
            else if (args.length == 4 && args[1].equalsIgnoreCase("motion")) choices.addAll(List.of("1", "16", "64"));
            else if ((args.length == 3 && (args[1].equalsIgnoreCase("controller")||args[1].equalsIgnoreCase("all"))) || (args.length == 5 && args[1].equalsIgnoreCase("motion"))) {
                Bukkit.getOnlinePlayers().forEach(p -> choices.add(p.getName()));
            }
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(c -> c.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
