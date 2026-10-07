package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

import nsui.MTLBuffer;
import nsui.MTLCommandBuffer;
import nsui.MTLCommandQueue;
import nsui.MTLClearColor;
import nsui.MTLDepthStencilDescriptor;
import nsui.MTLDepthStencilState;
import nsui.MTLDevice;
import nsui.MTLLibrary;
import nsui.MTLRegion;
import nsui.MTLRenderCommandEncoder;
import nsui.MTLRenderPassDepthAttachmentDescriptor;
import nsui.MTLRenderPassDescriptor;
import nsui.MTLRenderPassStencilAttachmentDescriptor;
import nsui.MTLRenderPipelineDescriptor;
import nsui.MTLRenderPipelineState;
import nsui.MTLScissorRect;
import nsui.MTLStencilDescriptor;
import nsui.MTLTexture;
import nsui.MTLTextureDescriptor;
import nsui.MTLViewport;
import nsui.objc.Autorelease;
import nsui.objc.ObjC;

/// MetalDepthStencilTest — depth/stencil completeness proved by CPU pixel
/// readback, plus the rasterizer-state additions (viewport, scissor), indexed
/// drawing, and the Metal error paths hardened alongside them.
///
/// Every render proof reads the colour attachment back to the CPU and asserts
/// the pixel within a small tolerance: a green centre can only come from the
/// depth test rejecting the later far triangle, and a half-green/half-red
/// image can only come from the stencil test consulting the reference value.
/// No section passes merely because a call did not crash.
public final class MetalDepthStencilTest {

    private static final int W = 64, H = 64;

    /// MI: BGRA8Unorm colour, Depth32Float depth, Depth32Float_Stencil8 combined.
    private static final long COLOR_FORMAT = 80;
    private static final long DEPTH_FORMAT = MTLTextureDescriptor.PIXEL_FORMAT_DEPTH32_FLOAT;
    private static final long DEPTH_STENCIL_FORMAT = MTLTextureDescriptor.PIXEL_FORMAT_DEPTH32_FLOAT_STENCIL8;

    /// MTLLoadAction / MTLStoreAction (MTLRenderPass.h).
    private static final long LOAD_LOAD = 1, LOAD_CLEAR = 2, STORE_STORE = 1;

    /// Vertex reads positions at buffer(0) and a per-draw colour at buffer(1).
    private static final String MSL =
            "#include <metal_stdlib>\n" +
            "using namespace metal;\n" +
            "struct VSOut { float4 pos [[position]]; float4 col; };\n" +
            "vertex VSOut vtx(uint vid [[vertex_id]], constant float4 *pos [[buffer(0)]], constant float4 &col [[buffer(1)]]) {\n" +
            "    VSOut o; o.pos = pos[vid]; o.col = col; return o;\n" +
            "}\n" +
            "fragment float4 frag(VSOut in [[stage_in]]) { return in.col; }\n";

    /// A centred triangle and a full-screen quad / left-half quad.
    private static final float[][] TRI = {{-0.8f, -0.8f}, {0.8f, -0.8f}, {0f, 0.8f}};
    private static final float[][] QUAD = {{-1, -1}, {1, -1}, {-1, 1}, {1, -1}, {1, 1}, {-1, 1}};
    private static final float[][] LEFT_HALF = {{-1, -1}, {0, -1}, {-1, 1}, {0, -1}, {0, 1}, {-1, 1}};

    private MetalDepthStencilTest() {}

    public static void main(String[] args) {
        System.out.println("=== MetalDepthStencilTest — depth/stencil by CPU readback ===");
        ObjC.init();
        MTLDevice device = MTLDevice.systemDefault();
        if (device == null) {
            TestKit.skip("no Metal GPU on this machine");
            return;
        }
        TestKit.probe("system default GPU resolves");

        MTLCommandQueue queue;
        MTLLibrary library;
        try {
            queue = device.newCommandQueue();
            library = device.newLibraryWithSource(MSL);
            TestKit.check(queue != null && library != null, "queue + depth/stencil shader compile");
        } catch (Throwable t) {
            TestKit.check(false, "setup threw: " + t);
            TestKit.end();
            return;
        }

        run("descriptor round-trips", () -> descriptorRoundTrips(device));
        run("render-pass attachment descriptors", () -> attachmentRoundTrips(device));
        run("depth test", () -> depthProof(device, queue, library));
        run("stencil test", () -> stencilProof(device, queue, library));
        run("viewport + scissor", () -> rasterizerProof(device, queue, library));
        run("indexed draw + vertex buffer", () -> indexedProof(device, queue, library));
        run("error paths", () -> errorPaths(device, queue, library));

        TestKit.end();
    }

