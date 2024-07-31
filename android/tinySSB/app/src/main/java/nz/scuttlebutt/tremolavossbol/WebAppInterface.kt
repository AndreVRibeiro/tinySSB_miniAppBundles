package nz.scuttlebutt.tremolavossbol

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.core.content.ContextCompat.checkSelfPermission
import com.google.zxing.integration.android.IntentIntegrator
import org.json.JSONObject


import nz.scuttlebutt.tremolavossbol.utils.Bipf
import nz.scuttlebutt.tremolavossbol.utils.Bipf.Companion.BIPF_LIST
import nz.scuttlebutt.tremolavossbol.utils.Constants.Companion.TINYSSB_APP_IAM
import nz.scuttlebutt.tremolavossbol.utils.Constants.Companion.TINYSSB_APP_TEXTANDVOICE
import nz.scuttlebutt.tremolavossbol.utils.HelperFunctions.Companion.toBase64
import nz.scuttlebutt.tremolavossbol.utils.HelperFunctions.Companion.toHex
import org.json.JSONArray
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


// pt 3 in https://betterprogramming.pub/5-android-webview-secrets-you-probably-didnt-know-b23f8a8b5a0c

class WebAppInterface(val act: MainActivity, val webView: WebView) {

    var frontend_ready = false
    val frontend_frontier = act.getSharedPreferences("frontend_frontier", Context.MODE_PRIVATE)

