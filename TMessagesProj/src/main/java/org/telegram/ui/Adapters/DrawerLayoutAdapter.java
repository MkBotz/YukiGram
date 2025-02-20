/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Adapters;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.DrawerLayoutContainer;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Cells.DrawerActionCell;
import org.telegram.ui.Cells.DrawerAddCell;
import org.telegram.ui.Cells.DrawerProfileCell;
import org.telegram.ui.Cells.DrawerUserCell;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SideMenultItemAnimator;

import java.util.ArrayList;
import java.util.Collections;

import me.onlyfire.yukigram.android.YukiConfig;
import me.onlyfire.yukigram.android.MenuOrderController;
import me.onlyfire.yukigram.android.PasscodeController;

public class DrawerLayoutAdapter extends RecyclerListView.SelectionAdapter {

    private Context mContext;
    private DrawerLayoutContainer mDrawerLayoutContainer;
    private ArrayList<Item> items = new ArrayList<>(11);
    private ArrayList<Integer> accountNumbers = new ArrayList<>();
    private boolean accountsShown;
    public DrawerProfileCell profileCell;
    private SideMenultItemAnimator itemAnimator;
    private boolean hasGps;

    public DrawerLayoutAdapter(Context context, SideMenultItemAnimator animator, DrawerLayoutContainer drawerLayoutContainer) {
        mContext = context;
        mDrawerLayoutContainer = drawerLayoutContainer;
        itemAnimator = animator;
        accountsShown = UserConfig.getActivatedAccountsCount() > 1 && MessagesController.getGlobalMainSettings().getBoolean("accountsShown", true);
        Theme.createCommonDialogResources(context);
        resetItems();
        try {
            hasGps = ApplicationLoader.applicationContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        } catch (Throwable e) {
            hasGps = false;
        }
    }

    private int getAccountRowsCount() {
        int count = accountNumbers.size() + 1;
        if (accountNumbers.size() < UserConfig.MAX_ACCOUNT_COUNT) {
            count++;
        }
        return count;
    }

    @Override
    public int getItemCount() {
        int count = items.size() + 2;
        if (accountsShown) {
            count += getAccountRowsCount();
        }
        return count;
    }

    public void setAccountsShown(boolean value, boolean animated) {
        if (accountsShown == value || itemAnimator.isRunning()) {
            return;
        }
        accountsShown = value;
        if (profileCell != null) {
            profileCell.setAccountsShown(accountsShown, animated);
        }
        MessagesController.getGlobalMainSettings().edit().putBoolean("accountsShown", accountsShown).commit();
        if (animated) {
            itemAnimator.setShouldClipChildren(false);
            if (accountsShown) {
                notifyItemRangeInserted(2, getAccountRowsCount());
            } else {
                notifyItemRangeRemoved(2, getAccountRowsCount());
            }
        } else {
            notifyDataSetChanged();
        }
    }

    public boolean isAccountsShown() {
        return accountsShown;
    }

    private View.OnClickListener onPremiumDrawableClick;
    public void setOnPremiumDrawableClick(View.OnClickListener listener) {
        onPremiumDrawableClick = listener;
    }

    @Override
    public void notifyDataSetChanged() {
        resetItems();
        super.notifyDataSetChanged();
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        int itemType = holder.getItemViewType();
        return itemType == 3 || itemType == 4 || itemType == 5 || itemType == 6;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case 0:
                view = profileCell = new DrawerProfileCell(mContext, mDrawerLayoutContainer) {
                    @Override
                    protected void onPremiumClick() {
                        if (onPremiumDrawableClick != null) {
                            onPremiumDrawableClick.onClick(this);
                        }
                    }
                };
                break;
            case 2:
                view = new DividerCell(mContext);
                break;
            case 3:
                view = new DrawerActionCell(mContext);
                break;
            case 4:
                view = new DrawerUserCell(mContext);
                break;
            case 5:
                view = new DrawerAddCell(mContext);
                break;
            case 1:
            default:
                view = new EmptyCell(mContext, AndroidUtilities.dp(8));
                break;
        }
        view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case 0: {
                DrawerProfileCell profileCell = (DrawerProfileCell) holder.itemView;
                profileCell.setUser(MessagesController.getInstance(UserConfig.selectedAccount).getUser(UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId()), accountsShown);
                break;
            }
            case 3: {
                DrawerActionCell drawerActionCell = (DrawerActionCell) holder.itemView;
                position -= 2;
                if (accountsShown) {
                    position -= getAccountRowsCount();
                }
                items.get(position).bind(drawerActionCell);
                drawerActionCell.setPadding(0, 0, 0, 0);
                break;
            }
            case 4: {
                DrawerUserCell drawerUserCell = (DrawerUserCell) holder.itemView;
                drawerUserCell.setAccount(accountNumbers.get(position - 2));
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int i) {
        if (i == 0) {
            return 0;
        } else if (i == 1) {
            return 1;
        }
        i -= 2;
        if (accountsShown) {
            if (i < accountNumbers.size()) {
                return 4;
            } else {
                if (accountNumbers.size() < UserConfig.MAX_ACCOUNT_COUNT) {
                    if (i == accountNumbers.size()){
                        return 5;
                    } else if (i == accountNumbers.size() + 1) {
                        return 2;
                    }
                } else {
                    if (i == accountNumbers.size()) {
                        return 2;
                    }
                }
            }
            i -= getAccountRowsCount();
        }
        if (i < 0 || i >= items.size() || items.get(i) == null) {
            return 2;
        }
        return 3;
    }

