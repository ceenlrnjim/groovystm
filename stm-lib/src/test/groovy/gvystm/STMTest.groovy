package gvystm

import clojure.lang.Ref;
import clojure.lang.PersistentHashMap;
import org.junit.Test
import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail
import static gvystm.STM.doSync
import static gvystm.STM.alter
import static gvystm.STM.ensure

class STMTest {

    @Test
    void testAlterRef() {
        Ref valueMapRef = new Ref(PersistentHashMap.EMPTY);    
        doSync {
            alter(valueMapRef) { m -> m.assoc(1,100) }
        }
        assertEquals 100, valueMapRef.deref().get(1);
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
                alter(r2) { v -> r.deref() * 2 }
            }
        }

        def t2 = Thread.start {
            doSync {
                alter(r) { -1 }
            }
        }

        Thread.sleep(2000)
        assertEquals r.deref(), -1
        assertEquals r2.deref(), 200
    }
}