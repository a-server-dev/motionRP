package dev.aserver.motionrp;

import dev.aserver.motionrp.core.Loadout;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import java.util.*;

/** Read-only previews: selection and favorite tags never transfer escrow items. */
final class SelectionMenu implements Listener {
 private static final int PAGE_SIZE=27;
 private final MotionPlugin plugin;
 SelectionMenu(MotionPlugin plugin){this.plugin=plugin;}
 static final class Holder implements InventoryHolder {
  final UUID owner;final String setId;final int heldSlot;final Inventory inventory;final int[] targets=new int[45];int page;
  Holder(Player player,MotionItems items){owner=player.getUniqueId();setId=items.setId(player.getInventory().getItemInMainHand());heldSlot=player.getInventory().getHeldItemSlot();inventory=Bukkit.createInventory(this,45,Component.text("モーション選択（上段: お気に入り）"));}
  public Inventory getInventory(){return inventory;}
 }
 void open(Player player){
  if(!player.hasPermission("motionrp.use"))return;
  if(!player.getItemOnCursor().isEmpty()){plugin.tell(player,"カーソルのアイテムを戻してから開いてください。");return;}
  try{var state=plugin.loadouts().read(player);plugin.playback().stop(player.getUniqueId(),"選択GUIを開く");var holder=new Holder(player,plugin.items());holder.page=state.selected()/PAGE_SIZE;render(holder,player,state);player.openInventory(holder.inventory);}catch(RuntimeException e){plugin.dataError(player,e);}
 }
 private ItemStack button(Material type,String name){var item=new ItemStack(type);var meta=item.getItemMeta();meta.displayName(MotionItems.text(name,NamedTextColor.GOLD));item.setItemMeta(meta);return item;}
 private void render(Holder h,Player player,Loadout state){
  Arrays.fill(h.targets,-1);h.inventory.clear();var favorites=plugin.loadouts().favorites(player,state);
  for(int i=0;i<9;i++)h.inventory.setItem(i,button(Material.YELLOW_STAINED_GLASS_PANE,"★ 空き枠（下のモーションを右クリックで登録）"));
  int favorite=0;
  for(String id:favorites)for(int i=0;i<Loadout.SIZE;i++)if(state.slot(i)!=null&&state.slot(i).id().equals(id)){h.targets[favorite]=i;h.inventory.setItem(favorite++,preview(state,i,true));break;}
  for(int slot=0;slot<PAGE_SIZE;slot++){int i=h.page*PAGE_SIZE+slot;if(i>=Loadout.SIZE)break;
   if(state.slot(i)!=null){h.targets[9+slot]=i;h.inventory.setItem(9+slot,preview(state,i,favorites.contains(state.slot(i).id())));}
   else h.inventory.setItem(9+slot,button(Material.GRAY_STAINED_GLASS_PANE,(i+1)+": 空き枠"));
  }
  h.inventory.setItem(37,button(Material.ARROW,"前のページ"));
  h.inventory.setItem(38,button(Material.PAPER,"登録枠 "+(h.page*PAGE_SIZE+1)+"〜"+Math.min((h.page+1)*PAGE_SIZE,Loadout.SIZE)+" / "+Loadout.SIZE));
  h.inventory.setItem(43,button(Material.ARROW,"次のページ"));
  h.inventory.setItem(36,button(Material.BOOK,"左クリック: 選択 / 右クリック: ★切替（最大9種）"));
  h.inventory.setItem(40,button(Material.CHEST,"登録・取り外し画面へ"));h.inventory.setItem(44,button(Material.BARRIER,"閉じる"));
 }
 private ItemStack preview(Loadout state,int i,boolean favorite){
  var item=plugin.loadouts().item(state.slot(i));var meta=item.getItemMeta();var lore=new ArrayList<Component>();
  lore.add(MotionItems.text("左クリックで選択 / 右クリックでお気に入り切替",NamedTextColor.YELLOW));
  if(favorite)lore.add(MotionItems.text("★ お気に入り",NamedTextColor.GOLD));
  if(i==state.selected())lore.add(MotionItems.text("▶ 選択中",NamedTextColor.GREEN));meta.lore(lore);item.setItemMeta(meta);return item;
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void click(InventoryClickEvent e){
  if(!(e.getView().getTopInventory().getHolder() instanceof Holder h))return;e.setCancelled(true);
  if(!(e.getWhoClicked() instanceof Player p)||!h.owner.equals(p.getUniqueId())||!p.hasPermission("motionrp.use"))return;
  if(!plugin.items().controller(p.getInventory().getItemInMainHand())||p.getInventory().getHeldItemSlot()!=h.heldSlot||!Objects.equals(h.setId,plugin.items().setId(p.getInventory().getItemInMainHand()))){p.closeInventory();return;}
  if(!p.getItemOnCursor().isEmpty()||e.getClickedInventory()!=h.inventory)return;
  if(e.getClick()!=ClickType.LEFT&&e.getClick()!=ClickType.RIGHT)return;
  int slot=e.getSlot();if(slot==37||slot==43){
   try{h.page=Math.floorMod(h.page+(slot==37?-1:1),(Loadout.SIZE+PAGE_SIZE-1)/PAGE_SIZE);render(h,p,plugin.loadouts().read(p));}catch(RuntimeException ex){plugin.dataError(p,ex);}return;
  }if(slot==40){p.closeInventory();plugin.menu().open(p);return;}if(slot==44){p.closeInventory();return;}
  int target=h.targets[slot];if(target<0)return;
  try{var state=plugin.loadouts().read(p);if(state.slot(target)==null)return;
   if(e.getClick()==ClickType.RIGHT){var favorites=plugin.loadouts().favorites(p,state);String id=state.slot(target).id();if(!favorites.remove(id)){if(favorites.size()>=9){plugin.tell(p,"お気に入りは9種類までです。先に★を解除してください。");return;}favorites.add(id);}plugin.loadouts().favorites(p,favorites);render(h,p,state);}
   else{plugin.loadouts().save(p,state.select(target));var def=plugin.catalog().motions().get(state.slot(target).id());p.closeInventory();p.sendActionBar(Component.text("選択: "+(def==null?state.slot(target).id():def.name()),NamedTextColor.AQUA));}
  }catch(RuntimeException ex){plugin.dataError(p,ex);}
 }
 @EventHandler(priority=EventPriority.HIGHEST) public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Holder)e.setCancelled(true);}
 @EventHandler public void close(InventoryCloseEvent e){if(e.getInventory().getHolder() instanceof Holder)e.getInventory().clear();}
}
