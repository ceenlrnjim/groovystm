package gvystm

import clojure.lang.LockingTransaction
import java.util.concurrent.Callable

class STM {
    static doSync(Closure c) {
        LockingTransaction.runInTransaction(new Callable<Void>() {
            public Void call() throws Exception {
                c()
                return null;
            }
        });
    }
}
