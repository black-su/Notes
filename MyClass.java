package com.example.lib2;


import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFCreationHelper;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.swing.filechooser.FileSystemView;

public class MyClass {

    //ASCII、GB2312、GBK、GB18030、UTF8、ANSI、Latin1
    public static Charset charset = Charset.forName("GBK");
    public static void main(String args[]){

        try {
            System.out.println("系统编码:"+getSystemFileCharset());
//            readExcel();
            createExcel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getSystemFileCharset(){
        Properties pro = System.getProperties();
        return pro.getProperty("file.encoding");
    }

    public static void readExcel() throws IOException {
        FileSystemView fsv = FileSystemView.getFileSystemView();
        String desktop = fsv.getHomeDirectory().getPath();
        String filePath = desktop + "/template.xls";

        FileInputStream fileInputStream = new FileInputStream(filePath);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        POIFSFileSystem fileSystem = new POIFSFileSystem(bufferedInputStream);
        HSSFWorkbook workbook = new HSSFWorkbook(fileSystem);
        HSSFSheet sheet = workbook.getSheet("Sheet1");

        int lastRowIndex = sheet.getLastRowNum();
        System.out.println(lastRowIndex);
        for (int i = 0; i <= lastRowIndex; i++) {
            HSSFRow row = sheet.getRow(i);
            if (row == null) {
                break;
            }

            short lastCellNum = row.getLastCellNum();
            for (int j = 0; j < lastCellNum; j++) {
                String cellValue = row.getCell(j).getStringCellValue();
                System.out.println(cellValue);
            }
        }
    }

    public static void createExcel() throws IOException {
        // 获取桌面路径
        FileSystemView cfsv = FileSystemView.getFileSystemView();
        String cdesktop = cfsv.getHomeDirectory().getPath();
        String cfilePath = cdesktop + "/template.xlsx";

        File file = new File(cfilePath);

        OutputStream outputStream = new FileOutputStream(file);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream,charset);
        XSSFWorkbook cworkbook = new XSSFWorkbook();

        XSSFSheet csheet = cworkbook.createSheet("中文");
        XSSFRow row = csheet.createRow(0);
        XSSFCell hssfCell0 = row.createCell(0);
        hssfCell0.setCellValue("id");
        XSSFCell hssfCell1 = row.createCell(1);
        hssfCell1.setCellValue("订单号");
        XSSFCell hssfCell2 = row.createCell(2);
        hssfCell2.setCellValue("下单时间");
        XSSFCell hssfCell3 = row.createCell(3);
        hssfCell3.setCellValue("个数");
        XSSFCell hssfCell4 = row.createCell(4);
        hssfCell4.setCellValue("单价");
        XSSFCell hssfCell5 = row.createCell(5);
        hssfCell5.setCellValue(new String("订单金额".getBytes(charset),charset));

        row.setHeightInPoints(30); // 设置行的高度

        XSSFRow row1 = csheet.createRow(1);
        row1.createCell(0).setCellValue("1");
        row1.createCell(1).setCellValue("NO00001");

        // 日期格式化
        XSSFCellStyle cellStyle2 = cworkbook.createCellStyle();
        XSSFCreationHelper creationHelper = cworkbook.getCreationHelper();
        cellStyle2.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));
        csheet.setColumnWidth(2, 20 * 256); // 设置列的宽度

        XSSFCell cell2 = row1.createCell(2);
        cell2.setCellStyle(cellStyle2);
        cell2.setCellValue(new Date());

        row1.createCell(3).setCellValue(2);


        // 保留两位小数
        XSSFCellStyle cellStyle3 = cworkbook.createCellStyle();
        XSSFCell cell4 = row1.createCell(4);
        cell4.setCellStyle(cellStyle3);
        cell4.setCellValue(29.5);


        // 货币格式化
        XSSFCellStyle cellStyle4 = cworkbook.createCellStyle();
        XSSFFont font = cworkbook.createFont();
        font.setFontName("华文行楷");
        font.setFontHeightInPoints((short)15);
        cellStyle4.setFont(font);

        XSSFCell cell5 = row1.createCell(5);
        cell5.setCellFormula("D2*E2");  // 设置计算公式

        // 获取计算公式的值
        XSSFFormulaEvaluator e = new XSSFFormulaEvaluator(cworkbook);
        cell5 = e.evaluateInCell(cell5);
        System.out.println(cell5.getNumericCellValue());


        cworkbook.setActiveSheet(0);
        cworkbook.write(outputStream);
        outputStream.close();
    }
}