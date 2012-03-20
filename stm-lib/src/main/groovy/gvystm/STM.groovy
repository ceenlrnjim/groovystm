package gvystm

import clojure.lang.LockingTransaction
import clojure.lang.Atom
import clojure.lang.Ref
import clojure.lang.Var
import clojure.lang.IRef
import clojure.lang.PersistentHashMap
import clojure.lang.Associative
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

    static Object deref(IRef r) {
        r.deref();
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

    // TODO: add agent support
    /** Sets the bindings into the threadlocal scope */
    static void binding(Map bindings, Closure c) {
        Map varMap = new HashMap();
        bindings.every { key, value -> 
            Var v = Var.create(key)
            v.setDynamic()
            varMap.put(v, value) 
        }

        Var.pushThreadBindings(PersistentHashMap.create(varMap))
        c()
        // TODO: need some finally
        Var.popThreadBindings()
    }

    /** Calls the specified closure with the current thread bindings */
    static void withCurrentBindings(Closure c) {
        //Associative bindings = Var.getThreadBindings();
        c(Var.getThreadBindings());
    }

    // TODO: is there a better way in the clojure collections?
    /*private static Map associativeToMap(Associative a) {
        IPersistentMap ret = PersistentHashMap.EMPTY;
        for(ISeq bs = a.seq(); bs != null; bs = bs.next())
            {
            IMapEntry e = (IMapEntry) bs.first();
            Var v = (Var) e.key();
            TBox b = (TBox) e.val();
            ret = ret.assoc(v, b.val);
            }
        return ret;

    }*/
}
