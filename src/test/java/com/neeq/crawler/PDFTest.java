package com.neeq.crawler;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by bj on 16/7/19.
 */
public class PDFTest {
    @Test
    public void test() throws IOException {
        PDDocument pdDocument = PDDocument.load(new FileInputStream(new File("/Users/bj/upfiles/8285688975034A56D983EAF35D45A399.pdf")));
//        PDDocument pdDocument = PDDocument.load(new FileInputStream(new File("/Users/bj/project/neeq-crawler/src/test.pdf")));
        int numberOfPages = pdDocument.getNumberOfPages();
        System.out.println(numberOfPages);
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);

        String pdf = stripper.getText(pdDocument);
        System.out.println(pdf);
    }

    @Test
    public void iText() throws IOException {
//        PdfReader reader = new PdfReader("/Users/bj/project/neeq-crawler/src/test.pdf"); //读取pdf所使用的输出流
//        PdfReader reader = new PdfReader("/Users/bj/upfiles/8285688975034A56D983EAF35D45A399.pdf"); //读取pdf所使用的输出流
        PdfReader reader = new PdfReader("/Users/bj/upfiles/just.pdf"); //读取pdf所使用的输出流

        int num = reader.getNumberOfPages();//获得页数

        String content = "";  //存放读取出的文档内容

        for (int i = 1; i < num; i++) {

            content += PdfTextExtractor.getTextFromPage(reader, i); //读取第i页的文档内容

        }
        System.out.println(content);
    }
}
