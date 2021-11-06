package hu.nagygm.server

import hu.nagygm.oauth2.client.registration.ClientConfigurationMap
import hu.nagygm.oauth2.client.registration.ClientConfigurationParamKeys
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import kotlinx.coroutines.*
import java.lang.ArithmeticException
import kotlin.system.measureTimeMillis

class CoroutinePractice : AnnotationSpec() {
    @Test
    fun `Testing stuff`() {
        runBlocking {
            launch {
                doWorld()
            }
            println("Hello ")
            1 shouldBe 1
        }
    }

    private suspend fun doWorld() {
        delay(1000L)
        println("World")
    }

    @Test
    fun `Scope builder test with coroutine scope`() {
        runB()
        println("Done")
    }

    private fun runB() = runBlocking {
        doWorldCoScope()
    }

    private suspend fun doWorldCoScope() = coroutineScope {
        launch {
            delay(2000L)
            println("World 2")
        }
        launch {
            delay(1000L)
            println("World 1")
        }
        println("Hello")
    }

    @Test
    fun `Testing join for coroutines`() {
        runBlocking {
            println("Start")
            var counter = 1
            val job = launch {
                delay(1000L)
                counter = counter.plus(1)
                println("Processing")
            }
            counter shouldBe 1
            job.join()
            counter shouldBe 2
            println("Done")
        }
    }

    @Test
    fun `Testing coroutine cancellation`() {
        runBlocking {
            val job = launch {
                repeat(1000) { i ->
                    println("job? I am sleeping $i.")
                    delay(500L)
                }
            }
            delay(1300L)
            println("main: No more waiting!")
//            job.cancel()
//            job.join()
            job.cancelAndJoin()
            println("Can quit now")
        }
    }

    @Test
    fun `Uncooperative cancellation`() {
        runBlocking {
            val startTime = System.currentTimeMillis()
            var counter = 0
            val job = launch(Dispatchers.Default) {
                var nextPrintTime = startTime
                var i = 0
                while (i < 5) {
                    if (System.currentTimeMillis() >= nextPrintTime) {
                        counter = i
                        println("job: I am sleeping ${i++}")
                        nextPrintTime += 500L
                    }
                }
            }
            delay(1300L)
            println("main: No more waiting!!!")
            job.cancelAndJoin()
            counter shouldBe 4
            println("main: Finally!!!")
        }
    }

    @Test
    fun `Cooperative cancellation`() {
        runBlocking {
            val startTime = System.currentTimeMillis()
            var counter = 0
            val job = launch(Dispatchers.Default) {
                var nextPrintTime = startTime
                var i = 0
                while (isActive && i < 5) {
                    if (System.currentTimeMillis() >= nextPrintTime) {
                        counter = i
                        println("job: I am sleeping ${i++}")
                        nextPrintTime += 500L
                    }
                }
            }
            delay(1300L)
            println("main: No more waiting!!!")
            job.cancelAndJoin()
            counter shouldBe 2
            println("main: Finally!!!")
        }
    }

    @Test
    fun `Closing resources with finally`() {
        runBlocking {
            var counter = 0
            val job = launch {
                try {
                    repeat(1000) { i ->
                        counter = i
                        println("job: I am sleeping $i")
                        delay(500L)
                    }
                } finally {
                    counter shouldBe 2
                    println("job: finally")
                    counter = 100
                }
            }
            delay(1300L)
            println("main: waiting")
            job.cancelAndJoin()
            counter shouldBe 100
            println("quiting")
        }
    }

    @Test
    fun `Cancelling job with finally with suspending call without Non cancelable context`() {
        runBlocking {
            var counter = 0
            val job = launch {
                try {
                    repeat(1000) { i ->
                        counter = i
                        println("job: I am sleeping $i")
                        delay(500L)
                    }
                } finally {
                    delay(100L)
                    counter shouldBe 2
                    println("job: finally")
                    counter = 100
                }
            }
            delay(1300L)
            println("main: waiting")
            job.cancelAndJoin()
            counter shouldBe 2
            println("quiting")
        }
    }

    @Test
    fun `Non cancelable block`() {
        runBlocking {
            var counter = 0
            val job = launch {
                try {
                    repeat(1000) { i ->
                        counter = i
                        println("job: I am sleeping $i")
                        delay(500L)
                    }
                } finally {
                    withContext(NonCancellable) {
                        delay(1000L)
                        counter shouldBe 2
                        println("job: finally")
                        counter = 100
                    }
                }
            }
            delay(1300L)
            println("main: waiting")
            job.cancelAndJoin()
            counter shouldBe 100
            println("quiting")
        }
    }

    @Test
    fun `Coroutine timeout`() {
        runBlocking {
            val result = withTimeoutOrNull(1300L) {
                repeat(1000) { i ->
                    println("I am sleeping $i")
                    delay(500L)
                }
                "Done"
            }
            result shouldBe null
            println("Result is $result")
        }
    }

//    @Test
    fun `Leaking resources`() {
        var acquired = 0

        class Resource {
            init {
                acquired++
            }

            fun close() {
                acquired--
            }
        }
        runBlocking {
            repeat(100_000) {
                launch {
                    val resource = withTimeout(60) {
                        delay(50) //49-52ms will work here
                        Resource()
                    }
                    resource.close()
                }
            }
        }
        println(acquired)
        acquired shouldNotBe 0
    }

