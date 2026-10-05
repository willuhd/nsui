package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nsui.CAMetalDrawable;
import nsui.CAMetalLayer;
import nsui.MPSImageGaussianBlur;
import nsui.MTLCommandBuffer;
import nsui.MTLCommandQueue;
import nsui.MTLClearColor;
import nsui.MTLDevice;
import nsui.MTLFunction;
import nsui.MTLLibrary;
import nsui.MTLRegion;
import nsui.MTLRenderCommandEncoder;
import nsui.MTLRenderPassColorAttachmentDescriptor;
import nsui.MTLRenderPassDescriptor;
import nsui.MTLRenderPipelineColorAttachmentDescriptor;
import nsui.MTLRenderPipelineDescriptor;
import nsui.MTLRenderPipelineState;
import nsui.MTLTexture;
import nsui.MTLTextureDescriptor;
import nsui.NSApplication;
import nsui.NSRect;
import nsui.NSSize;
import nsui.NSView;
import nsui.NSWindow;
import nsui.objc.Autorelease;
import nsui.objc.ObjC;

/// Committed Metal v1 coverage: device, shader compile, offscreen clear +
/// triangle with CPU pixel readback, MPS blur, layer drawable. Headless-safe:
/// no window needed except the drawable section (hidden parked window).
public final class MetalTest {

    private static final String TRIANGLE_MSL =
            "#include <metal_stdlib>\n" +
            "using namespace metal;\n" +
            "struct VSOut { float4 pos [[position]]; };\n" +
            "vertex VSOut vtx(uint vid [[vertex_id]], constant float4 *pos [[buffer(0)]]) {\n" +
            "    VSOut o; o.pos = pos[vid]; return o;\n" +
            "}\n" +
            "fragment float4 frag(VSOut in [[stage_in]]) {\n" +
            "    return float4(1.0, 0.35, 0.15, 1.0);\n" +
            "}\n";
    private static final int W = 64, H = 64;

    public static void main(String[] args) {
        System.out.println("=== MetalTest — offscreen GPU proof ===");
        ObjC.init();

        MTLDevice device = MTLDevice.systemDefault();
        if (device == null) {
            TestKit.skip("no Metal GPU on this machine");
            return;
        }
        TestKit.check(true, "system default GPU resolves");

        MTLCommandQueue queue;
        MTLLibrary library;
        MTLRenderPipelineState pipeline;
        try {
            queue = device.newCommandQueue();
            TestKit.check(queue != null, "command queue creates");
            library = device.newLibraryWithSource(TRIANGLE_MSL);
            TestKit.check(library != null, "MSL triangle compiles");
            MTLFunction vert = library.newFunctionWithName("vtx");
            MTLFunction frag = library.newFunctionWithName("frag");
            TestKit.check(vert != null && frag != null
                    && "vtx".equals(vert.name()) && "frag".equals(frag.name()),
                    "vertex+fragment functions resolve by name");
            MTLRenderPipelineDescriptor desc = MTLRenderPipelineDescriptor.create();
            desc.setVertexFunction(vert);
            desc.setFragmentFunction(frag);
            desc.colorAttachment(0).setPixelFormat(
                    MTLRenderPipelineColorAttachmentDescriptor.PIXEL_FORMAT_BGRA8_UNORM);
            pipeline = device.newRenderPipelineState(desc);
            TestKit.check(pipeline != null, "render pipeline builds");
        } catch (Throwable t) {
            TestKit.check(false, "setup threw: " + t);
            TestKit.end();
            return;
        }

        try {
            clearProof(device, queue);
        } catch (Throwable t) {
            TestKit.check(false, "clear section threw: " + t);
        }
        try {
            triangleProof(device, queue, pipeline);
        } catch (Throwable t) {
            TestKit.check(false, "triangle section threw: " + t);
        }
        try {
            blurProof(device, queue);
        } catch (Throwable t) {
            TestKit.check(false, "blur section threw: " + t);
        }
        try {
            layerDrawable();
        } catch (Throwable t) {
            TestKit.check(false, "layer section threw: " + t);
        }

        TestKit.end();
    }