    private interface Section { void run() throws Throwable; }

    private static void run(String name, Section body) {
        try {
            body.run();
        } catch (Throwable t) {
            TestKit.check(false, name + " section threw: " + t);
        }
    }

    // ------------------------------------------------------------------ setup

    private static MTLTexture target(MTLDevice device, long format, long storage, long usage) {
        MTLTextureDescriptor d = MTLTextureDescriptor.texture2D(format, W, H, false);
        d.setUsage(usage);
        d.setStorageMode(storage);
        MTLTexture t = device.newTexture(d);
        if (t == null) throw new IllegalStateException("texture is nil (format " + format + ")");
        return t;
    }

    private static MTLRenderPipelineState pipeline(MTLDevice device, MTLLibrary library,
            long depthFormat, long stencilFormat) {
        MTLRenderPipelineDescriptor d = MTLRenderPipelineDescriptor.create();
        d.setVertexFunction(library.newFunctionWithName("vtx"));
        d.setFragmentFunction(library.newFunctionWithName("frag"));
        d.colorAttachment(0).setPixelFormat(COLOR_FORMAT);
        if (depthFormat != 0) d.setDepthAttachmentPixelFormat(depthFormat);
        if (stencilFormat != 0) d.setStencilAttachmentPixelFormat(stencilFormat);
        return device.newRenderPipelineState(d);
    }

    private static void render(MTLCommandQueue queue, MTLRenderPassDescriptor pass,
            Consumer<MTLRenderCommandEncoder> body) {
        MemorySegment pool = Autorelease.push();
        try {
            MTLCommandBuffer cb = queue.commandBuffer();
            MTLRenderCommandEncoder enc = cb.renderEncoder(pass);
            body.accept(enc);
            enc.endEncoding();
            cb.commit();
            cb.waitUntilCompleted();
        } finally {
            Autorelease.pop(pool);
        }
    }

    private static byte[] vertices(float[][] xy, float z) {
        ByteBuffer bb = ByteBuffer.allocate(xy.length * 16).order(ByteOrder.nativeOrder());
        for (float[] p : xy) bb.putFloat(p[0]).putFloat(p[1]).putFloat(z).putFloat(1f);
        return bb.array();
    }

    private static byte[] color(float r, float g, float b) {
        ByteBuffer bb = ByteBuffer.allocate(16).order(ByteOrder.nativeOrder());
        bb.putFloat(r).putFloat(g).putFloat(b).putFloat(1f);
        return bb.array();
    }

    private static byte[] readback(MTLTexture tex) {
        return tex.getBytes(W * 4, MTLRegion.of2D(0, 0, W, H), 0);
    }

    /// Pixel as 0xRRGGBB from BGRA8 bytes.
    private static int rgb(byte[] img, int x, int y) {
        int i = (y * W + x) * 4;
        return ((img[i + 2] & 0xFF) << 16) | ((img[i + 1] & 0xFF) << 8) | (img[i] & 0xFF);
    }

    private static boolean near(int v, int target, int tol) { return Math.abs(v - target) <= tol; }

    /// Tolerance oracles (+-6 per channel): exact 0x00FF00-style equality
    /// flakes under dither, so every color verdict allows small drift.
    private static boolean isGreen(int c) {
        return near((c >> 16) & 0xFF, 0, 6) && near((c >> 8) & 0xFF, 255, 6) && near(c & 0xFF, 0, 6);
    }
    private static boolean isRed(int c) {
        return near((c >> 16) & 0xFF, 255, 6) && near((c >> 8) & 0xFF, 0, 6) && near(c & 0xFF, 0, 6);
    }
    private static boolean isBlack(int c) {
        return ((c >> 16) & 0xFF) <= 6 && ((c >> 8) & 0xFF) <= 6 && (c & 0xFF) <= 6;
    }
    private static boolean isOrange(int c) {
        return near((c >> 16) & 0xFF, 255, 6) && near((c >> 8) & 0xFF, 128, 6) && near(c & 0xFF, 0, 6);
    }

    // ------------------------------------------------------- descriptor proofs

