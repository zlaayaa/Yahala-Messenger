/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package com.yahala.objects;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.util.Linkify;

import com.yahala.SQLite.Messages;
import com.yahala.android.OSUtilities;
import com.yahala.messenger.R;
import com.yahala.messenger.FileLog;
import com.yahala.android.LocaleController;
import com.yahala.messenger.MessagesController;
import com.yahala.messenger.TLObject;
import com.yahala.messenger.TLRPC;
import com.yahala.messenger.UserConfig;
import com.yahala.android.emoji.EmojiManager;

import com.yahala.messenger.Utilities;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class MessageObject {
    public Messages messageOwner;
    public CharSequence messageText;
    public int type;
    public int contentType;
    public ArrayList<PhotoObject> photoThumbs;
    public Bitmap imagePreview;
    public PhotoObject previewPhoto;
    public String dateKey;
    public boolean deleted = false;
    public float audioProgress;
    public int audioProgressSec;

    private static TextPaint textPaint;
    public int lastLineWidth;
    public int textWidth;
    public int textHeight;
    public int blockHeight = Integer.MAX_VALUE;

    public static class TextLayoutBlock {
        public StaticLayout textLayout;
        public float textXOffset = 0;
        public float textYOffset = 0;
        public int charactersOffset = 0;
    }

    private static final int LINES_PER_BLOCK = 10;

    public ArrayList<TextLayoutBlock> textLayoutBlocks;

    public MessageObject(Messages message, AbstractMap<Integer, TLRPC.User> users) {
        if (textPaint == null) {
            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(0xff000000);
            textPaint.linkColor = 0xff316f9f;
        }

        textPaint.setTextSize(OSUtilities.dp(MessagesController.getInstance().fontSize));

        messageOwner = message;

        if (message.tl_message instanceof TLRPC.TL_messageService) {
            if (message.tl_message.action != null) {
                TLRPC.User fromUser = users.get(message.getId());
                if (fromUser == null) {
                    fromUser = MessagesController.getInstance().users.get(message.getId());
                }
                if (message.tl_message.action instanceof TLRPC.TL_messageActionChatCreate) {
                    if (isFromMe()) {
                        messageText = LocaleController.getString("ActionYouCreateGroup", R.string.ActionYouCreateGroup);
                    } else {
                        if (fromUser != null) {
                            messageText = LocaleController.getString("ActionCreateGroup", R.string.ActionCreateGroup).replace("un1", Utilities.formatName(fromUser.first_name, fromUser.last_name));
                        } else {
                            messageText = LocaleController.getString("ActionCreateGroup", R.string.ActionCreateGroup).replace("un1", "");
                        }
                    }
                } else if (message.tl_message.action instanceof TLRPC.TL_messageActionChatDeleteUser) {
                    if (message.tl_message.action.user_id == message.getJid()) {
                        if (isFromMe()) {
                            messageText = LocaleController.getString("ActionYouLeftUser", R.string.ActionYouLeftUser);
                        } else {
                            if (fromUser != null) {
                                messageText = LocaleController.getString("ActionLeftUser", R.string.ActionLeftUser).replace("un1", Utilities.formatName(fromUser.first_name, fromUser.last_name));
                            } else {
                                messageText = LocaleController.getString("ActionLeftUser", R.string.ActionLeftUser).replace("un1", "");
                            }
                        }
                    } else {
                        TLRPC.User who = users.get(message.tl_message.action.user_id);
                        if (who == null) {
                            MessagesController.getInstance().users.get(message.tl_message.action.user_id);
                        }
                        if (who != null && fromUser != null) {
                            if (isFromMe()) {
                                messageText = LocaleController.getString("ActionYouKickUser", R.string.ActionYouKickUser).replace("un2", Utilities.formatName(who.first_name, who.last_name));
                            } else if (message.tl_message.action.user_id == UserConfig.currentUser.phone) {
                                messageText = LocaleController.getString("ActionKickUserYou", R.string.ActionKickUserYou).replace("un1", Utilities.formatName(fromUser.first_name, fromUser.last_name));
                            } else {
                                messageText = LocaleController.getString("ActionKickUser", R.string.ActionKickUser).replace("un2", Utilities.formatName(who.first_name, who.last_name)).replace("un1", Utilities.formatName(fromUser.first_name, fromUser.last_name));
                            }
                        } else {
                            messageText = LocaleController.getString("ActionKickUser", R.string.ActionKickUser).replace("un2", "").replace("un1", "");
                        }
                    }
                } else if (message.tl_message.action instanceof TLRPC.TL_messageActionChatAddUser) {
                    TLRPC.User whoUser = users.get(message.tl_message.action.user_id);
                    if (whoUser == null) {
                        MessagesController.getInstance().users.get(message.tl_message.action.user_id);
                    }
                    if (whoUser != null && fromUser != null) {
                        if (isFromMe()) {
                            messageText = LocaleController.getString("ActionYouAddUser", R.string.ActionYouAddUser).replace("un2", Utilities.formatName(whoUser.first_name, whoUser.last_name));
                        } else if (message.tl_message.action.user_id == UserConfig.currentUser.phone) {
                            messageText = LocaleController.getString("ActionAddUserYou", R.string.ActionAddUserYou).replace("un1", Utilities.formatName(fromUser.first_name, fromUser.last_name));
                        } else {
                            messageText = LocaleController.getString("ActionAddUser", R.string.ActionAddUser).replace("un2", Utilities.formatName(whoUser.first_name, whoUser.last_name)).replace("un1", Utilities.formatName(fromUser.first_name, fromUser.last_name));
                        }
                    } else {
                        messageText = LocaleController.getString("ActionAddUser", R.string.ActionAddUser).replace("un2", "").replace("un1", "");
                    }
                } else if (message.tl_message.action instanceof TLRPC.TL_messageActionChatEditPhoto) {
                    photoThumbs = new ArrayList<PhotoObject>();
                    for (TLRPC.PhotoSize size : message.tl_message.action.photo.sizes) {
                        photoThumbs.add(new PhotoObject(size));
                    }
                    if (isFromMe()) {
                        messageText = LocaleController.getString("ActionYouChangedPhoto", R.string.ActionYouChangedPhoto);
                    } else {
                        if (fromUser != null) {
                            messageText = LocaleController.getString("ActionChangedPhoto", R.string.ActionChangedPhoto).replace("un1", Utilities.formatName(fromUser.first_name, fromUser.last_name));
                        } else {
                            messageText = LocaleController.getString("ActionChangedPhoto", R.string.ActionChangedPhoto).replace("un1", "");
                        }
                    }
                } else if (message.tl_message.action instanceof TLRPC.TL_messageActionChatEditTitle) {
                    if (isFromMe()) {
                        messageText = LocaleController.getString("ActionYouChangedTitle", R.string.ActionYouChangedTitle).replace("un2", message.tl_message.action.title);
                    } else {
                        if (fromUser != null) {
                            messageText = LocaleController.getString("ActionChangedTitle", R.string.ActionChangedTitle).replace("un1", Utilities.formatName(fromUser.first_name, fromUser.last_name)).replace("un2", message.tl_message.action.title);
                        } else {
                            messageText = LocaleController.getString("ActionChangedTitle", R.string.ActionChangedTitle).replace("un1", "").replace("un2", message.tl_message.action.title);
                        }
                    }
                } else if (message.tl_message.action instanceof TLRPC.TL_messageActionChatDeletePhoto) {
                    if (isFromMe()) {
                        messageText = LocaleController.getString("ActionYouRemovedPhoto", R.string.ActionYouRemovedPhoto);
                    } else {
                        if (fromUser != null) {
                            messageText = LocaleController.getString("ActionRemovedPhoto", R.string.ActionRemovedPhoto).replace("un1", Utilities.formatName(fromUser.first_name, fromUser.last_name));
                        } else {
                            messageText = LocaleController.getString("ActionRemovedPhoto", R.string.ActionRemovedPhoto).replace("un1", "");
                        }
                    }
                } else if (message.tl_message.action instanceof TLRPC.TL_messageActionTTLChange) {
                    if (message.tl_message.action.ttl != 0) {
                        String timeString;
                        if (message.tl_message.action.ttl == 2) {
                            timeString = LocaleController.getString("MessageLifetime2s", R.string.MessageLifetime2s);
                        } else if (message.tl_message.action.ttl == 5) {
                            timeString = LocaleController.getString("MessageLifetime5s", R.string.MessageLifetime5s);
                        } else if (message.tl_message.action.ttl == 60) {
                            timeString = LocaleController.getString("MessageLifetime1m", R.string.MessageLifetime1m);
                        } else if (message.tl_message.action.ttl == 60 * 60) {
                            timeString = LocaleController.getString("MessageLifetime1h", R.string.MessageLifetime1h);
                        } else if (message.tl_message.action.ttl == 60 * 60 * 24) {
                            timeString = LocaleController.getString("MessageLifetime1d", R.string.MessageLifetime1d);
                        } else if (message.tl_message.action.ttl == 60 * 60 * 24 * 7) {
                            timeString = LocaleController.getString("MessageLifetime1w", R.string.MessageLifetime1w);
                        } else {
                            timeString = String.format("%d", message.tl_message.action.ttl);
                        }
                        if (isFromMe()) {
                            messageText = LocaleController.formatString("MessageLifetimeChangedOutgoing", R.string.MessageLifetimeChangedOutgoing, timeString);
                        } else {
                            if (fromUser != null) {
                                messageText = LocaleController.formatString("MessageLifetimeChanged", R.string.MessageLifetimeChanged, fromUser.first_name, timeString);
                            } else {
                                messageText = LocaleController.formatString("MessageLifetimeChanged", R.string.MessageLifetimeChanged, "", timeString);
                            }
                        }
                    } else {
                        if (isFromMe()) {
                            messageText = LocaleController.getString("MessageLifetimeYouRemoved", R.string.MessageLifetimeYouRemoved);
                        } else {
                            if (fromUser != null) {
                                messageText = LocaleController.formatString("MessageLifetimeRemoved", R.string.MessageLifetimeRemoved, fromUser.first_name);
                            } else {
                                messageText = LocaleController.formatString("MessageLifetimeRemoved", R.string.MessageLifetimeRemoved, "");
                            }
                        }
                    }
                } else if (message.tl_message.action instanceof TLRPC.TL_messageActionLoginUnknownLocation) {
                    String date = String.format("%s %s %s", LocaleController.formatterYear.format(message.getDate()), LocaleController.getString("OtherAt", R.string.OtherAt), LocaleController.formatterDay.format(message.getDate()));
                    messageText = LocaleController.formatString("NotificationUnrecognizedDevice", R.string.NotificationUnrecognizedDevice, UserConfig.getCurrentUser().first_name, date, message.tl_message.action.title, message.tl_message.action.address);
                } else if (message.tl_message.action instanceof TLRPC.TL_messageActionUserJoined) {
                    if (fromUser != null) {
                        messageText = LocaleController.formatString("NotificationContactJoined", R.string.NotificationContactJoined, Utilities.formatName(fromUser.first_name, fromUser.last_name));
                    } else {
                        messageText = LocaleController.formatString("NotificationContactJoined", R.string.NotificationContactJoined, "");
                    }
                } else if (message.tl_message.action instanceof TLRPC.TL_messageActionUserUpdatedPhoto) {
                    if (fromUser != null) {
                        messageText = LocaleController.formatString("NotificationContactNewPhoto", R.string.NotificationContactNewPhoto, Utilities.formatName(fromUser.first_name, fromUser.last_name));
                    } else {
                        messageText = LocaleController.formatString("NotificationContactNewPhoto", R.string.NotificationContactNewPhoto, "");
                    }
                }
            }
        } else if (message.tl_message.media != null && !(message.tl_message.media instanceof TLRPC.TL_messageMediaEmpty)) {
            if (message.tl_message.media instanceof TLRPC.TL_messageMediaPhoto) {
                photoThumbs = new ArrayList<PhotoObject>();
                for (TLRPC.PhotoSize size : message.tl_message.media.photo.sizes) {
                    PhotoObject obj = new PhotoObject(size);
                    photoThumbs.add(obj);
                    if (imagePreview == null && obj.image != null) {
                        imagePreview = obj.image;
                    }
                }
                messageText = LocaleController.getString("AttachPhoto", R.string.AttachPhoto);
            } else if (message.tl_message.media instanceof TLRPC.TL_messageMediaVideo) {
                photoThumbs = new ArrayList<PhotoObject>();
                PhotoObject obj = new PhotoObject(message.tl_message.media.video.thumb);
                photoThumbs.add(obj);
                if (imagePreview == null && obj.image != null) {
                    imagePreview = obj.image;
                }
                messageText = LocaleController.getString("AttachVideo", R.string.AttachVideo);
            } else if (message.tl_message.media instanceof TLRPC.TL_messageMediaGeo) {
                messageText = LocaleController.getString("AttachLocation", R.string.AttachLocation);
            } else if (message.tl_message.media instanceof TLRPC.TL_messageMediaContact) {
                messageText = LocaleController.getString("AttachContact", R.string.AttachContact);
            } else if (message.tl_message.media instanceof TLRPC.TL_messageMediaUnsupported) {
                messageText = LocaleController.getString("UnsuppotedMedia", R.string.UnsuppotedMedia);
            } else if (message.tl_message.media instanceof TLRPC.TL_messageMediaDocument) {
                if (!(message.tl_message.media.document.thumb instanceof TLRPC.TL_photoSizeEmpty)) {
                    photoThumbs = new ArrayList<PhotoObject>();
                    PhotoObject obj = new PhotoObject(message.tl_message.media.document.thumb);
                    photoThumbs.add(obj);
                }
                messageText = LocaleController.getString("AttachDocument", R.string.AttachDocument);
            } else if (message.tl_message.media instanceof TLRPC.TL_messageMediaAudio) {
                messageText = LocaleController.getString("AttachAudio", R.string.AttachAudio);
            }
        } else {
            messageText = message.getMessage();
        }


        messageText = EmojiManager.getInstance().replaceEmoji(new SpannableString(messageText), textPaint.getFontMetricsInt(), OSUtilities.dp(20), true);

        //messageText =
      /*  try {

            messageText = EmojiManager.getInstance(ApplicationLoader.applicationContext).replaceEmoji(new SpannableString(messageText), textPaint.getFontMetricsInt(), OSUtilities.dp(20));
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        // Emoji.replaceEmoji(messageText, textPaint.getFontMetricsInt(), OSUtilities.dp(20));

        if (message.tl_message instanceof TLRPC.TL_message || (message.tl_message instanceof TLRPC.TL_messageForwarded && (message.tl_message.media == null || !(message.tl_message.media instanceof TLRPC.TL_messageMediaEmpty)))) {
            if (message.tl_message.media == null || message.tl_message.media instanceof TLRPC.TL_messageMediaEmpty) {
                contentType = type = 0;
            } else if (message.tl_message.media != null && message.tl_message.media instanceof TLRPC.TL_messageMediaPhoto) {
                contentType = type = 1;
            } else if (message.tl_message.media != null && message.tl_message.media instanceof TLRPC.TL_messageMediaGeo) {
                contentType = 1;
                type = 4;
            } else if (message.tl_message.media != null && message.tl_message.media instanceof TLRPC.TL_messageMediaVideo) {
                contentType = 1;
                type = 3;
            } else if (message.tl_message.media != null && message.tl_message.media instanceof TLRPC.TL_messageMediaContact) {
                if (isFromMe()) {
                    contentType = 4;
                    type = 12;
                } else {
                    contentType = 5;
                    type = 13;
                }
            } else if (message.tl_message.media != null && message.tl_message.media instanceof TLRPC.TL_messageMediaUnsupported) {
                contentType = type = 0;
            } else if (message.tl_message.media != null && message.tl_message.media instanceof TLRPC.TL_messageMediaDocument) {
                FileLog.e("message.tl_message.media", message.tl_message.media.document.mime_type + " " + isFromMe());
                if (message.tl_message.media.document.thumb != null && !(message.tl_message.media.document.thumb instanceof TLRPC.TL_photoSizeEmpty) && message.tl_message.media.document.mime_type != null && message.tl_message.media.document.mime_type.equals("image/gif")) {
                    contentType = 1;
                    type = 8;
                } else {
                    if (isFromMe()) {
                        contentType = type = 8;
                    } else {
                        contentType = type = 9;
                    }
                }
            } else if (message.tl_message.media != null && message.tl_message.media instanceof TLRPC.TL_messageMediaAudio) {
                contentType = type = 2;
            }
        } else if (message.tl_message instanceof TLRPC.TL_messageService) {
            if (message.tl_message.action instanceof TLRPC.TL_messageActionLoginUnknownLocation) {
                contentType = type = 0;
            } else if (message.tl_message.action instanceof TLRPC.TL_messageActionChatEditPhoto || message.tl_message.action instanceof TLRPC.TL_messageActionUserUpdatedPhoto) {
                contentType = type = 11;
            } else {
                contentType = type = 10;
            }
        } else if (message.tl_message instanceof TLRPC.TL_messageForwarded) {
            contentType = type = 0;
        }

        Calendar rightNow = new GregorianCalendar();
        rightNow.setTimeInMillis(messageOwner.getDate().getTime());
        int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
        int dateYear = rightNow.get(Calendar.YEAR);
        int dateMonth = rightNow.get(Calendar.MONTH);
        dateKey = String.format("%d_%02d_%02d", dateYear, dateMonth, dateDay);

        generateLayout();
    }

    public String getFileName() {
        if (messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaVideo) {
            return getAttachFileName(messageOwner.tl_message.media.video);
        } else if (messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaDocument) {
            return getAttachFileName(messageOwner.tl_message.media.document);
        } else if (messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaAudio) {
            return getAttachFileName(messageOwner.tl_message.media.audio);
        } else if (messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaPhoto) {
            ArrayList<TLRPC.PhotoSize> sizes = messageOwner.tl_message.media.photo.sizes;
            if (sizes.size() > 0) {
                TLRPC.PhotoSize sizeFull = PhotoObject.getClosestPhotoSizeWithSize(sizes, 800, 800);
                if (sizeFull != null) {
                    return getAttachFileName(sizeFull);
                }
            }
        }
        return "";
    }

    public static String getAttachFileName(TLObject attach) {
        if (attach instanceof TLRPC.Video) {
            TLRPC.Video video = (TLRPC.Video) attach;
            return video.dc_id + "_" + video.id + ".mp4";
        } else if (attach instanceof TLRPC.Document) {
            TLRPC.Document document = (TLRPC.Document) attach;
            String ext = document.file_name;
            int idx = -1;
            if (ext == null || (idx = ext.lastIndexOf(".")) == -1) {
                ext = "";
            } else {
                ext = ext.substring(idx);
            }
            if (ext.length() > 1) {
                return document.dc_id + "_" + document.id + ext;
            } else {
                return document.dc_id + "_" + document.id;
            }
        } else if (attach instanceof TLRPC.PhotoSize) {
            TLRPC.PhotoSize photo = (TLRPC.PhotoSize) attach;
            if (photo.location == null) {
                return "";
            }
            return photo.location.volume_id + "_" + photo.location.local_id + ".jpg";
        } else if (attach instanceof TLRPC.Audio) {
            TLRPC.Audio audio = (TLRPC.Audio) attach;
            return audio.dc_id + "_" + audio.id + ".m4a";
        }
        return "";
    }

    private void generateLayout() {
        if (type != 0 && type != 1 && type != 8 && type != 9 || messageOwner.getId() == null || messageText == null || messageText.length() == 0) {
            return;
        }

        textLayoutBlocks = new ArrayList<TextLayoutBlock>();

        if (messageText instanceof Spannable) {
            if (messageOwner.getMessage() != null && messageOwner.getMessage().contains(".") && (messageOwner.getMessage().contains(".com") || messageOwner.getMessage().contains("http") || messageOwner.getMessage().contains(".jo") || messageOwner.getMessage().contains(".org") || messageOwner.getMessage().contains(".net"))) {
                Linkify.addLinks((Spannable) messageText, Linkify.WEB_URLS);
            } else if (messageText.length() < 100) {
                Linkify.addLinks((Spannable) messageText, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS);
            }
        }

        int maxWidth;
        //if (messageOwner.to_id.chat_id != 0) {
        //     maxWidth = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) - AndroidUtilities.dp(122);
        // } else {
        maxWidth = Math.min(OSUtilities.displaySize.x, OSUtilities.displaySize.y) - OSUtilities.dp(80);
        // }

        StaticLayout textLayout = null;

        try {
            textLayout = new StaticLayout(messageText, textPaint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.05f, 0.0f, false);
        } catch (Exception e) {
            FileLog.e("tmessages", e);
            return;
        }

        textHeight = textLayout.getHeight();
        int linesCount = textLayout.getLineCount();

        int blocksCount = (int) Math.ceil((float) linesCount / LINES_PER_BLOCK);
        int linesOffset = 0;
        float prevOffset = 0;

        for (int a = 0; a < blocksCount; a++) {

            int currentBlockLinesCount = Math.min(LINES_PER_BLOCK, linesCount - linesOffset);
            TextLayoutBlock block = new TextLayoutBlock();

            if (blocksCount == 1) {
                block.textLayout = textLayout;
                block.textYOffset = 0;
                block.charactersOffset = 0;
                blockHeight = textHeight;
            } else {
                int startCharacter = textLayout.getLineStart(linesOffset);
                int endCharacter = textLayout.getLineEnd(linesOffset + currentBlockLinesCount - 1);
                if (endCharacter < startCharacter) {
                    continue;
                }
                block.charactersOffset = startCharacter;
                try {
                    CharSequence str = messageText.subSequence(startCharacter, endCharacter);
                    block.textLayout = new StaticLayout(str, textPaint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.05f, 0.0f, false);
                    block.textYOffset = textLayout.getLineTop(linesOffset);
                    if (a != blocksCount - 1) {
                        blockHeight = Math.min(blockHeight, block.textLayout.getHeight());
                        prevOffset = block.textYOffset;
                    } else {
                        blockHeight = Math.min(blockHeight, (int) (block.textYOffset - prevOffset));
                    }
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                    continue;
                }
            }

            textLayoutBlocks.add(block);

            float lastLeft = block.textXOffset = 0;
            try {
                lastLeft = block.textXOffset = block.textLayout.getLineLeft(currentBlockLinesCount - 1);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }

            float lastLine = 0;
            try {
                lastLine = block.textLayout.getLineWidth(currentBlockLinesCount - 1);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }

            int linesMaxWidth = (int) Math.ceil(lastLine);
            int lastLineWidthWithLeft;
            int linesMaxWidthWithLeft;
            boolean hasNonRTL = false;

            if (a == blocksCount - 1) {
                lastLineWidth = linesMaxWidth;
            }

            linesMaxWidthWithLeft = lastLineWidthWithLeft = (int) Math.ceil(lastLine + lastLeft);
            if (lastLeft == 0) {
                hasNonRTL = true;
            }

            if (currentBlockLinesCount > 1) {
                float textRealMaxWidth = 0, textRealMaxWidthWithLeft = 0, lineWidth, lineLeft;
                for (int n = 0; n < currentBlockLinesCount; ++n) {
                    try {
                        lineWidth = block.textLayout.getLineWidth(n);
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                        lineWidth = 0;
                    }

                    if (lineWidth > maxWidth + 100) {
                        int start = block.textLayout.getLineStart(n);
                        int end = block.textLayout.getLineEnd(n);
                        CharSequence text = block.textLayout.getText().subSequence(start, end);
                        continue;
                    }

                    try {
                        lineLeft = block.textLayout.getLineLeft(n);
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                        lineLeft = 0;
                    }

                    block.textXOffset = Math.min(block.textXOffset, lineLeft);

                    if (lineLeft == 0) {
                        hasNonRTL = true;
                    }
                    textRealMaxWidth = Math.max(textRealMaxWidth, lineWidth);
                    textRealMaxWidthWithLeft = Math.max(textRealMaxWidthWithLeft, lineWidth + lineLeft);
                    linesMaxWidth = Math.max(linesMaxWidth, (int) Math.ceil(lineWidth));
                    linesMaxWidthWithLeft = Math.max(linesMaxWidthWithLeft, (int) Math.ceil(lineWidth + lineLeft));
                }
                if (hasNonRTL) {
                    textRealMaxWidth = textRealMaxWidthWithLeft;
                    if (a == blocksCount - 1) {
                        lastLineWidth = lastLineWidthWithLeft;
                    }
                    linesMaxWidth = linesMaxWidthWithLeft;
                } else if (a == blocksCount - 1) {
                    lastLineWidth = linesMaxWidth;
                }
                textWidth = Math.max(textWidth, (int) Math.ceil(textRealMaxWidth));
            } else {
                textWidth = Math.max(textWidth, Math.min(maxWidth, linesMaxWidth));
            }

            if (hasNonRTL) {
                block.textXOffset = 0;
            }

            linesOffset += currentBlockLinesCount;
        }
    }

    public boolean isOut() {
        if (messageOwner.getOut() != null) {
            return messageOwner.getOut() == 1;
        } else {
            return false;
        }

    }

    public boolean isFromMe() {
        //FileLog.e("messageOwner.getJid() == UserConfig.currentUser.phone",StringUtils.parseName(messageOwner.getJid()) +"=="+ UserConfig.currentUser.phone);
        //return StringUtils.parseName(messageOwner.getJid())  == UserConfig.currentUser.phone;
        if (messageOwner.getOut() != null) {
            return messageOwner.getOut() == 1;
        } else {
            return false;
        }

    }


    public boolean isUnread() {
        return messageOwner.getRead_state() == 0;
    }

    public String getDialogId() {
        if (messageOwner.id != 0) {
            return messageOwner.getJid();
        } else {
          /*  if (messageOwner.to_id.chat_id != 0) {
                return -messageOwner.to_id.chat_id;
            } else if (isFromMe()) {
                return messageOwner.to_id.user_id;
            } else {
                return messageOwner.from_id;
            }*/
            return "0";
        }
    }

}
  

