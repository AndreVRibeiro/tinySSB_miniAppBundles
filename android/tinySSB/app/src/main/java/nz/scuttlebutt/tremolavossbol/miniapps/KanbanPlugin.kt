package nz.scuttlebutt.tremolavossbol.miniapps

import android.util.Base64
import android.util.Log
import android.webkit.WebView
import nz.scuttlebutt.tremolavossbol.MainActivity
import nz.scuttlebutt.tremolavossbol.MiniAppPlugin
import nz.scuttlebutt.tremolavossbol.utils.Bipf

// Define a constant for the Kanban app within the plugin
val TINYSSB_APP_KANBAN       = Bipf.mkString("KAN") // ...

/**
 * KanbanPlugin is a subclass of MiniAppPlugin that provides functionality specific to the Kanban board mini-app.
 * This class handles the initialization of Kanban-related UI elements, manages requests from the web view,
 * and interacts with the Kanban board by publishing updates and handling various operations.
 */
class KanbanPlugin(act: MainActivity, webView: WebView) : MiniAppPlugin(act, webView) {

    /**
     * Initializes the Kanban plugin by injecting CSS, JavaScript, and HTML content into the WebView.
     * Also sets up the UI elements and configurations specific to the Kanban board.
     */
    override fun initialize() {
        // Define the CSS styles for the Kanban board UI
        val css = """
            .board_item_button {
              border: none;
              text-align: left;
              vertical-align: top;
              height: 3em;
              font-size: medium;
              border-radius: 4pt;
              box-shadow: 0 0 5px rgba(0,0,0,0.7);
              /*background-color: #c1e1c1;*/
            }
            
            .columns_container {
              position: relative;
              width: 100%;
              /*height: 100%;*/
              display: flex;
              flex-direction: row;
              justify-content: flex-start;
              gap: 10px;
            }
            
            .column {
              display: flex;
              justify-content: flex-start;
              flex-direction: column;
              border-radius: 5pt;
            }
            
            .column_wrapper{
              flex: 0 0 36vw;
              width: 36vw;
            }
            
            .column_content{
              display: flex;
              justify-content: flex-start;
              flex-direction: column;
              gap: 10px;
              padding-bottom: 10px;
              padding-top: 10px;
            }
            
            .column_hdr {
              width: 100%;
              margin: 0 auto;
              padding-top:5px;
              padding-bottom:5px;
              overflow-wrap: break-word;
              background-color: #c1e1c1;
              border-bottom-color: gray;
              border-bottom-style: solid;
              border-bottom-width: 2px;
              border-radius: 5pt;
            }
            
            /*
            .column_options{
              display: flex;
              flex-direction: row;
              justify-content: space-between;
            }
            */
            
            .context_options_btn {
              border: none;
              text-align: left;
              height: 3em;
              width: 100%;
              font-size: medium;
              background-color: white;
            }
            
            .context_menu {
              display: none;
              width: 10%;
              max-height: 80vh;
              position: absolute;
              background-color: #f9f9f9;
              min-width: 160px;
              box-shadow: 0px 8px 16px 0px rgba(0,0,0,0.2);
              z-index: 1001;
              overflow-x: hidden;
              overflow-y:scroll;
            }
            
            .column_item {
              width:95%;
              margin: auto;
              font-size: medium;
              border-radius: 4pt;
              box-shadow: 0 0 5px rgb(0 0 0 / 70%);
              min-height: 3em;
            }
            
            .item_button {
              width:100%;
              border: none;
              text-align: left;
              vertical-align: top;
              font-size: medium;
              border-radius: 4pt;
              box-shadow: 0 0 5px rgba(0,0,0,0.7);
              white-space: normal;
              word-wrap: break-word;
              min-height: 3em;
            }
            
            .item_menu_content {
              padding-top: 10px;
              float: left;
              width: 70%;
            }
            
            .item_menu_desc {
              margin-top: 10px;
              border: none;
              outline: none;
              background-color: rgb(211,211,211);
            }
            
            .item_menu_buttons{
              padding-top: 10px;
              float: right;
              width: 30%;
            }
            
            .item_menu_button {
              width: 90%;
              float: right;
              margin:5px auto;
              display:block;
            }
            
            .div:item_menu_assignees_container {
              padding-top: 5px;
              width: 100%;
              display: flex;
              justify-content: flex-start;
              flex-direction: column;
            }
            
            .column_footer {
              width: 100%;
              padding-top: 5px;
              padding-bottom: 5px;
              border-top-color: gray;
              border-top-style: solid;
              border-top-width: 2px;
            }
           
            .kanban_invitation_container {
              display: grid;
              grid-template-columns: 1fr 1fr;
              grid-template-rows: 1fr;
              gap: 0px 0px;
              grid-template-areas:
                "text btns";
              width:100%;
              box-shadow: 0 0 5px rgba(0,0,0,0.7);
              border-radius: 4pt;
              height: 3em;
              margin-top: 5px;
            }
            
            .kanban_invitation_text_container {
              display: grid;
              grid-template-columns: 1fr;
              grid-template-rows: 1fr 1fr;
              gap: 0px 0px;
              grid-template-areas:
                "name"
                "author";
              grid-area: text;
            }
            
            .kanban_create_personal_btn {
              background: none;
              border: none;
              cursor: pointer;
              font-size: 16px;
              margin: 0 10px;
              background-color: #51a4d2;
              background-size: contain;
              background-repeat: no-repeat;
              background-position: center;
              height: 40px;
              width: 35px
            }
        """.trimIndent()

        val boardScript = "miniApps/board/src/board.js"
        val boardUIScript = "miniApps/board/src/board_ui.js"

        val scriptPaths = arrayOf(boardScript, boardUIScript)

        val kanbanHtmlContent = """     
            <div id='lst:kanban' class=w100 style="overflow: hidden; display: none;"></div>
            
            <div id='kanban-create-personal-board-overlay' class='qr-overlay'>
              <div style="display:flex; justify-content: center;align-items: center; flex-direction: column; gap: 20px">
                <div style="text-align: center">
                  <b>Would you like to create your own personal kanban board?</b> <br>
                  (You can also generate new kanban boards using the + icon)
                </div>
                <div>
                  <button class="kanban_create_personal_btn" style="background-image: url('img/checked.svg');" onclick="btn_create_personal_board_accept()"></button>
                  <button class="kanban_create_personal_btn" style="background-image: url('img/cancel.svg'); background-position-x: -1.5px;" onclick="btn_create_personal_board_decline()"></button>
                </div>
              </div>
            </div>

            <div id='kanban-invitations-overlay' class='qr-overlay'>
              <div style="text-align: center;font-size:20px;">
                Invitations
              </div>
              <div id='kanban_invitations_list' style="padding-top: 15px;">
              </div>
            </div>
        """.trimIndent()

        val boardHtmlContent = """ 
            <div id="div:board" style="display: none; height: calc(100% - 10px); margin-top: 10px;">

              <div id='div:debug' class="qr-overlay">
                <textarea rows="20" id="txt:debug"></textarea>
              </div>

              <div id="div:board_ui" style="height: 100%; overflow-x: scroll;overflow-y: hidden;">
                <div class='columns_container' id='div:columns_container'>
                  <div class='column_wrapper' style='order: 100000;'>
                  </div>
                </div>
              </div>

              <div id="div:menu_history" class="qr-overlay">
                <div class="menu_history_hdr" style="overflow:auto;padding-bottom: 10px;">
                  <b style="float: left;">History</b>
                </div>
                <div>
                  <label for='history_sort_select'>Sort by:</label>
                  <select id='history_sort_select' onchange="history_sort_select(this)">
                    <option value='latest_first'>Latest event first</option>
                    <option value='oldest_first'>Oldest event first</option>
                  </select>
                </div>
                <div id="menu_history_content" style="overflow-x: hidden;overflow-y: scroll;max-height: 70vh;display: flex;flex-direction: column;margin-top: 10px;"></div>
              </div>

              <div id="div:invite_menu" class="qr-overlay">
                <div id="menu_invite_hdr">
                  <b>Invite Users</b>
                </div>
        
                <div id="menu_invite_content" style="overflow-x: hidden;overflow-y: scroll;max-height: 70vh;"></div>
              </div>

              <div id="div:item_menu" class="qr-overlay">
                <div style="padding-bottom:20px;" onclick="close_board_context_menu()">
                  <div id="item_menu_title" style="text-align: center;"></div>
                </div>
        
                <div id="div:item_menu_content" class="item_menu_content" onclick="close_board_context_menu()">
                  <div id="div:item_menu_description">
                    <b style="margin-top:10px;">Description:</b>
                    <textarea class='item_menu_desc' id='div:item_menu_description_text' rows='8'></textarea>
                    <button id='btn:item_menu_description_save' style="display:none;" onclick="item_menu_save_description()">Save</button>
                    <button id='btn:item_menu_description_cancel' style="display:none;" onclick="item_menu_cancel_description()">Cancel</button>
                  </div>
        
                  <div style="padding-top: 10px; padding-bottom: 20px" onclick="close_board_context_menu()">
                    <b style="margin-top:10px;">Assignees:</b>
                    <div id="div:item_menu_assignees" class="div:item_menu_assignees_container"></div>
                  </div>
        
                  <div style="padding-top: 40px">
                    <b>Comments:</b>
                    <div id="div:item_menu_comments" style="overflow:auto;">
                      <textarea id="item_menu_comment_text" placeholder="Add comment..." style="float:left;width:84%;height: 50px;"></textarea>
                      <button class="flat passive buttontext" style="background-image: url('img/send.svg');float:right;width: 15%;" onclick="btn_post_comment()"></button>
                    </div>
        
                    <div id="lst:item_menu_posts" style="max-height: 33vh;overflow-x: hidden;overflow-y:scroll;">
                    </div>
        
                  </div>
                </div>

                <div class="item_menu_buttons">

                  <button class="item_menu_button" onclick="menu_edit('board_rename_item', 'Enter New Name', '')">rename card</button>

                  <!-- div class='dropdown_menu'>
                    <button id='btn:item_menu_change_column' class="item_menu_button"  onclick="contextmenu_change_column()">Change Column</button>
                    <div id="change_column_options">
                    </div>
                  </div -->

                  <!-- div class='dropdown_menu' id="item_menu_change_position">
                    <button id='btn:item_menu_change_position' class="item_menu_button"  onclick="contextmenu_item_change_position()">Change Position</button>
                    <div id="change_position_options">
                    </div>
                  </div -->

                  <div class='dropdown_menu' id="item_menu_assign">
                    <button id='btn:item_menu_assign' class="item_menu_button"  onclick="contextmenu_item_assign()">(un-) assign members</button>
                    <div id="assign_options">
                    </div>
                  </div>

                  <div class='dropdown_menu'>
                    <button id='btn:item_menu_change_color' class="item_menu_button"  onclick="contextmenu_change_color()">set name color</button>
                    <div id="change_color_options">
                    </div>
                  </div>

                  <button class="item_menu_button" onclick="btn_remove_item()">delete Card</button>

                </div>
              </div>
            </div>
        """.trimIndent()

        val htmlContents = arrayOf(kanbanHtmlContent, boardHtmlContent)

        // Define displayOrNot list for Kanban
        val displayOrNot = listOf(
            "lst:kanban",
            "div:board"
        )

        // Define scenarioDisplay map for Kanban
        val scenarioDisplay = mapOf(
            "kanban" to listOf("div:back", "core", "lst:kanban", "plus"),
            "board" to listOf("div:back", "core", "div:board")
        )

        // Define scenarioMenu map for Kanban
        val scenarioMenu = mapOf(
            "kanban" to listOf(
                "New Kanban board" to "menu_new_board",
                "Invitations" to "menu_board_invitations",
                "Connected Devices" to "menu_connection",
                "Settings" to "menu_settings",
                "About" to "menu_about"
            ),
            "board" to listOf(
                "Add list" to "menu_new_column",
                "Rename Kanban Board" to "menu_rename_board",
                "Invite Users" to "menu_invite",
                "History" to "menu_history",
                "Reload" to "reload_curr_board",
                "Leave" to "leave_curr_board",
                "(un)Forget" to "board_toggle_forget",
                "Debug" to "ui_debug"
            )
        )

        val manifestPath = "web/miniApps/board/manifest.json"

        injectAll(css, scriptPaths, htmlContents, displayOrNot, scenarioDisplay, scenarioMenu, manifestPath)
    }

