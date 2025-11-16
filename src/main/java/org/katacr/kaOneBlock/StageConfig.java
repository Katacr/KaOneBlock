package org.katacr.kaOneBlock;

import java.util.HashMap;
import java.util.Map;

public class StageConfig {
    public int amount = 500;
    public String nextStage = "";
    public String message = "";
    public Map<String, Double> chestChances = new HashMap<>();
    public String entityPack = "";  // 实体包名称
    public double entityChance = 0.05;  // 实体生成概率 (5%)
}