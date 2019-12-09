package com.bymason.kiosk.checkin.functions

import com.bymason.kiosk.checkin.utils.createInstance
import com.bymason.kiosk.checkin.utils.getAndInitCreds
import com.bymason.kiosk.checkin.utils.refreshDocusignCreds
import firebase.firestore.DocumentSnapshot
import firebase.functions.AuthContext
import firebase.functions.admin
import firebase.https.CallableContext
import firebase.https.HttpsError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asPromise
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json

fun generateNdaLink(data: Json, context: CallableContext): Promise<*>? {
    val auth = context.auth ?: throw HttpsError("unauthenticated")
    val guestName = data["guestName"] as? String
    val guestEmail = data["guestEmail"] as? String
    console.log("Generating DocuSign NDA for guest $guestName ($guestEmail)")

    if (guestName == null || guestEmail == null) {
        throw HttpsError("invalid-argument")
    }

    return GlobalScope.async { generateNdaLink(auth, guestName, guestEmail) }.asPromise()
}

private suspend fun generateNdaLink(auth: AuthContext, name: String, email: String): String {
    val creds = getAndInitCreds(auth.uid, "docusign")
    val metadataRef = admin.firestore()
            .collection("config")
            .doc(auth.uid)
            .collection("metadata")
    val companyName = metadataRef.doc("company").get().await().data()["name"]
    val ndaRef = metadataRef.doc("nda").get().await()
    if (!ndaRef.exists) throw HttpsError("not-found", "No NDA found.")
    val ndaBase64 = admin.asDynamic().storage().bucket()
            .file(ndaRef.data()["store"])
            .download()
            .unsafeCast<Promise<dynamic>>()
            .await()[0]
            .toString("base64")

    val docusign = js("require('docusign-esign')")
    val docusignClient: dynamic = createInstance(docusign.ApiClient)
    docusignClient.setBasePath("https://demo.docusign.net/restapi")
    docusignClient.addDefaultHeader(
            "Authorization", "Bearer ${creds.getValue("docusign")["access_token"]}")
    docusign.Configuration.default.setDefaultApiClient(docusignClient)

    val envelope: dynamic = createInstance(docusign.EnvelopeDefinition)
    envelope.status = "sent"
    envelope.emailSubject = "$companyName Nondisclosure Agreement"

    val document = docusign.Document.constructFromObject(json(
            "documentBase64" to ndaBase64,
            "fileExtension" to "pdf",
            "name" to "$companyName Nondisclosure Agreement",
            "documentId" to "1"
    ))
    envelope.documents = arrayOf(document)

    val signer = docusign.Signer.constructFromObject(json(
            "name" to name,
            "email" to email,
            "routingOrder" to "1",
            "recipientId" to "1",
            "clientUserId" to auth.uid
    ))
    signer.tabs = docusign.Tabs.constructFromObject(json(
            "fullNameTabs" to ndaRef.getTabs(docusign, "names"),
            "signHereTabs" to ndaRef.getTabs(docusign, "signatures"),
            "dateSignedTabs" to ndaRef.getTabs(docusign, "dates")
    ))
    envelope.recipients =
            docusign.Recipients.constructFromObject(json("signers" to arrayOf(signer)))

    val accountId = creds.getValue("docusign")["accounts"]
            .unsafeCast<Array<Json>>().first()["account_id"]
    val envelopeApi: dynamic = createInstance(docusign.EnvelopesApi)
    val envelopeResult: dynamic = makeDocusignRequest(
            auth,
            creds.getValue("docusign"),
            docusignClient
    ) { handler ->
        envelopeApi.createEnvelope(
                accountId,
                json("envelopeDefinition" to envelope),
                handler
        )
    }
    val viewResult: dynamic = makeDocusignRequest(
            auth,
            creds.getValue("docusign"),
            docusignClient
    ) { handler ->
        val viewRequest = docusign.RecipientViewRequest.constructFromObject(json(
                "authenticationMethod" to "None",
                "clientUserId" to auth.uid,
                "recipientId" to "1",
                "returnUrl" to "https://mason-check-in-kiosk.firebaseapp.com/redirect/docusign/app",
                "userName" to name,
                "email" to email
        ))
        envelopeApi.createRecipientView(
                accountId,
                envelopeResult.envelopeId,
                json("recipientViewRequest" to viewRequest),
                handler
        )
    }

    console.log("Generated NDA at ${viewResult.url}")
    return viewResult.url
}

private fun DocumentSnapshot.getTabs(
        docusign: dynamic,
        type: String
): Array<dynamic> = data()[type].unsafeCast<Array<Json>>().map { tab ->
    docusign.SignHere.constructFromObject(json(
            "documentId" to "1",
            "recipientId" to "1",
            "pageNumber" to tab["page"],
            "xPosition" to tab["x"],
            "yPosition" to tab["y"]
    ))
}.toTypedArray()

private suspend fun <T> makeDocusignRequest(
        auth: AuthContext,
        creds: Json,
        docusignClient: dynamic,
        block: (handler: dynamic) -> Unit
): T {
    val scope = CoroutineScope(coroutineContext)
    return suspendCoroutine { cont ->
        var hasRetried = false
        lateinit var handler: Any
        handler = fun(error: dynamic, result: dynamic, response: dynamic) {
            if (!hasRetried) {
                hasRetried = true
                if (response.statusCode == 401) {
                    scope.launch {
                        val newCreds = refreshDocusignCreds(auth.uid, creds)
                        docusignClient.addDefaultHeader(
                                "Authorization", "Bearer ${newCreds["access_token"]}")
                        block(handler)
                    }.invokeOnCompletion {
                        it?.let { cont.resumeWithException(it) }
                    }
                    return
                }
            }

            if (error == null) {
                cont.resume(result)
            } else {
                cont.resumeWithException(error)
            }
        }

        block(handler)
    }
}