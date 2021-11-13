package com.seibel.lod.objects.opengl;

import org.lwjgl.opengl.GL11;

/**
 * A (almost) exact copy of Minecraft's
 * VertexFormatElement class.
 * A number of things were removed from the original
 * object since we didn't need them, specifically "usage".
 * 
 * @author James Seibel
 * @version 11-13-2021
 */
public class LodVertexFormatElement
{
//	private static final Logger LOGGER = LogManager.getLogger();
	private final LodVertexFormatElement.Type type;
//	private final LodVertexFormatElement.Usage usage;
	private final int index;
	private final int count;
	private final int byteSize;
	
//	public LodVertexFormatElement(int p_i46096_1_, LodVertexFormatElement.Type p_i46096_2_, LodVertexFormatElement.Usage p_i46096_3_, int p_i46096_4_)
	public LodVertexFormatElement(int newIndex, LodVertexFormatElement.Type newType, int newCount)
	{
//		if (this.supportsUsage(p_i46096_1_, p_i46096_3_))
//		{
//			this.usage = p_i46096_3_;
//		}
//		else
//		{
//			LOGGER.warn("Multiple vertex elements of the same type other than UVs are not supported. Forcing type to UV.");
//			this.usage = LodVertexFormatElement.Usage.UV;
//		}
		
		this.type = newType;
		this.index = newIndex;
		this.count = newCount;
		this.byteSize = newType.getSize() * this.count;
	}
	
//	private boolean supportsUsage(int p_177372_1_, LodVertexFormatElement.Usage p_177372_2_)
//	{
//		return p_177372_1_ == 0 || p_177372_2_ == LodVertexFormatElement.Usage.UV;
//	}
	
	public final LodVertexFormatElement.Type getType()
	{
		return this.type;
	}
	
//	public final LodVertexFormatElement.Usage getUsage()
//	{
//		return this.usage;
//	}
	
	public final int getIndex()
	{
		return this.index;
	}
	
	@Override
	public String toString()
	{
//		return this.count + "," + this.usage.getName() + "," + this.type.getName();
		return this.count + "," + this.type.getName();
	}
	
