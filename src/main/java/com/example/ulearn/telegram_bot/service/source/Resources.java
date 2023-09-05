package com.example.ulearn.telegram_bot.service.source;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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



    public static InlineKeyboardMarkup getBuyMenu() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton("Купить все блоки");
        InlineKeyboardButton button2 = new InlineKeyboardButton("Купить один блок");
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

    public static String getHelpText() {

        return "Оплата проводится через систему платежей ЮКасса. После оплаты вам будут доступны выбранные блоки. " + "Чтобы их получить, перейдите /show.";
    }


}