    private static void descriptorRoundTrips(MTLDevice device) {
        MTLStencilDescriptor sd = MTLStencilDescriptor.create();
        sd.setStencilCompareFunction(MTLStencilDescriptor.COMPARE_EQUAL);
        sd.setStencilFailureOperation(MTLStencilDescriptor.OPERATION_ZERO);
        sd.setDepthFailureOperation(MTLStencilDescriptor.OPERATION_INCREMENT_CLAMP);
        sd.setDepthStencilPassOperation(MTLStencilDescriptor.OPERATION_REPLACE);
        sd.setReadMask(0x00FF00FFL);
        sd.setWriteMask(0x0F0F0F0FL);
        TestKit.check(sd.stencilCompareFunction() == MTLStencilDescriptor.COMPARE_EQUAL
                && sd.stencilFailureOperation() == MTLStencilDescriptor.OPERATION_ZERO
                && sd.depthFailureOperation() == MTLStencilDescriptor.OPERATION_INCREMENT_CLAMP
                && sd.depthStencilPassOperation() == MTLStencilDescriptor.OPERATION_REPLACE,
                "stencil descriptor compare + three operations round-trip");
        TestKit.check(sd.readMask() == 0x00FF00FFL && sd.writeMask() == 0x0F0F0F0FL,
                "stencil descriptor 32-bit read/write masks round-trip");

        MTLDepthStencilDescriptor dd = MTLDepthStencilDescriptor.create();
        dd.setDepthCompareFunction(MTLDepthStencilDescriptor.COMPARE_LESS);
        dd.setDepthWriteEnabled(true);
        dd.setFrontFaceStencil(sd);
        dd.setBackFaceStencil(sd);
        dd.setLabel("nsui-depth");
        TestKit.check(dd.depthCompareFunction() == MTLDepthStencilDescriptor.COMPARE_LESS
                && dd.isDepthWriteEnabled(),
                "depth/stencil descriptor compare + isDepthWriteEnabled round-trip");
        TestKit.check("nsui-depth".equals(dd.label()), "depth/stencil descriptor label round-trip");
        MTLStencilDescriptor front = dd.frontFaceStencil();
        TestKit.check(front != null
                && front.stencilCompareFunction() == MTLStencilDescriptor.COMPARE_EQUAL
                && front.depthStencilPassOperation() == MTLStencilDescriptor.OPERATION_REPLACE
                && front.writeMask() == 0x0F0F0F0FL,
                "frontFaceStencil is copied back with its values (front==back set)");

        MTLDepthStencilState state = device.newDepthStencilState(dd);
        TestKit.check(state != null && "nsui-depth".equals(state.label()),
                "newDepthStencilStateWithDescriptor: compiles with the label");
        TestKit.check(state.device() != null, "depth/stencil state reports its device");

        TestKit.expectThrows("newDepthStencilState(null) is rejected", IllegalArgumentException.class,
                () -> device.newDepthStencilState(null));
        TestKit.expectThrows("newTexture(null) is rejected", IllegalArgumentException.class,
                () -> device.newTexture(null));

        // Pixel format beacons: values verified against MTLPixelFormat.h.
        TestKit.check(MTLTextureDescriptor.PIXEL_FORMAT_DEPTH16_UNORM == 250
                && MTLTextureDescriptor.PIXEL_FORMAT_DEPTH32_FLOAT == 252
                && MTLTextureDescriptor.PIXEL_FORMAT_STENCIL8 == 253
                && MTLTextureDescriptor.PIXEL_FORMAT_DEPTH24_UNORM_STENCIL8 == 255
                && MTLTextureDescriptor.PIXEL_FORMAT_DEPTH32_FLOAT_STENCIL8 == 260
                && MTLTextureDescriptor.PIXEL_FORMAT_X32_STENCIL8 == 261
                && MTLTextureDescriptor.PIXEL_FORMAT_X24_STENCIL8 == 262,
                "depth/stencil MTLPixelFormat constants match the SDK header");
    }

