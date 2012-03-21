package groovystm

import clojure.lang.Atom;
import clojure.lang.Ref;
import clojure.lang.PersistentHashMap;
import org.junit.Test
import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail
import static groovystm.STM.doSync
import static groovystm.STM.alter
import static groovystm.STM.refSet
import static groovystm.STM.binding
import static groovystm.STM.withCurrentBindings
import static groovystm.STM.deref
import static groovystm.STM.ensure
import static groovystm.STM.addWatch
import static groovystm.STM.removeWatch
import static groovystm.STM.swap

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
    void testRefSet() {
        Ref r = new Ref(100)
        doSync {
            refSet(r, 200)
        }

        assertEquals 200, deref(r)
    }

    @Test
    void testNoTrans()  {
        Ref r = new Ref(100);
        boolean exceptionThrown = false

        try {
            r.set(200);
            fail "Exception not thrown"
        }
        catch (IllegalStateException e) {
            // this should be thrown since a transaction wasn't started
            exceptionThrown = true
        }
        assertEquals true, exceptionThrown
    }

    @Test
    void testEnsure() {
        Ref r = new Ref(100);
        Ref r2 = new Ref(0);

        def t1 = Thread.start {
            doSync {
                ensure(r)
                Thread.sleep(2000);
                alter(r2) { v -> deref(r) * 2 }
            }
        }

        Thread.sleep(500);
        def t2 = Thread.start {
            doSync {
                alter(r) { -1 }
            }
        }

        Thread.sleep(2000)
        assertEquals(-1, deref(r))
        assertEquals(200, deref(r2))
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
        binding(['name': 'Jim', 'id': '1']) {
            withCurrentBindings { m -> 
                println m
                assertEquals m['name'], "Jim" 
                assertEquals m['id'],'1'
            }
            testBindingValues()
        }
        testNoBindingValues()
    }

    void testBindingValues() {
        withCurrentBindings { m ->
            assertEquals m['name'], "Jim"
            assertEquals m['id'],'1'
        }
    }

    void testNoBindingValues() {
        withCurrentBindings { m ->
            assertEquals true, m.isEmpty()
        }
    }
}