    public void swapElements(int fromIndex, int toIndex) {
        int idx1 = fromIndex - 2;
        int idx2 = toIndex - 2;
        if (idx1 < 0 || idx2 < 0 || idx1 >= accountNumbers.size() || idx2 >= accountNumbers.size()) {
            return;
        }
        final UserConfig userConfig1 = UserConfig.getInstance(accountNumbers.get(idx1));
        final UserConfig userConfig2 = UserConfig.getInstance(accountNumbers.get(idx2));
        final int tempLoginTime = userConfig1.loginTime;
        userConfig1.loginTime = userConfig2.loginTime;
        userConfig2.loginTime = tempLoginTime;
        userConfig1.saveConfig(false);
        userConfig2.saveConfig(false);
        Collections.swap(accountNumbers, idx1, idx2);
        notifyItemMoved(fromIndex, toIndex);
    }

    private void resetItems() {
        accountNumbers.clear();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (PasscodeController.isProtectedAccount(UserConfig.getInstance(a).getClientUserId())) continue;
            if (UserConfig.getInstance(a).isClientActivated()) {
                accountNumbers.add(a);
            }
        }
        Collections.sort(accountNumbers, (o1, o2) -> {
            long l1 = UserConfig.getInstance(o1).loginTime;
            long l2 = UserConfig.getInstance(o2).loginTime;
            if (l1 > l2) {
                return 1;
            } else if (l1 < l2) {
                return -1;
            }
            return 0;
        });

