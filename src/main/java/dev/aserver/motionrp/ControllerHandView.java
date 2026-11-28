package dev.aserver.motionrp;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.lang.reflect.*;

/** Client-only hand masking: the real inventory, controller identity and escrow never change. */
final class ControllerHandView {
    private static Access access;
    private static boolean unavailable;
    private record Access(Method handle,Field connection,Method send,Constructor<?> packet,Object empty) {}
    private ControllerHandView(){}
    static void hide(MotionPlugin plugin,Player player,boolean main,boolean off){
        if(unavailable||(!main&&!off)||!player.isOnline())return;
        try{
            if(access==null){
                Class<?> item=Class.forName("net.minecraft.world.item.ItemStack");
                Class<?> packet=Class.forName("net.minecraft.network.protocol.Packet");
                Method handle=player.getClass().getMethod("getHandle");
                Field connection=handle.getReturnType().getField("connection");
                String craft=org.bukkit.Bukkit.getServer().getClass().getPackageName();
                Object empty=Class.forName(craft+".inventory.CraftItemStack").getMethod("asNMSCopy",ItemStack.class).invoke(null,new ItemStack(Material.AIR));
                access=new Access(handle,connection,connection.getType().getMethod("send",packet),Class.forName("net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket").getConstructor(int.class,item),empty);
            }
            Object connection=access.connection.get(access.handle.invoke(player));
            if(main)access.send.invoke(connection,access.packet.newInstance(player.getInventory().getHeldItemSlot(),access.empty));
            if(off)access.send.invoke(connection,access.packet.newInstance(40,access.empty));
        }catch(ReflectiveOperationException failure){
            unavailable=true;
            plugin.getLogger().log(java.util.logging.Level.WARNING,"Cannot hide first-person controller on this server version",failure);
        }
    }
    static void restore(Player player){if(player!=null&&player.isOnline())player.updateInventory();}
}
