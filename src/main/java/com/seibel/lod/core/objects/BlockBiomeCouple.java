package com.seibel.lod.core.objects;

import com.seibel.lod.core.wrapperInterfaces.block.IBlockColorWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

import java.util.Objects;

public class BlockBiomeCouple
{
	String blockName;
	String biomeName;
	
	IBiomeWrapper biomeColor;
	IBlockColorWrapper blockColor;
	
	public BlockBiomeCouple(IBiomeWrapper biomeColor, IBlockColorWrapper blockColor)
	{
		this.biomeColor = biomeColor;
		this.blockColor = blockColor;
		biomeName = biomeColor.getClass().getName();
		blockName = blockColor.getClass().getName();
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
