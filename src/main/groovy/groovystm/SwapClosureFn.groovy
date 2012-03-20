/**
 *   Copyright (c) James Kirkwood. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

package groovystm

import clojure.lang.AFn

// TODO: can I make this work based on the number of arguments in the closure
class SwapClosureFn extends AFn {
    Closure closure;

    SwapClosureFn(Closure c) {
        closure = c
    }

    @Override
    public Object invoke(Object arg1) {
        closure(arg1)
    }
}
