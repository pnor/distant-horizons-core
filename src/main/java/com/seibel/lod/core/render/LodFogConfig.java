/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.render;

import com.seibel.lod.core.enums.rendering.*;
import com.seibel.lod.core.handlers.IReflectionHandler;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.render.objects.Shader;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.seibel.lod.core.render.GLProxy.GL_LOGGER;

/**
 * This object is just a replacement for an array
 * to make things easier to understand in the LodRenderer.
 * 
 * @author James Seibel
 * @version 11-26-2021
 */
public class LodFogConfig
{
	private static final IReflectionHandler REFLECTION_HANDLER = SingletonHandler.get(IReflectionHandler.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);

	public static final boolean DEBUG_DUMP_GENERATED_CODE = false;

	public final FogSetting farFogSetting;
	public final FogSetting heightFogSetting;
	public final HeightFogMixMode heightFogMixMode;
	public final HeightFogMode heightFogMode;
	public final float heightFogHeight;

	final boolean drawNearFog;

	public static LodFogConfig generateFogConfig() {
		FogDrawMode doDraw = CONFIG.client().graphics().fogQuality().getFogDrawMode();
		if (doDraw == FogDrawMode.USE_OPTIFINE_SETTING)
			doDraw = REFLECTION_HANDLER.getFogDrawMode();
		return new LodFogConfig(doDraw);
	}

	private LodFogConfig(FogDrawMode fogDrawMode) {
		if (fogDrawMode == FogDrawMode.FOG_DISABLED) {
			drawNearFog = false;
			farFogSetting = null;
			heightFogMixMode = null;
			heightFogMode = null;
			heightFogSetting = null;
			heightFogHeight = 0.f;
		} else {
			ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality setting = CONFIG.client().graphics().fogQuality();
			FogDistance fogDistance = setting.getFogDistance();
			drawNearFog = (fogDistance == FogDistance.NEAR || fogDistance == FogDistance.NEAR_AND_FAR);
			if (fogDistance == FogDistance.FAR || fogDistance == FogDistance.NEAR_AND_FAR) {
				farFogSetting = setting.advancedFog().computeFarFogSetting();
				heightFogMixMode = setting.advancedFog().heightFog().getHeightFogMixMode();
				if (heightFogMixMode != HeightFogMixMode.IGNORE_HEIGHT && heightFogMixMode != HeightFogMixMode.BASIC) {
					heightFogSetting = setting.advancedFog().heightFog().computeHeightFogSetting();
					heightFogMode = setting.advancedFog().heightFog().getHeightFogMode();
					if (heightFogMode.basedOnCamera) {
						heightFogHeight = 0.f;
					} else {
						heightFogHeight = (float) setting.advancedFog().heightFog().getHeightFogHeight();
					}
				} else {
					heightFogSetting = null;
					heightFogMode = null;
					heightFogHeight = 0.f;
				}
			} else {
				farFogSetting = null;
				heightFogSetting = null;
				heightFogMode = null;
				heightFogMixMode = null;
				heightFogHeight = 0.f;
			}
		}
	}

	public StringBuilder loadAndProcessFragShader(String path, boolean absoluteFilePath) {
		StringBuilder str = makeRuntimeDefine();
		generateRuntimeShaderCode(Shader.loadFile(path, absoluteFilePath, str));
		if (DEBUG_DUMP_GENERATED_CODE) {
			try (FileOutputStream file = new FileOutputStream("debugGenerated.frag", false)) {
				file.write(str.toString().getBytes(StandardCharsets.UTF_8));
				GL_LOGGER.info("Debug dumped generated code to debugGenerated.frag for {}", path);
			} catch (IOException e) {
				GL_LOGGER.warn("Failed to debug dump generated code to file for {}", path);
			}
		}
		return str;
	}

