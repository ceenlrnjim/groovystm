package gvystm

import clojure.lang.LockingTransaction
import clojure.lang.Ref
import java.util.concurrent.Callable

class STM {
    static void doSync(Closure c) {
        LockingTransaction.runInTransaction(new Callable<Void>() {
            public Void call() throws Exception {
                c()
                return null;
            }
        });
    }

    /** Calls the specified closure with the current value of the ref and sets
    *   the ref to the value returned.  Must be executed in a doSync block 
    *   See rules for the alter function in clojure. Inspired by, but definitely different than
    *   the clojure alter function
    */
    static void alter(Ref r, Closure c) {
        def v = r.deref();
        r.set(c(v));
    }

    /** See clojure ensure function - protects the specified reference from modifcation by another transaction */
    static Object ensure(Ref r) {
        r.touch()
        r.deref()
    }
}