    private static void attachmentRoundTrips(MTLDevice device) {
        MTLTexture color = target(device, COLOR_FORMAT, MTLTextureDescriptor.STORAGE_SHARED,
                MTLTextureDescriptor.USAGE_RENDER_TARGET | MTLTextureDescriptor.USAGE_SHADER_READ);
        MTLTexture resolve = target(device, COLOR_FORMAT, MTLTextureDescriptor.STORAGE_SHARED,
                MTLTextureDescriptor.USAGE_RENDER_TARGET);

        MTLRenderPassDescriptor pass = MTLRenderPassDescriptor.create();
        MTLRenderPassDepthAttachmentDescriptor depth = pass.depthAttachment();
        TestKit.check(depth != null, "render pass exposes a non-nil depthAttachment");
        depth.setTexture(color);
        depth.setLoadAction(MTLRenderPassDepthAttachmentDescriptor.LOAD_CLEAR);
        depth.setStoreAction(MTLRenderPassDepthAttachmentDescriptor.STORE_STORE);
        depth.setClearDepth(0.25);
        depth.setLevel(2);
        depth.setSlice(3);
        depth.setResolveTexture(resolve);
        TestKit.check(depth.texture() != null
                && depth.loadAction() == MTLRenderPassDepthAttachmentDescriptor.LOAD_CLEAR
                && depth.storeAction() == MTLRenderPassDepthAttachmentDescriptor.STORE_STORE
                && Math.abs(depth.clearDepth() - 0.25) < 1e-9
                && depth.level() == 2 && depth.slice() == 3
                && depth.resolveTexture() != null,
                "depth attachment texture/load/store/clearDepth/level/slice/resolveTexture round-trip");

        MTLRenderPassStencilAttachmentDescriptor stencil = pass.stencilAttachment();
        TestKit.check(stencil != null, "render pass exposes a non-nil stencilAttachment");
        stencil.setTexture(color);
        stencil.setLoadAction(MTLRenderPassStencilAttachmentDescriptor.LOAD_LOAD);
        stencil.setStoreAction(MTLRenderPassStencilAttachmentDescriptor.STORE_STORE);
        stencil.setClearStencil(7);
        TestKit.check(stencil.texture() != null
                && stencil.loadAction() == MTLRenderPassStencilAttachmentDescriptor.LOAD_LOAD
                && stencil.storeAction() == MTLRenderPassStencilAttachmentDescriptor.STORE_STORE
                && stencil.clearStencil() == 7,
                "stencil attachment texture/load/store/clearStencil round-trip");

        MTLRenderPipelineDescriptor pd = MTLRenderPipelineDescriptor.create();
        pd.setDepthAttachmentPixelFormat(DEPTH_FORMAT);
        pd.setStencilAttachmentPixelFormat(DEPTH_STENCIL_FORMAT);
        TestKit.check(pd.depthAttachmentPixelFormat() == DEPTH_FORMAT
                && pd.stencilAttachmentPixelFormat() == DEPTH_STENCIL_FORMAT,
                "pipeline depth/stencil attachment pixel formats round-trip");

        MTLTextureDescriptor td = MTLTextureDescriptor.texture2D(DEPTH_FORMAT, W, H, false);
        TestKit.check(td.pixelFormat() == DEPTH_FORMAT, "texture descriptor pixelFormat getter");
        td.setPixelFormat(DEPTH_STENCIL_FORMAT);
        TestKit.check(td.pixelFormat() == DEPTH_STENCIL_FORMAT, "texture descriptor setPixelFormat: round-trip");
        MTLTexture dsTex = target(device, DEPTH_STENCIL_FORMAT, MTLTextureDescriptor.STORAGE_PRIVATE,
                MTLTextureDescriptor.USAGE_RENDER_TARGET);
        TestKit.check(dsTex.pixelFormat() == DEPTH_STENCIL_FORMAT, "texture reports its depth/stencil format");
    }

    // ------------------------------------------------------------- depth proof

    private static MTLDepthStencilState depthState(MTLDevice device, long compare, boolean write) {
        MTLDepthStencilDescriptor d = MTLDepthStencilDescriptor.create();
        d.setDepthCompareFunction(compare);
        d.setDepthWriteEnabled(write);
        return device.newDepthStencilState(d);
    }

