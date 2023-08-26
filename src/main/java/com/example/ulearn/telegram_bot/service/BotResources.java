package com.example.ulearn.telegram_bot.service;

import com.example.ulearn.generator.Block;
import com.example.ulearn.generator.CodeUnit;
import com.example.ulearn.generator.FormattedCodeUnit;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

@Component
@Slf4j
@PropertySource("application.properties")
public class BotResources {
    public List<Block> blocks = new ArrayList<>();

    @Value("${SERVER_URL}")
    public String SERVER_URL;
    @Value("${price.all_blocks}")
    public int PRICE_ALL_BLOCKS;
    @Value("${price.one_block}")
    public int PRICE_ONE_BLOCK;
    public String BUY_ALL_STRING = "BUY_ALL";
    public String BUY_ONE_STRING = "BUY_ONE";
    @Value("${admin.chatId}")
    public long ADMIN_CHATID;

    {
        for (int i = 2; i < 11; i++) {
            blocks.add(new Block(i));
        }
        blocks.get(0).addAllCodeUnits(List.of(new FormattedCodeUnit("Calculator"), new CodeUnit("check"), new CodeUnit("calculate"), new CodeUnit("getRevertString"))); //2
        blocks.get(1).addAllCodeUnits(List.of(new FormattedCodeUnit("Hospital"), new FormattedCodeUnit("TodoList"), new CodeUnit("getTwoDimensionalArray"))); //3
        blocks.get(2).addAllCodeUnits(List.of(new FormattedCodeUnit("School"), new FormattedCodeUnit("PhoneBook"), new CodeUnit("Line"), new CodeUnit("Client"))); //4
        blocks.get(3).addAllCodeUnits(List.of(new FormattedCodeUnit("AbstractLogger"), new CodeUnit("TimeUnit"), new CodeUnit("Animal"))); //5
        blocks.get(4).addAllCodeUnits(List.of(new FormattedCodeUnit("Customers"), new CodeUnit("Handler"))); //6
        blocks.get(5).addAllCodeUnits(List.of(new FormattedCodeUnit("Utils"), new CodeUnit("FileUtils"), new CodeUnit("ImageResizer"))); //7
        blocks.get(6).addAllCodeUnits(List.of(new FormattedCodeUnit("Parser"), new CodeUnit("Buffer"))); //8
        blocks.get(7).addAllCodeUnits(List.of(new FormattedCodeUnit("Employees"))); //9
        blocks.get(8).addAllCodeUnits(List.of(new FormattedCodeUnit("Airport"))); //10
    }

    InlineKeyboardMarkup getBuyMenu() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton("Купить все блоки");
        InlineKeyboardButton button2 = new InlineKeyboardButton("Купить один блок");
        button1.setCallbackData(BUY_ALL_STRING);
        button2.setCallbackData(BUY_ONE_STRING);
        keyboardRow.add(button1);
        keyboardRow.add(button2);
        keyboardRows.add(keyboardRow);
        inlineKeyboardMarkup.setKeyboard(keyboardRows);
        return inlineKeyboardMarkup;
    }

    InlineKeyboardMarkup getOneButtonKeyboardMarkup(String text, String url, String callBackData) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton(text);
        button.setCallbackData("Купить");
        if (url != null) button.setUrl(url);
        if (callBackData != null) button.setCallbackData(callBackData);
        keyboardRow.add(button);
        keyboardRows.add(keyboardRow);
        inlineKeyboardMarkup.setKeyboard(keyboardRows);
        return inlineKeyboardMarkup;
    }

    InlineKeyboardMarkup getBlockChoosingMenu() {
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

    String getOneBlockDescriptionPaymentText(Block block) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Вы купите практики " + block.inRussian().replace(" ", "го ") + "a:");
        for (CodeUnit codeUnit : block.getCodeUnits()) {
            joiner.add(codeUnit.getName());
        }
        joiner.add("Цена " + PRICE_ONE_BLOCK + " рублей");
        return joiner.toString();
    }

    String getAllBlocksDescriptionPaymentText() {
        return "Вам будут доступны все блоки!\nЦена " + PRICE_ALL_BLOCKS + " рублей";
    }

    String getChoosingTwoOptionsText() {
        return EmojiParser.parseToUnicode("Вы можете купить все блоки или только один. " + "Цена всех блоков - 1800, одного - 300 рублей :innocent:");
    }

    String getHelpText() {

        return "Оплата проводится через систему платежей ЮКасса. После оплаты вам будут доступны выбранные блоки. " + "Чтобы их получить, перейдите /show.";
    }


}