        items.clear();
        if (!UserConfig.getInstance(UserConfig.selectedAccount).isClientActivated()) {
            return;
        }
        int eventType = Theme.getEventType();
        if (YukiConfig.eventType > 0) {
            eventType = YukiConfig.eventType - 1;
        }
        int newGroupIcon;
        int newSecretIcon;
        int newChannelIcon;
        int contactsIcon;
        int callsIcon;
        int savedIcon;
        int settingsIcon;
        int inviteIcon;
        int helpIcon;
        int peopleNearbyIcon;
        if (eventType == 0) {
            newGroupIcon = R.drawable.msg_groups_ny;
            newSecretIcon = R.drawable.msg_secret_ny;
            newChannelIcon = R.drawable.msg_channel_ny;
            contactsIcon = R.drawable.msg_contacts_ny;
            callsIcon = R.drawable.msg_calls_ny;
            savedIcon = R.drawable.msg_saved_ny;
            settingsIcon = R.drawable.msg_settings_ny;
            inviteIcon = R.drawable.msg_invite_ny;
            helpIcon = R.drawable.msg_help_ny;
            peopleNearbyIcon = R.drawable.msg_nearby_ny;
        } else if (eventType == 1) {
            newGroupIcon = R.drawable.msg_groups_14;
            newSecretIcon = R.drawable.msg_secret_14;
            newChannelIcon = R.drawable.msg_channel_14;
            contactsIcon = R.drawable.msg_contacts_14;
            callsIcon = R.drawable.msg_calls_14;
            savedIcon = R.drawable.msg_saved_14;
            settingsIcon = R.drawable.msg_settings_14;
            inviteIcon = R.drawable.msg_secret_ny;
            helpIcon = R.drawable.msg_help_14;
            peopleNearbyIcon = R.drawable.msg_secret_14;
        } else if (eventType == 2) {
            newGroupIcon = R.drawable.msg_groups_hw;
            newSecretIcon = R.drawable.msg_secret_hw;
            newChannelIcon = R.drawable.msg_channel_hw;
            contactsIcon = R.drawable.msg_contacts_hw;
            callsIcon = R.drawable.msg_calls_hw;
            savedIcon = R.drawable.msg_saved_hw;
            settingsIcon = R.drawable.msg_settings_hw;
            inviteIcon = R.drawable.msg_invite_hw;
            helpIcon = R.drawable.msg_help_hw;
            peopleNearbyIcon = R.drawable.msg_nearby_hw;
        } else if (eventType == 3) {
            newGroupIcon = R.drawable.menu_groups_cn;
            newSecretIcon = R.drawable.menu_secret_cn;
            newChannelIcon = R.drawable.menu_broadcast_cn;
            contactsIcon = R.drawable.menu_contacts_cn;
            callsIcon = R.drawable.menu_calls_cn;
            savedIcon = R.drawable.menu_bookmarks_cn;
            settingsIcon = R.drawable.menu_settings_cn;
            inviteIcon = R.drawable.menu_invite_cn;
            helpIcon = R.drawable.msg_help_hw;
            peopleNearbyIcon = R.drawable.menu_nearby_cn;
        } else {
            newGroupIcon = R.drawable.msg_groups;
            newSecretIcon = R.drawable.msg_secret;
            newChannelIcon = R.drawable.msg_channel;
            contactsIcon = R.drawable.msg_contacts;
            callsIcon = R.drawable.msg_calls;
            savedIcon = R.drawable.msg_saved;
            settingsIcon = R.drawable.msg_settings_old;
            inviteIcon = R.drawable.msg_invite;
            helpIcon = R.drawable.msg_help;
            peopleNearbyIcon = R.drawable.msg_nearby;
        }
        UserConfig me = UserConfig.getInstance(UserConfig.selectedAccount);
        int item_size = MenuOrderController.sizeAvailable();
        for(int i = 0; i < item_size; i++) {
            MenuOrderController.EditableMenuItem data = MenuOrderController.getSingleAvailableMenuItem(i);
            if (data != null) {
                switch (data.id) {
                    case "new_group":
                        items.add(new Item(2, data.text, newGroupIcon));
                        break;
                    case "new_channel":
                        items.add(new Item(4, data.text, newChannelIcon));
                        break;
                    case "new_secret_chat":
                        items.add(new Item(3, data.text, newSecretIcon));
                        break;
                    case "contacts":
                        items.add(new Item(6, data.text, contactsIcon));
                        break;
                    case "calls":
                        items.add(new Item(10, data.text, callsIcon));
                        break;
                    case "nearby_people":
                        if (hasGps) {
                            items.add(new Item(12, data.text, peopleNearbyIcon));
                        }
                        break;
                    case "saved_message":
                        items.add(new Item(11, data.text, savedIcon));
                        break;
                    case "settings":
                        items.add(new Item(8, data.text, settingsIcon));
                        break;
                    case "yukigram_settings":
                        items.add(new Item(201, data.text, settingsIcon));
                        break;
                    case "invite_friends":
                        items.add(new Item(7, data.text, inviteIcon));
                        break;
                    case "telegram_features":
                        items.add(new Item(13, data.text, helpIcon));
                        break;
                    case "archived_messages":
                        items.add(new Item(202, data.text, R.drawable.msg_archive));
                        break;
                    case "datacenter_status":
                        items.add(new Item(203, data.text, R.drawable.datacenter_status));
                        break;
                    case "qr_login":
                        items.add(new Item(204, data.text, R.drawable.msg_qrcode));
                        break;
                    case "set_status":
                        if (me != null && me.isPremium()) {
                            if (me.getEmojiStatus() != null) {
                                items.add(new Item(15, LocaleController.getString("ChangeEmojiStatus", R.string.ChangeEmojiStatus), R.drawable.msg_status_edit));
                            } else {
                                items.add(new Item(15, LocaleController.getString("SetEmojiStatus", R.string.SetEmojiStatus), R.drawable.msg_status_set));
                            }
                        }
                        break;
                    case "connected_devices":
                        items.add(new Item(205, data.text, R.drawable.msg2_devices));
                        break;
                    case "power_usage":
                        items.add(new Item(206, data.text, R.drawable.msg2_battery));
                        break;
                    case "proxy_settings":
                        items.add(new Item(207, data.text, R.drawable.msg2_proxy));
                        break;
                    case "divider":
                        boolean foundPreviousDivider = false;
                        if (i > 0) {
                            MenuOrderController.EditableMenuItem previousData = MenuOrderController.getSingleAvailableMenuItem(i - 1);
                            if (previousData != null && previousData.id.equals("divider")) {
                                foundPreviousDivider = true;
                            }
                        }
                        if ((items.size() != 0 || i == 0) && !foundPreviousDivider) {
                            items.add(null);
                        }
                        break;
                }
            }
        }
    }

    public int getId(int position) {
        position -= 2;
        if (accountsShown) {
            position -= getAccountRowsCount();
        }
        if (position < 0 || position >= items.size()) {
            return -1;
        }
        Item item = items.get(position);
        return item != null ? item.id : -1;
    }

    public int getFirstAccountPosition() {
        if (!accountsShown) {
            return RecyclerView.NO_POSITION;
        }
        return 2;
    }

    public int getLastAccountPosition() {
        if (!accountsShown) {
            return RecyclerView.NO_POSITION;
        }
        return 1 + accountNumbers.size();
    }

    private static class Item {
        public int icon;
        public String text;
        public int id;

        public Item(int id, String text, int icon) {
            this.icon = icon;
            this.id = id;
            this.text = text;
        }

        public void bind(DrawerActionCell actionCell) {
            actionCell.setTextAndIcon(id, text, icon);
        }
    }
}