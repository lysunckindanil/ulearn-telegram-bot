package com.example.ulearn.generator;

import lombok.Getter;

import java.io.File;

import static com.example.ulearn.generator.Generator.src;


@Getter
public class CodeUnit {
    private final String name;
    private final File file;

    public CodeUnit(String name) {
        this.name = name;
        this.file = new File(src + File.separator + "CodeOriginalFiles" + File.separator + name + ".txt");
    }

    @Override
    public String toString() {
        return name;
    }
}