    @JavascriptInterface
    fun onFrontendRequest(s: String) {
        //handle the data captured from webview}
        Log.d("FrontendRequest", s)
        val args = s.split(" ")

        // Allow plugins to handle the frontend requests
        for (plugin in (act as MainActivity).plugins) {
            plugin.handleRequest(args)
        }

        when (args[0]) {
            "onBackPressed" -> {
                (act as MainActivity)._onBackPressed()
            }
            "ready" -> {
                eval("b2f_initialize('${act.idStore.identity.toRef()}', '${act.settings!!.getSettings()}')")
                frontend_ready = true
                act.tinyRepo.addNumberOfPendingChunks(0) // initialize chunk progress bar
                act.tinyNode.beacon()
            }
            "reset" -> { // UI reset
                // erase DB content
                eval("b2f_initialize(\"${act.idStore.identity.toRef()}\")")
                onFrontendRequest("restream")
            }
            "restream" -> {
                eval("restream = true")
                for (fid in act.tinyRepo.listFeeds()) {
                    Log.d("wai", "restreaming ${fid.toHex()}")
                    var i = 1
                    while (true) {
                        val r = act.tinyRepo.fid2replica(fid)
                        if(r == null)
                            break
                        val payload = r.read_content(i)
                        val mid = r.get_mid(i)
                        if (payload == null || mid == null) break
                        Log.d("restream", "${i}, ${payload.size} Bytes")
                        sendTinyEventToFrontend(fid, i, mid, payload)
                        i++
                    }
                }
                eval("restream = false")
            }
            "wipe:others" -> {
                for (fid in act.tinyRepo.listFeeds()) {
                    if (fid.contentEquals(act.idStore.identity.verifyKey))
                        continue
                    act.tinyRepo.delete_feed(fid)
                }
            }
            "qrscan.init" -> {
                val intentIntegrator = IntentIntegrator(act)
                intentIntegrator.setBeepEnabled(false)
                intentIntegrator.setCameraId(0)
                intentIntegrator.setPrompt("SCAN")
                intentIntegrator.setBarcodeImageEnabled(false)
                intentIntegrator.initiateScan()
                return
            }
            "exportSecret" -> {
                val json = act.idStore.identity.toExportString()!!
                eval("b2f_showSecret('${json}');")
                val clipboard = act.getSystemService(ClipboardManager::class.java)
                val clip = ClipData.newPlainText("simple text", json)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(act, "secret key was also\ncopied to clipboard",
                    Toast.LENGTH_LONG).show()
            }
            "importSecret" -> {
                act.idStore.setNewIdentity(Base64.decode(args[1], Base64.NO_WRAP))
                act.tinyRepo.reset()

                // restart App
                if (act.websocket != null)
                    act.websocket!!.stop()
                if (act.ble != null)
                    act.ble!!.stopBluetooth()
                val ctx = act.applicationContext
                ctx.startActivity(Intent.makeRestartActivityTask(act.applicationContext.packageManager.getLaunchIntentForPackage(ctx.packageName)!!.component))
                Runtime.getRuntime().exit(0)
            }
            "wipe" -> {
                act.settings!!.resetToDefault()
                act.idStore.setNewIdentity(null) // creates new identity
                act.tinyRepo.reset()

                if (act.websocket != null)
                    act.websocket!!.stop()
                if (act.ble != null)
                    act.ble!!.stopBluetooth()
                val ctx = act.applicationContext
                ctx.startActivity(Intent.makeRestartActivityTask(act.applicationContext.packageManager.getLaunchIntentForPackage(ctx.packageName)!!.component))
                Runtime.getRuntime().exit(0)

                // eval("b2f_initialize(\"${tremolaState.idStore.identity.toRef()}\")")
                // FIXME: should kill all active connections, or better then the app
                //act.finishAffinity()
            }
            "add:contact" -> {

                val id = args[1].substring(1,args[1].length-8)
                Log.d("ADD", id)
                act.tinyGoset._add_key(Base64.decode(id, Base64.NO_WRAP))
            }
            /* no alias publishing in tinyTremola
            "add:contact" -> { // ID and alias
                tremolaState.addContact(args[1],
                    Base64.decode(args[2], Base64.NO_WRAP).decodeToString())
                val rawStr = tremolaState.msgTypes.mkFollow(args[1])
                val evnt = tremolaState.msgTypes.jsonToLogEntry(rawStr,
                    rawStr.encodeToByteArray())
                evnt?.let {
                    rx_event(it) // persist it, propagate horizontally and also up
                    tremolaState.peers.newContact(args[1]) // inform online peers via EBT
                }
                    return
            }
            */
            "publ:post" -> { // publ:post tips txt voice
                val a = JSONArray(args[1])
                val tips = ArrayList<String>(0)
                for (i in 0..a.length()-1) {
                    val s = (a[i] as JSONObject).toString()
                    Log.d("publ:post", s)
                    tips.add(s)
                }
                var t: String? = null
                if (args[2] != "null")
                    t = Base64.decode(args[2], Base64.NO_WRAP).decodeToString()
                var v: ByteArray? = null
                if (args.size > 3 && args[3] != "null")
                    v = Base64.decode(args[3], Base64.NO_WRAP)
                public_post_with_voice(tips, t, v)
                return
            }
            "priv:post" -> { // priv:post tips atob(text) atob(voice) rcp1 rcp2 ...
                val a = JSONObject(args[1]) as JSONArray
                val tips = ArrayList<String>(0)
                for (i in 0..a.length()-1) {
                    val s = (a[i] as JSONObject).toString()
                    Log.d("priv;post", s)
                    tips.add(s)
                }
                var t: String? = null
                if (args[2] != "null")
                    t = Base64.decode(args[2], Base64.NO_WRAP).decodeToString()
                var v: ByteArray? = null
                if (args.size > 3 && args[3] != "null")
                    v = Base64.decode(args[3], Base64.NO_WRAP)
                private_post_with_voice(tips, t, v, args.slice(4..args.lastIndex))
                return
            }
            "get:media" -> {
                if (checkSelfPermission(act, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(act, "No permission to access media files",
                        Toast.LENGTH_SHORT).show()
                    return
                }
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT); // , MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.type = "image/*"
                act.startActivityForResult(intent, 1001)
            }

            "get:voice" -> { // get:voice
                val intent = Intent(act, RecordActivity::class.java)
                act.startActivityForResult(intent, 808)
                return
            }
            "play:voice" -> { // play:voice b64enc(codec2) from date)
                Log.d("wai", s)
                val voice = Base64.decode(args[1], Base64.NO_WRAP)
                val intent = Intent(act, PlayActivity::class.java)
                intent.putExtra("c2Data", voice)
                if (args.size > 2)
                    intent.putExtra("from", Base64.decode(args[2], Base64.NO_WRAP).decodeToString())
                if (args.size > 3)
                    intent.putExtra("date", Base64.decode(args[3], Base64.NO_WRAP).decodeToString())
                act.startActivity(intent)
                return
            }
            "iam" -> {
                val new_alias = Base64.decode(args[1], Base64.NO_WRAP).decodeToString()
                val lst = Bipf.mkList()
                Bipf.list_append(lst, TINYSSB_APP_IAM)
                Bipf.list_append(lst, Bipf.mkString(new_alias))

                val body = Bipf.encode(lst)
                if (body != null) {
                    act.tinyNode.publish_public_content(body)
                }
            }
            // Not needed anymore but kept for reference
            "writeManifestPaths" -> {
                //Log.d("HERE", "i am here!!!!")
                sendManifestPathsToFrontend()
            }
            // Not needed anymore but kept for reference
            "getManifestData" -> {
                if (args.size > 1) {
                    val path = args[1]
                    readManifestFile(path)
                }
            }
            "settings:set" -> {
                act.settings!!.set(args[1], args[2])
            }
            "settings:get" -> {
                val settings = act.settings!!.getSettings()
                act.wai.eval("b2f_get_settings('${settings}')")
            }
            else -> {
                Log.d("onFrontendRequest", "unknown")
            }
        }
    }

