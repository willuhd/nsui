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
import nsui.MTLSamplerDescriptor;
import nsui.MTLSamplerState;
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

    private static final String TEXTURE_MSL =
            "#include <metal_stdlib>\n" +
            "using namespace metal;\n" +
            "struct QTOut { float4 pos [[position]]; float2 uv; };\n" +
            "vertex QTOut vtxQ(uint vid [[vertex_id]], constant float4 *pos [[buffer(0)]]) {\n" +
            "    QTOut o; o.pos = pos[vid]; o.uv = o.pos.xy * 0.5 + 0.5; return o;\n" +
            "}\n" +
            "fragment float4 fragTex(QTOut in [[stage_in]], texture2d<float> tex [[texture(0)]],\n" +
            "        sampler smp [[sampler(0)]]) {\n" +
            "    return tex.sample(smp, in.uv);\n" +
            "}\n";
    private static final String BLEND_MSL =
            "#include <metal_stdlib>\n" +
            "using namespace metal;\n" +
            "struct VSOutB { float4 pos [[position]]; };\n" +
            "vertex VSOutB vtxB(uint vid [[vertex_id]], constant float4 *pos [[buffer(0)]]) {\n" +
            "    VSOutB o; o.pos = pos[vid]; return o;\n" +
            "}\n" +
            "fragment float4 fragA(VSOutB in [[stage_in]]) {\n" +
            "    return float4(1.0, 0.0, 0.0, 0.5);\n" +
            "}\n";

    public static void main(String[] args) {
        System.out.println("=== MetalTest — offscreen GPU proof ===");
        ObjC.init();

        MTLDevice device = MTLDevice.systemDefault();
        if (device == null) {
            TestKit.skip("no Metal GPU on this machine");
            return;
        }
        TestKit.probe("system default GPU resolves");

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
            uploadProof(device);
        } catch (Throwable t) {
            TestKit.check(false, "upload section threw: " + t);
        }
        try {
            samplerProof(device, queue);
        } catch (Throwable t) {
            TestKit.check(false, "sampler section threw: " + t);
        }
        try {
            blendProof(device, queue);
        } catch (Throwable t) {
            TestKit.check(false, "blend section threw: " + t);
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

    /// MPS blur over the triangle render: edge-adjacent pixels must spread
    /// toward the neighbor average while a solid interior block holds.
    /// Statistics, not exact bytes: blurred edges move, solid fills stay
    /// within +-8 levels per channel (dither-safe).
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
        // Orange mask on the source (same hue window as triangleProof).
        boolean[] orange = new boolean[W * H];
        int orangeCount = 0;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int i = (y * W + x) * 4;
                int r = before[i + 2] & 0xFF, g = before[i + 1] & 0xFF, b = before[i] & 0xFF;
                boolean o = r > 200 && g > 60 && g < 120 && b < 70;
                orange[y * W + x] = o;
                if (o) orangeCount++;
            }
        }
        TestKit.check(orangeCount > 100, "blur source has a real triangle to work on (" + orangeCount + " orange px)");
        // Edge band: orange pixels touching non-orange, plus non-orange pixels
        // touching orange (blur spreads both ways). Only meaningful if nonzero.
        boolean[] edgeBand = new boolean[W * H];
        int bandCount = 0;
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                boolean o = orange[y * W + x];
                for (int k = 0; k < 4; k++) {
                    int nx = x + dx[k], ny = y + dy[k];
                    if (nx < 0 || ny < 0 || nx >= W || ny >= H) continue;
                    if (orange[ny * W + nx] != o) {
                        edgeBand[y * W + x] = true;
                        bandCount++;
                        break;
                    }
                }
            }
        }
        // Edge-spread: band pixels whose max channel moved more than 8 levels.
        int moved = 0;
        for (int p = 0; p < W * H; p++) {
            if (!edgeBand[p]) continue;
            int i = p * 4;
            int d = 0;
            for (int c = 0; c < 4; c++) {
                d = Math.max(d, Math.abs((after[i + c] & 0xFF) - (before[i + c] & 0xFF)));
            }
            if (d > 8) moved++;
        }
        TestKit.check(bandCount > 0 && moved >= Math.max(10, bandCount / 4),
                "blurred edges spread toward neighbor average (moved " + moved + "/" + bandCount + " band px)");
        // Interior-hold: a solid block around the orange centroid stays put.
        long sx = 0, sy = 0;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (orange[y * W + x]) { sx += x; sy += y; }
            }
        }
        int cx0 = (int) (sx / Math.max(1, orangeCount));
        int cy0 = (int) (sy / Math.max(1, orangeCount));
        int held = 0, total = 0;
        for (int y = cy0 - 3; y <= cy0 + 3; y++) {
            for (int x = cx0 - 3; x <= cx0 + 3; x++) {
                if (x < 0 || y < 0 || x >= W || y >= H) continue;
                if (!orange[y * W + x]) continue;
                total++;
                int i = (y * W + x) * 4;
                int d = 0;
                for (int c = 0; c < 4; c++) {
                    d = Math.max(d, Math.abs((after[i + c] & 0xFF) - (before[i + c] & 0xFF)));
                }
                if (d <= 8) held++;
            }
        }
        TestKit.check(total >= 20 && held * 10 >= total * 9,
                "solid interior block holds within +-8 levels (" + held + "/" + total + " px)");
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

    /// Upload round-trip: full checkerboard then a subregion overwrite that
    /// must leave every neighbor byte identical. No encoder needed.
    private static void uploadProof(MTLDevice device) {
        MTLTextureDescriptor desc = MTLTextureDescriptor.texture2D(80, 4, 4, false);
        desc.setUsage(MTLTextureDescriptor.USAGE_SHADER_READ);
        desc.setStorageMode(MTLTextureDescriptor.STORAGE_SHARED);
        MTLTexture tex = device.newTexture(desc);
        TestKit.check(tex != null && tex.width() == 4 && tex.height() == 4,
                "upload target creates 4x4");
        byte[] checker = new byte[4 * 4 * 4];
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                int i = (y * 4 + x) * 4;
                byte v = ((x + y) % 2 == 0) ? (byte) 255 : (byte) 0;
                checker[i] = v;
                checker[i + 1] = v;
                checker[i + 2] = v;
                checker[i + 3] = (byte) 255;
            }
        }
        tex.replaceRegion(MTLRegion.of2D(0, 0, 4, 4), 0, checker, 16);
        byte[] back = tex.getBytes(16, MTLRegion.of2D(0, 0, 4, 4), 0);
        boolean same = back.length == checker.length;
        for (int i = 0; same && i < checker.length; i++) same = back[i] == checker[i];
        TestKit.check(same, "full-checker upload reads back byte-identical");
        byte[] red = new byte[2 * 2 * 4];
        for (int i = 0; i < 2 * 2; i++) {
            red[i * 4] = 0;
            red[i * 4 + 1] = 0;
            red[i * 4 + 2] = (byte) 255;
            red[i * 4 + 3] = (byte) 255;
        }
        tex.replaceRegion(MTLRegion.of2D(1, 1, 2, 2), 0, red, 8);
        byte[] after = tex.getBytes(16, MTLRegion.of2D(0, 0, 4, 4), 0);
        boolean neighbors = true;
        for (int y = 0; y < 4 && neighbors; y++) {
            for (int x = 0; x < 4 && neighbors; x++) {
                boolean inside = x >= 1 && x < 3 && y >= 1 && y < 3;
                int i = (y * 4 + x) * 4;
                for (int c = 0; c < 4 && neighbors; c++) {
                    byte want = inside ? red[((y - 1) * 2 + (x - 1)) * 4 + c] : checker[i + c];
                    neighbors = after[i + c] == want;
                }
            }
        }
        TestKit.check(neighbors, "2x2 subregion overwrite leaves neighbors identical");
        try {
            tex.replaceRegion(MTLRegion.of2D(3, 3, 2, 2), 0, checker, 16);
            TestKit.check(false, "out-of-range upload did not throw");
        } catch (IllegalArgumentException e) {
            TestKit.check(true, "out-of-range upload rejected");
        }
    }

    /// Textured quad through a bound texture and sampler: a 2x2 pattern
    /// texture (red/green/blue/white) sampled nearest/clamp must read back
    /// per quadrant within dither tolerance, proving the binding path.
    private static void samplerProof(MTLDevice device, MTLCommandQueue queue) {
        MTLSamplerDescriptor sdesc = MTLSamplerDescriptor.create();
        sdesc.setMinFilter(MTLSamplerDescriptor.MIN_MAG_FILTER_NEAREST);
        sdesc.setMagFilter(MTLSamplerDescriptor.MIN_MAG_FILTER_NEAREST);
        sdesc.setNormalizedCoordinates(true);
        TestKit.check(sdesc.minFilter() == MTLSamplerDescriptor.MIN_MAG_FILTER_NEAREST
                && sdesc.normalizedCoordinates(), "sampler descriptor round-trips");
        MTLSamplerState sampler = device.newSamplerState(sdesc);
        TestKit.check(sampler != null && sampler.device() != null, "sampler state compiles");
        MTLTextureDescriptor tdesc = MTLTextureDescriptor.texture2D(70, 2, 2, false);
        tdesc.setUsage(MTLTextureDescriptor.USAGE_SHADER_READ);
        tdesc.setStorageMode(MTLTextureDescriptor.STORAGE_SHARED);
        MTLTexture pattern = device.newTexture(tdesc);
        byte[] quad = {
            (byte) 255, 0, 0, (byte) 255, 0, (byte) 255, 0, (byte) 255,
            0, 0, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
        };
        pattern.replaceRegion(MTLRegion.of2D(0, 0, 2, 2), 0, quad, 8);
        MTLLibrary lib = device.newLibraryWithSource(TEXTURE_MSL);
        MTLRenderPipelineDescriptor desc = MTLRenderPipelineDescriptor.create();
        desc.setVertexFunction(lib.newFunctionWithName("vtxQ"));
        desc.setFragmentFunction(lib.newFunctionWithName("fragTex"));
        desc.colorAttachment(0).setPixelFormat(
                MTLRenderPipelineColorAttachmentDescriptor.PIXEL_FORMAT_BGRA8_UNORM);
        MTLRenderPipelineState pipe = device.newRenderPipelineState(desc);
        MTLTexture tex = target(device);
        float[] q = {-1f, -1f, 0f, 1f, 3f, -1f, 0f, 1f, -1f, 3f, 0f, 1f};
        ByteBuffer bb = ByteBuffer.allocate(q.length * 4).order(ByteOrder.nativeOrder());
        for (float f : q) bb.putFloat(f);
        MemorySegment pool = Autorelease.push();
        try {
            MTLCommandBuffer buf = queue.commandBuffer();
            MTLRenderCommandEncoder enc = buf.renderEncoder(
                    passFor(tex, new MTLClearColor(0, 0, 0, 1)));
            enc.setRenderPipelineState(pipe);
            enc.setVertexBytes(bb.array(), 0);
            enc.setFragmentTexture(pattern, 0);
            enc.setFragmentSamplerState(sampler, 0);
            enc.drawTriangles(0, 3);
            enc.endEncoding();
            buf.commit();
            buf.waitUntilCompleted();
        } finally {
            Autorelease.pop(pool);
        }
        byte[] px = readback(tex);
        // uv = pos*0.5+0.5 puts texel row 0 at the framebuffer bottom half.
        int[][] want = {{255, 0, 0}, {0, 255, 0}, {0, 0, 255}, {255, 255, 255}};
        int[][] at = {{W / 4, H / 4 * 3}, {W / 4 * 3, H / 4 * 3}, {W / 4, H / 4}, {W / 4 * 3, H / 4}};
        String[] names = {"bottom-left red", "bottom-right green", "top-left blue", "top-right white"};
        for (int k = 0; k < 4; k++) {
            int i = (at[k][1] * W + at[k][0]) * 4;
            int b = px[i] & 0xFF, g = px[i + 1] & 0xFF, r = px[i + 2] & 0xFF;
            TestKit.check(Math.abs(r - want[k][0]) <= 6 && Math.abs(g - want[k][1]) <= 6
                    && Math.abs(b - want[k][2]) <= 6,
                    "sampled quadrant " + names[k] + " (got R" + r + " G" + g + " B" + b + ")");
        }
    }

    /// Blending + write masks: half-alpha red over blue must average the
    /// channels; mask NONE must preserve the clear; mask RED moves R only.
    private static void blendProof(MTLDevice device, MTLCommandQueue queue) {
        MTLLibrary lib = device.newLibraryWithSource(BLEND_MSL);
        MTLFunction vert = lib.newFunctionWithName("vtxB");
        MTLFunction fragA = lib.newFunctionWithName("fragA");
        TestKit.check(vert != null && fragA != null, "half-alpha shader compiles");
        MTLRenderPipelineDescriptor desc = MTLRenderPipelineDescriptor.create();
        desc.setVertexFunction(vert);
        desc.setFragmentFunction(fragA);
        desc.colorAttachment(0).setPixelFormat(
                MTLRenderPipelineColorAttachmentDescriptor.PIXEL_FORMAT_BGRA8_UNORM);
        MTLRenderPipelineColorAttachmentDescriptor att = desc.colorAttachment(0);
        att.setBlendingEnabled(true);
        att.setSourceRGBBlendFactor(
                MTLRenderPipelineColorAttachmentDescriptor.BLEND_FACTOR_SOURCE_ALPHA);
        att.setDestinationRGBBlendFactor(
                MTLRenderPipelineColorAttachmentDescriptor.BLEND_FACTOR_ONE_MINUS_SOURCE_ALPHA);
        TestKit.check(att.isBlendingEnabled()
                && att.sourceRGBBlendFactor()
                        == MTLRenderPipelineColorAttachmentDescriptor.BLEND_FACTOR_SOURCE_ALPHA,
                "blend state round-trips on the descriptor");
        MTLRenderPipelineState blendPipe = device.newRenderPipelineState(desc);
        MTLTexture tex = target(device);
        MemorySegment pool = Autorelease.push();
        try {
            MTLCommandBuffer buf = queue.commandBuffer();
            MTLRenderCommandEncoder enc = buf.renderEncoder(
                    passFor(tex, new MTLClearColor(0, 0, 1, 1)));
            enc.setRenderPipelineState(blendPipe);
            enc.setVertexBytes(triangleBytes(), 0);
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
        TestKit.check(Math.abs(r - 128) <= 8 && g <= 8 && Math.abs(b - 128) <= 8,
                "half-alpha red over blue averages to purple (got R" + r + " G" + g + " B" + b + ")");
        att.setBlendingEnabled(false);
        att.setWriteMask(MTLRenderPipelineColorAttachmentDescriptor.WRITE_MASK_NONE);
        MTLRenderPipelineState maskPipe = device.newRenderPipelineState(desc);
        MTLTexture tex2 = target(device);
        MemorySegment pool2 = Autorelease.push();
        try {
            MTLCommandBuffer buf = queue.commandBuffer();
            MTLRenderCommandEncoder enc = buf.renderEncoder(
                    passFor(tex2, new MTLClearColor(0, 0, 0, 1)));
            enc.setRenderPipelineState(maskPipe);
            enc.setVertexBytes(triangleBytes(), 0);
            enc.drawTriangles(0, 3);
            enc.endEncoding();
            buf.commit();
            buf.waitUntilCompleted();
        } finally {
            Autorelease.pop(pool2);
        }
        byte[] px2 = readback(tex2);
        int cx2 = (H / 2) * W * 4 + (W / 2) * 4;
        TestKit.check(px2[cx2] == 0 && px2[cx2 + 1] == 0 && px2[cx2 + 2] == 0,
                "write mask NONE preserves the clear");
        att.setWriteMask(MTLRenderPipelineColorAttachmentDescriptor.WRITE_MASK_RED);
        MTLRenderPipelineState redPipe = device.newRenderPipelineState(desc);
        MTLTexture tex3 = target(device);
        MemorySegment pool3 = Autorelease.push();
        try {
            MTLCommandBuffer buf = queue.commandBuffer();
            MTLRenderCommandEncoder enc = buf.renderEncoder(
                    passFor(tex3, new MTLClearColor(0, 0, 0, 1)));
            enc.setRenderPipelineState(redPipe);
            enc.setVertexBytes(triangleBytes(), 0);
            enc.drawTriangles(0, 3);
            enc.endEncoding();
            buf.commit();
            buf.waitUntilCompleted();
        } finally {
            Autorelease.pop(pool3);
        }
        byte[] px3 = readback(tex3);
        int cx3 = (H / 2) * W * 4 + (W / 2) * 4;
        TestKit.check((px3[cx3 + 2] & 0xFF) == 255 && px3[cx3 + 1] == 0 && px3[cx3] == 0,
                "write mask RED moves only R");
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
        NSSize dsize = layer.drawableSize();
        TestKit.check(Math.abs(dsize.width() - 200) < 0.5 && Math.abs(dsize.height() - 200) < 0.5,
                "CAMetalLayer drawableSize by-value round-trip (got " + dsize + ")");
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
        if (drawable == null) {
            TestKit.skipCase("layer drawable unavailable (no window server)");
            TestKit.close(win);
            return;
        }
        MTLTexture dtex = drawable.texture();
        if (dtex == null) {
            TestKit.skipCase("drawable texture unavailable (no window server)");
            TestKit.close(win);
            return;
        }
        // Clear through the drawable's own texture and read the pixels back,
        // mirroring the offscreen clearProof pattern (generous +-6 tolerance).
        try {
            MTLCommandQueue queue = device.newCommandQueue();
            int dw = (int) dtex.width(), dh = (int) dtex.height();
            TestKit.check(dw > 0 && dh > 0, "drawable texture has real dimensions (" + dw + "x" + dh + ")");
            MTLRenderPassDescriptor pass = MTLRenderPassDescriptor.create();
            MTLRenderPassColorAttachmentDescriptor att = pass.colorAttachment(0);
            att.setTexture(dtex);
            att.setLoadAction(MTLRenderPassColorAttachmentDescriptor.LOAD_CLEAR);
            att.setStoreAction(MTLRenderPassColorAttachmentDescriptor.STORE_STORE);
            att.setClearColor(new MTLClearColor(0, 1, 0, 1));
            MemorySegment pool = Autorelease.push();
            try {
                MTLCommandBuffer buf = queue.commandBuffer();
                MTLRenderCommandEncoder enc = buf.renderEncoder(pass);
                enc.endEncoding();
                buf.presentDrawable(drawable);
                buf.commit();
                buf.waitUntilCompleted();
            } finally {
                Autorelease.pop(pool);
            }
            byte[] px = dtex.getBytes(dw * 4, MTLRegion.of2D(0, 0, dw, dh), 0);
            int cx = (dh / 2) * dw * 4 + (dw / 2) * 4;
            int b = px[cx] & 0xFF, g = px[cx + 1] & 0xFF, r = px[cx + 2] & 0xFF, a = px[cx + 3] & 0xFF;
            TestKit.check(r <= 6 && g >= 249 && b <= 6 && a >= 249,
                    "drawable clear green reads back through its texture (BGRA " + b + "," + g + "," + r + "," + a + ")");
        } catch (Throwable t) {
            TestKit.check(false, "drawable clear+readback threw: " + t);
        }
        TestKit.close(win);
    }
}
