package com.sepker.content;

import com.sepker.content.*;
import com.sepker.blocks.*;
import com.sepker.bullets.*;
import com.sepker.units.*;
import com.sepker.logic.*;
import com.sepker.*;
import mindustry.maps.planet.SerpuloPlanetGenerator;
import mindustry.world.Tiles;

public class SepkerPlanetGenerator extends SerpuloPlanetGenerator {

    @Override
    protected void generate() {
        // 先生成原始地形
        super.generate();

        // 再把生成的矿物全部清除，只保留地形
        // 这样就不会覆盖你地图里画好的固定矿物了
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                mindustry.world.Tile tile = tiles.get(x, y);
                if (tile != null && tile.overlay() != null && tile.overlay().isOverlay()) {
                    // 只清除矿物层，不动地形
                    tile.setOverlay(mindustry.content.Blocks.air);
                }
            }
        }
    }
}