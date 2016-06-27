package main;

import unemployment.Converter;
import unemployment.DataUploader;

import java.io.IOException;

public class Main
{
    public static void main(String[] args) throws IOException
    {
        Configuration.init();
        System.out.println();
        Converter.convertCSV();
        System.out.println();
        DataUploader.uploadRdf();
    }
}
