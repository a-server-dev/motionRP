package dev.aserver.motionrp.core;

import java.util.*;

/** Per-viewer reference counts: overlapping casts cannot restore each other's cuts. */
public final class OverlayLedger<V,K> {
    public record Delta<K>(Set<K> shown,Set<K> restored) {}
    private final Map<Object,Map<V,Set<K>>> owners=new IdentityHashMap<>();
    private final Map<V,Map<K,Integer>> counts=new HashMap<>();
    public Map<V,Delta<K>> update(Object owner,Map<V,Set<K>> desired) {
        var previous=owners.getOrDefault(owner,Map.of());Set<V> viewers=new HashSet<>(previous.keySet());viewers.addAll(desired.keySet());
        var result=new HashMap<V,Delta<K>>();
        for(V viewer:viewers) {
            Set<K> before=previous.getOrDefault(viewer,Set.of()),after=desired.getOrDefault(viewer,Set.of());
            var refs=counts.computeIfAbsent(viewer,k->new HashMap<>());var shown=new HashSet<K>();var restored=new HashSet<K>();
            for(K cell:before) if(!after.contains(cell)) {
                int remaining=refs.getOrDefault(cell,1)-1;
                if(remaining==0) { refs.remove(cell);restored.add(cell); } else refs.put(cell,remaining);
            }
            for(K cell:after) if(!before.contains(cell)) {
                int old=refs.getOrDefault(cell,0);refs.put(cell,old+1);if(old==0) shown.add(cell);
            }
            if(refs.isEmpty()) counts.remove(viewer);
            if(!shown.isEmpty() || !restored.isEmpty()) result.put(viewer,new Delta<>(Set.copyOf(shown),Set.copyOf(restored)));
        }
        if(desired.isEmpty()) owners.remove(owner);
        else { var copy=new HashMap<V,Set<K>>();desired.forEach((v,k)->copy.put(v,Set.copyOf(k)));owners.put(owner,copy); }
        return result;
    }
    public Map<V,Delta<K>> release(Object owner) { return update(owner,Map.of()); }
    public Set<K> removeViewer(V viewer){
        var cells=Set.copyOf(counts.getOrDefault(viewer,Map.of()).keySet());forget(viewer);return cells;
    }
    public void forget(V viewer) {
        counts.remove(viewer);for(var map:owners.values()) map.remove(viewer);owners.values().removeIf(Map::isEmpty);
    }
    public Map<V,Set<K>> clear() {
        var result=new HashMap<V,Set<K>>();counts.forEach((v,c)->result.put(v,Set.copyOf(c.keySet())));counts.clear();owners.clear();return result;
    }
}
