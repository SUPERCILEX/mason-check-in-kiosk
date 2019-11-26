package com.bymason.kiosk.checkin.functions

import com.bymason.kiosk.checkin.utils.getAndInitCreds
import firebase.functions.AuthContext
import firebase.https.CallableContext
import firebase.https.HttpsError
import google.google
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asPromise
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import superagent.superagent
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json

fun finishCheckIn(data: Json, context: CallableContext): Promise<*>? {
    val auth = context.auth ?: throw HttpsError("unauthenticated")
    val employeeId = data["employeeId"] as? String
    val guestName = data["guestName"] as? String
    val guestEmail = data["guestEmail"] as? String
    console.log("Finishing check-in for guest $guestName ($guestEmail) " +
                        "to see employee '$employeeId'")

    if (employeeId == null || guestName == null || guestEmail == null) {
        throw HttpsError("invalid-argument")
    }

    return GlobalScope.async { finishCheckIn(auth, employeeId, guestName) }.asPromise()
}

private suspend fun finishCheckIn(auth: AuthContext, employeeId: String, guestName: String?) {
    val creds = getAndInitCreds(auth.uid)
    val directory = google.admin(json("version" to "directory_v1"))
    val employee = directory.users.get(json(
            "viewType" to "domain_public",
            "userKey" to employeeId
    )).await().data

    val employeeEmail = employee.primaryEmail
    val slackUser = superagent.get("https://slack.com/api/users.lookupByEmail")
            .query(json("token" to creds.slackToken, "email" to employeeEmail))
            .await().body
    if (!slackUser["ok"].unsafeCast<Boolean>()) {
        throw HttpsError("not-found", slackUser["error"].unsafeCast<String>())
    }

    val slackUserId = slackUser.asDynamic().user.id
    val slackMessage = superagent.post("https://slack.com/api/chat.postMessage")
            .query(json(
                    "token" to creds.slackToken,
                    "channel" to slackUserId,
                    "text" to "Your guest ($guestName) just arrived! \uD83D\uDC4B"
            ))
            .await().body

    if (!slackMessage["ok"].unsafeCast<Boolean>()) {
        throw HttpsError("unknown", slackUser["error"].unsafeCast<String>())
    }
}