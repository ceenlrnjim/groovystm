package gvystm

import clojure.lang.Atom;
import clojure.lang.Ref;
import clojure.lang.PersistentHashMap;
import org.junit.Test
import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail
import static gvystm.STM.doSync
import static gvystm.STM.alter
import static gvystm.STM.binding
import static gvystm.STM.withCurrentBindings
import static gvystm.STM.deref
import static gvystm.STM.ensure
import static gvystm.STM.addWatch
import static gvystm.STM.removeWatch
import static gvystm.STM.swap

class STMTest {

    @Test
    void testAlterRef() {
        Ref valueMapRef = new Ref(PersistentHashMap.EMPTY);    
        doSync {
            alter(valueMapRef) { m -> m.assoc(1,100) }
        }
        assertEquals 100, deref(valueMapRef).get(1);
    }

    @Test
    void testNoTrans()  {
        Ref r = new Ref(100);

        try {
            r.set(200);
            fail "Exception not thrown"
        }
        catch (IllegalStateException e) {
            // this should be thrown since a transaction wasn't started
        }
    }

    @Test
    void testEnsure() {
        // TODO: how do I test this?
        Ref r = new Ref(100);
        Ref r2 = new Ref(0);

        def t1 = Thread.start {
            doSync {
                ensure(r)
                Thread.sleep(1000);
                alter(r2) { v -> deref(r) * 2 }
            }
        }

        def t2 = Thread.start {
            doSync {
                alter(r) { -1 }
            }
        }

        Thread.sleep(2000)
        assertEquals deref(r), -1
        assertEquals deref(r2), 200
    }

    @Test
    void testRefAddWatch() {
        Ref r = new Ref(0);
        String k = "unitTestCallback"
        def fired = false

        addWatch(r, k) { key, ref, oldValue, newValue ->
            assertEquals oldValue, 0
            assertEquals newValue, 100
            // note: normally don't want side effects
            fired = true
        }

        doSync {
            alter(r) { 100 }
        }

        assertEquals fired, true
    }

    @Test
    void removeWatch() {
        Ref r = new Ref(100);
        def fired = false;

        addWatch(r, "key") {
            fired = true;
        }

        removeWatch(r, "key")

        doSync { alter(r) { 0 } }
        assertEquals fired, false
    }

    @Test
    void testAtomSwap() {
        Atom a = new Atom(100);
        swap(a) { v -> v * 2 }
        assertEquals deref(a), 200
    }

    @Test
    void testAtomWatch() {
        Atom a = new Atom(0)
        def fired = false

        addWatch(a, "AtomWatchKey") { key, ref, oldval, newval ->
            fired = true
            assertEquals oldval, 0
            assertEquals newval, 1
        }

        swap(a) { v -> v + 1 }
        assertEquals deref(a), 1
    }

    @Test
    void testBindings() {

        Thread.start {
            binding([name: "Jim", id: 1]) {
                testBindingValues("Jim", 1)
            }
        }

        Thread.start {
            binding([name: "Jeff", id: 2]) {
                testBindingValues("Jeff", 1)
            }
        }
    }

    void TestBindingValues(String name, int id) {
        withCurrentBindings { m ->
            assertEquals m.name, name
            assertEquals m.id, id
        }
    }
}