	private StringBuilder makeRuntimeDefine() {
		StringBuilder str = new StringBuilder();
		str.append("// =======RUNTIME GENERATED DEFINE SECTION========\n#version 150 core\n");

		if (farFogSetting == null) {
			str.append("\n"+
"#define farFogStart 0.0\n"+
"#define farFogLength 0.0\n"+
"#define farFogMin 0.0\n"+
"#define farFogRange 0.0\n"+
"#define farFogDensity 0.0\n"+
"#define heightFogStart 0.0\n"+
"#define heightFogLength 0.0\n"+
"#define heightFogMin 0.0\n"+
"#define heightFogRange 0.0\n"+
"#define heightFogDensity 0.0\n"+
					"\n");
		} else {
			str.append("\n#define farFogStart ");
			str.append(farFogSetting.start);
			str.append("\n#define farFogLength ");
			str.append(farFogSetting.end - farFogSetting.start);
			str.append("\n#define farFogMin ");
			str.append(farFogSetting.min);
			str.append("\n#define farFogRange ");
			str.append(farFogSetting.max - farFogSetting.min);
			str.append("\n#define farFogDensity ");
			str.append(farFogSetting.density);
			str.append("\n");

			if (heightFogSetting == null) {
				str.append("\n"+
"#define heightFogStart 0.0\n"+
"#define heightFogLength 0.0\n"+
"#define heightFogMin 0.0\n"+
"#define heightFogRange 0.0\n"+
"#define heightFogDensity 0.0\n"+
						"\n");
			} else {
				str.append("\n#define heightFogStart ");
				str.append(heightFogSetting.start);
				str.append("\n#define heightFogLength ");
				str.append(heightFogSetting.end - heightFogSetting.start);
				str.append("\n#define heightFogMin ");
				str.append(heightFogSetting.min);
				str.append("\n#define heightFogRange ");
				str.append(heightFogSetting.max - heightFogSetting.min);
				str.append("\n#define heightFogDensity ");
				str.append(heightFogSetting.density);
				str.append("\n");
			}
		}
		str.append("// =======RUNTIME END========\n");
		return str;
	}

	private static String getFarFogMethod(FogSetting.FogType fogType) {
		switch (fogType) {
			case LINEAR:
				return "	return linearFog(dist, farFogStart, farFogLength, farFogMin, farFogRange);\n";
			case EXPONENTIAL:
				return "	return exponentialFog(dist, farFogStart, farFogLength, farFogMin, farFogRange, farFogDensity);\n";
			case EXPONENTIAL_SQUARED:
				return "	return exponentialSquaredFog(dist, farFogStart, farFogLength, farFogMin, farFogRange, farFogDensity);\n";
		}
		throw new IllegalArgumentException();
	}
	private static String getHeightDepthMethod(HeightFogMode mode, float heightFogHeight) {
		String str = "";
		if (!mode.basedOnCamera) {
			str =  "	vertical = realY - (" + heightFogHeight + ");\n";
		}
		if (mode.below && mode.above) {
			str += "	return abs(vertical);\n";
		} else if (mode.below) {
			str += "	return -vertical;\n";
		} else if (mode.above) {
			str += "	return vertical;\n";
		} else {
			str += "	return 0;\n";
		}
		return str;
	}

	private static String getHeightFogMethod(FogSetting.FogType fogType) {
		switch (fogType) {
			case LINEAR:
				return "	return linearFog(dist, heightFogStart, heightFogLength, heightFogMin, heightFogRange);\n";
			case EXPONENTIAL:
				return "	return exponentialFog(dist, heightFogStart, heightFogLength, heightFogMin, heightFogRange, heightFogDensity);\n";
			case EXPONENTIAL_SQUARED:
				return "	return exponentialSquaredFog(dist, heightFogStart, heightFogLength, heightFogMin, heightFogRange, heightFogDensity);\n";
		}
		throw new IllegalArgumentException();
	}
	private static String getMixFogMethod(HeightFogMixMode mode, boolean drawNearFog) {
		if (drawNearFog) {
			switch (mode) {
				case BASIC:
				case IGNORE_HEIGHT:
					return "	return max(1.0-near, far);\n";
				case ADDITION:
					return "	return max(1.0-near, far + height);\n";
				case MAX:
					return "	return max(1.0-near, max(far, height));\n";
				case INVERSE_MULTIPLY:
					return "	return max(1.0-near, 1.0 - (1.0-far)*(1.0-height));\n";
				case MULTIPLY:
					return "	return max(1.0-near, far*height);\n";
				case LIMITED_ADDITION:
					return "	return max(1.0-near, far + max(far, height));\n";
				case MULTIPLY_ADDITION:
					return "	return max(1.0-near, far + far*height);\n";
				case INVERSE_MULTIPLY_ADDITION:
					return "	return max(1.0-near, far + 1.0 - (1.0-far)*(1.0-height));\n";
				case AVERAGE:
					return "	return max(1.0-near, far*0.5 + height*0.5);\n";
			}
		} else {
			switch (mode) {
				case BASIC:
				case IGNORE_HEIGHT:
					return "	return near * far;\n";
				case ADDITION:
					return "	return near * (far + height);\n";
				case MAX:
					return "	return near * max(far, height);\n";
				case INVERSE_MULTIPLY:
					return "	return near * (1.0 - (1.0-far)*(1.0-height));\n";
				case MULTIPLY:
					return "	return near * far*height);\n";
				case LIMITED_ADDITION:
					return "	return near * (far + max(far, height));\n";
				case MULTIPLY_ADDITION:
					return "	return near * (far + far*height);\n";
				case INVERSE_MULTIPLY_ADDITION:
					return "	return near * (far + 1.0 - (1.0-far)*(1.0-height));\n";
				case AVERAGE:
					return "	return near * (far*0.5 + height*0.5);\n";
			}
		}

		throw new IllegalArgumentException();
	}

