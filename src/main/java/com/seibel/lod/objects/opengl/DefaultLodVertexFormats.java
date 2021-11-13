package com.seibel.lod.objects.opengl;

import com.google.common.collect.ImmutableList;

/**
 * A (almost) exact copy of MC's
 * DefaultVertexFormats class.
 * 
 * @author James Seibel
 * @version 11-13-2021
 */
public class DefaultLodVertexFormats
{
	public static final LodVertexFormatElement ELEMENT_POSITION = new LodVertexFormatElement(0, LodVertexFormatElement.Type.FLOAT, 3);
	public static final LodVertexFormatElement ELEMENT_COLOR = new LodVertexFormatElement(0, LodVertexFormatElement.Type.UBYTE, 4);
	public static final LodVertexFormatElement ELEMENT_UV = new LodVertexFormatElement(0, LodVertexFormatElement.Type.FLOAT, 2);
	public static final LodVertexFormatElement ELEMENT_LIGHT_MAP_UV = new LodVertexFormatElement(1, LodVertexFormatElement.Type.SHORT, 2);
	public static final LodVertexFormatElement ELEMENT_NORMAL = new LodVertexFormatElement(0, LodVertexFormatElement.Type.BYTE, 3);
	public static final LodVertexFormatElement ELEMENT_PADDING = new LodVertexFormatElement(0, LodVertexFormatElement.Type.BYTE, 1);
	
	
	public static final LodVertexFormat POSITION = 						new LodVertexFormat(ImmutableList.<LodVertexFormatElement>builder().add(ELEMENT_POSITION).build());
	public static final LodVertexFormat POSITION_COLOR = 				new LodVertexFormat(ImmutableList.<LodVertexFormatElement>builder().add(ELEMENT_POSITION).add(ELEMENT_COLOR).build());
	public static final LodVertexFormat POSITION_COLOR_LIGHTMAP = 		new LodVertexFormat(ImmutableList.<LodVertexFormatElement>builder().add(ELEMENT_POSITION).add(ELEMENT_COLOR).add(ELEMENT_LIGHT_MAP_UV).build());
	public static final LodVertexFormat POSITION_TEX = 					new LodVertexFormat(ImmutableList.<LodVertexFormatElement>builder().add(ELEMENT_POSITION).add(ELEMENT_UV).build());
	public static final LodVertexFormat POSITION_COLOR_TEX = 			new LodVertexFormat(ImmutableList.<LodVertexFormatElement>builder().add(ELEMENT_POSITION).add(ELEMENT_COLOR).add(ELEMENT_UV).build());
	public static final LodVertexFormat POSITION_COLOR_TEX_LIGHTMAP = 	new LodVertexFormat(ImmutableList.<LodVertexFormatElement>builder().add(ELEMENT_POSITION).add(ELEMENT_COLOR).add(ELEMENT_UV).add(ELEMENT_LIGHT_MAP_UV).build());
	
}
