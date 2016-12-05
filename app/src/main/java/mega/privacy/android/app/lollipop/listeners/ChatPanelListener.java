package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.ContactChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRoom;

public class ChatPanelListener implements View.OnClickListener {

    Context context;
    ManagerActivityLollipop.DrawerItem drawerItem;
    ChatController chatC;

    public ChatPanelListener(Context context){
        log("ChatPanelListener created");
        this.context = context;
        chatC = new ChatController(context);
    }

    @Override
    public void onClick(View v) {
        log("onClick ChatPanelListener");

        MegaChatListItem selectedChatItem = null;
        if(context instanceof ManagerActivityLollipop){
            selectedChatItem = ((ManagerActivityLollipop) context).getSelectedChat();
        }

        switch(v.getId()){

            case R.id.chat_list_info_chat_layout:{
                log("click contact info");
                ((ManagerActivityLollipop)context).hideChatPanel();
                if(selectedChatItem==null){
                    log("Selected chat NULL");
                }

                if(selectedChatItem.isGroup()){
                    Intent i = new Intent(context, GroupChatInfoActivityLollipop.class);
//                i.putExtra("userEmail", selectedChatItem.getContacts().get(0).getMail());
//                i.putExtra("userFullName", ((ManagerActivityLollipop) context).getFullNameChat());
                    i.putExtra("handle", selectedChatItem.getChatId());
                    context.startActivity(i);
                }
                else{
                    Intent i = new Intent(context, ContactChatInfoActivityLollipop.class);
//                i.putExtra("userEmail", selectedChatItem.getContacts().get(0).getMail());
//                i.putExtra("userFullName", ((ManagerActivityLollipop) context).getFullNameChat());
                    i.putExtra("handle", selectedChatItem.getChatId());
                    context.startActivity(i);
                }

                break;
            }
            case R.id.chat_list_leave_chat_layout:{
                log("click leave chat");
                ((ManagerActivityLollipop)context).hideChatPanel();
                if(selectedChatItem==null){
                    log("Selected chat NULL");
                }
                log("Leave chat with: "+selectedChatItem.getTitle());
                ((ManagerActivityLollipop)context).showConfirmationLeaveChat(selectedChatItem);
                break;
            }
            case R.id.chat_list_clear_history_chat_layout:{
                log("click clear history chat");
                ((ManagerActivityLollipop)context).hideChatPanel();
                if(selectedChatItem==null){
                    log("Selected chat NULL");
                }
                log("Clear chat with: "+selectedChatItem.getTitle());
                chatC.clearHistory(selectedChatItem.getChatId());
                break;
            }
            case R.id.chat_list_mute_chat_layout:{
                log("click mute chat");
                ((ManagerActivityLollipop)context).hideChatPanel();
                ChatController chatC = new ChatController(context);
                chatC.muteChat(selectedChatItem);
                ((ManagerActivityLollipop)context).setChats();
                break;
            }

            case R.id.chat_list_out_chat:{
                log("click out chat panel");
                ((ManagerActivityLollipop)context).hideChatPanel();
                break;
            }
        }
    }

    public static void log(String message) {
        Util.log("ChatPanelListener", message);
    }
}
