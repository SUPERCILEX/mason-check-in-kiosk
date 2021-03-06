package support.bymason.kiosk.checkin.functions

import firebase.functions.AuthContext
import firebase.https.HttpsError
import google.google
import kotlinx.coroutines.await
import support.bymason.kiosk.checkin.utils.getAndInitCreds
import support.bymason.kiosk.checkin.utils.installGoogleAuth
import support.bymason.kiosk.checkin.utils.maybeRefreshGsuiteCreds
import kotlin.js.Json
import kotlin.js.json

suspend fun findHosts(auth: AuthContext, data: Json): Array<Json> {
    val hostName = data["query"] as? String
    console.log("Searching for host '$hostName' as user '${auth.uid}'")

    if (hostName == null) {
        throw HttpsError("invalid-argument")
    }

    return findHosts(auth, hostName)
}

private suspend fun findHosts(auth: AuthContext, hostName: String): Array<Json> {
    val creds = getAndInitCreds(auth.uid, "gsuite")
    val state = installGoogleAuth(creds.getValue("gsuite"))
    val directory = google.admin(json("version" to "directory_v1"))
    val hosts = directory.users.list(json(
            "customer" to "my_customer",
            "viewType" to "domain_public",
            "query" to hostName
    )).await().data
    maybeRefreshGsuiteCreds(auth.uid, state)
    console.log("Hosts: ", JSON.stringify(hosts.users))

    return hosts.users.orEmpty().map {
        val emailHash = js("require('md5')")(it.primaryEmail?.toLowerCase())
        val fallbackPic = if (it.thumbnailPhotoUrl.isNullOrBlank()) {
            "https://ssl.gstatic.com/s2/profiles/images/silhouette200.png"
        } else {
            it.thumbnailPhotoUrl
        }
        val profilePic = "https://www.gravatar.com/avatar/$emailHash?default=$fallbackPic"

        json(
                "id" to it.id,
                "name" to it.name?.fullName,
                "photoUrl" to profilePic
        )
    }.toTypedArray()
}
