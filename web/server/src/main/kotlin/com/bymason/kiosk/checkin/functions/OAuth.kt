package com.bymason.kiosk.checkin.functions

import com.bymason.kiosk.checkin.utils.buildGoogleAuthClient
import express.Request
import express.Response
import firebase.firestore.SetOptions
import firebase.functions.admin
import firebase.functions.functions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asPromise
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import superagent.superagent
import kotlin.js.Promise
import kotlin.js.json

fun handleGSuiteAuth(req: Request<Any?>, res: Response<Any?>): Promise<*>? {
    if (!checkForError(req, res)) return null
    return GlobalScope.async { fetchGSuiteAccessToken(req, res) }.asPromise()
}

fun handleSlackAuth(req: Request<Any?>, res: Response<Any?>): Promise<*>? {
    if (!checkForError(req, res)) return null
    return GlobalScope.async { fetchSlackAccessToken(req, res) }.asPromise()
}

private fun checkForError(req: Request<Any?>, res: Response<Any?>): Boolean {
    val error = req.query["error"]
    if (error != null) {
        console.log(error)
        res.status(400).send(error)
        return false
    }
    return true
}

private suspend fun fetchGSuiteAccessToken(req: Request<Any?>, res: Response<Any?>) {
    val client = buildGoogleAuthClient()
    val creds = try {
        client.getToken(req.query["code"].unsafeCast<String>()).await().tokens
    } catch (e: Throwable) {
        console.log(e)
        res.status(401).send(e.asDynamic().response.data)
        return
    }

    admin.firestore()
            .collection("credentials")
            .doc(req.query["state"].unsafeCast<String>())
            .set(json("gsuite" to creds), SetOptions.merge)
            .await()
    res.redirect("/")
}

private suspend fun fetchSlackAccessToken(req: Request<Any?>, res: Response<Any?>) {
    val result = superagent.get("https://slack.com/api/oauth.v2.access")
            .query(json(
                    "code" to req.query["code"],
                    "client_id" to functions.config().slack.client_id,
                    "client_secret" to functions.config().slack.client_secret
            )).await()

    if (result.body["ok"].unsafeCast<Boolean>()) {
        admin.firestore()
                .collection("credentials")
                .doc(req.query["state"].unsafeCast<String>())
                .set(json("slack" to result.body), SetOptions.merge)
                .await()
        res.redirect("/")
    } else {
        console.log(result.body)
        res.status(401).send(result.body)
    }
}