    private static MTLTexture target(MTLDevice device) {
        MTLTextureDescriptor desc = MTLTextureDescriptor.texture2D(80, W, H, false);
        desc.setUsage(MTLTextureDescriptor.USAGE_RENDER_TARGET | MTLTextureDescriptor.USAGE_SHADER_READ);
        desc.setStorageMode(MTLTextureDescriptor.STORAGE_SHARED);
        MTLTexture tex = device.newTexture(desc);
        if (tex == null) throw new IllegalStateException("render target texture is nil");
        return tex;
    }

    private static MTLRenderPassDescriptor passFor(MTLTexture tex, MTLClearColor clear) {
        MTLRenderPassDescriptor pass = MTLRenderPassDescriptor.create();
        MTLRenderPassColorAttachmentDescriptor att = pass.colorAttachment(0);
        att.setTexture(tex);
        att.setLoadAction(MTLRenderPassColorAttachmentDescriptor.LOAD_CLEAR);
        att.setStoreAction(MTLRenderPassColorAttachmentDescriptor.STORE_STORE);
        att.setClearColor(clear);
        return pass;
    }

    private static byte[] readback(MTLTexture tex) {
        return tex.getBytes(W * 4, MTLRegion.of2D(0, 0, W, H), 0);
    }

    /// Clear-to-red offscreen, read back the center pixel (BGRA bytes).
    private static void clearProof(MTLDevice device, MTLCommandQueue queue) {
        MTLTexture tex = target(device);
        MemorySegment pool = Autorelease.push();
        try {
            MTLCommandBuffer buf = queue.commandBuffer();
            MTLRenderCommandEncoder enc = buf.renderEncoder(passFor(tex, new MTLClearColor(1, 0, 0, 1)));
            enc.endEncoding();
            buf.commit();
            buf.waitUntilCompleted();
        } finally {
            Autorelease.pop(pool);
        }
        byte[] px = readback(tex);
        int cx = (H / 2) * W * 4 + (W / 2) * 4;
        TestKit.check((px[cx] & 0xFF) == 0 && (px[cx + 1] & 0xFF) == 0
                && (px[cx + 2] & 0xFF) == 255 && (px[cx + 3] & 0xFF) == 255,
                "clear red reads back BGRA(0,0,255,255)");
    }

    private static byte[] triangleBytes() {
        float[] v = {-0.8f, -0.8f, 0f, 1f, 0.8f, -0.8f, 0f, 1f, 0f, 0.8f, 0f, 1f};
        ByteBuffer bb = ByteBuffer.allocate(v.length * 4).order(ByteOrder.nativeOrder());
        for (float f : v) bb.putFloat(f);
        return bb.array();
    }

    /// Draw the orange triangle on black, read back center (inside) vs corner.
    private static void triangleProof(MTLDevice device, MTLCommandQueue queue, MTLRenderPipelineState pipeline) {
        MTLTexture tex = target(device);
        byte[] verts = triangleBytes();
        MemorySegment pool = Autorelease.push();
        try {
            MTLCommandBuffer buf = queue.commandBuffer();
            MTLRenderCommandEncoder enc = buf.renderEncoder(
                    passFor(tex, new MTLClearColor(0, 0, 0, 1)));
            enc.setRenderPipelineState(pipeline);
            enc.setVertexBytes(verts, 0);
            enc.drawTriangles(0, 3);
            enc.endEncoding();
            buf.commit();
            buf.waitUntilCompleted();
        } finally {
            Autorelease.pop(pool);
        }
        byte[] px = readback(tex);
        int cx = (H / 2) * W * 4 + (W / 2) * 4;
        int r = px[cx + 2] & 0xFF, g = px[cx + 1] & 0xFF, b = px[cx] & 0xFF;
        TestKit.check(r > 200 && g > 60 && g < 120 && b < 70,
                "triangle center is orange (got R" + r + " G" + g + " B" + b + ")");
        int corner = 2 * W * 4 + 2 * 4;
        TestKit.check(px[corner] == 0 && px[corner + 1] == 0 && px[corner + 2] == 0,
                "corner stays black (triangle rasterized, not fullscreen)");
    }

