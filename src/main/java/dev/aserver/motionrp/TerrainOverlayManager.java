package dev.aserver.motionrp;

import dev.aserver.motionrp.core.OverlayLedger;
import io.papermc.paper.math.Position;
import org.bukkit.*;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import java.util.*;

/** Client block packets only. The real world is never changed or snapshotted back. */
final class TerrainOverlayManager implements Listener {
    record Cell(World world,int x,int y,int z) {
        Location location() { return new Location(world,x,y,z); }
    }
    private final OverlayLedger<UUID,Cell> ledger=new OverlayLedger<>();
    private final BlockData air=Material.AIR.createBlockData();
    void show(Object owner,Set<Cell> cells,Collection<Player> audience) {
        var desired=new HashMap<UUID,Set<Cell>>();
        for(Player p:audience) {
            if(!p.isOnline()) continue;var visible=new HashSet<Cell>();
            for(Cell c:cells) if(available(p,c)) visible.add(c);
            if(!visible.isEmpty()) desired.put(p.getUniqueId(),visible);
        }
        apply(ledger.update(owner,desired));
    }
    void hideViewer(Player p){send(p,Set.of(),ledger.removeViewer(p.getUniqueId()));}
    void release(Object owner) { apply(ledger.release(owner)); }
    void restoreAll() {
        ledger.clear().forEach((id,cells)->{ var p=Bukkit.getPlayer(id);if(p!=null) send(p,Set.of(),cells); });
    }
    private void apply(Map<UUID,OverlayLedger.Delta<Cell>> changes) {
        changes.forEach((id,delta)->{ var p=Bukkit.getPlayer(id);if(p!=null) send(p,delta.shown(),delta.restored()); });
    }
    private static boolean available(Player p,Cell c) {
        return p.getWorld()==c.world() && c.world().isChunkLoaded(c.x()>>4,c.z()>>4)
                && p.isChunkSent(Chunk.getChunkKey(c.x()>>4,c.z()>>4));
    }
    private void send(Player p,Set<Cell> shown,Set<Cell> restored) {
        var data=new HashMap<Position,BlockData>();var tiles=new ArrayList<Cell>();
        for(Cell c:shown) if(available(p,c)) data.put(Position.block(c.x(),c.y(),c.z()),air);
        for(Cell c:restored) if(available(p,c)) {
            var block=c.world().getBlockAt(c.x(),c.y(),c.z());data.put(Position.block(c.x(),c.y(),c.z()),block.getBlockData());
            if(block.getState(false) instanceof TileState) tiles.add(c);
        }
        if(!data.isEmpty()) p.sendMultiBlockChange(data);
        for(Cell c:tiles) if(c.world().getBlockAt(c.x(),c.y(),c.z()).getState(false) instanceof TileState state) p.sendBlockUpdate(c.location(),state);
    }
    @EventHandler public void quit(PlayerQuitEvent e) { ledger.forget(e.getPlayer().getUniqueId()); }
    @EventHandler public void world(PlayerChangedWorldEvent e) { ledger.forget(e.getPlayer().getUniqueId()); }
}