    @Test
    fun `Fixing leaking resources`() {
        var acquired = 0

        class Resource {
            init {
                acquired++
            }

            fun close() {
                acquired--
            }
        }
        runBlocking {
            repeat(100_000) {
                launch {
                    var resource: Resource? = null
                    try {
                        withTimeout(60) {
                            delay(50)
                            resource = Resource()
                        }
                    } finally {
                        resource?.close()
                    }
                }
            }
        }
        println(acquired)
        acquired shouldBe 0
    }

    private suspend fun doUsefulOne(): Int {
        delay(1000L)
        return 13
    }

    private suspend fun doUsefulTwo(): Int {
        delay(1000L)
        return 29
    }

    @Test
    fun `Sequential by default`() {
        runBlocking {
            val time = measureTimeMillis {
                val one = doUsefulOne()
                val two = doUsefulTwo()
                one + two shouldBe 42
            }
            time shouldBeGreaterThan 2000
        }
    }

    @Test
    fun `Async version`() {
        runBlocking {
            val time = measureTimeMillis {
                val one = async { doUsefulOne() }
                val two = async { doUsefulTwo() }
                one.await() + two.await() shouldBe 42
            }
            time shouldBeLessThan 2000
        }
    }

    @Test
    fun `Lazy started async`() {
        runBlocking {
            val time = measureTimeMillis {
                val one = async(start = CoroutineStart.LAZY) { doUsefulOne() }
                val two = async(start = CoroutineStart.LAZY) { doUsefulTwo() }
                one.start()
                two.start()
                one.await() + two.await() shouldBe 42
            }
            println("Completed in $time ms")
        }
    }

//    @Test
    fun `Async style function, dont use test`() {
        val time = measureTimeMillis {
            val one = doUsefulOneAsync()
            val two = doUsefulTwoAsync()
            runBlocking {
                one.await() + two.await() shouldBe 42
            }
        }
        time shouldBeLessThan 2000
    }

//    @OptIn(DelicateCoroutinesApi::class)
    fun doUsefulOneAsync() = GlobalScope.async { doUsefulOne() }

//    @OptIn(DelicateCoroutinesApi::class)
    fun doUsefulTwoAsync() = GlobalScope.async { doUsefulTwo() }

//    @Test
    fun `Structured concurrency with async`() {
        val time = measureTimeMillis {
            runBlocking {
                concurrentSum() shouldBe 42
            }
        }
        time shouldBeLessThan 2000
    }

    private suspend fun concurrentSum(): Int = coroutineScope {
        val one = async(start = CoroutineStart.LAZY) { doUsefulOne() }
        val two = async(start = CoroutineStart.LAZY) { doUsefulTwo() }
        one.await() + two.await()

    }

    @Test
    suspend fun `Failed concurrent sum`() {
        shouldThrow<ArithmeticException> {
            failedConcurrentSum()
        }
    }

    private suspend fun failedConcurrentSum(): Int = coroutineScope {
        val one = async {
            try {
                delay(Long.MAX_VALUE)
                42
            } finally {
                println("First child was cancelled")
            }
        }
        val two = async<Int> {
            println("Second child throws an exception")
            throw ArithmeticException()
        }
        one.await() + two.await()
    }

//    @OptIn(ObsoleteCoroutinesApi::class)
//    @Test
    fun `Coroutine context`() {
        Thread.currentThread().name = "main"
        runBlocking {
            launch {
                Thread.currentThread().name shouldBe "main"
            }
            launch (Dispatchers.Unconfined) {
                Thread.currentThread().name shouldBe "main"
            }
            launch(Dispatchers.Default) {
                Thread.currentThread().name shouldNotBe "main"
                Thread.currentThread().name shouldMatch "^DefaultDispatcher.*"
            }
            launch(newSingleThreadContext("MyOwnThread")) {
                Thread.currentThread().name shouldBe "MyOwnThread"
            }
        }
    }

    @Test
    fun `Coroutine context inheritance`() {
        runBlocking {
            val request = launch {
                val parentContext = coroutineContext
                println(parentContext.fold("") { a, b -> "$a ${b.javaClass}:$b" })
                launch(Job()) {
                    println(coroutineContext.fold("") { a, b -> "$a ${b.javaClass}:$b" })
                    delay(1000L)
                    println("job1: Wont get cancelled")
                }
                launch {
                    delay(100)
                    println(coroutineContext.fold("") { a, b -> "$a ${b.javaClass}:$b" })
                    println(parentContext.fold("") { a, b -> "$a ${b.javaClass}:$b" })

                    println("job2: will get cancelled")
                    delay(1000)
                    println("I am not cancelled")
                }
            }
            delay(500)
            request.cancel()
            delay(1000)
            println("The survivor of the cancellation is job1")
        }
    }


}