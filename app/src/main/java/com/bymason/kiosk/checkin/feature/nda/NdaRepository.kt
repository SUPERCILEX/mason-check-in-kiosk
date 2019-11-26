package com.bymason.kiosk.checkin.feature.nda

import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.invoke
import kotlinx.coroutines.tasks.await

interface NdaRepository {
    suspend fun finish(employeeId: String, guestName: String, guestEmail: String)
}

class DefaultNdaRepository : NdaRepository {
    override suspend fun finish(
            employeeId: String,
            guestName: String,
            guestEmail: String
    ) {
        Default {
            Firebase.functions.getHttpsCallable("finishCheckIn")
                    .call(mapOf(
                            "employeeId" to employeeId,
                            "guestName" to guestName,
                            "guestEmail" to guestEmail
                    ))
                    .await()
        }
    }
}