    /// Depth test proved by readback. A far triangle (z=0.75) is drawn, then a
    /// near one (z=0.25) overlapping it. Because both are drawn with Less +
    /// depth-write, the near colour must win. The discriminating half is the
    /// REVERSED order: near first, then far. With a working depth test the far
    /// triangle is rejected by the stored 0.25 and the centre stays green; with
    /// no depth test the later far draw would paint red. Draw order alone cannot
    /// produce that result, so the reversed pass is the proof.
    private static void depthProof(MTLDevice device, MTLCommandQueue queue, MTLLibrary library) {
        MTLRenderPipelineState pipe = pipeline(device, library, DEPTH_FORMAT, 0);
        MTLDepthStencilState less = depthState(device, MTLDepthStencilDescriptor.COMPARE_LESS, true);

        MTLTexture color = target(device, COLOR_FORMAT, MTLTextureDescriptor.STORAGE_SHARED,
                MTLTextureDescriptor.USAGE_RENDER_TARGET | MTLTextureDescriptor.USAGE_SHADER_READ);
        MTLTexture depth = target(device, DEPTH_FORMAT, MTLTextureDescriptor.STORAGE_PRIVATE,
                MTLTextureDescriptor.USAGE_RENDER_TARGET);

        MTLRenderPassDescriptor pass = MTLRenderPassDescriptor.create();
        pass.colorAttachment(0).setTexture(color);
        pass.colorAttachment(0).setLoadAction(LOAD_CLEAR);
        pass.colorAttachment(0).setStoreAction(STORE_STORE);
        pass.colorAttachment(0).setClearColor(new MTLClearColor(0, 0, 0, 1));
        pass.depthAttachment().setTexture(depth);
        pass.depthAttachment().setLoadAction(LOAD_CLEAR);
        pass.depthAttachment().setStoreAction(STORE_STORE);
        pass.depthAttachment().setClearDepth(1.0);
        TestKit.check(Math.abs(pass.depthAttachment().clearDepth() - 1.0) < 1e-9,
                "depth attachment clearDepth persists as 1.0");

        // Requested order: FAR (red) then NEAR (green).
        render(queue, pass, enc -> {
            enc.setRenderPipelineState(pipe);
            enc.setDepthStencilState(less);
            enc.setCullMode(MTLRenderCommandEncoder.CULL_NONE);
            enc.setFrontFacingWinding(MTLRenderCommandEncoder.WINDING_COUNTER_CLOCKWISE);
            enc.setDepthClipMode(MTLRenderCommandEncoder.DEPTH_CLIP_CLIP);
            enc.setDepthBias(0f, 0f, 0f);
            enc.setViewport(MTLViewport.of(W, H));
            enc.setScissorRect(new MTLScissorRect(0, 0, W, H));
            enc.setVertexBytes(vertices(TRI, 0.75f), 0);
            enc.setVertexBytes(color(1, 0, 0), 1);
            enc.drawTriangles(0, 3);
            enc.setVertexBytes(vertices(TRI, 0.25f), 0);
            enc.setVertexBytes(color(0, 1, 0), 1);
            enc.drawTriangles(0, 3);
        });
        byte[] farThenNear = readback(color);
        TestKit.check(isGreen(rgb(farThenNear, W / 2, H / 2)),
                "far-then-near: depth test keeps the near green (got 0x"
                        + Integer.toHexString(rgb(farThenNear, W / 2, H / 2)) + ")");
        TestKit.check(isBlack(rgb(farThenNear, 2, 2)), "corner outside the triangle stays cleared");

        // Discriminator: NEAR (green) first, then FAR (red) must be depth-rejected.
        render(queue, pass, enc -> {
            enc.setRenderPipelineState(pipe);
            enc.setDepthStencilState(less);
            enc.setVertexBytes(vertices(TRI, 0.25f), 0);
            enc.setVertexBytes(color(0, 1, 0), 1);
            enc.drawTriangles(0, 3);
            enc.setVertexBytes(vertices(TRI, 0.75f), 0);
            enc.setVertexBytes(color(1, 0, 0), 1);
            enc.drawTriangles(0, 3);
        });
        byte[] nearThenFar = readback(color);
        TestKit.check(isGreen(rgb(nearThenFar, W / 2, H / 2)),
                "near-then-far: the farther triangle is depth-rejected, not drawn (got 0x"
                        + Integer.toHexString(rgb(nearThenFar, W / 2, H / 2)) + ")");
    }

    // ----------------------------------------------------------- stencil proof

    private static MTLStencilDescriptor face(long compare, long passOp, long readMask, long writeMask) {
        MTLStencilDescriptor sd = MTLStencilDescriptor.create();
        sd.setStencilCompareFunction(compare);
        sd.setStencilFailureOperation(MTLStencilDescriptor.OPERATION_KEEP);
        sd.setDepthFailureOperation(MTLStencilDescriptor.OPERATION_KEEP);
        sd.setDepthStencilPassOperation(passOp);
        sd.setReadMask(readMask);
        sd.setWriteMask(writeMask);
        return sd;
    }

    private static MTLDepthStencilState stencilState(MTLDevice device, MTLStencilDescriptor face) {
        MTLDepthStencilDescriptor d = MTLDepthStencilDescriptor.create();
        d.setDepthCompareFunction(MTLDepthStencilDescriptor.COMPARE_ALWAYS);
        d.setDepthWriteEnabled(false);
        d.setFrontFaceStencil(face);
        d.setBackFaceStencil(face);
        return device.newDepthStencilState(d);
    }

