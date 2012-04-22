package groovystm

import clojure.lang.Atom;
import clojure.lang.Agent;
import clojure.lang.Ref;
import clojure.lang.Var;
import clojure.lang.PersistentHashMap;
import org.junit.Test
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail
import static groovystm.STM.doSync
import static groovystm.STM.alter
import static groovystm.STM.refSet
import static groovystm.STM.binding
import static groovystm.STM.deref
import static groovystm.STM.ensure
import static groovystm.STM.addWatch
import static groovystm.STM.removeWatch
import static groovystm.STM.swap
import static groovystm.STM.send
import static groovystm.STM.sendOff
import static groovystm.STM.setErrorHandler
import static groovystm.STM.setErrorMode
import static groovystm.STM.restartAgent
import static groovystm.STM.agentError
import static groovystm.STM.setErrorHandler
import static groovystm.STM.errorHandler
import static groovystm.STM.setValidator
import static groovystm.STM.getValidator
import static groovystm.STM.await
import static groovystm.STM.awaitFor
import static groovystm.STM.var

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
    void testReturn() {
        assertEquals 100, returningFunction()
    }

    Object returningFunction() {
        Ref r = new Ref(0)
        doSync {
            alter(r) { 100 }
        }
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

    Var v = var(this.class, "v", 0);

    @Test
    void testBindings() {

        // parens required - otherwise a string key 'v' will be used
        Thread.start {
            binding([(v): 10]) {
                Thread.sleep(100)
                assertEquals 10, deref(v)
            }
        }

        Thread.start {
            binding([(v): 20]) {
                Thread.sleep(100)
                assertEquals 20, deref(v)
            }
        }

        Thread.start {
            binding(Collections.EMPTY_MAP) {
                Thread.sleep(100)
                assertEquals 0, deref(v)
            }
        }
    }

    @Test
    public void testSend() {
        Agent a = new Agent(100)
        a.setErrorMode(Agent.FAIL)

        long sendThreadId = Thread.currentThread().getId()
        send(a) { v -> 
            assertTrue sendThreadId != Thread.currentThread().getId()
            v + 100 
        }
        //Thread.sleep(2000);
        await(a)
        assertEquals null, a.getError()
        assertEquals 200, deref(a)
    }

    @Test
    public void testSendOff() {
        Agent a = new Agent(100)
        a.setErrorMode(Agent.FAIL)

        long sendThreadId = Thread.currentThread().getId()
        sendOff(a) { v -> 
            assertTrue sendThreadId != Thread.currentThread().getId()
            v + 100 
        }
        //Thread.sleep(2000);
        await(a)
        assertEquals null, a.getError()
        assertEquals 200, deref(a)
    }


    @Test
    public void testAgentError() {
        Agent a = new Agent(0)
        setErrorMode(a, Agent.FAIL)
        assertEquals Agent.FAIL, a.errorMode

        send(a) { throw new RuntimeException("I FAILED!") }

        Thread.sleep(2000);
        assertTrue  agentError(a) != null
        restartAgent(a, 0, true)

        setErrorMode(a, Agent.CONTINUE)
        assertEquals Agent.CONTINUE, a.errorMode

        send(a) { throw new RuntimeException("I FAILED!") }
        Thread.sleep(2000);
        assertTrue agentError(a) == null
    }

    @Test
    public void testAgentErrorHandler() {
        Agent a = new Agent(0)
        setErrorMode(a, Agent.FAIL)
        boolean errorHandled = false

        Closure handler = { agent, error ->
            errorHandled = true
        }

        setErrorHandler(a, handler)
        assertEquals handler, errorHandler(a)

        send(a) { throw new RuntimeException("FAIL") }
        Thread.sleep(2000);
        assertEquals true, errorHandled
    }

    @Test
    public void testAgentWatch() {
        boolean watchFired = false
        Agent a = new Agent(0);
        setErrorMode(a, Agent.FAIL)

        addWatch(a, "somekey") { key, ref, oldval, newval ->
            watchFired = true
        }

        send(a) { v -> v + 1 }
        //Thread.sleep(1000);
        await(a)
        assertTrue agentError(a) == null
        assertTrue watchFired
    }

    // TODO: test validators
    @Test
    public void testValidator() {
        // atom
        Atom a = new Atom(0)
        setValidator(a) { v -> v >= 0 }
        try {
            swap(a) { v -> v + 1 }
            swap(a) { v -> v - 100 }
            fail "Exception not thrown"
        }
        catch (Exception e) {
            assertEquals 1, deref(a)
        }

        // ref
        Ref r = new Ref(0)
        setValidator(r) { v -> v >= 0 }
        try {
            doSync {
                alter(r) { v -> v + 1 }
                alter(r) { v -> v - 100 }
            }
            fail "Exception not thrown"
        }
        catch (Exception e) {
            // here the whole transaction would be rolled back
            assertEquals 0, deref(r)
        }

        // agent
        Agent g = new Agent(0)
        setValidator(g) { v -> v >= 0 }
        setErrorMode(g, Agent.FAIL)
        send(g) { v -> v - 100 }
        Thread.sleep(1000)
        assertTrue agentError(g) != null
        assertEquals 0, deref(g)

    }

    @Test
    public void testGetValidator() {
        Closure validator = { v -> v >= 0 }
        Atom a = new Atom(0)
        setValidator(a, validator)
        assertEquals validator, getValidator(a)
    }

    @Test
    public void testAwait() {
        // TODO: change tests above to use await
        Agent a = new Agent(0)

        send(a) { v->
            Thread.sleep(1000)
            v + 1 
        }

        await(a)
        assertEquals 1, deref(a)

    }

    @Test
    public void testAwaitFor() {
        Agent a = new Agent(0)

        send(a) { v->
            Thread.sleep(1000)
            v + 1 
        }

        def rv = awaitFor(200,a)
        assertTrue !rv
        rv = awaitFor(1200, a)
        assertTrue rv
        assertEquals 1, deref(a)
    }
}
