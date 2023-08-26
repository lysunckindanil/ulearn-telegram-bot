package com.example.ulearn.generator;

import lombok.Getter;

import java.io.File;

@Getter
public class FormattedCodeUnit extends CodeUnit {

    public FormattedCodeUnit(String name) {
        super(name);
    }

    @Override
    public File getFile() {
//        return Generator.getFile(this.getName(), block);
        return null;
    }
}
