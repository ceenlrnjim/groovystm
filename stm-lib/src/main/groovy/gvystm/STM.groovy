package gvystm

import clojure.lang.LockingTransaction
import clojure.lang.Atom
import clojure.lang.Ref
import clojure.lang.IRef
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

    /** Adds a callback that will get invoked when the value of the specified reference/atom/agent/etc. is changed.
    *   See clojure add-watch function.
    *   The specified closure should take 4 arguments - key, reference, old value and new value
    */
    static void addWatch(IRef r, Object key, Closure c) {
        r.addWatch(key, new WatchClosureFn(c));
    }

    static void removeWatch(IRef r, Object key) {
        r.removeWatch(key)
    }

    /** see clojure swap! function and atoms - closure must take the current value and return the new value */
    static Object swap(Atom a, Closure c) {
        return a.swap(new SwapClosureFn(c))
    }
}
