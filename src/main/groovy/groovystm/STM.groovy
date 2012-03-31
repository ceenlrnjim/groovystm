/**
 *   Copyright (c) James Kirkwood. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

package groovystm

import clojure.lang.LockingTransaction
import clojure.lang.Atom
import clojure.lang.Agent
import clojure.lang.Ref
import clojure.lang.RT
import clojure.lang.Var
import clojure.lang.IRef
import clojure.lang.PersistentHashMap
import clojure.lang.PersistentList
import clojure.lang.Associative
import java.util.concurrent.Callable

class STM {
    static enum AgentErrorMode {
        FAIL, CONTINUE
    }
    /** Executes the specified closure in a clojure LockingTransaction, allowing
    *   changes to Refs. Make sure an use immutable values for your Refs or you get no guarantees from the STM
    */
    static Object doSync(Closure c) {
        LockingTransaction.runInTransaction(new Callable<Object>() {
            public Object call() throws Exception {
                return c()
            }
        });
    }

    /** Calls the specified closure with the current value of the ref and sets
    *   the ref to the value returned.  Must be executed in a doSync block 
    *   See rules for the alter function in clojure. Inspired by, but definitely different than
    *   the clojure alter function.  
    */
    static Object alter(Ref r, Closure c) {
        def v = r.deref();
        r.set(c(v));
    }

    static Object refSet(Ref r, Object val) {
        r.set(val)
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
        r.addWatch(key, new ClosureFn(c));
    }

    static void removeWatch(IRef r, Object key) {
        r.removeWatch(key)
    }

    /** see clojure swap! function and atoms - closure must take the current value and return the new value */
    static Object swap(Atom a, Closure c) {
        return a.swap(new ClosureFn(c))
    }

    /** Closure should take one argument, the state of the agent.  Note that this
    *   implementation uses the Agent java functionality directly.  All the processing done by the clojure
    *   send function has not yet been incorporated (still trying to figure out what its doing)
    */
    static Object send(Agent a, Closure c) {
        return a.dispatch(new ClosureFn(c), PersistentList.EMPTY, false)
    }

    /** Closure should take one argument, the state of the agent.  Note that this
    *   implementation uses the Agent java functionality directly.  All the processing done by the clojure
    *   send function has not yet been incorporated (still trying to figure out what its doing)
    */
    static Object sendOff(Agent a, Closure c) {
        return a.dispatch(new ClosureFn(c), PersistentList.EMPTY, true)
    }

    static void setErrorMode(Agent a, AgentErrorMode mode) {
        a.setErrorMode(mode == AgentErrorMode.FAIL ? Agent.FAIL : Agent.CONTINUE)
    }

    static Throwable agentError(Agent a) {
        a.getError()
    }

    static Object restartAgent(Agent a, Object newState, boolean clearActions) {
        a.restart(newState, clearActions)
    }

    /** Closure should take two arguments, the agent and the exception */
    static void setErrorHandler(Agent a, Closure handler) {
        // TODO
    }

    static void errorHandler(Agent a) {
        a.getErrorHandler()
    }

    /** Closure should take one argument - the proposed new state - and return null or throw an exception if state is unacceptable */
    static void setValidator(IRef r, Closure c) {
        r.setValidator(new ClosureFn(c));
    }

    /** creates vars for each entry in the map and pushes them into thread local scope
    *   inspired by the clojure binding function and using those internals, but doesn't really
    *   exposed the concept of a Var
    */
    static void binding(Map bindings, Closure c) {
        Map varMap = new HashMap();
        bindings.each { key, value -> 
            // TODO: how to handle namespaces?
            // How would I expect namespaces to work in threading these bindings through multiple classes/methods?
            Var v = RT.var("hardcodedfornow",key.toString(), key)
            v.setDynamic()
            varMap.put(v, value) 
        }

        Var.pushThreadBindings(PersistentHashMap.create(varMap))
        try {
            c()
        } finally {
            Var.popThreadBindings()
        }
    }

    /** Calls the specified closure with the current thread bindings.  The 
    *   closure should take one argument that will be the map of variables from the
    *   thread local
    */
    static void withCurrentBindings(Closure c) {
        // All kinds of ugly dependency on internal implementations
        Map currVals = (Map)Var.getThreadBindings();
        Map usableVals = new HashMap()
        currVals.each { var, value -> usableVals.put(var.sym.getName(), value) }
            
        c(usableVals)
    }
}