    fun eval(js: String) { // send JS string to webkit frontend for execution
        webView.post(Runnable {
            webView.evaluateJavascript(js, null)
        })
    }

    private fun importIdentity(secret: String): Boolean {
        Log.d("D/importIdentity", secret)
        if (act.idStore.setNewIdentity(Base64.decode(secret, Base64.DEFAULT))) {
            // FIXME: remove all decrypted content in the database, try to decode new one
            Toast.makeText(act, "Imported of ID worked. You must restart the app.",
                Toast.LENGTH_SHORT).show()
            return true
        }
        Toast.makeText(act, "Import of new ID failed.", Toast.LENGTH_LONG).show()
        return false
    }

    /**
     * The following function was part of the initial approach for retrieving the manifest
     * paths and sending them to the frontend and ultimately help creating a button for
     * the mini App menu. It has been replaced by a more efficient method but is kept here
     * for reference. If not needed, consider removing this section entirely to clean up the
     * codebase.
    */
    fun sendManifestPathsToFrontend() {

        //Log.d("INSIDE", "i am inside the function")
        // Access the assets directory (only read)
        val assetManager = act.assets
        val manifestFilePaths = mutableListOf<String>()

        try {
            // List all directories in the "miniApps" directory
            val miniAppFolders = assetManager.list("web/miniApps") ?: arrayOf()
            for (folder in miniAppFolders) {

                // Construct the path to each manifest.jon file
                val manifestPath = "web/miniApps/$folder/manifest.json"
                try {
                    val inputStream = assetManager.open(manifestPath)
                    inputStream.close()
                    manifestFilePaths.add(manifestPath)
                } catch (e: IOException) {
                    Log.e("Manifest", "Manifest file not found: $manifestPath")
                }
            }
        } catch (e: IOException) {
            Log.e("Manifest", "Error accessing assets", e)
            return
        }

        // Convert list of paths to a JSON array and pass to JavaScript frontend
        val jsonArray = JSONArray(manifestFilePaths)
        Log.d("Path", jsonArray.toString())

        eval("handleManifestPaths('${jsonArray.toString()}')")
    }

    /**
     * The following function was also part of the initial approach for passing the content
     * of the manifest file to the frontend and ultimately help creating a button for
     * the mini App menu. It has been replaced by a more efficient method but is kept here
     * for reference. If not needed, consider removing this section entirely to clean up the
     * codebase.
     */
    fun readManifestFile(path: String) {
        try {
            val assetManager = act.assets
            val inputStream = assetManager.open(path)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val content = reader.readText()
            reader.close()

            val quotedContent = JSONObject.quote(content)
            // Send the content to the frontend
            eval("handleManifestContent($quotedContent)")
        } catch (e: IOException) {
            Log.e("AssetError", "Error reading asset file", e)
        }
    }