    /**
     * Handles requests from the web view. Manages different types of actions based on the request type.
     */
    override fun handleRequest(args: List<String>) {
        Log.d("Inside the Kanban handleRequest", args[0])
        when(args[0]) {
            "kanban" -> {
                // Kanban-specific logic
                val bid: String? = if (args[1] != "null") args[1] else null
                val prev: List<String>? = if (args[2] != "null") Base64.decode(args[2], Base64.NO_WRAP).decodeToString().split(",").map{ Base64.decode(it, Base64.NO_WRAP).decodeToString()} else null
                val op: String = args[3]
                val argsList: List<String>? = if(args[4] != "null") Base64.decode(args[4], Base64.NO_WRAP).decodeToString().split(",").map{ Base64.decode(it, Base64.NO_WRAP).decodeToString()} else null

                kanban(bid, prev , op, argsList)
            }
            "backPressed" -> {
                val jsCode = """
                    if (curr_scenario == 'board') {
                        setKanbanScenario('kanban');
                    } else if (curr_scenario == 'kanban'){
                        setScenario('miniapps'); 
                    }
                """.trimIndent()

                webView.post(Runnable {
                    webView.evaluateJavascript(jsCode, null)
                })
            }
            "kanban:members_confirmed" -> {
                webView.post(Runnable {
                    webView.evaluateJavascript("menu_new_board_name()", null)
                })
            }
            "kanban:plus_button" -> {
                webView.post(Runnable {
                    webView.evaluateJavascript("menu_new_board()", null)
                })
            }
            "edit_confirmed" -> {
                webView.post(Runnable {
                    webView.evaluateJavascript("kanban_edit_confirmed()", null)
                })
            }
            "b2f_initialize" -> {
                webView.post(Runnable {
                    webView.evaluateJavascript("load_board_list()", null)
                })
            }
            "b2f_new_event" -> {
                webView.post(Runnable {
                    webView.evaluateJavascript("load_board_list()", null)
                })
            }
        }
    }

