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
import clojure.lang.Keyword
import clojure.lang.Ref
import clojure.lang.RT
import clojure.lang.Var
import clojure.lang.IRef
import clojure.lang.IFn
import clojure.lang.PersistentHashMap
import clojure.lang.PersistentList
import clojure.lang.Associative
import java.util.concurrent.Callable

class STM {
    /** Executes the specified closure in a clojure LockingTransaction, allowing
    *   changes to Refs. Make sure an use immutable values for your Refs or you get no guarantees from the STM.
    *   See http://clojure.org/refs for details on refs and transactions.
    *   See http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/dosync for documentation of clojure dosync function.
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
    *   See http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/alter for details on clojure's alter function. Note that in this implementation.
    */
    static Object alter(Ref r, Closure c) {
        def v = r.deref();
        r.set(c(v));
    }

    /** Sets the value of the specified reference to the specified value.  Must be invoked in a transaction (doSync).
    *   See http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/ref-set for details on the clojure function.
    */
    static Object refSet(Ref r, Object val) {
        r.set(val)
    }

    /** Protects the specified ref from modification by other transactions. 
    *   Returns the in-transaction value of the ref.
    */
    static Object ensure(Ref r) {
        r.touch()
        r.deref()
    }

    /** In a transaction returns the in-transaction value of the ref, otherwise the most recently committed value. 
    *   See http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/deref for clojure definition 
    */
    static Object deref(IRef r) {
        r.deref();
    }

    /** Adds a callback that will get invoked when the value of the specified reference/atom/agent/etc. is changed.
    *   See clojure add-watch function at http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/add-watch .
    *   The specified closure should take 4 arguments - key, reference, old value and new value
    */
    static void addWatch(IRef r, Object key, Closure c) {
        r.addWatch(key, new ClosureFn(c));
    }

    /** Removes the watch on the specified ref that was added with the specified key.
    *   See http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/remove-watch
    */
    static void removeWatch(IRef r, Object key) {
        r.removeWatch(key)
    }

    /** Atomically swaps the value on the specified atom by invoking the specified clojure.  
    *   The closure is passed the current value of the atom and the return value is used as its new value.
    *   See http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/swap!
    */
    static Object swap(Atom a, Closure c) {
        return a.swap(new ClosureFn(c))
    }

    /** Asynchronously update an agent with the return value of the specified closure.  Immediately
    *   returns the agent.  The closure should take one argument, the current value of the agent
    *   See http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/send and
    *   http://clojure.org/agents
    */
    static Object send(Agent a, Closure c) {
        executeCljFn("clojure.core", "send", [a, new ClosureFn(c)])
    }

    /** Asynchronously updates an agent with the return value of the specified closure which potentially blocks.  Immediately
    *   returns the agent.  The closure should take one argument, the current value of the agent
    *   See http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/send-off and
    *   http://clojure.org/agents
    */
    static Object sendOff(Agent a, Closure c) {
        executeCljFn("clojure.core", "send-off", [a, new ClosureFn(c)])
    }

    /** Sets the error mode of the agent - valid values are Agent.FAIL or Agent.CONTINUE */
    static void setErrorMode(Agent a, Keyword mode) {
        a.setErrorMode(mode)
    }

    /** 
    * Returns the exception thrown during an asynchronous action of the
    * agent if the agent is failed.  Returns nil if the agent is not
    * failed. See http://clojure.org/agents
    */
    static Throwable agentError(Agent a) {
        a.getError()
    }

    /** Restarts a failed agent with the specified new state and optionally clears out any actions
    * remaining when it failed.  See http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/restart-agent and
    * http://clojure.org/agents
    */
    static Object restartAgent(Agent a, Object newState, boolean clearActions) {
        a.restart(newState, clearActions)
    }

    /** Sets a closure that should be invoked if an agent fails or a validator doesn't pass.
    *   Closure should take two arguments, the agent and the exception.
    *   See http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/set-error-handler!
    */
    static void setErrorHandler(Agent a, Closure handler) {
        a.setErrorHandler(new ClosureFn(handler))
    }

    /** Returns the error handler associated with the agent or null if none is set.
    *   See http://clojure.org/agents
    */
    static Closure errorHandler(Agent a) {
        ClosureFn handler = (ClosureFn)a.getErrorHandler()
        handler.closure;
    }

    /** Initiates a shutdown of the thread pools that back the agent
    * system. Running actions will complete, but no new actions will be
    * accepted. See http://clojure.org/agents
    */
    static void shutdownAgents() {
        Agent.shutdown()
    }

    /** Adds a closure that will be called when the value changes.  If the value is not acceptable, validator should return false
    *   or throw an exception.  closure must be side effect free.
    * See http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/set-validator! .
    * Closure should take one argument - the proposed new state - and return null or throw an exception if state is unacceptable */
    static void setValidator(IRef r, Closure c) {
        r.setValidator(new ClosureFn(c));
    }

    /** Returns the validator for the specified reference or null if one is not set */
    static Closure getValidator(IRef r) {
        ClosureFn fn = r.getValidator()
        fn.closure
    }

    /** blocks current thread indefinitely until all actions dispatched thus far to the specified
    *   agents complete.  Will never return if a failed agent is restarted with clearActions true
    */
    static void await(Agent... agents) {
        executeCljFn("clojure.core", "await", Arrays.asList(agents))
    }

    /** blocks current thread indefinitely until all actions dispatched thus far to the specified
    *   agents complete or the specified timeout elapses.
    */
    static Object awaitFor(long timeoutMillis, Agent... agents) {
        List args = new ArrayList(agents.length + 1)
        args.add(timeoutMillis)
        args.addAll(Arrays.asList(agents))
        executeCljFn("clojure.core", "await-for", args)
    }

    /** creates vars for each entry in the map and pushes them into thread local scope
    *   inspired by the clojure binding function and using those internals, but doesn't really
    *   expose the concept of a Var
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

    /** Executes the specified clojure function with the specified arguments */
    static Object executeCljFn(String ns, String name, List args) {
        RT.var(ns, name).applyTo(RT.seq(args))
    }
}
