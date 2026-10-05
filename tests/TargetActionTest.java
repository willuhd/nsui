package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.objc.DelegateProxy;
import nsui.objc.ObjC;

/// Verify the DelegateProxy.actionTarget target/action shape: a Java {@link
/// DelegateProxy.VoidArg} is invoked when a control fires the registered selector
/// against the returned instance.
///
/// Pass: (1) firing the registered "ballPopped:" invokes the handler with the
/// correct sender; (2) firing an UNREGISTERED selector on the same target is a
/// harmless no-op (no crash).
public final class TargetActionTest {

    public static void main(String[] args) {
        System.out.println("=== TargetActionTest — DelegateProxy.actionTarget ===");
        ObjC.init(); // FFM bindings (must be first)

        final boolean[] flag = {false};
        final long[] senderAddress = {-1L};

        MemorySegment target = DelegateProxy.actionTarget("ballPopped:", (MemorySegment sender) -> {
            flag[0] = true;
            senderAddress[0] = sender.address();
        });

        TestKit.check(target != null && target.address() != 0, "actionTarget returned a live instance");
        TestKit.check(DelegateProxy.registrySize() == 1, "registry holds exactly the one target (size=" + DelegateProxy.registrySize() + ")");

        // A sender object: a fresh NSObject.
        MemorySegment someSender = ObjC.msgSendId(ObjC.cls("NSObject"), ObjC.sel("new"));
        TestKit.check(someSender.address() != 0, "created sender NSObject");

        // Simulate a control firing "[target ballPopped:]" -> must reach Java.
        ObjC.msgSendVoidId(target, ObjC.sel("ballPopped:"), someSender);
        TestKit.check(flag[0], "registered selector 'ballPopped:' invoked the VoidArg handler");
        TestKit.check(senderAddress[0] == someSender.address(),
                "handler received the exact sender object (sender=" + Long.toHexString(senderAddress[0])
                        + " expected=" + Long.toHexString(someSender.address()) + ")");

        // Firing an UNREGISTERED selector must not crash and must be a no-op.
        boolean threw = false;
        try {
            ObjC.msgSendVoidId(target, ObjC.sel("otherSelector:"), someSender);
        } catch (Throwable t) {
            threw = true;
            System.out.println("FAIL: unregistered selector threw: " + t);
        }
        TestKit.check(!threw, "unregistered selector 'otherSelector:' fired without crashing (no-op)");
        TestKit.check(DelegateProxy.registrySize() == 1, "registry unchanged after firing (size=" + DelegateProxy.registrySize() + ")");

        System.out.println(TestKit.failures() == 0 ? "RESULT: ALL PASS" : "RESULT: " + TestKit.failures() + " FAILURE(S)");
        TestKit.end();
    }
}
