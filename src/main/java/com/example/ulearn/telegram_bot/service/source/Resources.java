package com.example.ulearn.telegram_bot.service.source;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class Resources {
    public static String BUY_ALL = "BUY_ALL";
    public static String BUY_ONE = "BUY_ONE";

    public static String SOURCE = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "CodeData";
    public static final String DEFAULT_QUESTIONS_PATH = SOURCE + File.separator + "UlearnTestQuestions";
    public static final String USERS_CODE_FILES = SOURCE + File.separator + "UsersCodeFiles";
    public static final String FORMATTED_FILES = SOURCE + File.separator + "CodeFormattedFiles";
    public static final String PATTERN_FILES = SOURCE + File.separator + "CodePatternFiles";

    public static InlineKeyboardMarkup getBuyMenu() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton("Все");
        InlineKeyboardButton button2 = new InlineKeyboardButton("Один");
        button1.setCallbackData(BUY_ALL);
        button2.setCallbackData(BUY_ONE);
        keyboardRow.add(button1);
        keyboardRow.add(button2);
        keyboardRows.add(keyboardRow);
        inlineKeyboardMarkup.setKeyboard(keyboardRows);
        return inlineKeyboardMarkup;
    }

    public static InlineKeyboardMarkup getOneButtonKeyboardMarkup(String text, String callBackData) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton(text);
        button.setCallbackData(callBackData);
        keyboardRow.add(button);
        keyboardRows.add(keyboardRow);
        inlineKeyboardMarkup.setKeyboard(keyboardRows);
        return inlineKeyboardMarkup;
    }

    public static InlineKeyboardMarkup getBlockChoosingMenu() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();

        List<InlineKeyboardButton> keyboardRow1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton("2 блок");
        InlineKeyboardButton button2 = new InlineKeyboardButton("3 блок");
        InlineKeyboardButton button3 = new InlineKeyboardButton("4 блок");
        List<InlineKeyboardButton> keyboardRow2 = new ArrayList<>();
        InlineKeyboardButton button4 = new InlineKeyboardButton("5 блок");
        InlineKeyboardButton button5 = new InlineKeyboardButton("6 блок");
        InlineKeyboardButton button6 = new InlineKeyboardButton("7 блок");
        List<InlineKeyboardButton> keyboardRow3 = new ArrayList<>();
        InlineKeyboardButton button7 = new InlineKeyboardButton("8 блок");
        InlineKeyboardButton button8 = new InlineKeyboardButton("9 блок");
        InlineKeyboardButton button9 = new InlineKeyboardButton("10 блок");

        button1.setCallbackData("block2");
        button2.setCallbackData("block3");
        button3.setCallbackData("block4");
        button4.setCallbackData("block5");
        button5.setCallbackData("block6");
        button6.setCallbackData("block7");
        button7.setCallbackData("block8");
        button8.setCallbackData("block9");
        button9.setCallbackData("block10");

        keyboardRow1.add(button1);
        keyboardRow1.add(button2);
        keyboardRow1.add(button3);
        keyboardRow2.add(button4);
        keyboardRow2.add(button5);
        keyboardRow2.add(button6);
        keyboardRow3.add(button7);
        keyboardRow3.add(button8);
        keyboardRow3.add(button9);

        keyboardRows.add(keyboardRow1);
        keyboardRows.add(keyboardRow2);
        keyboardRows.add(keyboardRow3);
        inlineKeyboardMarkup.setKeyboard(keyboardRows);
        return inlineKeyboardMarkup;
    }

    public static InlineKeyboardMarkup getOneButtonLinkKeyboardMarkup(String text, String url) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton(text);
        button.setUrl(url);
        keyboardRow.add(button);
        keyboardRows.add(keyboardRow);
        inlineKeyboardMarkup.setKeyboard(keyboardRows);
        return inlineKeyboardMarkup;
    }

    public static String getHelpText(Chat chat) {
        String name = chat.getFirstName() == null ? chat.getUserName() : chat.getFirstName();
        return EmojiParser.parseToUnicode("Привет, " + name + "!\n" + "Я продаю практики и ответы на контрольные вопросы с сайта Ulearn по Java :innocent:\n" + "Пару положений по функционалу:\n" + ":white_check_mark: Можно купить все блоки или один. Прошу внимательно читать, что покупаете, все написано в описании платежа! Контрольные вопросы первого блока вы получаете бесплатно.\n" + ":white_check_mark: Код практик у всех индивидуальный, поэтому риск получить плагиат минимален. Но я все же рекомендую изменить имена некоторых перемеренных, тем более в IDEA это делается за пару секунд (SHIFT + F6).\n" + ":white_check_mark: После покупки вы получите все практики блока в формате txt и скриншоты ответов на его контрольные вопросы. Все вы сможете просмотреть здесь /show.\n" + ":white_check_mark: Покупка осуществляется через систему платежей yookassa. Там доступны все популярные способы платы, поэтому проблем быть не должно.\n" + ":white_check_mark: В случае ошибки покупки или если код не работает, то пишите мне, ссылка в профиле бота.\n");
    }

}