package dev.aserver.motionrp.core;

import java.util.*;

/** Command-only catalog: these celebrations never enter the physical-token loadout. */
public enum Performance {
    RONALDO("goal","ronaldo","ロナウド：Siu",112,46),
    KNEESLIDE("goal","kneeslide","膝スライディング",100,20),
    KAZU("goal","kazu","カズダンス",140,28),
    DESTASHA("hr","destasha","ベイスターズ：デスターシャ",112,48),
    MBAPPE("goal","mbappe","エムバペ：腕組み",100,26),
    YAMAKAWA("hr","yamakawa","山川穂高：どすこい",112,28),
    MATSUDA("hr","matsuda","松田宣浩：熱男",100,30);
    public static final String MODEL="motionrp_performances";
    private final String category,id,label;private final int ticks,highlight;
    Performance(String category,String id,String label,int ticks,int highlight){this.category=category;this.id=id;this.label=label;this.ticks=ticks;this.highlight=highlight;}
    public String category(){return category;}public String id(){return id;}public String label(){return label;}public int ticks(){return ticks;}public int highlight(){return highlight;}
    public static List<Performance> choices(String category){return Arrays.stream(values()).filter(p->p.category.equals(category)).toList();}
    public static Optional<Performance> find(String category,String id){String key=id.toLowerCase(Locale.ROOT);key=switch(key){case "slide","knee_slide"->"kneeslide";case "cr7","siu"->"ronaldo";case "dosukoi"->"yamakawa";case "atsuo"->"matsuda";default->key;};String selected=key;return choices(category).stream().filter(p->p.id.equals(selected)).findFirst();}
}