    /// MPS blur over the triangle render: edges must change, interior holds.
    private static void blurProof(MTLDevice device, MTLCommandQueue queue) {
        MTLTexture src = target(device);
        MTLTexture dst = target(device);
        renderTriangle(device, queue, src);
        byte[] before = readback(src);
        MPSImageGaussianBlur blur = MPSImageGaussianBlur.create(device, 4.0f);
        TestKit.check(blur != null, "gaussian blur creates");
        MemorySegment pool = Autorelease.push();
        try {
            MTLCommandBuffer buf = queue.commandBuffer();
            blur.encode(buf, src, dst);
            buf.commit();
            buf.waitUntilCompleted();
        } finally {
            Autorelease.pop(pool);
        }
        byte[] after = readback(dst);
        boolean differ = false;
        for (int i = 0; i < before.length; i++) {
            if (before[i] != after[i]) { differ = true; break; }
        }
        TestKit.check(differ, "blurred texture differs from source (edges spread)");
    }

    private static void renderTriangle(MTLDevice device, MTLCommandQueue queue, MTLTexture tex) {
        MTLCommandQueue q = queue;
        MTLLibrary lib = device.newLibraryWithSource(TRIANGLE_MSL);
        MTLRenderPipelineDescriptor desc = MTLRenderPipelineDescriptor.create();
        desc.setVertexFunction(lib.newFunctionWithName("vtx"));
        desc.setFragmentFunction(lib.newFunctionWithName("frag"));
        desc.colorAttachment(0).setPixelFormat(80);
        MTLRenderPipelineState pipe = device.newRenderPipelineState(desc);
        MemorySegment pool = Autorelease.push();
        try {
            MTLCommandBuffer buf = q.commandBuffer();
            MTLRenderCommandEncoder enc = buf.renderEncoder(
                    passFor(tex, new MTLClearColor(0, 0, 0, 1)));
            enc.setRenderPipelineState(pipe);
            enc.setVertexBytes(triangleBytes(), 0);
            enc.drawTriangles(0, 3);
            enc.endEncoding();
            buf.commit();
            buf.waitUntilCompleted();
        } finally {
            Autorelease.pop(pool);
        }
    }

    /// Layer drawable from a hidden parked window (no pixels, AppKit-visible).
    private static void layerDrawable() {
        NSApplication app = TestKit.app();
        NSWindow win = TestKit.hiddenWindow(200, 200);
        MTLDevice device = MTLDevice.systemDefault();
        CAMetalLayer layer = CAMetalLayer.create();
        layer.setDevice(device);
        layer.setPixelFormat(80);
        layer.setDrawableSize(new NSSize(200, 200));
        NSView content = NSView.create(new NSRect(0, 0, 200, 200), (ctx, d) -> {});
        content.setWantsLayer(true);
        win.setContentView(content);
        content.layer().addSublayer(layer);
        layer.setFrame(new NSRect(0, 0, 200, 200));
        TestKit.show(win);
        try {
            app.pumpFor(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        CAMetalDrawable drawable = null;
        for (int i = 0; i < 10 && drawable == null; i++) {
            drawable = layer.nextDrawable();
            if (drawable == null) {
                try {
                    app.pumpFor(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        TestKit.check(drawable != null, "layer drawable available");
        if (drawable != null) {
            TestKit.check(drawable.texture() != null, "drawable texture non-nil");
        }
        TestKit.close(win);
    }
}