	private void generateRuntimeShaderCode(StringBuilder str) {
		str.append("// =======RUNTIME GENERATED CODE SECTION========\n");

		// Generate method: float getNearFogThickness(float dist);
			str.append(""+
"float getNearFogThickness(float dist) {\n"+
"	return linearFog(dist, nearFogStart, nearFogLength, 0.0, 1.0);\n"+
"}\n"+"\n");

		if (farFogSetting == null) {
			str.append("\n"+
"float getFarFogThickness(float dist) { return 0.0; }\n"+
"float getHeightFogThickness(float dist) { return 0.0; }\n"+
"float calculateFarFogDepth(float horizontal, float dist, float nearFogStart) { return 0.0; }\n"+
"float calculateHeightFogDepth(float vertical, float realY) { return 0.0; }\n"+
"float mixFogThickness(float near, float far, float height) {" +
					(drawNearFog ? "return 1.0-near;" : "return 0.0;") +
					"}\n"+
					"\n");
		} else {
			// Generate method: float getFarFogThickness(float dist);
			str.append("float getFarFogThickness(float dist) {\n");
			str.append(getFarFogMethod(farFogSetting.fogType));
			str.append("\n}\n");

			// Generate method: float getHeightFogThickness(float dist);
			if (heightFogSetting == null) {
				str.append("\n"+
"float getHeightFogThickness(float dist) { return 0.0; }\n"+
"float calculateHeightFogDepth(float vertical, float realY) { return 0.0; }\n"+
					"\n");
			} else {
				str.append("float getHeightFogThickness(float dist) {\n");
				str.append(getHeightFogMethod(heightFogSetting.fogType));
				str.append("\n}\n");
				str.append("float calculateHeightFogDepth(float vertical, float realY) {\n");
				str.append(getHeightDepthMethod(heightFogMode, heightFogHeight));
				str.append("\n}\n");
			}

			// Generate method: calculateFarFogDepth(float horizontal, float dist, float nearFogStart);
			str.append("float calculateFarFogDepth(float horizontal, float dist, float nearFogStart) {\n");
			if (heightFogMixMode == HeightFogMixMode.BASIC) {
				str.append("	return (dist - nearFogStart)/(1.0 - nearFogStart);\n");
			} else {
				str.append("	return (horizontal - nearFogStart)/(1.0 - nearFogStart);\n");
			}
			str.append("}\n");

			// Generate method: float mixFogThickness(float near, float far, float height);
			str.append("float mixFogThickness(float near, float far, float height) {\n");
			str.append(getMixFogMethod(heightFogMixMode, drawNearFog));
			str.append("}\n");
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LodFogConfig that = (LodFogConfig) o;
		return Float.compare(that.heightFogHeight, heightFogHeight) == 0 && drawNearFog == that.drawNearFog && Objects.equals(farFogSetting, that.farFogSetting) && Objects.equals(heightFogSetting, that.heightFogSetting) && heightFogMixMode == that.heightFogMixMode && heightFogMode == that.heightFogMode;
	}

	@Override
	public int hashCode() {
		return Objects.hash(farFogSetting, heightFogSetting, heightFogMixMode, heightFogMode, heightFogHeight, drawNearFog);
	}
}
