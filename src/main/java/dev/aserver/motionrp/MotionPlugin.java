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
    private AutomaticResourcePack resourcePack;

    @Override public void onEnable() {
        saveDefaultConfig();
        // Add only the new built-in definition during upgrade; keep user entries.
        for (String builtIn : java.util.List.of("rasengan", "rasenshuriken", "gate_of_babylon", "susanoo", "senbonzakura","titan_transform","gate_of_babylon_100","gate_of_babylon_enuma")) {
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
        if("motionrp_gate_of_babylon_enuma".equals(getConfig().getString("motions.gate_of_babylon_enuma.model"))
                && "gate_of_babylon_enuma".equals(getConfig().getString("motions.gate_of_babylon_enuma.effect"))
                && (getConfig().getInt("motions.gate_of_babylon_enuma.max-ticks",0)==280
                || getConfig().getInt("motions.gate_of_babylon_enuma.max-ticks",0)<dev.aserver.motionrp.core.TreasuryTimeline.SERIOUS_END)) {
            getConfig().set("motions.gate_of_babylon_enuma.max-ticks",320);saveConfig();
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
        catalog = MotionCatalog.read(getConfig());
        items = new MotionItems(this);
        loadouts = new PlayerLoadouts(this, items);
        menu = new LoadoutMenu(this, loadouts, items);
        playback = new MotionPlayback(this);
        Bukkit.getPluginManager().registerEvents(menu, this);
        Bukkit.getPluginManager().registerEvents(new ControllerListener(this), this);
        for(Player player:Bukkit.getOnlinePlayers()) TitanState.recover(this,player);
        var command = Objects.requireNonNull(getCommand("motionrp"));
        command.setExecutor(this);
        command.setTabCompleter(this);
        getLogger().info("MotionRP enabled: " + catalog.motions().size() + " motion(s), 9 slots per player.");
        new BundledModels(this).install();
        configureResourcePack();
    }

    @Override public void onDisable() {
        if(resourcePack!=null) resourcePack.close();
        if (playback != null) playback.stopAll();
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
    public MotionPlayback playback() { return playback; }

    public void tell(CommandSender sender, String message) {
        sender.sendMessage(Component.text("[MotionRP] ", NamedTextColor.GOLD).append(Component.text(message, NamedTextColor.WHITE)));
    }
    public void dataError(Player player, RuntimeException failure) {
        tell(player, "保存データを操作できません。アイテムを保護するため処理を中止しました。管理者に連絡してください。");
        getLogger().log(Level.SEVERE, "Loadout operation failed for " + player.getUniqueId() + "; existing data retained", failure);
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            if (sender instanceof Player player) menu.open(player);
            else tell(sender, "プレイヤーのみ実行できます。");
            return true;
        }
        if (!sender.hasPermission("motionrp.admin")) { tell(sender, "管理者権限が必要です。"); return true; }
        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "reload" -> {
                    reloadConfig();
                    MotionCatalog updated = MotionCatalog.read(getConfig());
                    playback.stopAll();
                    catalog = updated;
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

    private void give(CommandSender sender, String[] args) {
        if (args.length < 2) { usage(sender); return; }
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
        tell(sender, "/motionrp gui | status | reload | pack status | pack publish");
        tell(sender, "/motionrp give controller [プレイヤー]");
        tell(sender, "/motionrp give motion <ID> [個数1〜64] [プレイヤー]");
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> choices = new ArrayList<>();
        if (args.length == 1) {
            choices.add("gui");
            if (sender.hasPermission("motionrp.admin")) choices.addAll(List.of("give", "reload", "status", "pack"));
        } else if(sender.hasPermission("motionrp.admin") && args[0].equalsIgnoreCase("pack") && args.length==2) {
            choices.addAll(List.of("status","publish"));
        } else if (sender.hasPermission("motionrp.admin") && args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) choices.addAll(List.of("controller", "motion"));
            else if (args.length == 3 && args[1].equalsIgnoreCase("motion")) choices.addAll(catalog.motions().keySet());
            else if (args.length == 4 && args[1].equalsIgnoreCase("motion")) choices.addAll(List.of("1", "16", "64"));
            else if ((args.length == 3 && args[1].equalsIgnoreCase("controller")) || (args.length == 5 && args[1].equalsIgnoreCase("motion"))) {
                Bukkit.getOnlinePlayers().forEach(p -> choices.add(p.getName()));
            }
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(c -> c.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
