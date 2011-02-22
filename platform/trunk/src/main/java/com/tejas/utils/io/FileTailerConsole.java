package com.tejas.utils.io;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.tejas.core.TejasContext;
import com.tejas.utils.console.ConsoleUtils;
import com.tejas.utils.console.TejasServletBase;
import com.tejas.utils.io.FileTailer.DatabaseMapper;
import com.tejas.utils.io.FileTailer.FileTailInfo;

public class FileTailerConsole extends TejasServletBase
{
    @Override
    public void execute(HttpServletRequest request, PrintWriter out) throws Throwable
    {
        ConsoleUtils.printRefreshTag(out, 3);

        TejasContext self = new TejasContext();

        DatabaseMapper mapper = self.dbl.getMybatisMapper(FileTailer.DatabaseMapper.class);

        List<FileTailInfo> data = mapper.selectAllData();

        List<String> columnNames = new ArrayList<String>();
        List<List<Object>> dataRows = new ArrayList<List<Object>>();

        columnNames.add("File Name");
        columnNames.add("Tailer Position");
        columnNames.add("File Size");
        columnNames.add("Tailer Status");
        columnNames.add("Tail Start");
        columnNames.add("Tail End");
        columnNames.add("Last Updated");

        for (FileTailInfo fileTailInfo : data)
        {
            ArrayList<Object> dataRow = new ArrayList<Object>();

            String file_name = fileTailInfo.getFile_name();
            File file = new File(file_name);
            dataRow.add(file_name);
            dataRow.add(fileTailInfo.getFile_position());
            dataRow.add((file.exists() ? file.length() + "" : "UNKNOWN"));
            dataRow.add(fileTailInfo.getTail_status());
            dataRow.add(fileTailInfo.getStart_time());
            dataRow.add(fileTailInfo.getEnd_time());
            dataRow.add(fileTailInfo.getLast_updated());

            dataRows.add(dataRow);
        }

        ConsoleUtils.printTable(out, "File Tailer Status", columnNames, dataRows, true);
    }
}