    /// Stencil replace-on-pass marks the left half, then an Equal test with a
    /// different colour draws a full-screen quad: only the marked half may take
    /// the second colour, and bumping the reference value to a non-matching 2
    /// must leave the rest untouched.
    private static void stencilProof(MTLDevice device, MTLCommandQueue queue, MTLLibrary library) {
        MTLRenderPipelineState pipe = pipeline(device, library, DEPTH_STENCIL_FORMAT, DEPTH_STENCIL_FORMAT);
        TestKit.check(pipe != null, "depth+stencil pipeline compiles (Depth32Float_Stencil8)");
        MTLDepthStencilState mark = stencilState(device,
                face(MTLStencilDescriptor.COMPARE_ALWAYS, MTLStencilDescriptor.OPERATION_REPLACE, 0xFF, 0xFF));
        MTLDepthStencilState equal = stencilState(device,
                face(MTLStencilDescriptor.COMPARE_EQUAL, MTLStencilDescriptor.OPERATION_KEEP, 0xFF, 0x00));

        MTLTexture color = target(device, COLOR_FORMAT, MTLTextureDescriptor.STORAGE_SHARED,
                MTLTextureDescriptor.USAGE_RENDER_TARGET | MTLTextureDescriptor.USAGE_SHADER_READ);
        MTLTexture ds = target(device, DEPTH_STENCIL_FORMAT, MTLTextureDescriptor.STORAGE_PRIVATE,
                MTLTextureDescriptor.USAGE_RENDER_TARGET);

        // Pass 1: clear red, stencil 0, depth 1.0; mark the left half stencil=1.
        MTLRenderPassDescriptor pass1 = MTLRenderPassDescriptor.create();
        pass1.colorAttachment(0).setTexture(color);
        pass1.colorAttachment(0).setLoadAction(LOAD_CLEAR);
        pass1.colorAttachment(0).setStoreAction(STORE_STORE);
        pass1.colorAttachment(0).setClearColor(new MTLClearColor(1, 0, 0, 1));
        pass1.depthAttachment().setTexture(ds);
        pass1.depthAttachment().setLoadAction(LOAD_CLEAR);
        pass1.depthAttachment().setStoreAction(STORE_STORE);
        pass1.depthAttachment().setClearDepth(1.0);
        pass1.stencilAttachment().setTexture(ds);
        pass1.stencilAttachment().setLoadAction(LOAD_CLEAR);
        pass1.stencilAttachment().setStoreAction(STORE_STORE);
        pass1.stencilAttachment().setClearStencil(0);
        TestKit.check(pass1.stencilAttachment().clearStencil() == 0,
                "stencil attachment clearStencil persists as 0");

        render(queue, pass1, enc -> {
            enc.setRenderPipelineState(pipe);
            enc.setDepthStencilState(mark);
            enc.setStencilReferenceValue(1);
            enc.setVertexBytes(vertices(LEFT_HALF, 0.5f), 0);
            enc.setVertexBytes(color(1, 0, 0), 1);
            enc.drawTriangles(0, 6);
        });

        // Pass 2: load colour + stencil; Equal ref=1 paints the marked half
        // green, then ref=2 must fail everywhere and change nothing.
        MTLRenderPassDescriptor pass2 = MTLRenderPassDescriptor.create();
        pass2.colorAttachment(0).setTexture(color);
        pass2.colorAttachment(0).setLoadAction(LOAD_LOAD);
        pass2.colorAttachment(0).setStoreAction(STORE_STORE);
        pass2.depthAttachment().setTexture(ds);
        pass2.depthAttachment().setLoadAction(LOAD_LOAD);
        pass2.depthAttachment().setStoreAction(STORE_STORE);
        pass2.stencilAttachment().setTexture(ds);
        pass2.stencilAttachment().setLoadAction(LOAD_LOAD);
        pass2.stencilAttachment().setStoreAction(STORE_STORE);

        render(queue, pass2, enc -> {
            enc.setRenderPipelineState(pipe);
            enc.setDepthStencilState(equal);
            enc.setStencilFrontReferenceValue(1, 1);
            enc.setVertexBytes(vertices(QUAD, 0.5f), 0);
            enc.setVertexBytes(color(0, 1, 0), 1);
            enc.drawTriangles(0, 6);
            enc.setStencilFrontReferenceValue(2, 2);
            enc.setVertexBytes(color(0, 0, 1), 1);
            enc.drawTriangles(0, 6);
        });

        byte[] img = readback(color);
        int left = rgb(img, W / 4, H / 2), right = rgb(img, 3 * W / 4, H / 2);
        TestKit.check(isGreen(left),
                "stencil Equal ref=1 paints the marked half green (got 0x" + Integer.toHexString(left) + ")");
        TestKit.check(isRed(right),
                "unmarked half keeps the first colour and ref=2 never matches (got 0x"
                        + Integer.toHexString(right) + ")");
    }