    /**
     * Executes Kanban-specific operations based on the provided arguments. This includes
     * publishing content to the Kanban board with operations and arguments.
     *
     * @param bid The board ID.
     * @param prev The list of previous board states.
     * @param operation The operation to perform (e.g., create, update).
     * @param args Additional arguments for the operation.
     */
    fun kanban(bid: String?, prev: List<String>?, operation: String, args: List<String>?) {
        val lst = Bipf.mkList()
        Bipf.list_append(lst, TINYSSB_APP_KANBAN)
        if (bid != null)
            Bipf.list_append(lst, Bipf.mkBytes(Base64.decode(bid, Base64.NO_WRAP)))
        else
            Bipf.list_append(lst, Bipf.mkNone())

        if(prev != null) {
            val prevList = Bipf.mkList()
            for(p in prev) {
                Bipf.list_append(prevList, Bipf.mkBytes(Base64.decode(p, Base64.NO_WRAP)))
            }
            Bipf.list_append(lst, prevList)
        } else {
            Bipf.list_append(lst, Bipf.mkString("null"))  // TODO: Change to Bipf.mkNone(), but would be incompatible with the old format
        }

        Bipf.list_append(lst, Bipf.mkString(operation))

        if(args != null) {
            for(arg in args) {
                if (Regex("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?\$").matches(arg)) {
                    Bipf.list_append(lst, Bipf.mkBytes(Base64.decode(arg, Base64.NO_WRAP)))
                } else { // arg is not a b64 string
                    Bipf.list_append(lst, Bipf.mkString(arg))
                }
            }
        }

        val body = Bipf.encode(lst)

        if (body != null) {
            Log.d("kanban", "published bytes: " + Bipf.decode(body))
            act.tinyNode.publish_public_content(body)
        }
        //val body = Bipf.encode(lst)
        //Log.d("KANBAN BIPF ENCODE", Bipf.bipf_list2JSON(Bipf.decode(body!!)!!).toString())
        //if (body != null)
        //act.tinyNode.publish_public_content(body)

    }

}