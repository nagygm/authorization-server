package hu.nagygm.oauth2.util

import kotlinx.coroutines.CompletableJob

suspend fun CompletableJob.completeAndJoinChildren() {
    this.complete()
    this.join()
}