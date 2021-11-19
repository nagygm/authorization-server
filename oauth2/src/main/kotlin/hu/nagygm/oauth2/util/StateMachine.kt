package hu.nagygm.oauth2.util

/**
 * Abstract StateMachine to describe in class fixed state small state machines
 */
interface StateMachine<E: Event, S: State, SE: SideEffect> {
    fun transition(from: S, event: E): State
    class Transition(val from: State, val to: State) {
        
    }
}


interface Event {}
interface State {
    val code: String
//    fun code(): String
}
interface SideEffect {
}

class StateMachineImpl<E: Event, S: State, SE: SideEffect> : StateMachine<E, S, SE> {
    override fun transition(from: S, event: E): State {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

