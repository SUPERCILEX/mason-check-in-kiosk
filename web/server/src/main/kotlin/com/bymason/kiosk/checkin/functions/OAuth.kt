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
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json

fun handleGSuiteAuth(req: Request<Any?>, res: Response<Any?>): Promise<*>? {
    if (!checkForError(req, res)) return null
    return GlobalScope.async { fetchGSuiteAccessToken(req, res) }.asPromise()
}

fun handleDocusignAuth(req: Request<Any?>, res: Response<Any?>): Promise<*>? {
    if (!checkForError(req, res)) return null
    return GlobalScope.async { fetchDocusignAccessToken(req, res) }.asPromise()
}

fun handleSlackAuth(req: Request<Any?>, res: Response<Any?>): Promise<*>? {
    if (!checkForError(req, res)) return null
    return GlobalScope.async { fetchSlackAccessToken(req, res) }.asPromise()
}

suspend fun refreshDocusignCreds(uid: String, creds: Json): Json {
    val credsResult = superagent.post("https://account-d.docusign.com/oauth/token")
            .set(json("Authorization" to "Basic ${functions.config().docusign.client_hash}"))
            .query(json(
                    "grant_type" to "refresh_token",
                    "refresh_token" to creds["refresh_token"]
            )).await()

    admin.firestore()
            .collection("config")
            .doc(uid)
            .collection("credentials")
            .doc("docusign")
            .set(credsResult.body, SetOptions.merge)

    return credsResult.body
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
        val error = e.asDynamic().response.data
        console.log(error)
        res.status(401).send(error)
        return
    }

    admin.firestore()
            .collection("config")
            .doc(req.query["state"].unsafeCast<String>())
            .collection("credentials")
            .doc("gsuite")
            .set(creds.unsafeCast<Json>(), SetOptions.merge)
            .await()
    res.redirect("/")
}

private suspend fun fetchDocusignAccessToken(req: Request<Any?>, res: Response<Any?>) {
    val credsResult = try {
        superagent.post("https://account-d.docusign.com/oauth/token")
                .set(json("Authorization" to "Basic ${functions.config().docusign.client_hash}"))
                .query(json(
                        "grant_type" to "authorization_code",
                        "code" to req.query["code"]
                )).await()
    } catch (e: Throwable) {
        val error = e.asDynamic().response.text
        console.log(error)
        res.status(401).send(error)
        return
    }

    val accountResult = try {
        superagent.get("https://account-d.docusign.com/oauth/userinfo")
                .set(json("Authorization" to "Bearer ${credsResult.body["access_token"]}"))
                .await()
    } catch (e: Throwable) {
        val error = e.asDynamic().response.text
        console.log(error)
        res.status(401).send(error)
        return
    }

    val ref = admin.firestore()
            .collection("config")
            .doc(req.query["state"].unsafeCast<String>())
            .collection("credentials")
            .doc("docusign")
    ref.set(accountResult.body, SetOptions.merge).await()
    ref.set(credsResult.body, SetOptions.merge).await()
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
                .collection("config")
                .doc(req.query["state"].unsafeCast<String>())
                .collection("credentials")
                .doc("slack")
                .set(result.body, SetOptions.merge)
                .await()
        res.redirect("/")
    } else {
        console.log(result.body)
        res.status(401).send(result.body)
    }
}
