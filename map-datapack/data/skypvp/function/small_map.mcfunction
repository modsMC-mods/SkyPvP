# Small SkyPvP map generator
# Run /function skypvp:small_map while standing at the CENTER of a clear area.
# The block under your feet becomes the arena floor height. This does not clear terrain.

# Main floating island underside and fighting floor
fill ~-11 ~-3 ~-11 ~11 ~-1 ~11 minecraft:stone_bricks
fill ~-9 ~ ~-9 ~9 ~ ~9 minecraft:polished_andesite
fill ~-8 ~ ~-8 ~8 ~ ~8 minecraft:smooth_stone

# Dark border around the central platform
fill ~-9 ~1 ~-9 ~9 ~1 ~-9 minecraft:polished_deepslate
fill ~-9 ~1 ~9 ~9 ~1 ~9 minecraft:polished_deepslate
fill ~-9 ~1 ~-8 ~-9 ~1 ~8 minecraft:polished_deepslate
fill ~9 ~1 ~-8 ~9 ~1 ~8 minecraft:polished_deepslate

# Four small side islands
fill ~-19 ~-2 ~-3 ~-13 ~-1 ~3 minecraft:stone_bricks
fill ~-18 ~ ~-2 ~-14 ~ ~2 minecraft:oak_planks
fill ~13 ~-2 ~-3 ~19 ~-1 ~3 minecraft:stone_bricks
fill ~14 ~ ~-2 ~18 ~ ~2 minecraft:oak_planks
fill ~-3 ~-2 ~-19 ~3 ~-1 ~-13 minecraft:stone_bricks
fill ~-2 ~ ~-18 ~2 ~ ~-14 minecraft:oak_planks
fill ~-3 ~-2 ~13 ~3 ~-1 ~19 minecraft:stone_bricks
fill ~-2 ~ ~14 ~2 ~ ~18 minecraft:oak_planks

# Bridges connecting the islands to the main platform
fill ~-13 ~ ~-1 ~-9 ~ ~1 minecraft:oak_planks
fill ~9 ~ ~-1 ~13 ~ ~1 minecraft:oak_planks
fill ~-1 ~ ~-13 ~1 ~ ~-9 minecraft:oak_planks
fill ~-1 ~ ~9 ~1 ~ ~13 minecraft:oak_planks

# Gold markers show suggested event spawn points
setblock ~-6 ~1 ~0 minecraft:gold_block
setblock ~6 ~1 ~0 minecraft:gold_block
setblock ~0 ~1 ~-6 minecraft:gold_block
setblock ~0 ~1 ~6 minecraft:gold_block

# Four decorative corner posts
fill ~-7 ~1 ~-7 ~-7 ~4 ~-7 minecraft:polished_blackstone_bricks
fill ~7 ~1 ~-7 ~7 ~4 ~-7 minecraft:polished_blackstone_bricks
fill ~-7 ~1 ~7 ~-7 ~4 ~7 minecraft:polished_blackstone_bricks
fill ~7 ~1 ~7 ~7 ~4 ~7 minecraft:polished_blackstone_bricks
setblock ~-7 ~5 ~-7 minecraft:sea_lantern
setblock ~7 ~5 ~-7 minecraft:sea_lantern
setblock ~-7 ~5 ~7 minecraft:sea_lantern
setblock ~7 ~5 ~7 minecraft:sea_lantern

# Center landmark
fill ~-1 ~1 ~-1 ~1 ~2 ~1 minecraft:quartz_pillar
setblock ~0 ~3 ~0 minecraft:sea_lantern
