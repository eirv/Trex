package io.github.eirv.trex.demo;

import io.github.eirv.trex.Trex;
import io.github.eirv.trex.TrexJvm;
import io.github.eirv.trex.TrexOption;
import io.github.eirv.trex.test.Test;

import java.security.MessageDigest;

public class Main {
    public Main() {
        Exception e = new Exception();
        try {
            MessageDigest.class.getMethod("getInstance", String.class).invoke(null, "0");
        } catch (Exception ex) {
            e.initCause(Test.test());
            e.addSuppressed(ex);
        }
        e.printStackTrace();
        TrexJvm.init();
        Trex.printStackFrame(e, new TrexOption().applyBaseColorScheme());
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; 16 > i; i++) {
            System.out.printf("\u001b[48;5;%dm %3d", i, i);
            if ((i + 1) % 8 == 0) {
                System.out.println("\u001b[0m");
            }
        }
        for (int i = 0; 240 > i; i++) {
            System.out.printf("\u001b[48;5;%1$dm %1$3d", i + 16);
            if ((i + 1) % 12 == 0) {
                System.out.println("\u001b[0m");
            }
        }
        System.out.println();
        /*System.out.println(Mirror.getOffset(Mirror.JLJ8_Throwable.class, "backtrace"));
        System.out.println(Mirror.getOffset(Mirror.JLJ9_Throwable.class, "backtrace"));
        System.out.println(Mirror.getOffset(Mirror.JLRJ8_Constructor.class, "slot"));
        System.out.println(Mirror.getOffset(Mirror.JLRJ8_Method.class, "slot"));*/
    }
}