    // ------------------------------------------------------ rasterizer proof

    /// A half-width viewport squeezes a full-screen quad into the right half:
    /// the left half must stay cleared. That is only true if the 48-byte
    /// MTLViewport travelled by value with the right field order. The scissor
    /// proof does the mirror image with the 32-byte MTLScissorRect.
    private static void rasterizerProof(MTLDevice device, MTLCommandQueue queue, MTLLibrary library) {
        MTLRenderPipelineState pipe = pipeline(device, library, 0, 0);
        MTLTexture color = target(device, COLOR_FORMAT, MTLTextureDescriptor.STORAGE_SHARED,
                MTLTextureDescriptor.USAGE_RENDER_TARGET | MTLTextureDescriptor.USAGE_SHADER_READ);
        MTLRenderPassDescriptor pass = MTLRenderPassDescriptor.create();
        pass.colorAttachment(0).setTexture(color);
        pass.colorAttachment(0).setLoadAction(LOAD_CLEAR);
        pass.colorAttachment(0).setStoreAction(STORE_STORE);
        pass.colorAttachment(0).setClearColor(new MTLClearColor(0, 0, 0, 1));

        render(queue, pass, enc -> {
            enc.setRenderPipelineState(pipe);
            enc.setViewport(new MTLViewport(W / 2.0, 0, W / 2.0, H, 0, 1));
            enc.setScissorRect(new MTLScissorRect(0, 0, W, H));
            enc.setVertexBytes(vertices(QUAD, 0.5f), 0);
            enc.setVertexBytes(color(1, 0, 0), 1);
            enc.drawTriangles(0, 6);
        });
        byte[] img = readback(color);
        TestKit.check(isBlack(rgb(img, W / 4, H / 2)) && isRed(rgb(img, 3 * W / 4, H / 2)),
                "viewport maps the quad into the right half only (left 0x"
                        + Integer.toHexString(rgb(img, W / 4, H / 2)) + ", right 0x"
                        + Integer.toHexString(rgb(img, 3 * W / 4, H / 2)) + ")");

        render(queue, pass, enc -> {
            enc.setRenderPipelineState(pipe);
            enc.setViewport(MTLViewport.of(W, H));
            enc.setScissorRect(new MTLScissorRect(0, 0, W / 2, H));
            enc.setVertexBytes(vertices(QUAD, 0.5f), 0);
            enc.setVertexBytes(color(1, 0, 0), 1);
            enc.drawTriangles(0, 6);
        });
        byte[] scissored = readback(color);
        TestKit.check(isRed(rgb(scissored, W / 4, H / 2)) && isBlack(rgb(scissored, 3 * W / 4, H / 2)),
                "scissor rect clips the quad to the left half only (left 0x"
                        + Integer.toHexString(rgb(scissored, W / 4, H / 2)) + ", right 0x"
                        + Integer.toHexString(rgb(scissored, 3 * W / 4, H / 2)) + ")");
    }

    // ---------------------------------------------------- indexed draw proof

