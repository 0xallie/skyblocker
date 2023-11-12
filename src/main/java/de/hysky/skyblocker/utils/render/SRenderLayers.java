package de.hysky.skyblocker.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayer.MultiPhase;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderPhase.Cull;
import net.minecraft.client.render.RenderPhase.DepthTest;
import net.minecraft.client.render.RenderPhase.Transparency;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;

public class SRenderLayers {
	private static final Transparency DEFAULT_TRANSPARENCY = new Transparency("default", () -> {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
	}, () -> RenderSystem.disableBlend());
	
	private static final MultiPhase FILLED = RenderLayer.of("filled", VertexFormats.POSITION_COLOR, DrawMode.TRIANGLE_STRIP, 131072, false, true, MultiPhaseParameters.builder()
			.program(RenderPhase.COLOR_PROGRAM)
			.cull(Cull.DISABLE_CULLING)
			.layering(RenderPhase.POLYGON_OFFSET_LAYERING)
			.transparency(DEFAULT_TRANSPARENCY)
			.depthTest(DepthTest.LEQUAL_DEPTH_TEST)
			.build(false));
	
	private static final MultiPhase FILLED_THROUGH_WALLS = RenderLayer.of("filled_through_walls", VertexFormats.POSITION_COLOR, DrawMode.TRIANGLE_STRIP, 131072, false, true, MultiPhaseParameters.builder()
			.program(RenderPhase.COLOR_PROGRAM)
			.cull(Cull.DISABLE_CULLING)
			.layering(RenderPhase.POLYGON_OFFSET_LAYERING)
			.transparency(DEFAULT_TRANSPARENCY)
			.depthTest(DepthTest.ALWAYS_DEPTH_TEST)
			.build(false));
	
	public static MultiPhase getFilled() {
		return FILLED;
	}
		
	public static MultiPhase getFilledThroughWalls() {
		return FILLED_THROUGH_WALLS;
	}
}