	public final int getByteSize()
	{
		return this.byteSize;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		else if (obj != null && this.getClass() == obj.getClass())
		{
			LodVertexFormatElement LodVertexFormatElement = (LodVertexFormatElement) obj;
			if (this.count != LodVertexFormatElement.count)
			{
				return false;
			}
			else if (this.index != LodVertexFormatElement.index)
			{
				return false;
			}
			else if (this.type != LodVertexFormatElement.type)
			{
				return false;
			}
			else
			{
//				return this.usage == LodVertexFormatElement.usage;
				return false;
			}
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public int hashCode()
	{
		int i = this.type.hashCode();
//		i = 31 * i + this.usage.hashCode();
		i = 31 * i + this.index;
		return 31 * i + this.count;
	}
	
//	public void setupBufferState(long p_227897_1_, int p_227897_3_)
//	{
//		this.usage.setupBufferState(this.count, this.type.getGlType(), p_227897_3_, p_227897_1_, this.index);
//	}
	
//	public void clearBufferState()
//	{
//		this.usage.clearBufferState(this.index);
//	}
	
	//Forge Start
	public int getElementCount()
	{
		return count;
	}
	//Forge End
	
	public static enum Type
	{
		FLOAT(4, "Float", GL11.GL_FLOAT),
		UBYTE(1, "Unsigned Byte", GL11.GL_UNSIGNED_BYTE),
		BYTE(1, "Byte", GL11.GL_BYTE),
		USHORT(2, "Unsigned Short", GL11.GL_UNSIGNED_SHORT),
		SHORT(2, "Short", GL11.GL_SHORT),
		UINT(4, "Unsigned Int", GL11.GL_UNSIGNED_INT),
		INT(4, "Int", GL11.GL_INT);
		
		private final int size;
		private final String name;
		private final int glType;
		
		private Type(int sizeInBytes, String newName, int openGlDataType)
		{
			this.size = sizeInBytes;
			this.name = newName;
			this.glType = openGlDataType;
		}
		
		public int getSize()
		{
			return this.size;
		}
		
		public String getName()
		{
			return this.name;
		}
		
		public int getGlType()
		{
			return this.glType;
		}
	}
	
	
	
//	public static enum Usage
//	{
//		POSITION("Position", (p_227914_0_, p_227914_1_, p_227914_2_, p_227914_3_, p_227914_5_) ->
//		{
//			GlStateManager._vertexPointer(p_227914_0_, p_227914_1_, p_227914_2_, p_227914_3_);
//			GlStateManager._enableClientState(32884);
//		}, (p_227912_0_) ->
//		{
//			GlStateManager._disableClientState(32884);
//		}),
//		NORMAL("Normal", (p_227913_0_, p_227913_1_, p_227913_2_, p_227913_3_, p_227913_5_) ->
//		{
//			GlStateManager._normalPointer(p_227913_1_, p_227913_2_, p_227913_3_);
//			GlStateManager._enableClientState(32885);
//		}, (p_227910_0_) ->
//		{
//			GlStateManager._disableClientState(32885);
//		}),
//		COLOR("Vertex Color", (p_227911_0_, p_227911_1_, p_227911_2_, p_227911_3_, p_227911_5_) ->
//		{
//			GlStateManager._colorPointer(p_227911_0_, p_227911_1_, p_227911_2_, p_227911_3_);
//			GlStateManager._enableClientState(32886);
//		}, (p_227908_0_) ->
//		{
//			GlStateManager._disableClientState(32886);
//			GlStateManager._clearCurrentColor();
//		}),
//		UV("UV", (p_227909_0_, p_227909_1_, p_227909_2_, p_227909_3_, p_227909_5_) ->
//		{
//			GlStateManager._glClientActiveTexture('\u84c0' + p_227909_5_);
//			GlStateManager._texCoordPointer(p_227909_0_, p_227909_1_, p_227909_2_, p_227909_3_);
//			GlStateManager._enableClientState(32888);
//			GlStateManager._glClientActiveTexture(33984);
//		}, (p_227906_0_) ->
//		{
//			GlStateManager._glClientActiveTexture('\u84c0' + p_227906_0_);
//			GlStateManager._disableClientState(32888);
//			GlStateManager._glClientActiveTexture(33984);
//		}),
//		PADDING("Padding", (p_227907_0_, p_227907_1_, p_227907_2_, p_227907_3_, p_227907_5_) ->
//		{
//		}, (p_227904_0_) ->
//		{
//		}),
//		GENERIC("Generic", (p_227905_0_, p_227905_1_, p_227905_2_, p_227905_3_, p_227905_5_) ->
//		{
//			GlStateManager._enableVertexAttribArray(p_227905_5_);
//			GlStateManager._vertexAttribPointer(p_227905_5_, p_227905_0_, p_227905_1_, false, p_227905_2_, p_227905_3_);
//		}, GlStateManager::_disableVertexAttribArray);
//		
//		private final String name;
//		private final LodVertexFormatElement.Usage.ISetupState setupState;
//		private final IntConsumer clearState;
//		
//		private Usage(String p_i225912_3_, LodVertexFormatElement.Usage.ISetupState p_i225912_4_, IntConsumer p_i225912_5_)
//		{
//			this.name = p_i225912_3_;
//			this.setupState = p_i225912_4_;
//			this.clearState = p_i225912_5_;
//		}
//		
//		private void setupBufferState(int p_227902_1_, int p_227902_2_, int p_227902_3_, long p_227902_4_, int p_227902_6_)
//		{
//			this.setupState.setupBufferState(p_227902_1_, p_227902_2_, p_227902_3_, p_227902_4_, p_227902_6_);
//		}
//		
//		public void clearBufferState(int p_227901_1_)
//		{
//			this.clearState.accept(p_227901_1_);
//		}
//		
//		public String getName()
//		{
//			return this.name;
//		}
//		
//		@OnlyIn(Dist.CLIENT)
//		interface ISetupState
//		{
//			void setupBufferState(int p_setupBufferState_1_, int p_setupBufferState_2_, int p_setupBufferState_3_, long p_setupBufferState_4_, int p_setupBufferState_6_);
//		}
//	}
	
	
}