    /// An indexed triangle drawn from an MTLBuffer through
    /// setVertexBuffer:offset:atIndex: / drawIndexedPrimitives: must rasterize
    /// exactly like the vertex-bytes path.
    private static void indexedProof(MTLDevice device, MTLCommandQueue queue, MTLLibrary library) {
        MTLRenderPipelineState pipe = pipeline(device, library, 0, 0);
        MTLBuffer vertexBuf = device.newBuffer(TRI.length * 16L, 0);
        MTLBuffer indexBuf = device.newBuffer(3 * 4L, 0);
        TestKit.check(vertexBuf != null && vertexBuf.length() == TRI.length * 16L
                && indexBuf != null && indexBuf.length() == 12L,
                "vertex + index buffers allocate at the requested length");

        MemorySegment vc = vertexBuf.contents();
        MemorySegment ic = indexBuf.contents();
        TestKit.check(vc != null && ic != null, "shared buffers expose writable contents()");
        byte[] vbytes = vertices(TRI, 0.5f);
        MemorySegment.copy(vbytes, 0, vc,
                java.lang.foreign.ValueLayout.JAVA_BYTE, 0, vbytes.length);
        for (int i = 0; i < 3; i++) ic.set(java.lang.foreign.ValueLayout.JAVA_INT, i * 4L, i);

        MTLTexture color = target(device, COLOR_FORMAT, MTLTextureDescriptor.STORAGE_SHARED,
                MTLTextureDescriptor.USAGE_RENDER_TARGET | MTLTextureDescriptor.USAGE_SHADER_READ);
        MTLRenderPassDescriptor pass = MTLRenderPassDescriptor.create();
        pass.colorAttachment(0).setTexture(color);
        pass.colorAttachment(0).setLoadAction(LOAD_CLEAR);
        pass.colorAttachment(0).setStoreAction(STORE_STORE);
        pass.colorAttachment(0).setClearColor(new MTLClearColor(0, 0, 0, 1));

        render(queue, pass, enc -> {
            enc.setRenderPipelineState(pipe);
            enc.setVertexBuffer(vertexBuf, 0, 0);
            enc.setVertexBytes(color(1, 0.5f, 0), 1);
            enc.drawIndexed(3, MTLRenderCommandEncoder.INDEX_TYPE_UINT32, indexBuf, 0);
        });
        byte[] img = readback(color);
        int center = rgb(img, W / 2, H / 2);
        TestKit.check(isOrange(center),
                "indexed draw from an MTLBuffer rasterizes orange (got 0x" + Integer.toHexString(center) + ")");
        TestKit.check(isBlack(rgb(img, 2, 2)), "indexed draw does not cover the corner");

        MemorySegment pool = Autorelease.push();
        try {
            MTLCommandBuffer cb = queue.commandBuffer();
            MTLRenderCommandEncoder enc = cb.renderEncoder(pass);
            TestKit.expectThrows("drawIndexed with a null index buffer is rejected",
                    IllegalArgumentException.class,
                    () -> enc.drawIndexed(3, MTLRenderCommandEncoder.INDEX_TYPE_UINT16, null, 0));
            enc.endEncoding();
            cb.commit();
            cb.waitUntilCompleted();
        } finally {
            Autorelease.pop(pool);
        }
    }

    // ---------------------------------------------------------- error paths

    /// Metal-specific failure handling: a compile error must carry the
    /// compiler message, a pass with no attachments must fail loudly instead of
    /// handing back a nil encoder, a nil drawable is a legal no-op, and a bad
    /// readback region is rejected before it can read out of bounds.
    private static void errorPaths(MTLDevice device, MTLCommandQueue queue, MTLLibrary library) {
        Throwable compile = null;
        try {
            device.newLibraryWithSource("#include <metal_stdlib>\nthis is not valid MSL\n");
        } catch (Throwable t) {
            compile = t;
        }
        TestKit.check(compile instanceof RuntimeException
                && compile.getMessage() != null
                && compile.getMessage().startsWith("Metal shader compile failed:")
                && compile.getMessage().length() > "Metal shader compile failed:".length(),
                "failed shader compile reports the compiler message (got "
                        + (compile == null ? "no throw" : compile.getMessage()) + ")");

        TestKit.expectThrows("attachment-less render pass fails loudly instead of returning nil",
                IllegalStateException.class, () -> {
                    MTLCommandBuffer cb = queue.commandBuffer();
                    cb.renderEncoder(MTLRenderPassDescriptor.create());
                });

        MTLTexture color = target(device, COLOR_FORMAT, MTLTextureDescriptor.STORAGE_SHARED,
                MTLTextureDescriptor.USAGE_RENDER_TARGET | MTLTextureDescriptor.USAGE_SHADER_READ);
        TestKit.noThrow("presentDrawable(null) is a legal no-op", () -> {
            MTLCommandBuffer cb = queue.commandBuffer();
            cb.presentDrawable(null);
        });

        TestKit.expectThrows("readback region outside the texture is rejected",
                IllegalArgumentException.class, () -> color.getBytes(W * 4, MTLRegion.of2D(W - 4, 0, 8, 8), 0));
        TestKit.expectThrows("readback row shorter than width*bytesPerPixel is rejected",
                IllegalArgumentException.class, () -> color.getBytes(4, MTLRegion.of2D(0, 0, W, H), 0));

        TestKit.check(color.pixelFormat() == COLOR_FORMAT, "colour texture reports BGRA8Unorm");
    }
}