    fun public_post_with_voice(tips: ArrayList<String>, text: String?, voice: ByteArray?) {
        if (text != null)
            Log.d("wai", "post_voice t- ${text}/${text.length}")
        if (voice != null)
            Log.d("wai", "post_voice v- ${voice}/${voice.size}")
        val lst = Bipf.mkList()
        Bipf.list_append(lst, TINYSSB_APP_TEXTANDVOICE)
        // add tips
        Bipf.list_append(lst, if (text == null) Bipf.mkNone() else Bipf.mkString(text))
        Bipf.list_append(lst, if (voice == null) Bipf.mkNone() else Bipf.mkBytes(voice))
        val tst = Bipf.mkInt((System.currentTimeMillis() / 1000).toInt())
        Log.d("wai", "send time is ${tst.getInt()}")
        Bipf.list_append(lst, tst)
        val body = Bipf.encode(lst)
        if (body != null)
            act.tinyNode.publish_public_content(body)
    }

    fun private_post_with_voice(tips: ArrayList<String>, text: String?, voice: ByteArray?, rcps: List<String>) {
        if (text != null)
            Log.d("wai", "post_voice t- ${text}/${text.length}")
        if (voice != null)
            Log.d("wai", "post_voice v- ${voice}/${voice.size}")
        val lst = Bipf.mkList()
        Bipf.list_append(lst, TINYSSB_APP_TEXTANDVOICE)
        // add tips
        Bipf.list_append(lst, if (text == null) Bipf.mkNone() else Bipf.mkString(text))
        Bipf.list_append(lst, if (voice == null) Bipf.mkNone() else Bipf.mkBytes(voice))
        val tst = Bipf.mkInt((System.currentTimeMillis() / 1000).toInt())
        Log.d("wai", "send time is ${tst.getInt()}")
        Bipf.list_append(lst, tst)
        val body = Bipf.encode(lst)
        if (body != null)
            act.tinyNode.publish_public_content(body)
    }

    fun return_voice(voice: ByteArray) {
        var cmd = "b2f_new_voice('" + voice.toBase64() + "');"
        Log.d("CMD", cmd)
        eval(cmd)
    }

    fun sendIncompleteEntryToFrontend(fid: ByteArray, seq: Int, mid:ByteArray, body: ByteArray) {
        val e = toFrontendObject(fid, seq, mid, body)
        if (e != null)
            eval("b2f_new_incomplete_event($e)")

    }

    fun sendTinyEventToFrontend(fid: ByteArray, seq: Int, mid:ByteArray, body: ByteArray) {
        Log.d("wai","sendTinyEvent ${body.toHex()}")
        var e = toFrontendObject(fid, seq, mid, body)
        if (e != null)
            eval("b2f_new_event($e)")

        // in-order api
        val replica = act.tinyRepo.fid2replica(fid)

        if (frontend_frontier.getInt(fid.toHex(), 1) == seq && replica != null) {
            for (i in seq .. replica.state.max_seq ) {
                val content = replica.read_content(i)
                val message_id= replica.get_mid(seq)
                if(content == null || message_id == null || !replica.isSidechainComplete(i))
                    break
                e = toFrontendObject(fid, i, message_id, content)
                if (e != null)
                    eval("b2f_new_in_order_event($e)")
                frontend_frontier.edit().putInt(fid.toHex(), i + 1).apply()
            }
        }
    }

    fun toFrontendObject(fid: ByteArray, seq: Int, mid: ByteArray, payload: ByteArray): String? {
        val bodyList = Bipf.decode(payload)
        if (bodyList == null || bodyList.typ != BIPF_LIST) {
            Log.d("toFrontendObject", "decoded payload == null")
            return null
        }
        val param = Bipf.bipf_list2JSON(bodyList)
        var hdr = JSONObject()
        hdr.put("fid", "@" + fid.toBase64() + ".ed25519")
        hdr.put("ref", mid.toBase64())
        hdr.put("seq", seq)
        return "{header:${hdr.toString()}, public:${param.toString()}}"
    }


}
