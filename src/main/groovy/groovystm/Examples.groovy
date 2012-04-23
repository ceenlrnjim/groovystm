package groovystm

import clojure.lang.Atom;
import clojure.lang.Agent;
import clojure.lang.Ref;
import clojure.lang.Var;
import clojure.lang.PersistentHashMap;
import clojure.lang.PersistentVector;
import java.util.concurrent.*;

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


public class Examples {

    static void main(String[] args) throws Exception {
        new RefExample(100,10,10,100000).run()
    }

}
