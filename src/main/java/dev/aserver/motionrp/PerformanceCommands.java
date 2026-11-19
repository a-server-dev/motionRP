package dev.aserver.motionrp;

import dev.aserver.motionrp.core.Performance;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

final class PerformanceCommands implements CommandExecutor,TabCompleter,Listener {
    private final MotionPlugin plugin;
    PerformanceCommands(MotionPlugin plugin){this.plugin=plugin;}
    // Brigadier literals are case-sensitive; preserve the requested /HRperformance spelling.
    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true)
    public void normalizeCommand(PlayerCommandPreprocessEvent event){
        String message=event.getMessage();int space=message.indexOf(' ');
        String head=space<0?message:message.substring(0,space);
        if(head.equalsIgnoreCase("/hrperformance") && !head.equals("/hrperformance"))
            event.setMessage("/hrperformance"+(space<0?"":message.substring(space)));
    }
    private String category(Command command){return command.getName().equalsIgnoreCase("goalperformance")?"goal":"hr";}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player player)){plugin.tell(sender,"プレイヤーがゲーム内で実行してください。");return true;}
        if(!player.hasPermission("motionrp.performance")){plugin.tell(player,"この操作の権限がありません。");return true;}
        String category=category(command);
        if(args.length==1 && args[0].equalsIgnoreCase("stop")){plugin.playback().stop(player.getUniqueId());plugin.tell(player,"モーションを停止しました。");return true;}
        var choices=Performance.choices(category);Performance performance;
        if(args.length==0)performance=choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
        else if(args.length==1){performance=Performance.find(category,args[0]).orElse(null);if(performance==null){usage(player,label,choices);return true;}}
        else {usage(player,label,choices);return true;}
        plugin.playback().play(player,new MotionCatalog.Definition("performance_"+performance.id(),performance.label(),Performance.MODEL,performance.id(),performance.ticks()+10,null,"performance"));return true;
    }
    private void usage(CommandSender sender,String label,List<Performance> choices){plugin.tell(sender,"使い方: /"+label+" ["+String.join("|",choices.stream().map(Performance::id).toList())+"|stop]");}
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        if(!sender.hasPermission("motionrp.performance") || args.length!=1)return List.of();
        var options=new ArrayList<>(Performance.choices(category(command)).stream().map(Performance::id).toList());options.add("stop");String prefix=args[0].toLowerCase(Locale.ROOT);return options.stream().filter(s->s.startsWith(prefix)).toList();
    }
}
