package com.seibel.lod.core.objects;

/*
import com.seibel.lod.core.wrapperInterfaces.block.BlockDetail;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BlockBiomeCouple
{
	public static final ConcurrentMap<BlockDetail, BlockBiomeCouple> noBiomeIstanceCache = new ConcurrentHashMap<>();
	public static ConcurrentMap<IBiomeWrapper, ConcurrentMap<BlockDetail, BlockBiomeCouple>> withBiomeIstanceCache = new ConcurrentHashMap<>();
	
	String blockName;
	String biomeName;
	String coupleName;
	
	IBiomeWrapper biomeColor;
	BlockDetail blockColor;
	
	public static void addBlockBiomeToCache(IBlockColorWrapper blockColor){
	}
	
	public static BlockBiomeCouple getBlockBiomeCouple(IBlockColorWrapper blockColor){
		if(noBiomeIstanceCache.containsKey(blockColor))
		{
			return noBiomeIstanceCache.get(blockColor);
		}
		else
		{
			BlockBiomeCouple couple = new BlockBiomeCouple(blockColor);
			noBiomeIstanceCache.put(blockColor,couple);
			return couple;
		}
	}
	
	public static BlockBiomeCouple getBlockBiomeCouple(IBiomeWrapper biomeColor, IBlockColorWrapper blockColor){
		if(biomeColor == null)
		{
			return getBlockBiomeCouple(blockColor);
		}
		else
		{
			if(withBiomeIstanceCache.containsKey(biomeColor))
			{
				withBiomeIstanceCache.put(biomeColor, new ConcurrentHashMap<>());
			}
			ConcurrentMap<IBlockColorWrapper, BlockBiomeCouple> blockToCoupleMap = withBiomeIstanceCache.get(biomeColor);
			if(blockToCoupleMap.containsKey(blockColor))
			{
				return blockToCoupleMap.get(blockColor);
			}
			else
			{
				BlockBiomeCouple couple = new BlockBiomeCouple(blockColor,biomeColor);
				blockToCoupleMap.put(blockColor,couple);
				return couple;
			}
		}
	}
	
	public BlockBiomeCouple(IBlockColorWrapper blockColor)
	{
		this.biomeColor = null;
		this.blockColor = blockColor;
		biomeName = "";
		blockName = blockColor.getName();
		coupleName = blockName;
	}
	
	public BlockBiomeCouple(IBlockColorWrapper blockColor, IBiomeWrapper biomeColor)
	{
		this.biomeColor = biomeColor;
		this.blockColor = blockColor;
		
		if(biomeColor == null)
			biomeName = biomeColor.getName();
		else
			biomeName = "";
		
		blockName = blockColor.getName();
		
		coupleName = blockName + biomeName;
	}
	
	@Override public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (!(o instanceof BlockBiomeCouple))
			return false;
		BlockBiomeCouple that = (BlockBiomeCouple) o;
		return Objects.equals(blockName, that.blockName) && Objects.equals(biomeName, that.biomeName);
	}
	
	@Override public int hashCode()
	{
		return Objects.hash(blockName, biomeName);
	}
	
	
}
*/